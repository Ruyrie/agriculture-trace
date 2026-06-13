package com.example.agriculturetrace.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式工具。
 *
 * 数据规范要求时间字段保存为 yyyy-MM-dd HH:mm:ss 字符串。
 */
public final class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtils() {
    }

    public static String nowText() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
