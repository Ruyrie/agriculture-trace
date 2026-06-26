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
        // user.dir 是 JVM 启动时的当前工作目录。
        // IDE 直接运行时通常是 backend/，Maven 打 jar 后运行时通常是项目根目录，两种情况都要兼容。
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        // 如果当前目录的最后一段是 "backend"，则上溯到父目录，确保 uploads 始终在项目根目录下。
        // equalsIgnoreCase 兼容 Windows/macOS 大小写差异（如 Backend/backend）。
        Path projectRoot = "backend".equalsIgnoreCase(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
        // resolve("uploads") 在项目根目录下拼接 uploads 子目录，normalize() 消除多余的 ./ 和 ../。
        return projectRoot.resolve("uploads").normalize();
    }
}
