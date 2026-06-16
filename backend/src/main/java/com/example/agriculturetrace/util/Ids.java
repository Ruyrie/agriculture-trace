package com.example.agriculturetrace.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 主键生成工具。
 *
 * 需求要求主键为 varchar(32)，因此统一使用去横线 UUID。
 */
public final class Ids {

    /** 审计日志主键的进程内自增序列，用于在同一毫秒内保证严格递增。 */
    private static final AtomicLong LOG_COUNTER = new AtomicLong(0);

    private Ids() {
    }

    /**
     * 生成 32 位无横线 UUID，满足数据库 varchar(32) 主键长度设计。
     */
    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成“时间前缀、单调递增”的审计日志主键（varchar(32)）。
     *
     * 结构：13 位毫秒时间戳 + 6 位进程内自增序列 + 13 位随机十六进制。
     * - 毫秒前缀（定宽、补零，字典序等价于数值序）保证跨重启按时间有序；
     * - 自增序列保证同一毫秒内严格有序；
     * - 随机后缀仅用于防止极端并发下的主键碰撞。
     *
     * 这样按 (timestamp, id) 升序排序时，顺序始终等于真实写入顺序，
     * 链式哈希（previous_hash -> data_hash）不会再因排序歧义而误报“日志链断裂”。
     */
    public static String logId() {
        long millis = System.currentTimeMillis();
        long seq = LOG_COUNTER.getAndIncrement() % 1_000_000L;
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        return String.format("%013d%06d%s", millis, seq, random);
    }
}
