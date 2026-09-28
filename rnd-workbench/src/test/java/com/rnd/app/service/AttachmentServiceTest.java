package com.rnd.app.service;

import com.rnd.app.entity.Attachment;
import com.rnd.app.repository.AttachmentRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock AttachmentRepository attachmentRepository;
    @InjectMocks AttachmentService service;

    @Test
    void acceptsAllowedFileWithinLimit() {
        assertDoesNotThrow(() -> service.validateUpload(1024, "image/png"));
    }

    @Test
    void rejectsOversizedAndUnknownFiles() {
        assertThrows(BusinessException.class,
                () -> service.validateUpload(AttachmentService.MAX_FILE_SIZE + 1, "image/png"));
        assertThrows(BusinessException.class,
                () -> service.validateUpload(1024, "application/x-executable"));
    }

    @Test
    void cleanupFailedUploadDeletesRecordAndPartialFile(@TempDir Path tempDir) throws Exception {
        Path partial = tempDir.resolve("partial-upload.png");
        Files.writeString(partial, "partial");

        service.cleanupFailedUpload(9L, partial);

        verify(attachmentRepository).deleteById(9L);
        assertFalse(Files.exists(partial));
    }

    @Test
    void deletingAttachmentRemovesFileAfterCommit(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("stored.png");
        Files.writeString(file, "image");
        Attachment attachment = Attachment.builder().id(7L).storagePath("stored.png").build();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(attachment, tempDir);
            assertFalse(Files.exists(file));
            verify(attachmentRepository).deleteById(7L);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
                sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            try (var files = Files.list(tempDir)) {
                assertEquals(0, files.count());
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletingAttachmentRestoresFileOnRollback(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("stored.png");
        Files.writeString(file, "image");
        Attachment attachment = Attachment.builder().id(8L).storagePath("stored.png").build();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(attachment, tempDir);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertEquals("image", Files.readString(file));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void invalidAttachmentPathCannotDeleteOutsideStorage(@TempDir Path tempDir) {
        Attachment attachment = Attachment.builder().id(9L).storagePath("../outside.png").build();
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(BusinessException.class, () -> service.delete(attachment, tempDir));
            verify(attachmentRepository, never()).deleteById(9L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void fileCleanupFailureIsReportedAfterCommit(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("stored.png");
        Files.writeString(file, "image");
        Attachment attachment = Attachment.builder().id(10L).storagePath("stored.png").build();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(attachment, tempDir);
            Path staged;
            try (var files = Files.list(tempDir)) {
                staged = files.findFirst().orElseThrow();
            }
            Files.delete(staged);
            Files.createDirectory(staged);
            Files.writeString(staged.resolve("blocker"), "block");

            BusinessException error = assertThrows(BusinessException.class, () ->
                    TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit());
            assertEquals(ErrorCode.INTERNAL_ERROR, error.getErrorCode());
            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
