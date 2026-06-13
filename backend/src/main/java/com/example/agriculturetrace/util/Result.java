package com.example.agriculturetrace.util;

/**
 * REST API 统一响应结构。
 *
 * 前端只需要判断 code 和读取 data，错误提示统一从 message 展示。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
