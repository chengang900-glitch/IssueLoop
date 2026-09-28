package com.rnd.app.controller;

import com.rnd.app.dto.CreateCommentRequest;
import com.rnd.app.service.CommentService;
import com.rnd.app.service.ProjectService;
import com.rnd.app.service.WorkItemService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final WorkItemService workItemService;
    private final ProjectService projectService;

    @GetMapping("/work-items/{id}/comments")
    public ApiResponse list(@PathVariable String id,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) {
        var item = workItemService.detail(id);
        projectService.ensureProjectMember(item.getProjectId(), SecurityUtil.currentUserId());
        var pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = commentService.list(id, pageable);
        return ApiResponse.page(result.getContent(), result.getTotalElements(), page, size);
    }

    @PostMapping("/work-items/{id}/comments")
    public ApiResponse add(@PathVariable String id, @Valid @RequestBody CreateCommentRequest req) {
        var item = workItemService.detail(id);
        projectService.ensureProjectWriter(item.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(commentService.add(id, req.getContent(), SecurityUtil.currentUserId()));
    }
}
