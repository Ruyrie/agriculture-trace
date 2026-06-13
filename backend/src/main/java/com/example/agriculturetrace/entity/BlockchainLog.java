package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 区块链审计日志。
 *
 * 每条日志保存上一条日志哈希和自身哈希，形成可验证的模拟链。
 */
@Getter
@Setter
@Entity
@Table(name = "blockchain_log")
public class BlockchainLog {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 32)
    private String targetId;

    @Column(nullable = false, length = 64)
    private String operator;

    @Column(name = "data_before", columnDefinition = "TEXT")
    private String dataBefore;

    @Column(name = "data_after", columnDefinition = "TEXT")
    private String dataAfter;

    @Column(name = "data_hash", nullable = false, length = 64)
    private String dataHash;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(nullable = false, length = 19)
    private String timestamp;
}
