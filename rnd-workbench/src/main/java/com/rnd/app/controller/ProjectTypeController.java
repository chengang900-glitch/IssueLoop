package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.dto.ProjectTypeDto;
import com.rnd.app.service.ProjectTypeService;
import com.rnd.app.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/project-types")
@RequiredArgsConstructor
public class ProjectTypeController {
    private final ProjectTypeService service;

    @GetMapping
    public ApiResponse list(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        if (includeDisabled) requireAdmin();
        return ApiResponse.ok(service.list(includeDisabled));
    }

    @PostMapping
    public ApiResponse create(@RequestBody ProjectTypeDto request) {
        requireAdmin();
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody ProjectTypeDto request) {
        requireAdmin();
        return ApiResponse.ok(service.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse status(@PathVariable Long id, @RequestParam boolean enabled) {
        requireAdmin();
        return ApiResponse.ok(service.setEnabled(id, enabled));
    }

    private void requireAdmin() {
        RndPrincipal principal = SecurityUtil.currentUser();
        if (principal == null || !"ADMIN".equals(principal.getSystemRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要系统管理员权限");
    }
}
