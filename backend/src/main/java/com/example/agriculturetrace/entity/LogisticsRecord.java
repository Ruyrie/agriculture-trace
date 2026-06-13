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
 * 物流轨迹记录。
 *
 * 每条记录代表批次流转中的一个节点，例如产地入库、冷链运输、门店签收。
 */
@Getter
@Setter
@Entity
@Table(name = "logistics_record")
public class LogisticsRecord {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;

    @Column(length = 128)
    private String location;

    @Column(length = 64)
    private String operator;

    @Column(name = "update_time", length = 19)
    private String updateTime;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
