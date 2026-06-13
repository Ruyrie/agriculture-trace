package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.BlockchainLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 区块链审计日志仓库。
 */
public interface BlockchainLogRepository extends JpaRepository<BlockchainLog, String> {

    @Query(value = "SELECT * FROM blockchain_log ORDER BY timestamp ASC, id ASC", nativeQuery = true)
    List<BlockchainLog> findAllByOrderByTimestampAsc();

    @Query(value = "SELECT * FROM blockchain_log ORDER BY timestamp DESC, id DESC LIMIT 1", nativeQuery = true)
    BlockchainLog findLastLog();
}
