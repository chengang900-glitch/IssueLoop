package com.rnd.app.controller;

import com.rnd.app.dto.*;
import com.rnd.app.service.ProjectService;
import com.rnd.app.service.BulkWorkItemService;
import com.rnd.app.service.WorkItemService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.PageRequests;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;
    private final ProjectService projectService;
    private final BulkWorkItemService bulkWorkItemService;

    // ==================== 工作项列表 ====================

    @GetMapping("/projects/{id}/work-items")
    public ApiResponse list(@PathVariable Long id,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Long ownerId,
                            @RequestParam(required = false) Long creatorId,
                            @RequestParam(required = false) Long sprintId,
                            @RequestParam(required = false) String priority,
                            @RequestParam(required = false) String severity,
                            @RequestParam(required = false) String module,
                            @RequestParam(required = false) String tag,
                            @RequestParam(required = false) java.time.Instant dueFrom,
                            @RequestParam(required = false) java.time.Instant dueTo,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String view,
                            @RequestParam(defaultValue = "createdAt,desc") String sort,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        int safePage = PageRequests.page(page);
        int safeSize = PageRequests.size(size);
        var pageable = PageRequest.of(safePage - 1, safeSize, parseSort(sort));
        var result = workItemService.search(id, type, status, ownerId, creatorId, sprintId,
                priority, severity, module, tag, dueFrom, dueTo, keyword, view,
                SecurityUtil.currentUserId(), pageable);
        return ApiResponse.page(result.getContent(), result.getTotalElements(), safePage, safeSize);
    }

    private Sort parseSort(String value) {
        String[] parts = value.split(",", 2);
        java.util.Set<String> allowed = java.util.Set.of("createdAt", "updatedAt", "dueDate", "priority", "title");
        if (!allowed.contains(parts[0])) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序字段");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }

    @PostMapping("/projects/{id}/work-items")
    public ApiResponse create(@PathVariable Long id, @Valid @RequestBody CreateWorkItemRequest req) {
        projectService.ensureProjectWriter(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.create(id, req, SecurityUtil.currentUserId()));
    }

    @PostMapping("/projects/{id}/work-items/bulk-update")
    public ApiResponse bulkUpdate(@PathVariable Long id, @RequestBody BulkUpdateRequest request) {
        Long userId=SecurityUtil.currentUserId(); projectService.ensureProjectWriter(id,userId);
        return ApiResponse.ok(bulkWorkItemService.update(id,request,userId));
    }
    @PostMapping("/projects/{id}/work-items/bulk-delete")
    public ApiResponse bulkDelete(@PathVariable Long id,@RequestBody BulkDeleteRequest request){ projectService.ensureProjectAdmin(id,SecurityUtil.currentUserId()); return ApiResponse.ok(bulkWorkItemService.delete(id,request)); }

    @GetMapping("/work-items/{id}")
    public ApiResponse detail(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(dto.getProjectId(), userId);
        dto.setWatched(workItemService.isWatched(id, userId));
        return ApiResponse.ok(dto);
    }

    @PostMapping("/work-items/{id}/watchers/me")
    public ApiResponse watch(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(dto.getProjectId(), userId);
        workItemService.watch(id, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/work-items/{id}/watchers/me")
    public ApiResponse unwatch(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(dto.getProjectId(), userId);
        workItemService.unwatch(id, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/work-items/{id}/watchers")
    public ApiResponse watchers(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        projectService.ensureProjectMember(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.watchers(id));
    }

    @GetMapping("/projects/{id}/watched-work-items")
    public ApiResponse watchedItems(@PathVariable Long id) {
        Long userId = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(id, userId);
        return ApiResponse.ok(workItemService.watchedItems(id, userId));
    }

    @PutMapping("/work-items/{id}")
    public ApiResponse update(@PathVariable String id, @Valid @RequestBody UpdateWorkItemRequest req) {
        WorkItemDto dto = workItemService.detail(id);
        projectService.ensureProjectWriter(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.update(id, req, SecurityUtil.currentUserId()));
    }

    // ==================== 状态流转 ====================

    @PatchMapping("/work-items/{id}/status")
    public ApiResponse transition(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest req) {
        WorkItemDto dto = workItemService.detail(id);
        projectService.ensureProjectWriter(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.transitionStatus(id, req.getStatus(), SecurityUtil.currentUserId(), req.getReason(), req.getDueDate(), req.getDelayApproved(), req.getActualCompletedAt(), req.getActualHours()));
    }

    @PostMapping("/work-items/{id}/assign-me")
    public ApiResponse assignMe(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        projectService.ensureProjectWriter(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.assignMe(id, SecurityUtil.currentUserId()));
    }

    @DeleteMapping("/work-items/{id}")
    public ApiResponse delete(@PathVariable String id) {
        WorkItemDto dto = workItemService.detail(id);
        projectService.ensureProjectAdmin(dto.getProjectId(), SecurityUtil.currentUserId());
        workItemService.delete(id);
        return ApiResponse.ok();
    }

    // ==================== 看板 ====================

    @GetMapping("/projects/{id}/work-items/board")
    public ApiResponse board(@PathVariable Long id, @RequestParam(defaultValue = "50") int limit,
                             @ModelAttribute ExportWorkItemsRequest filters) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.board(id, PageRequests.size(limit, 200), filters, SecurityUtil.currentUserId()));
    }

    // ==================== 到期 ====================

    @GetMapping("/projects/{id}/due-items")
    public ApiResponse dueItems(@PathVariable Long id) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        return ApiResponse.ok(workItemService.dueItems(id, SecurityUtil.currentUserId()));
    }
}
