package com.rnd.app.config;

import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 异常 → HTTP 状态码/错误码 的映射表（客户端错误不能落到 500）。 */
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOptimisticLockConflictTo409() {
        ResponseEntity<ApiResponse> response =
                handler.handleOptimisticLock(new OptimisticLockingFailureException("stale"));
        assertEquals(409, response.getStatusCodeValue());
        assertEquals(ErrorCode.STATUS_CONFLICT.code, response.getBody().getCode());
    }

    @Test
    void mapsDataIntegrityViolationTo409() {
        ResponseEntity<ApiResponse> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));
        assertEquals(409, response.getStatusCodeValue());
        assertEquals(ErrorCode.DUPLICATE.code, response.getBody().getCode());
    }

    @Test
    void mapsOversizedUploadTo413() {
        ResponseEntity<ApiResponse> response =
                handler.handleMultipart(new MaxUploadSizeExceededException(1024L));
        assertEquals(413, response.getStatusCodeValue());
    }
}
