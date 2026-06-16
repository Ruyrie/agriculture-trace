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

    /**
     * 按批次号查询批次，用于新增/编辑时校验批次号唯一性。
     */
    Optional<Batch> findByBatchNo(String batchNo);

    /**
     * 取出当前编号最大的 batch_N 批次 ID（仅匹配 batch_ + 纯数字的规范 ID），
     * 用于生成下一个可读的顺序批次 ID。历史遗留的随机 UUID 主键不参与计算。
     */
    @Query(value = "SELECT id FROM batch WHERE id REGEXP '^batch_[0-9]+$' "
            + "ORDER BY CAST(SUBSTRING(id, 7) AS UNSIGNED) DESC LIMIT 1", nativeQuery = true)
    String findMaxBatchId();

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

    /**
     * 按产品 ID 分页查询批次，并预加载 product 供列表展示。
     */
    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_Id(String productId, Pageable pageable);

    /**
     * 按产品名称精确匹配查询批次，用于批次列表按产品名筛选。
     */
    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_NameIgnoreCase(String productName, Pageable pageable);

    /**
     * 按批次号模糊查询批次。
     */
    @EntityGraph(attributePaths = "product")
    Page<Batch> findByBatchNoContaining(String batchNo, Pageable pageable);

    /**
     * 按产品 ID 和批次号模糊条件组合查询批次。
     */
    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_IdAndBatchNoContaining(String productId, String batchNo, Pageable pageable);

    /**
     * 按产品名称和批次号组合查询批次。
     */
    @EntityGraph(attributePaths = "product")
    Page<Batch> findByProduct_NameIgnoreCaseAndBatchNoContaining(String productName, String batchNo, Pageable pageable);

    /**
     * 查询某个产品下的所有批次，按创建时间倒序供公开溯源页展示。
     */
    List<Batch> findByProduct_IdOrderByCreateTimeDesc(String productId);
}
