package com.rnd.app.config;

import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
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

    /** 请求体不是合法 JSON / 类型不匹配：都属于客户端错误，不能落到 500。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(400).body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求体格式不正确"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(400)
                .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "参数 " + e.getName() + " 格式不正确"));
    }

    /**
     * 唯一约束/外键约束冲突（并发建项目、删除仍被引用的迭代或模块等）。
     * 具体 SQL 只写日志，不回传给客户端。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突：{}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409)
                .body(ApiResponse.fail(ErrorCode.DUPLICATE, "数据存在唯一性或关联约束冲突，请检查后重试"));
    }

    /** 上传超过 spring.servlet.multipart 限制时由容器抛出，映射为 413。 */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse> handleMultipart(MultipartException e) {
        log.warn("上传请求被拒绝：{}", e.getMessage());
        return ResponseEntity.status(413).body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "上传文件超过大小限制"));
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