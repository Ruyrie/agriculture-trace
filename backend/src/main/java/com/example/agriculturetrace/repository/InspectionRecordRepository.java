package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.InspectionRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 质检记录 JPA 仓库。
 */
public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, String> {

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from InspectionRecord record
            where record.batch.product.id = :productId
            order by record.batch.productionDate desc, record.sortOrder asc
            """)
    List<InspectionRecord> findRowsByProductId(@Param("productId") String productId);

    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from InspectionRecord record
            where record.batch.id = :batchId
            order by record.sortOrder asc
            """)
    List<InspectionRecord> findRowsByBatchId(@Param("batchId") String batchId);

    void deleteByBatch_Id(String batchId);
}
