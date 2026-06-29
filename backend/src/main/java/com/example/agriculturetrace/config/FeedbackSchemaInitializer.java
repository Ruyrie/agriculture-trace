package com.example.agriculturetrace.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时确保意见反馈表存在且字段与实体一致。
 *
 * 项目关闭了 Hibernate 自动建表（ddl-auto: none），且本项目的数据库 agriculture_trace 可能
 * 与历史版本共用——历史版本可能已建过结构不同的 feedback 表（用 category/admin_reply/replied_by，
 * 且缺少 title/type/reply/reply_by）。仅靠 CREATE TABLE IF NOT EXISTS 不会修补旧表，会导致
 * "Unknown column 'reply'" 之类报错。因此这里在建表后再做一次幂等的列级兼容迁移，
 * 与 BlockchainSchemaInitializer 的 columnExists 思路一致。
 */
@Component
@Order(20)
public class FeedbackSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 1) 全新环境：按当前实体结构直接建表。
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `feedback` (
                  `id` varchar(32) NOT NULL COMMENT '反馈ID (UUID)',
                  `user_id` varchar(32) NOT NULL COMMENT '提交人用户ID',
                  `username` varchar(64) DEFAULT NULL COMMENT '提交人用户名(冗余)',
                  `type` varchar(20) DEFAULT NULL COMMENT '类型: BUG/SUGGESTION/OTHER',
                  `title` varchar(128) NOT NULL COMMENT '反馈标题',
                  `content` text NOT NULL COMMENT '反馈内容',
                  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/REPLIED/CLOSED',
                  `reply` text COMMENT '管理员回复内容',
                  `reply_by` varchar(64) DEFAULT NULL COMMENT '回复人用户名',
                  `reply_time` varchar(19) DEFAULT NULL COMMENT '回复时间 yyyy-MM-dd HH:mm:ss',
                  `create_time` varchar(19) DEFAULT NULL COMMENT '提交时间 yyyy-MM-dd HH:mm:ss',
                  PRIMARY KEY (`id`),
                  KEY `idx_feedback_user` (`user_id`),
                  KEY `idx_feedback_status` (`status`),
                  KEY `idx_feedback_create_time` (`create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        // 2) 旧库兼容：缺什么补什么，并尽量从历史同义列回填，避免丢数据。
        ensureTitleColumn();
        ensureTypeColumn();
        ensureReplyColumns();
        relaxLegacyNotNull();
    }

    /**
     * 补 title 列。历史表没有标题，回填为内容前 50 字，保证非空且可读。
     */
    private void ensureTitleColumn() {
        if (!columnExists("feedback", "title")) {
            jdbcTemplate.execute("ALTER TABLE `feedback` ADD COLUMN `title` varchar(128) DEFAULT NULL COMMENT '反馈标题'");
            jdbcTemplate.update("UPDATE `feedback` SET `title` = LEFT(`content`, 50) WHERE `title` IS NULL OR `title` = ''");
        }
    }

    /**
     * 补 type 列，并从历史 category 列回填；非法值归一为 OTHER。
     */
    private void ensureTypeColumn() {
        if (!columnExists("feedback", "type")) {
            jdbcTemplate.execute("ALTER TABLE `feedback` ADD COLUMN `type` varchar(20) DEFAULT NULL COMMENT '类型: BUG/SUGGESTION/OTHER'");
            if (columnExists("feedback", "category")) {
                jdbcTemplate.update("UPDATE `feedback` SET `type` = `category` WHERE `type` IS NULL");
            }
            jdbcTemplate.update("UPDATE `feedback` SET `type` = 'OTHER' WHERE `type` IS NULL OR `type` NOT IN ('BUG','SUGGESTION','OTHER')");
        }
    }

    /**
     * 补 reply / reply_by 列，并从历史 admin_reply / replied_by 列回填。
     */
    private void ensureReplyColumns() {
        if (!columnExists("feedback", "reply")) {
            jdbcTemplate.execute("ALTER TABLE `feedback` ADD COLUMN `reply` text COMMENT '管理员回复内容'");
            if (columnExists("feedback", "admin_reply")) {
                jdbcTemplate.update("UPDATE `feedback` SET `reply` = `admin_reply` WHERE `reply` IS NULL AND `admin_reply` IS NOT NULL");
            }
        }
        if (!columnExists("feedback", "reply_by")) {
            jdbcTemplate.execute("ALTER TABLE `feedback` ADD COLUMN `reply_by` varchar(64) DEFAULT NULL COMMENT '回复人用户名'");
            if (columnExists("feedback", "replied_by")) {
                jdbcTemplate.update("UPDATE `feedback` SET `reply_by` = `replied_by` WHERE `reply_by` IS NULL AND `replied_by` IS NOT NULL");
            }
        }
    }

    /**
     * 历史 category 列为 NOT NULL 且无默认值，而实体不再写入它，
     * 会导致新插入因缺少 category 失败。这里放开为可空，避免阻断写入。
     */
    private void relaxLegacyNotNull() {
        if (columnExists("feedback", "category")) {
            jdbcTemplate.execute("ALTER TABLE `feedback` MODIFY COLUMN `category` varchar(32) NULL");
        }
    }

    /**
     * 判断当前库的指定表是否存在某列，用于幂等迁移。
     */
    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
