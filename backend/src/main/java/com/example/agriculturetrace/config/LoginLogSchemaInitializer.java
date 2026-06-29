package com.example.agriculturetrace.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时确保登录日志表存在。
 *
 * 与 BlockchainSchemaInitializer / FeedbackSchemaInitializer 一致，
 * 在 ddl-auto: none 下用 CREATE TABLE IF NOT EXISTS 幂等建表。
 */
@Component
@Order(21)
public class LoginLogSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public LoginLogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `login_log` (
                  `id` varchar(32) NOT NULL COMMENT '日志ID (UUID)',
                  `username` varchar(64) DEFAULT NULL COMMENT '登录用户名',
                  `ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
                  `status` varchar(16) DEFAULT NULL COMMENT '结果: SUCCESS/FAILURE',
                  `message` varchar(128) DEFAULT NULL COMMENT '结果说明',
                  `user_agent` varchar(256) DEFAULT NULL COMMENT '浏览器UA',
                  `login_time` varchar(19) DEFAULT NULL COMMENT '登录时间 yyyy-MM-dd HH:mm:ss',
                  PRIMARY KEY (`id`),
                  KEY `idx_login_log_status` (`status`),
                  KEY `idx_login_log_time` (`login_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
