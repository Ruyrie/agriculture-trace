package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Batch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 批次 JPA 仓库。
 */
public interface BatchRepository extends JpaRepository<Batch, String> {

    Optional<Batch> findByBatchNo(String batchNo);

    /**
     * 批次溯源详情需要产品基础信息，提前加载 product，避免懒加载失效。
     */
    @EntityGraph(attributePaths = "product")
    @Query("select batch from Batch batch where batch.id = :id")
    Optional<Batch> findDetailById(@Param("id") String id);

    /**
     * 批次列表需要展示产品名称，提前抓取 product，避免控制器组装响应时触发懒加载异常。
     */
    @Override
    @EntityGraph(attributePaths = "product")
    Page<Batch> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "product")
    @Query("select batch from Batch batch")
    List<Batch> findAllWithProduct();

    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_Id(String productId, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_NameIgnoreCase(String productName, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<Batch> findByBatchNoContaining(String batchNo, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_IdAndBatchNoContaining(String productId, String batchNo, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_NameIgnoreCaseAndBatchNoContaining(String productName, String batchNo, Pageable pageable);

    List<Batch> findByProduct_IdOrderByCreateTimeDesc(String productId);
}
