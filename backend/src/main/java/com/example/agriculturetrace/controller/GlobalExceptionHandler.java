package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.util.Result;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * API 异常统一转为 Result，保证前端可以稳定读取 code/message。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 资源查询失败时返回 404，覆盖 Optional.orElseThrow 和 JPA EntityNotFound 两类常见异常。
     */
    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public Result<?> notFound() {
        return Result.error(404, "资源不存在");
    }

    /**
     * 业务参数错误和数据库唯一约束错误统一返回 400。
     * 对批次号唯一冲突做定制提示，前端可直接展示给用户。
     */
    @ExceptionHandler({IllegalArgumentException.class, DataIntegrityViolationException.class})
    public Result<?> badRequest(Exception exception) {
        if (exception instanceof DataIntegrityViolationException
                && exception.getMessage() != null
                && exception.getMessage().contains("batch.batch_no")) {
            return Result.error(400, "批次号已存在，请更换批次号");
        }
        return Result.error(400, exception.getMessage());
    }

    /**
     * 头像文件超过后端上传限制时返回友好错误。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> uploadTooLarge() {
        return Result.error(400, "头像图片不能超过 50MB");
    }

    /**
     * 兜底异常处理，防止未捕获异常变成 HTML 错误页。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> serverError(Exception exception) {
        return Result.error(500, "服务器错误：" + exception.getMessage());
    }
}
