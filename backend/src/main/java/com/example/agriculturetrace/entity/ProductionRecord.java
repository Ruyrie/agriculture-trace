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
 * 生产环节记录。
 *
 * 一条批次可以对应播种、施肥、采摘、入库等多条生产活动。
 */
@Getter
@Setter
@Entity
@Table(name = "production_record")
public class ProductionRecord {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "activity_name", nullable = false, length = 128)
    private String activityName;

    @Column(length = 64)
    private String operator;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Column(length = 256)
    private String remark;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
