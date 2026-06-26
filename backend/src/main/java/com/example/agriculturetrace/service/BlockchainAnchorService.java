package com.example.agriculturetrace.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 区块链审计锚点服务。
 *
 * blockchain_log 的链式校验只能发现“中间日志被改/被删”，却发现不了“从链尾整段删除最新日志”——
 * 因为剩下的日志彼此 previous_hash 仍然连续，遍历校验会照样通过。为此把“期望的日志总数 +
 * 链尾哈希”单独锚定在 blockchain_anchor 表（单行，主键恒为 1）。系统每次正常写入日志后刷新锚点，
 * 验证时拿当前日志和锚点比对：条数变少或链尾哈希对不上，就说明有人绕过系统从尾部删除了日志。
 *
 * 局限：锚点与日志同库，能挡住“只删日志、不动锚点”的误操作或直改库，但挡不住同时篡改锚点行的攻击者。
 * 真实区块链需要把链尾哈希外锚到独立可信源（如另一条链/公证服务）。
 */
@Service
public class BlockchainAnchorService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ANCHOR_ID = 1;

    private final JdbcTemplate jdbcTemplate;

    public BlockchainAnchorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 锚点快照：期望日志条数、期望链尾哈希、更新时间。
     * Java record 自动生成构造器和访问器，适合表达只读数据载体。
     */
    public record Anchor(long logCount, String tipHash, String updatedAt) {
    }

    /**
     * 用当前日志状态刷新锚点。仅在系统正常写入日志后调用——
     * 因为只有合法写入才应推进“期望条数/链尾哈希”，事后的删除不会触发刷新，于是会被验证发现。
     */
    public void refresh(long logCount, String tipHash) {
        // 记录锚点更新时间，便于后续排查"锚点上次刷新"与"日志最后写入"是否吻合。
        String now = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        // ON DUPLICATE KEY UPDATE：表中已有 id=1 的行时执行 UPDATE，否则执行 INSERT。
        // 这样无论是首次写入还是后续刷新，都能用同一条 SQL 完成。
        // tipHash == null 时写 "0"（创世值），避免 NULL 存入后与字符串比对时产生歧义。
        jdbcTemplate.update("""
                INSERT INTO `blockchain_anchor` (`id`, `log_count`, `tip_hash`, `updated_at`)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE `log_count` = VALUES(`log_count`),
                                        `tip_hash` = VALUES(`tip_hash`),
                                        `updated_at` = VALUES(`updated_at`)
                """, ANCHOR_ID, logCount, tipHash == null ? "0" : tipHash, now);
    }

    /**
     * 读取锚点；尚未建立时返回 null。
     * 初始化器会在启动时补建缺失锚点，验证接口据此判断链尾是否被截断。
     */
    public Anchor getAnchor() {
        // jdbcTemplate.query 搭配 ResultSetExtractor（rs -> ...）：
        //   rs.next() 尝试移到第一行；有行时构造 Anchor 对象，无行时返回 null。
        // 这与 queryForObject 的区别是：queryForObject 在无结果时抛异常，这里直接返回 null 更安全。
        return jdbcTemplate.query("""
                        SELECT `log_count`, `tip_hash`, `updated_at`
                        FROM `blockchain_anchor`
                        WHERE `id` = ?
                        """,
                rs -> rs.next()
                        ? new Anchor(rs.getLong("log_count"), rs.getString("tip_hash"), rs.getString("updated_at"))
                        : null,
                ANCHOR_ID);  // ANCHOR_ID=1：锚点表只有唯一一行，主键固定为 1
    }
}
