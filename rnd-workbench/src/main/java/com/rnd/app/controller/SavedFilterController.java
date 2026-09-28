package com.rnd.app.controller;

import com.rnd.app.dto.SavedFilterRequest;
import com.rnd.app.service.ProjectService;
import com.rnd.app.service.SavedFilterService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SavedFilterController {
    private final SavedFilterService service;
    private final ProjectService projectService;

    @GetMapping("/projects/{projectId}/saved-filters")
    public ApiResponse list(@PathVariable Long projectId) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(projectId, userId);
        return ApiResponse.ok(service.list(projectId, userId));
    }

    @PostMapping("/projects/{projectId}/saved-filters")
    public ApiResponse create(@PathVariable Long projectId, @Valid @RequestBody SavedFilterRequest request) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(projectId, userId);
        return ApiResponse.ok(service.create(projectId, userId, request));
    }

    @PutMapping("/saved-filters/{id}")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody SavedFilterRequest request) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(service.projectId(id, userId), userId);
        return ApiResponse.ok(service.update(id, userId, request));
    }

    @PutMapping("/saved-filters/{id}/default")
    public ApiResponse makeDefault(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(service.projectId(id, userId), userId);
        return ApiResponse.ok(service.makeDefault(id, userId));
    }

    @PostMapping("/saved-filters/{id}/copy")
    public ApiResponse copy(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(service.projectId(id, userId), userId);
        return ApiResponse.ok(service.copy(id, userId));
    }

    @DeleteMapping("/saved-filters/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(service.projectId(id, userId), userId);
        service.delete(id, userId);
        return ApiResponse.ok();
    }
}
