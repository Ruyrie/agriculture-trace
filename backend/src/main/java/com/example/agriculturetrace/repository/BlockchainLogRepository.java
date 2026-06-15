package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.BlockchainLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 区块链审计日志仓库。
 */
public interface BlockchainLogRepository extends JpaRepository<BlockchainLog, String> {

    // 链的顺序必须按 id（Ids.logId() 生成，前缀是 UTC 毫秒，单调且与时区无关）排序，
    // 不能按 timestamp。timestamp 是各机器的本地墙钟字符串，跨时区会乱序，
    // 导致写入链尾与验证遍历顺序不一致，从而误报“上一哈希不连续”。
    @Query(value = "SELECT * FROM blockchain_log ORDER BY id ASC", nativeQuery = true)
    List<BlockchainLog> findAllOrderedByIdAsc();

    @Query(value = "SELECT * FROM blockchain_log ORDER BY id DESC LIMIT 1", nativeQuery = true)
    BlockchainLog findLastLog();
}
