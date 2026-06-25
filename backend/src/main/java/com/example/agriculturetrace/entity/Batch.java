package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 产品批次实体。
 *
 * 批次号全局唯一，用于生产、质检、物流等溯源信息的业务串联。
 */
@Getter
@Setter
@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "batch_no", nullable = false, unique = true, length = 64)
    private String batchNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(length = 256)
    private String remark;

    @Column(name = "create_time", length = 19)
    private String createTime;

    @Column(name = "data_hash", length = 64)
    private String dataHash;

    @Column(name = "image_urls", columnDefinition = "text")
    private String imageUrls;
}
