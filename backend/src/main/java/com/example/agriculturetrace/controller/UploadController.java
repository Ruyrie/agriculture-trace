package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.util.Result;
import com.example.agriculturetrace.util.UploadPaths;
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
        // 拒绝空文件（前端没选文件就点上传，或者文件为 0 字节时触发）。
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        // contentType 是浏览器根据文件头判断的 MIME 类型；统一小写，便于后续 startsWith 比对。
        // 例如 "image/jpeg"、"image/png"，非图片类型（如 text/plain）会被此处过滤掉。
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }
        // 取原始文件名提取扩展名；浏览器上传时 originalFilename 来自 Content-Disposition，
        // 不带扩展名时默认 .png（保证文件名合法）。
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        // lastIndexOf(".") 定位最后一个点，截取从点开始到结尾的部分即为扩展名（含点，如 ".jpg"）。
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : ".png";
        // 白名单校验：只允许常见图片格式，防止上传可执行文件（.exe/.sh）绕过 MIME 检查。
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(400, "仅支持 jpg、png、gif、webp 图片");
        }
        // trace-images 子目录：把溯源图片和头像（直接存 uploads/）分开，方便维护清理。
        Path uploadDir = UploadPaths.rootUploadDir().resolve("trace-images");
        // createDirectories 如目录已存在不抛异常，首次启动自动创建目录树。
        Files.createDirectories(uploadDir);
        // 用随机 UUID 做文件名，避免原始文件名冲突和路径遍历攻击（../../../etc/passwd）。
        String filename = UUID.randomUUID() + extension;
        // 将上传内容以字节数组写入磁盘；getBytes() 把 MultipartFile 内存临时文件转为字节。
        Files.write(uploadDir.resolve(filename), file.getBytes());
        // 返回的 URL 以 /uploads/ 开头，由 WebMvcConfig 的静态资源映射直接访问，不经过鉴权。
        return Result.success(Map.of("url", "/uploads/trace-images/" + filename));
    }
}
