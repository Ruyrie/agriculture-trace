package com.example.agriculturetrace.util;

import java.nio.file.Path;

/**
 * 统一解析上传目录，避免从项目根目录、backend 目录或 IDE 启动时路径漂移。
 */
public final class UploadPaths {

    private UploadPaths() {
    }

    /**
     * 固定使用项目根目录 uploads；如果当前运行目录是 backend，则回到父目录。
     */
    public static Path rootUploadDir() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path projectRoot = "backend".equalsIgnoreCase(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
        return projectRoot.resolve("uploads").normalize();
    }
}
