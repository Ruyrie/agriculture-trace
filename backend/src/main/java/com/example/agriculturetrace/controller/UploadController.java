package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.util.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 通用图片上传接口。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    /**
     * 上传农产品、批次和生产记录图片，返回可公开访问的 /uploads/** URL。
     */
    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : ".png";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(400, "仅支持 jpg、png、gif、webp 图片");
        }
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "trace-images");
        Files.createDirectories(uploadDir);
        String filename = UUID.randomUUID() + extension;
        Files.write(uploadDir.resolve(filename), file.getBytes());
        return Result.success(Map.of("url", "/uploads/trace-images/" + filename));
    }
}
