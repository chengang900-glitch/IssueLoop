package com.rnd.app.service;

import com.rnd.app.dto.AttachmentDto;
import com.rnd.app.entity.Attachment;
import com.rnd.app.repository.AttachmentRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {
    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/csv",
            "application/zip", "application/x-zip-compressed",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "video/mp4");
    private final AttachmentRepository attachmentRepo;

    public void validateUpload(long size, String mimeType) {
        if (size <= 0 || size > MAX_FILE_SIZE)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "附件大小必须在 1B 到 50MB 之间");
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的附件类型");
    }

    public List<AttachmentDto> list(String workItemId) {
        return attachmentRepo.findByWorkItemId(workItemId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AttachmentDto upload(String workItemId, String fileName, long size,
                                 String mimeType, Long uploaderId, Path storagePath) throws IOException {
        validateUpload(size, mimeType);
        // 净化文件名：去除路径分隔符，只保留末尾文件名部分
        String safeName = fileName != null ? fileName.replaceAll("[/\\\\]", "_") : "unnamed";
        String storeName = UUID.randomUUID() + "_" + safeName;
        Path target = storagePath.resolve(storeName);
        Files.createDirectories(storagePath);

        Attachment a = attachmentRepo.save(Attachment.builder()
                .workItemId(workItemId).fileName(fileName).size(size)
                .mimeType(mimeType).storagePath(storeName).uploaderId(uploaderId).build());
        return toDto(a);
    }

    public Attachment getFile(Long id) {
        return attachmentRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public void delete(Attachment attachment, Path storageRoot) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("附件删除需要数据库事务");
        }
        Path root = storageRoot.toAbsolutePath().normalize();
        Path file = root.resolve(attachment.getStoragePath()).normalize();
        if (!file.startsWith(root)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件存储路径无效");
        }
        Path staged = null;
        try {
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件文件类型异常");
                }
                staged = root.resolve(".deleting-" + UUID.randomUUID());
                Files.move(file, staged);
            }
            Path stagedFile = staged;
            attachmentRepo.deleteById(attachment.getId());
            if (stagedFile != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            Files.deleteIfExists(stagedFile);
                        } catch (IOException e) {
                            log.error("附件 {} 的文件清理失败", attachment.getId(), e);
                            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                                    "附件记录已删除，但磁盘文件清理失败，请联系管理员");
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) return;
                        try {
                            Files.move(stagedFile, file);
                        } catch (IOException e) {
                            log.error("附件 {} 的文件恢复失败", attachment.getId(), e);
                        }
                    }
                });
            }
        } catch (IOException | RuntimeException e) {
            if (staged != null && Files.exists(staged, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.move(staged, file);
                } catch (IOException restoreError) {
                    log.error("附件 {} 的文件恢复失败", attachment.getId(), restoreError);
                }
            }
            if (e instanceof BusinessException) throw (BusinessException) e;
            if (e instanceof IOException) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件文件清理失败");
            throw (RuntimeException) e;
        }
    }

    @Transactional
    public void cleanupFailedUpload(Long id, Path file) {
        attachmentRepo.deleteById(id);
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private AttachmentDto toDto(Attachment a) {
        return new AttachmentDto(a.getId(), a.getFileName(), a.getSize(),
                a.getMimeType(), a.getStoragePath(), a.getUploaderId(), a.getCreatedAt());
    }
}
