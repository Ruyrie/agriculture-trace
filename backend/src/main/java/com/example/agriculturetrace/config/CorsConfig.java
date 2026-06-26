package com.example.agriculturetrace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 配置。
 *
 * allowCredentials=true 用于跨域携带 JSESSIONID Cookie；允许域名从配置读取，
 * 生产环境不要使用通配符。
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    /**
     * CORS 跨域访问规则
     * 注册全局 CORS 规则，允许前端 Vite 域名跨域访问后端接口并携带 Cookie。
     * Session 登录依赖 JSESSIONID，因此 allowCredentials 必须为 true。
     * 允许的请求方法：GET：查询
     * POST：新增/登录/上传
     * PUT：修改
     * DELETE：删除
     * OPTIONS：浏览器跨域预检请求
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        //允许前端请求携带任意请求头
        configuration.setAllowedHeaders(List.of("*"));
        //允许携带 Cookie
        configuration.setAllowCredentials(true);
        //创建一个基于 URL 路径的 CORS 配置源，/**把上面的跨域规则应用到所有后端接口路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
