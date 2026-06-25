package com.example.agriculturetrace.config;

import com.example.agriculturetrace.util.UploadPaths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 静态资源配置。
 *
 * 上传文件统一放到项目根目录 uploads 文件夹，通过 /uploads/** 暴露给前端展示。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 把项目根目录下的 uploads 文件夹映射成 /uploads/** 静态资源地址，
     * 避免从不同工作目录启动后端时头像路径漂移。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = UploadPaths.rootUploadDir().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
    }
}
