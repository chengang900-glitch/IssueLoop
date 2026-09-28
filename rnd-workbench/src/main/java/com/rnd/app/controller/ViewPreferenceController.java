package com.rnd.app.controller;
import com.rnd.app.dto.ViewPreferenceDto;
import com.rnd.app.service.*;
import com.rnd.app.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class ViewPreferenceController {
    private final ViewPreferenceService service; private final ProjectService projectService;
    @GetMapping("/projects/{projectId}/view-preference")
    public ApiResponse get(@PathVariable Long projectId) { Long u=SecurityUtil.currentUserId(); projectService.ensureProjectMember(projectId,u); return ApiResponse.ok(service.get(projectId,u)); }
    @PutMapping("/projects/{projectId}/view-preference")
    public ApiResponse save(@PathVariable Long projectId, @RequestBody ViewPreferenceDto dto) { Long u=SecurityUtil.currentUserId(); projectService.ensureProjectMember(projectId,u); return ApiResponse.ok(service.save(projectId,u,dto)); }
}
