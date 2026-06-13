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

/**
 * 溯源访问记录。
 *
 * 每次访问溯源页时记录产品、访问时间和 IP，供仪表盘趋势统计使用。
 */
@Getter
@Setter
@Entity
@Table(name = "trace_record")
public class TraceRecord {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "trace_time", length = 19)
    private String traceTime;

    @Column(length = 64)
    private String ip;
}
