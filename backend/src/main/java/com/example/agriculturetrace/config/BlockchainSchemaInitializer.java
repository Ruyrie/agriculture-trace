package com.example.agriculturetrace.config;

import com.example.agriculturetrace.service.BlockchainAnchorService;
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
    private final BlockchainAnchorService anchorService;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BlockchainSchemaInitializer(JdbcTemplate jdbcTemplate,
                                       ObjectMapper objectMapper,
                                       BlockchainAnchorService anchorService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.anchorService = anchorService;
    }

    /**
     * 应用启动后执行一次兼容初始化：补字段、建审计表/锚点表、回填旧数据哈希并生成初始日志。
     */
    @Override
    public void run(ApplicationArguments args) {
        boolean productHashColumnCreated = ensureProductHashColumn();
        boolean batchHashColumnCreated = ensureBatchHashColumn();
        ensureImageColumns();
        ensureTraceDateTimeColumns();
        ensureBlockchainLogTable();
        ensureBlockchainAnchorTable();
        if (productHashColumnCreated) {
            backfillProductHashes();
        }
        if (batchHashColumnCreated) {
            backfillBatchHashes();
        }
        seedInitialAuditLogsIfEmpty();
        initAnchorIfMissing();
    }

    /**
     * 确保 product 表存在 data_hash 字段。
     * 返回 true 表示本次新建了字段，需要随后为历史产品回填哈希。
     */
    private boolean ensureProductHashColumn() {
        if (!columnExists("product", "data_hash")) {
            jdbcTemplate.execute("ALTER TABLE `product` ADD COLUMN `data_hash` varchar(64) DEFAULT NULL COMMENT '产品数据哈希(SHA-256)'");
            return true;
        }
        return false;
    }

    /**
     * 确保 batch 表存在 data_hash 字段。
     */
    private boolean ensureBatchHashColumn() {
        if (!columnExists("batch", "data_hash")) {
            jdbcTemplate.execute("ALTER TABLE `batch` ADD COLUMN `data_hash` varchar(64) DEFAULT NULL COMMENT '批次数据哈希(SHA-256)'");
            return true;
        }
        return false;
    }

    /**
     * 图片只用于前端展示，不进入既有业务指纹，避免补图影响历史哈希校验。
     */
    private void ensureImageColumns() {
        if (!columnExists("product", "image_urls")) {
            jdbcTemplate.execute("ALTER TABLE `product` ADD COLUMN `image_urls` text COMMENT '产品图片URL列表JSON'");
        }
        if (!columnExists("batch", "image_urls")) {
            jdbcTemplate.execute("ALTER TABLE `batch` ADD COLUMN `image_urls` text COMMENT '批次图片URL列表JSON'");
        }
        if (!columnExists("production_record", "image_urls")) {
            jdbcTemplate.execute("ALTER TABLE `production_record` ADD COLUMN `image_urls` text COMMENT '生产记录图片URL列表JSON'");
        }
    }

    /**
     * 生产记录和质检记录需要保留具体时分秒；旧库里的 date 列在这里升级为文本时间。
     */
    private void ensureTraceDateTimeColumns() {
        if (columnExists("production_record", "activity_date")) {
            if (!"varchar".equalsIgnoreCase(columnDataType("production_record", "activity_date"))) {
                jdbcTemplate.execute("ALTER TABLE `production_record` MODIFY COLUMN `activity_date` varchar(19) DEFAULT NULL COMMENT '活动时间'");
            }
            jdbcTemplate.update("""
                    UPDATE `production_record`
                    SET `activity_date` = CONCAT(`activity_date`, ' 00:00:00')
                    WHERE `activity_date` REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
                    """);
        }
        if (columnExists("inspection_record", "inspection_date")) {
            if (!"varchar".equalsIgnoreCase(columnDataType("inspection_record", "inspection_date"))) {
                jdbcTemplate.execute("ALTER TABLE `inspection_record` MODIFY COLUMN `inspection_date` varchar(19) DEFAULT NULL COMMENT '检测时间'");
            }
            jdbcTemplate.update("""
                    UPDATE `inspection_record`
                    SET `inspection_date` = CONCAT(`inspection_date`, ' 00:00:00')
                    WHERE `inspection_date` REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
                    """);
        }
    }

    /**
     * 创建审计日志表，用 previous_hash 和 data_hash 保存链式日志结构。
     */
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

    /**
     * 创建链尾锚点表，用于检测“删除最新几条日志”这类链尾截断问题。
     */
    private void ensureBlockchainAnchorTable() {
        // 单行锚点表：把“期望日志条数 + 链尾哈希”存在 blockchain_log 之外，
        // 用来发现链条遍历无法察觉的“从链尾整段删除最新日志”。
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `blockchain_anchor` (
                  `id` int NOT NULL COMMENT '锚点固定主键，恒为1',
                  `log_count` bigint NOT NULL COMMENT '期望的日志总条数',
                  `tip_hash` varchar(64) NOT NULL COMMENT '期望的链尾(最后一条日志)哈希',
                  `updated_at` varchar(19) NOT NULL COMMENT '锚点更新时间 yyyy-MM-dd HH:mm:ss',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    /**
     * 如果锚点尚不存在，就用当前日志总数和链尾哈希建立首次基线。
     * 已有锚点绝不重置，防止启动时掩盖宕机期间的删尾篡改。
     */
    private void initAnchorIfMissing() {
        // 关键：只在锚点缺失时按当前日志建立基线，绝不能在启动时重置已有锚点，
        // 否则应用宕机期间发生的“删尾”会被当成新基线放过，二次校验就失效了。
        if (anchorService.getAnchor() != null) {
            return;
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `blockchain_log`", Long.class);
        String tipHash = jdbcTemplate.query(
                "SELECT `data_hash` FROM `blockchain_log` ORDER BY `id` DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : "0");
        anchorService.refresh(count == null ? 0L : count, tipHash);
    }

    /**
     * 为历史产品记录按 Java 侧相同字段顺序回填 data_hash。
     */
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

    /**
     * 为历史批次记录按 Java 侧相同字段顺序回填 data_hash。
     */
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

    /**
     * 查询当前数据库中指定表列是否存在，用于安全执行幂等迁移。
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

    /**
     * 查询列的数据类型，供启动迁移判断是否需要真正改表。
     */
    private String columnDataType(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, String.class, tableName, columnName);
    }

    /**
     * 当审计日志表为空时，根据已有产品和批次生成一组 CREATE 初始日志。
     * 这样旧数据也能接入后续链式校验。
     */
    private void seedInitialAuditLogsIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `blockchain_log`", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        String previousHash = "0";
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(5);

        List<Map<String, Object>> products = jdbcTemplate.queryForList("""
                SELECT `id`, `name`, `category`, `origin`, `price`, `create_time`, `data_hash`, `image_urls`
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
                       b.`remark`, b.`create_time`, b.`data_hash`, b.`image_urls`
                FROM `batch` b
                LEFT JOIN `product` p ON p.`id` = b.`product_id`
                ORDER BY b.`create_time`, b.`id`
                """);
        for (Map<String, Object> batch : batches) {
            timestamp = timestamp.plusSeconds(1);
            previousHash = insertInitialLog("CREATE", "BATCH", stringValue(batch.get("id")), batchRow(batch), previousHash, timestamp);
        }
    }

    /**
     * 插入一条初始化审计日志，并返回该日志 dataHash 作为下一条日志的 previousHash。
     */
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

    /**
     * 把数据库产品行转换成审计日志 data_after 使用的稳定 JSON 对象。
     */
    private Map<String, Object> productRow(Map<String, Object> product) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.get("id"));
        row.put("name", product.get("name"));
        row.put("category", product.get("category"));
        row.put("origin", product.get("origin"));
        row.put("price", normalizePrice(product.get("price")));
        row.put("createTime", product.get("create_time"));
        row.put("dataHash", product.get("data_hash"));
        row.put("imageUrls", product.get("image_urls"));
        return row;
    }

    /**
     * 把数据库批次行转换成审计日志 data_after 使用的稳定 JSON 对象。
     */
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
        row.put("imageUrls", batch.get("image_urls"));
        return row;
    }

    /**
     * 将对象序列化为 JSON 字符串；失败时返回空字符串避免启动迁移中断。
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 标准化数据库 price 值，使 SQL 回填哈希和 Java Service 计算规则一致。
     */
    private String normalizePrice(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    /**
     * 将可能为空的数据库字段转换为字符串。
     */
    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
