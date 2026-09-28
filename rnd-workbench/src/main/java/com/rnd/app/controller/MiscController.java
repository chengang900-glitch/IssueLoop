package com.rnd.app.controller;

import com.rnd.app.entity.Attachment;
import com.rnd.app.service.ActivityService;
import com.rnd.app.service.AttachmentService;
import com.rnd.app.service.ProjectService;
import com.rnd.app.service.WorkItemService;
import com.rnd.app.config.AppConfig;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MiscController {

    private final ActivityService activityService;
    private final AttachmentService attachmentService;
    private final ProjectService projectService;
    private final WorkItemService workItemService;
    private final AppConfig appConfig;

    // ==================== 活动 ====================

    @GetMapping("/projects/{id}/activities")
    public ApiResponse projectActivities(@PathVariable Long id) {
        projectService.ensureProjectMember(id, SecurityUtil.currentUserId());
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(activityService.listByProject(id, pageable));
    }

    @GetMapping("/projects/{id}/audit-activities")
    public ApiResponse projectAuditActivities(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        projectService.ensureProjectAdmin(id, SecurityUtil.currentUserId());
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.min(200, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(activityService.listByProject(id, pageable));
    }

    @GetMapping("/work-items/{id}/activities")
    public ApiResponse itemActivities(@PathVariable String id) {
        var dto = workItemService.detail(id);
        projectService.ensureProjectMember(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(activityService.listByWorkItem(id));
    }

    // ==================== 附件 ====================

    @GetMapping("/work-items/{id}/attachments")
    public ApiResponse listAttachment(@PathVariable String id) {
        var dto = workItemService.detail(id);
        projectService.ensureProjectMember(dto.getProjectId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(attachmentService.list(id));
    }

    @PostMapping("/work-items/{id}/attachments")
    public ApiResponse upload(@PathVariable String id, @RequestParam("file") MultipartFile file) throws Exception {
        var dto = workItemService.detail(id);
        projectService.ensureProjectWriter(dto.getProjectId(), SecurityUtil.currentUserId());
        attachmentService.validateUpload(file.getSize(), file.getContentType());
        Path storage = Paths.get(appConfig.getStorage().getPath());
        var result = attachmentService.upload(id, file.getOriginalFilename(), file.getSize(),
                file.getContentType(), SecurityUtil.currentUserId(), storage);
        Path target = storage.resolve(result.getStoragePath());
        try {
            file.transferTo(target);
        } catch (Exception e) {
            attachmentService.cleanupFailedUpload(result.getId(), target);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件保存失败，请检查服务器磁盘空间");
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/attachments/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) throws Exception {
        Attachment a = attachmentService.getFile(fileId);
        var dto = workItemService.detail(a.getWorkItemId());
        projectService.ensureProjectMember(dto.getProjectId(), SecurityUtil.currentUserId());
        Path file = Paths.get(appConfig.getStorage().getPath()).resolve(a.getStoragePath());
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件文件不存在");
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(a.getFileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(a.getMimeType() != null ? a.getMimeType() : "application/octet-stream"))
                .body(resource);
    }

    @DeleteMapping("/attachments/{fileId}")
    @Transactional
    public ApiResponse deleteAttachment(@PathVariable Long fileId) {
        Attachment a = attachmentService.getFile(fileId);
        // 通过 attachment 关联到 work item → project 校验项目成员
        var dto = workItemService.detail(a.getWorkItemId());
        projectService.ensureProjectWriter(dto.getProjectId(), SecurityUtil.currentUserId());
        if (!a.getUploaderId().equals(SecurityUtil.currentUserId())) {
            projectService.ensureProjectAdmin(dto.getProjectId(), SecurityUtil.currentUserId());
        }
        attachmentService.delete(a, Paths.get(appConfig.getStorage().getPath()));
        workItemService.replaceDeletedImageMarker(a.getWorkItemId(), fileId);
        return ApiResponse.ok();
    }
}
