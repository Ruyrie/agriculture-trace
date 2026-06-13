package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.ProductionRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 生产记录 JPA 仓库。
 */
public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, String> {

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from ProductionRecord record
            where record.batch.product.id = :productId
            order by record.batch.productionDate desc, record.sortOrder asc
            """)
    List<ProductionRecord> findRowsByProductId(@Param("productId") String productId);

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from ProductionRecord record
            where record.batch.id = :batchId
            order by record.sortOrder asc
            """)
    List<ProductionRecord> findRowsByBatchId(@Param("batchId") String batchId);

    void deleteByBatch_Id(String batchId);
}
