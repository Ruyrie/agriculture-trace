package com.example.agriculturetrace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 静态资源配置。
 *
 * 头像上传到项目运行目录的 uploads 文件夹，通过 /uploads/** 暴露给前端展示。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 把运行目录下的 uploads 文件夹映射成 /uploads/** 静态资源地址，
     * 用户上传头像后前端可以直接用返回的 URL 展示图片。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Path.of(System.getProperty("user.dir"), "uploads").toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
    }
}
