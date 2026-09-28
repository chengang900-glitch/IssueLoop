package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.dto.*;
import com.rnd.app.entity.*;
import com.rnd.app.service.ProjectService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ==================== 项目 ====================

    @GetMapping("/projects")
    public ApiResponse listProjects() {
        Long uid = SecurityUtil.currentUserId();
        return ApiResponse.ok(projectService.listVisibleProjects(uid).stream()
                .map(project -> projectService.toDto(project, uid)).collect(Collectors.toList()));
    }

    @GetMapping("/projects/manage")
    public ApiResponse listAllProjects() {
        com.rnd.app.config.RndPrincipal p = SecurityUtil.currentUser();
        if (p == null || !"ADMIN".equals(p.getSystemRole())) {
            return ApiResponse.fail(com.rnd.app.util.ErrorCode.FORBIDDEN, "需要系统管理员权限");
        }
        return ApiResponse.ok(projectService.listAllProjects().stream()
                .map(projectService::toDto).collect(Collectors.toList()));
    }

    @PostMapping("/projects")
    public ApiResponse create(@Valid @RequestBody CreateProjectRequest req) {
        com.rnd.app.config.RndPrincipal p = com.rnd.app.util.SecurityUtil.currentUser();
        if (p == null || !"ADMIN".equals(p.getSystemRole())) {
            return ApiResponse.fail(com.rnd.app.util.ErrorCode.FORBIDDEN, "需要系统管理员权限");
        }
        Project proj = projectService.createProject(req, SecurityUtil.currentUserId());
        return ApiResponse.ok(projectService.toDto(proj));
    }

    @GetMapping("/projects/{id}")
    public ApiResponse get(@PathVariable Long id) {
        ensureProjectReadAuthority(id);
        return ApiResponse.ok(projectService.toDto(projectService.listAllProjects().stream()
                .filter(p -> p.getId().equals(id)).findFirst()
                .orElseThrow(() -> new com.rnd.app.util.BusinessException(com.rnd.app.util.ErrorCode.NOT_FOUND)), SecurityUtil.currentUserId()));
    }

    @PutMapping("/projects/{id}")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody CreateProjectRequest req) {
        ensureProjectUpdateAuthority(id);
        projectService.updateProject(id, req);
        return ApiResponse.ok();
    }

    @PutMapping("/projects/{id}/archive")
    public ApiResponse archive(@PathVariable Long id, @RequestParam boolean archived) {
        com.rnd.app.config.RndPrincipal p = SecurityUtil.currentUser();
        if (p == null || !"ADMIN".equals(p.getSystemRole())) return ApiResponse.fail(com.rnd.app.util.ErrorCode.FORBIDDEN, "需要系统管理员权限");
        projectService.setArchived(id, archived);
        return ApiResponse.ok();
    }

    @GetMapping("/projects/{id}/summary")
    public ApiResponse summary(@PathVariable Long id) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(projectService.summary(id));
    }

    // ==================== 里程碑 ====================

    @GetMapping("/projects/{id}/milestones")
    public ApiResponse listMilestones(@PathVariable Long id) {
        ensureProjectReadAuthority(id);
        return ApiResponse.ok(projectService.listMilestones(id));
    }

    @PostMapping("/projects/{id}/milestones")
    public ApiResponse createMilestone(@PathVariable Long id, @RequestBody ProjectMilestoneDto req) {
        ensureProjectUpdateAuthority(id);
        return ApiResponse.ok(projectService.createMilestone(id, req));
    }

    @PutMapping("/milestones/{milestoneId}")
    public ApiResponse updateMilestone(@PathVariable Long milestoneId, @RequestBody ProjectMilestoneDto req) {
        ensureProjectUpdateAuthority(projectService.milestoneProjectId(milestoneId));
        return ApiResponse.ok(projectService.updateMilestone(milestoneId, req));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ApiResponse deleteMilestone(@PathVariable Long milestoneId) {
        ensureProjectUpdateAuthority(projectService.milestoneProjectId(milestoneId));
        projectService.deleteMilestone(milestoneId);
        return ApiResponse.ok();
    }

    // ==================== 成员 ====================

    @GetMapping("/projects/{id}/members")
    public ApiResponse listMembers(@PathVariable Long id) {
        com.rnd.app.config.RndPrincipal p = SecurityUtil.currentUser();
        if (p == null || (!"ADMIN".equals(p.getSystemRole()))) projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(projectService.listMembers(id));
    }

    @PostMapping("/projects/{id}/members")
    public ApiResponse addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest req) {
        ensureMemberManagementAuthority(id);
        projectService.addMember(id, req.getUserId(), req.getRole());
        return ApiResponse.ok();
    }

    @PutMapping("/projects/{id}/members/{userId}")
    public ApiResponse updateMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                         @RequestBody AddMemberRequest req) {
        ensureMemberManagementAuthority(id);
        projectService.updateMemberRole(id, userId, req.getRole());
        return ApiResponse.ok();
    }

    @DeleteMapping("/projects/{id}/members/{userId}")
    public ApiResponse removeMember(@PathVariable Long id, @PathVariable Long userId) {
        ensureMemberManagementAuthority(id);
        projectService.removeMember(id, userId);
        return ApiResponse.ok();
    }

    // ==================== 模块 ====================

    @GetMapping("/projects/{id}/modules")
    public ApiResponse listModules(@PathVariable Long id) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(projectService.listModules(id));
    }

    @PostMapping("/projects/{id}/modules")
    public ApiResponse createModule(@PathVariable Long id, @Valid @RequestBody CreateModuleRequest req) {
        projectService.ensureProjectAdmin(id, SecurityUtil.currentUserId());
        ModuleEntity m = projectService.createModule(id, req.getName(), req.getParentId());
        return ApiResponse.ok(m);
    }

    @DeleteMapping("/modules/{moduleId}")
    public ApiResponse deleteModule(@PathVariable Long moduleId) {
        projectService.ensureProjectAdmin(projectService.moduleProjectId(moduleId), SecurityUtil.currentUserId());
        projectService.deleteModule(moduleId);
        return ApiResponse.ok();
    }

    // ==================== 迭代 ====================

    @GetMapping("/projects/{id}/sprints")
    public ApiResponse listSprints(@PathVariable Long id) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(projectService.listSprints(id));
    }

    @PostMapping("/projects/{id}/sprints")
    public ApiResponse createSprint(@PathVariable Long id, @Valid @RequestBody CreateSprintRequest req) {
        projectService.ensureProjectAdmin(id, SecurityUtil.currentUserId());
        Sprint s = projectService.createSprint(id, req);
        return ApiResponse.ok(s);
    }

    @PutMapping("/sprints/{sprintId}")
    public ApiResponse updateSprint(@PathVariable Long sprintId, @Valid @RequestBody UpdateSprintRequest req) {
        projectService.ensureProjectAdmin(projectService.sprintProjectId(sprintId), SecurityUtil.currentUserId());
        Sprint s = projectService.updateSprint(sprintId, req);
        return ApiResponse.ok(s);
    }

    @DeleteMapping("/sprints/{sprintId}")
    public ApiResponse deleteSprint(@PathVariable Long sprintId) {
        projectService.ensureProjectAdmin(projectService.sprintProjectId(sprintId), SecurityUtil.currentUserId());
        projectService.deleteSprint(sprintId);
        return ApiResponse.ok();
    }

    private void ensureMemberManagementAuthority(Long projectId) {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal != null && "ADMIN".equals(principal.getSystemRole())) {
            projectService.ensureActiveProject(projectId);
            return;
        }
        projectService.ensureProjectAdmin(projectId, SecurityUtil.currentUserId());
    }

    private void ensureProjectUpdateAuthority(Long projectId) {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal != null && "ADMIN".equals(principal.getSystemRole())) {
            projectService.ensureActiveProject(projectId);
            return;
        }
        projectService.ensureProjectAdmin(projectId, SecurityUtil.currentUserId());
    }

    private void ensureProjectReadAuthority(Long projectId) {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal != null && "ADMIN".equals(principal.getSystemRole())) {
            return;
        }
        projectService.ensureProjectMember(projectId, SecurityUtil.currentUserId());
    }
}
