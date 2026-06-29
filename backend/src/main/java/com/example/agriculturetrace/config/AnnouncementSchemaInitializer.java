package com.example.agriculturetrace.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时确保系统公告表存在。
 *
 * 与其他 SchemaInitializer 一致，在 ddl-auto: none 下用 CREATE TABLE IF NOT EXISTS 幂等建表。
 */
@Component
@Order(22)
public class AnnouncementSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AnnouncementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `announcement` (
                  `id` varchar(32) NOT NULL COMMENT '公告ID (UUID)',
                  `title` varchar(128) NOT NULL COMMENT '公告标题',
                  `content` text NOT NULL COMMENT '公告内容',
                  `status` varchar(20) DEFAULT 'PUBLISHED' COMMENT '状态: DRAFT/PUBLISHED',
                  `pinned` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
                  `creator` varchar(64) DEFAULT NULL COMMENT '发布人用户名',
                  `create_time` varchar(19) DEFAULT NULL COMMENT '创建时间 yyyy-MM-dd HH:mm:ss',
                  `update_time` varchar(19) DEFAULT NULL COMMENT '更新时间 yyyy-MM-dd HH:mm:ss',
                  PRIMARY KEY (`id`),
                  KEY `idx_announcement_status` (`status`),
                  KEY `idx_announcement_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
