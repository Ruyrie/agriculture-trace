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

    /**
     * 查询某产品下所有批次的质检记录，按批次生产日期和录入顺序排序。
     */
    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from InspectionRecord record
            where record.batch.product.id = :productId
            order by record.batch.productionDate desc, record.sortOrder asc
            """)
    List<InspectionRecord> findRowsByProductId(@Param("productId") String productId);

    /**
     * 查询单个批次的质检记录，供批次维度溯源详情使用。
     */
    @EntityGraph(attributePaths = "batch")
    @Query("""
            select record from InspectionRecord record
            where record.batch.id = :batchId
            order by record.sortOrder asc
            """)
    List<InspectionRecord> findRowsByBatchId(@Param("batchId") String batchId);

    /**
     * 删除某批次下全部质检记录，用于批次溯源明细覆盖式更新。
     */
    void deleteByBatch_Id(String batchId);

    /**
     * 预加载批次及其产品，取出全部质检记录，供预警中心扫描"质检不合格"。
     */
    @EntityGraph(attributePaths = {"batch", "batch.product"})
    @Query("select record from InspectionRecord record")
    List<InspectionRecord> findAllWithBatch();

    /**
     * 取出所有"已存在质检记录"的批次 ID（去重），供预警中心计算"缺少质检"的批次集合。
     */
    @Query("select distinct record.batch.id from InspectionRecord record")
    List<String> findDistinctBatchIds();
}
