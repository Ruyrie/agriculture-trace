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

    /**
     * 查询某产品下所有批次的物流轨迹，最近节点优先。
     */
    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from LogisticsRecord record
            where record.batch.product.id = :productId
            order by record.updateTime desc, record.sortOrder asc
            """)
    List<LogisticsRecord> findRowsByProductId(@Param("productId") String productId);

    /**
     * 查询单个批次的物流轨迹，按时间正序形成时间线。
     */
    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from LogisticsRecord record
            where record.batch.id = :batchId
            order by record.updateTime asc, record.sortOrder asc
            """)
    List<LogisticsRecord> findRowsByBatchId(@Param("batchId") String batchId);

    /**
     * 删除某批次下全部物流记录，用于批次溯源明细覆盖式更新。
     */
    void deleteByBatch_Id(String batchId);
}
