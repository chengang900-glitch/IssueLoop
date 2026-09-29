package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.dto.ProjectSettingsDto;
import com.rnd.app.dto.ThirdPartyLoginSettingsDto;
import com.rnd.app.service.SystemSettingService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {
    private final SystemSettingService settingService;

    @GetMapping("/project-code")
    public ApiResponse getProjectCode() {
        requireAdmin();
        ProjectSettingsDto dto = new ProjectSettingsDto();
        dto.setProjectCodePrefix(settingService.getProjectCodePrefix());
        return ApiResponse.ok(dto);
    }

    @PutMapping("/project-code")
    public ApiResponse updateProjectCode(@RequestBody ProjectSettingsDto request) {
        requireAdmin();
        settingService.updateProjectCodePrefix(request.getProjectCodePrefix());
        return ApiResponse.ok();
    }

    @GetMapping("/third-party-login")
    public ApiResponse getThirdPartyLogin() {
        requireAdmin();
        return ApiResponse.ok(settingService.getThirdPartyLoginSettings());
    }

    @PutMapping("/third-party-login")
    public ApiResponse updateThirdPartyLogin(@RequestBody ThirdPartyLoginSettingsDto request) {
        requireAdmin();
        settingService.updateThirdPartyLoginSettings(request);
        return ApiResponse.ok(settingService.getThirdPartyLoginSettings());
    }

    private void requireAdmin() {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal == null || !"ADMIN".equals(principal.getSystemRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要系统管理员权限");
    }
}
