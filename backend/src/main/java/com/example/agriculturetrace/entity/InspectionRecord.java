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
 * 质检记录。
 *
 * 记录检测项目、检测结果和检测人员，用于监管员核验产品质量。
 */
@Getter
@Setter
@Entity
@Table(name = "inspection_record")
public class InspectionRecord {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "inspection_item", nullable = false, length = 128)
    private String inspectionItem;

    @Column(length = 128)
    private String result;

    @Column(length = 64)
    private String inspector;

    @Column(name = "inspection_date")
    private LocalDate inspectionDate;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
