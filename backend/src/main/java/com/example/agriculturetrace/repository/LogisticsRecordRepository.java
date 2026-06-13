package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.LogisticsRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 物流轨迹 JPA 仓库。
 */
public interface LogisticsRecordRepository extends JpaRepository<LogisticsRecord, String> {

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from LogisticsRecord record
            where record.batch.product.id = :productId
            order by record.updateTime desc, record.sortOrder asc
            """)
    List<LogisticsRecord> findRowsByProductId(@Param("productId") String productId);

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from LogisticsRecord record
            where record.batch.id = :batchId
            order by record.updateTime asc, record.sortOrder asc
            """)
    List<LogisticsRecord> findRowsByBatchId(@Param("batchId") String batchId);

    void deleteByBatch_Id(String batchId);
}
