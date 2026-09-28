package com.rnd.app;

import com.rnd.app.entity.Attachment;
import com.rnd.app.repository.AttachmentRepository;
import com.rnd.app.service.AttachmentService;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AttachmentDeletionIntegrationTest {
    @Autowired private AttachmentService service;
    @Autowired private RollbackHarness rollbackHarness;
    @MockBean private AttachmentRepository attachments;
    @TempDir Path storage;

    @Test
    void committedDeletionRemovesStoredFile() throws Exception {
        Path file = storage.resolve("stored.png");
        Files.writeString(file, "image");

        service.delete(Attachment.builder().id(31L).storagePath("stored.png").build(), storage);

        verify(attachments).deleteById(31L);
        assertFalse(Files.exists(file));
        try (var files = Files.list(storage)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rolledBackDeletionRestoresStoredFile() throws Exception {
        Path file = storage.resolve("stored.png");
        Files.writeString(file, "image");

        assertThrows(IllegalStateException.class, () -> rollbackHarness.deleteThenFail(
                Attachment.builder().id(32L).storagePath("stored.png").build(), storage));

        assertEquals("image", Files.readString(file));
        try (var files = Files.list(storage)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void committedMetadataWithFileCleanupFailureReturnsExplicitError() throws Exception {
        Path file = storage.resolve("stored.png");
        Files.writeString(file, "image");

        BusinessException error = assertThrows(BusinessException.class, () -> rollbackHarness.deleteThenBlockCleanup(
                Attachment.builder().id(33L).storagePath("stored.png").build(), storage));

        assertEquals(ErrorCode.INTERNAL_ERROR, error.getErrorCode());
        assertFalse(Files.exists(file));
    }

    @TestConfiguration
    static class HarnessConfig {
        @Bean RollbackHarness rollbackHarness(AttachmentService service) {
            return new RollbackHarness(service);
        }
    }

    static class RollbackHarness {
        private final AttachmentService service;

        RollbackHarness(AttachmentService service) { this.service = service; }

        @Transactional
        public void deleteThenFail(Attachment attachment, Path storage) {
            service.delete(attachment, storage);
            throw new IllegalStateException("rollback for test");
        }

        @Transactional
        public void deleteThenBlockCleanup(Attachment attachment, Path storage) throws Exception {
            service.delete(attachment, storage);
            Path staged;
            try (var files = Files.list(storage)) {
                staged = files.findFirst().orElseThrow();
            }
            Files.delete(staged);
            Files.createDirectory(staged);
            Files.writeString(staged.resolve("blocker"), "block");
        }
    }
}
