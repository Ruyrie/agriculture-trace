package com.example.agriculturetrace.config;

import com.example.agriculturetrace.util.HashUtil;
import com.example.agriculturetrace.util.Ids;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为已有数据库补齐模拟区块链功能需要的字段和日志表。
 *
 * 项目关闭了 Hibernate 自动改表，旧库升级时需要在应用启动期做一次兼容迁移。
 */
@Component
public class BlockchainSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BlockchainSchemaInitializer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureProductHashColumn();
        ensureBatchHashColumn();
        ensureBlockchainLogTable();
        backfillProductHashes();
        backfillBatchHashes();
        seedInitialAuditLogsIfEmpty();
    }

    private void ensureProductHashColumn() {
        if (!columnExists("product", "data_hash")) {
            jdbcTemplate.execute("ALTER TABLE `product` ADD COLUMN `data_hash` varchar(64) DEFAULT NULL COMMENT '产品数据哈希(SHA-256)'");
        }
    }

    private void ensureBatchHashColumn() {
        if (!columnExists("batch", "data_hash")) {
            jdbcTemplate.execute("ALTER TABLE `batch` ADD COLUMN `data_hash` varchar(64) DEFAULT NULL COMMENT '批次数据哈希(SHA-256)'");
        }
    }

    private void ensureBlockchainLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `blockchain_log` (
                  `id` varchar(32) NOT NULL COMMENT '日志ID (时间前缀UUID)',
                  `action_type` varchar(20) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
                  `target_type` varchar(20) NOT NULL COMMENT '目标类型: PRODUCT/BATCH',
                  `target_id` varchar(32) NOT NULL COMMENT '目标ID',
                  `operator` varchar(64) NOT NULL COMMENT '操作人用户名',
                  `data_before` text COMMENT '操作前数据JSON',
                  `data_after` text COMMENT '操作后数据JSON',
                  `data_hash` varchar(64) NOT NULL COMMENT '本条日志哈希',
                  `previous_hash` varchar(64) DEFAULT NULL COMMENT '上一条日志哈希',
                  `timestamp` varchar(19) NOT NULL COMMENT '操作时间 yyyy-MM-dd HH:mm:ss',
                  PRIMARY KEY (`id`),
                  KEY `idx_blockchain_target` (`target_type`,`target_id`),
                  KEY `idx_blockchain_timestamp` (`timestamp`,`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void backfillProductHashes() {
        jdbcTemplate.update("""
                UPDATE `product`
                SET `data_hash` = SHA2(CONCAT(
                  IFNULL(`id`, ''), '|',
                  IFNULL(`name`, ''), '|',
                  IFNULL(`category`, ''), '|',
                  IFNULL(`origin`, ''), '|',
                  CASE
                    WHEN `price` IS NULL THEN ''
                    WHEN `price` = 0 THEN '0'
                    ELSE TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM CAST(`price` AS CHAR)))
                  END, '|',
                  IFNULL(`create_time`, '')
                ), 256)
                WHERE `data_hash` IS NULL OR `data_hash` = ''
                """);
    }

    private void backfillBatchHashes() {
        jdbcTemplate.update("""
                UPDATE `batch`
                SET `data_hash` = SHA2(CONCAT(
                  IFNULL(`id`, ''), '|',
                  IFNULL(`batch_no`, ''), '|',
                  IFNULL(`product_id`, ''), '|',
                  IFNULL(DATE_FORMAT(`production_date`, '%Y-%m-%d'), ''), '|',
                  IFNULL(`remark`, ''), '|',
                  IFNULL(`create_time`, '')
                ), 256)
                WHERE `data_hash` IS NULL OR `data_hash` = ''
                """);
    }

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

    private void seedInitialAuditLogsIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `blockchain_log`", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        String previousHash = "0";
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(5);

        List<Map<String, Object>> products = jdbcTemplate.queryForList("""
                SELECT `id`, `name`, `category`, `origin`, `price`, `create_time`, `data_hash`
                FROM `product`
                ORDER BY `id`
                """);
        for (Map<String, Object> product : products) {
            timestamp = timestamp.plusSeconds(1);
            previousHash = insertInitialLog("CREATE", "PRODUCT", stringValue(product.get("id")), productRow(product), previousHash, timestamp);
        }

        List<Map<String, Object>> batches = jdbcTemplate.queryForList("""
                SELECT b.`id`, b.`batch_no`, b.`product_id`, p.`name` AS `product_name`,
                       DATE_FORMAT(b.`production_date`, '%Y-%m-%d') AS `production_date`,
                       b.`remark`, b.`create_time`, b.`data_hash`
                FROM `batch` b
                LEFT JOIN `product` p ON p.`id` = b.`product_id`
                ORDER BY b.`create_time`, b.`id`
                """);
        for (Map<String, Object> batch : batches) {
            timestamp = timestamp.plusSeconds(1);
            previousHash = insertInitialLog("CREATE", "BATCH", stringValue(batch.get("id")), batchRow(batch), previousHash, timestamp);
        }
    }

    private String insertInitialLog(String action,
                                    String targetType,
                                    String targetId,
                                    Map<String, Object> dataAfter,
                                    String previousHash,
                                    LocalDateTime timestamp) {
        String timestampText = timestamp.format(TIMESTAMP_FORMATTER);
        String dataAfterJson = toJson(dataAfter);
        String dataHash = HashUtil.sha256(action + targetId + "system" + timestampText + previousHash + dataAfterJson);
        jdbcTemplate.update("""
                INSERT INTO `blockchain_log`
                (`id`, `action_type`, `target_type`, `target_id`, `operator`, `data_before`, `data_after`, `data_hash`, `previous_hash`, `timestamp`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Ids.logId(),
                action,
                targetType,
                targetId,
                "system",
                null,
                dataAfterJson,
                dataHash,
                previousHash,
                timestampText);
        return dataHash;
    }

    private Map<String, Object> productRow(Map<String, Object> product) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.get("id"));
        row.put("name", product.get("name"));
        row.put("category", product.get("category"));
        row.put("origin", product.get("origin"));
        row.put("price", normalizePrice(product.get("price")));
        row.put("createTime", product.get("create_time"));
        row.put("dataHash", product.get("data_hash"));
        return row;
    }

    private Map<String, Object> batchRow(Map<String, Object> batch) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", batch.get("id"));
        row.put("batchNo", batch.get("batch_no"));
        row.put("productId", batch.get("product_id"));
        row.put("productName", batch.get("product_name"));
        row.put("productionDate", batch.get("production_date"));
        row.put("remark", batch.get("remark"));
        row.put("createTime", batch.get("create_time"));
        row.put("dataHash", batch.get("data_hash"));
        return row;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizePrice(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
