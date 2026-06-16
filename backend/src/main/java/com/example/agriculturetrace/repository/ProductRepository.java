package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 产品 JPA 仓库。
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * 按产品名称忽略大小写模糊查询，用于产品列表搜索框。
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 取出当前编号最大的 prod_N 产品 ID（仅匹配 prod_ + 纯数字的规范 ID），
     * 用于生成下一个可读的顺序产品 ID。历史遗留的随机 UUID 主键不参与计算。
     */
    @Query(value = "SELECT id FROM product WHERE id REGEXP '^prod_[0-9]+$' "
            + "ORDER BY CAST(SUBSTRING(id, 6) AS UNSIGNED) DESC LIMIT 1", nativeQuery = true)
    String findMaxProductId();
}
