package com.example.agriculturetrace.util;

/**
 * REST API 统一响应结构。
 *
 * 前端只需要判断 code 和读取 data，错误提示统一从 message 展示。
 */
public record Result<T>(int code, String message, T data) {

    /**
     * 构造成功响应，约定 code=200、message=success。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 构造失败响应，data 固定为空，message 用于前端直接提示。
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
