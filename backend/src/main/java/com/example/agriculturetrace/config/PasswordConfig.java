package com.example.agriculturetrace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 *
 * 独立出来可以避免 SecurityConfig 与 UserService 之间形成循环依赖。
 */
@Configuration
public class PasswordConfig {

    /**
     * 暴露 BCrypt 密码编码器 Bean。
     * UserService 用 encode 存储加盐哈希，Spring Security 登录时用 matches 比对明文输入。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
