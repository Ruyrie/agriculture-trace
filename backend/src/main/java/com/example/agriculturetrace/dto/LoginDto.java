package com.example.agriculturetrace.dto;

/**
 * 预留的登录 DTO。
 *
 * 当前登录由 Spring Security formLogin 接管，此类保留给接口文档和后续扩展使用。
 */
public record LoginDto(String username, String password) {
}
