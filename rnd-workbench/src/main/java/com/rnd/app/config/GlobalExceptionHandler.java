package com.rnd.app.config;

import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBiz(BusinessException e) {
        return ResponseEntity.status(mapHttp(e.getErrorCode()))
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage() == null ? e.getErrorCode().message : e.getMessage()));
    }

    /** 乐观锁冲突：并发写入了同一行，让客户端刷新后重试，而不是 500。 */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("并发写冲突：{}", e.getMessage());
        return ResponseEntity.status(409)
                .body(ApiResponse.fail(ErrorCode.STATUS_CONFLICT, "数据已被其他人修改，请刷新后重试"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(400).body(ApiResponse.fail(ErrorCode.BAD_REQUEST, detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleOther(Exception e, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(500).body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    private int mapHttp(ErrorCode ec) {
        switch (ec) {
            case INVALID_TOKEN: return 401;
            case FORBIDDEN: case ACCOUNT_LOCKED: return 403;
            case NOT_FOUND: return 404;
            case BAD_REQUEST: return 400;
            case STATUS_CONFLICT: case DUPLICATE: return 409;
            default: return 500;
        }
    }
}