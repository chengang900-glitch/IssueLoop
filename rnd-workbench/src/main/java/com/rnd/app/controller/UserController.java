package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.PageRequests;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final List<String> SYSTEM_ROLES = List.of("ADMIN", "USER");
    private static final List<String> JOB_ROLES = List.of("项目经理", "实施顾问", "开发顾问", "测试顾问");

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/auth/me")
    public ApiResponse me() {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal == null) return ApiResponse.fail(ErrorCode.INVALID_TOKEN);
        User user = userRepo.findById(principal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ApiResponse.ok(userData(user));
    }

    @PutMapping("/users/me")
    public ApiResponse updateMe(@Valid @RequestBody UpdateMeRequest req) {
        User user = userRepo.findById(SecurityUtil.currentUserId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (user.isMustChangePassword() && !hasText(req.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先修改初始密码");
        }
        if (hasText(req.getNewPassword())) {
            if (!hasText(req.getOldPassword()) || !passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码错误");
            }
            if (req.getNewPassword().length() < 8 || passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码至少 8 位，且不能与原密码相同");
            }
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
            user.setMustChangePassword(false);
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setFailCount(0);
            user.setLockUntil(null);
        }
        userRepo.save(user);
        return ApiResponse.ok(userData(user));
    }

    @GetMapping("/users")
    public ApiResponse listUsers(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        ensureAdmin();
        int safePage = PageRequests.page(page);
        int safeSize = PageRequests.size(size, 100);
        Page<User> result = userRepo.findAll(PageRequest.of(safePage - 1, safeSize, Sort.by("id")));
        return ApiResponse.page(result.getContent().stream().map(this::userData).collect(Collectors.toList()), result.getTotalElements(), safePage, safeSize);
    }

    @PostMapping("/users")
    public ApiResponse createUser(@Valid @RequestBody CreateUserRequest req) {
        ensureAdmin();
        if (userRepo.existsByUsername(req.getUsername())) throw new BusinessException(ErrorCode.DUPLICATE, "用户名已存在");
        validateSystemRole(req.getSystemRole() == null ? "USER" : req.getSystemRole());
        validateJobRole(req.getJobRole());
        String initialPassword = randomPassword();
        User user = userRepo.save(User.builder().username(req.getUsername()).passwordHash(passwordEncoder.encode(initialPassword))
                .nickname(req.getNickname()).department(req.getDepartment()).jobRole(req.getJobRole())
                .systemRole(req.getSystemRole() == null ? "USER" : req.getSystemRole()).mustChangePassword(true).build());
        Map<String, Object> data = userData(user);
        data.put("initialPassword", initialPassword);
        return ApiResponse.ok(data);
    }

    @PutMapping("/users/{id}")
    @Transactional
    public ApiResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        ensureAdmin();
        userRepo.lockAdministrators();
        User user = userRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getJobRole() != null) { validateJobRole(req.getJobRole()); user.setJobRole(req.getJobRole()); }
        if (req.getSystemRole() != null) {
            validateSystemRole(req.getSystemRole());
            if (user.getStatus() == 1 && "ADMIN".equals(user.getSystemRole()) && !"ADMIN".equals(req.getSystemRole())
                    && userRepo.countBySystemRoleAndStatus("ADMIN", 1) <= 1) {
                throw new BusinessException(ErrorCode.STATUS_CONFLICT, "系统至少保留一名启用的系统管理员");
            }
            user.setSystemRole(req.getSystemRole());
        }
        userRepo.save(user);
        return ApiResponse.ok(userData(user));
    }

    @PutMapping("/users/{id}/status")
    @Transactional
    public ApiResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        ensureAdmin();
        userRepo.lockAdministrators();
        if (req.getStatus() == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态只能为启用或停用");
        }
        User user = userRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (Integer.valueOf(0).equals(req.getStatus()) && "ADMIN".equals(user.getSystemRole())
                && user.getStatus() == 1 && userRepo.countBySystemRoleAndStatus("ADMIN", 1) <= 1) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT, "系统至少保留一名启用的系统管理员");
        }
        user.setStatus(req.getStatus());
        userRepo.save(user);
        return ApiResponse.ok(userData(user));
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse resetPassword(@PathVariable Long id) {
        ensureAdmin();
        User user = userRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String initialPassword = randomPassword();
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setMustChangePassword(true); user.setFailCount(0); user.setLockUntil(null);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepo.save(user);
        return ApiResponse.ok(Map.of("initialPassword", initialPassword));
    }

    private Map<String, Object> userData(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId()); data.put("username", user.getUsername()); data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar()); data.put("department", user.getDepartment()); data.put("jobRole", user.getJobRole());
        data.put("systemRole", user.getSystemRole()); data.put("status", user.getStatus()); data.put("mustChangePassword", user.isMustChangePassword());
        return data;
    }

    private void ensureAdmin() { RndPrincipal p = SecurityUtil.currentUser(); if (p == null || !"ADMIN".equals(p.getSystemRole())) throw new BusinessException(ErrorCode.FORBIDDEN, "需要系统管理员权限"); }
    private void validateSystemRole(String role) { if (!SYSTEM_ROLES.contains(role)) throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的系统角色"); }
    private void validateJobRole(String role) { if (role != null && !JOB_ROLES.contains(role)) throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的岗位角色"); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String randomPassword() {
        StringBuilder value = new StringBuilder(16);
        for (int i = 0; i < 16; i++) value.append(PASSWORD_CHARS.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_CHARS.length())));
        return value.toString();
    }

    public static class CreateUserRequest {
        @NotBlank @Email private String username;
        @NotBlank private String nickname;
        private String department; private String jobRole; private String systemRole;
        public String getUsername() { return username; } public void setUsername(String v) { username = v; }
        public String getNickname() { return nickname; } public void setNickname(String v) { nickname = v; }
        public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
        public String getJobRole() { return jobRole; } public void setJobRole(String v) { jobRole = v; }
        public String getSystemRole() { return systemRole; } public void setSystemRole(String v) { systemRole = v; }
    }
    public static class UpdateUserRequest {
        private String nickname; private String department; private String jobRole; private String systemRole;
        public String getNickname() { return nickname; } public void setNickname(String v) { nickname = v; }
        public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
        public String getJobRole() { return jobRole; } public void setJobRole(String v) { jobRole = v; }
        public String getSystemRole() { return systemRole; } public void setSystemRole(String v) { systemRole = v; }
    }
    public static class StatusRequest { private Integer status; public Integer getStatus() { return status; } public void setStatus(Integer v) { status = v; } }
    public static class UpdateMeRequest {
        private String nickname; private String avatar; private String department; private String oldPassword; private String newPassword;
        public String getNickname() { return nickname; } public void setNickname(String v) { nickname = v; }
        public String getAvatar() { return avatar; } public void setAvatar(String v) { avatar = v; }
        public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
        public String getOldPassword() { return oldPassword; } public void setOldPassword(String v) { oldPassword = v; }
        public String getNewPassword() { return newPassword; } public void setNewPassword(String v) { newPassword = v; }
    }
}
