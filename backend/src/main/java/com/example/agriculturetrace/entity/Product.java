package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 农产品基础信息实体。
 */
@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(length = 128)
    private String origin;

    private BigDecimal price;

    @Column(name = "create_time", length = 19)
    private String createTime;
}
