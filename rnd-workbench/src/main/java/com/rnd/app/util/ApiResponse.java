package com.rnd.app.util;

import java.util.Map;

public class ApiResponse {
    private int code;
    private String message;
    private Object data;

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok() { return new ApiResponse(0, "ok", null); }
    public static ApiResponse ok(Object data) { return new ApiResponse(0, "ok", data); }
    public static ApiResponse fail(ErrorCode ec) { return new ApiResponse(ec.code, ec.message, null); }
    public static ApiResponse fail(ErrorCode ec, String detail) { return new ApiResponse(ec.code, detail, null); }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }

    /** 分页快捷构造 */
    public static ApiResponse page(java.util.List<?> list, long total, int page, int size) {
        return ok(Map.of("list", list, "total", total, "page", page, "size", size));
    }
}