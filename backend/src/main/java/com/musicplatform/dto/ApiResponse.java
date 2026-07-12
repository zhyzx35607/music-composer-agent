package com.musicplatform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应包装类。
 * 所有接口响应都用此类包装，前端可统一按 {code, message, data} 解析。
 *
 * @param <T> data 的类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 业务状态码，对齐 HTTP 状态码 */
    private int code;

    /** 人类可读的消息说明 */
    private String message;

    /** 响应数据，可能为 null */
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 工厂方法 ====================

    /** 成功（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    /** 成功（不带数据） */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "ok", null);
    }

    /** 成功（自定义消息） */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 创建成功（201） */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "created", data);
    }

    /** 客户端错误 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 400 Bad Request */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /** 404 Not Found */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    /** 500 Internal Server Error */
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(500, message, null);
    }

    // ==================== Getter ====================

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
