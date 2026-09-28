package com.rnd.app.util;

/**
 * 统一业务错误码
 */
public enum ErrorCode {
    SUCCESS(0, "ok"),
    INVALID_TOKEN(1001, "未登录或 token 失效"),
    FORBIDDEN(1002, "无权限"),
    NOT_FOUND(1003, "资源不存在"),
    BAD_REQUEST(1004, "参数校验失败"),
    STATUS_CONFLICT(1005, "状态流转非法"),
    DUPLICATE(1006, "唯一约束冲突"),
    ACCOUNT_LOCKED(1007, "账号已锁定"),
    INTERNAL_ERROR(1500, "服务器内部错误");

    public final int code;
    public final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}