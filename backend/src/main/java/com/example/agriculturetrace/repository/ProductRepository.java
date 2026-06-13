package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 产品 JPA 仓库。
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
