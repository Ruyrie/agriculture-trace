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
 * 产品批次实体，对应数据库 batch 表。
 *
 * 批次是溯源系统的核心业务单元：生产记录（ProductionRecord）、质检记录（InspectionRecord）
 * 和物流轨迹（LogisticsRecord）都通过 batch_id 外键挂在具体批次上。
 * 批次号（batchNo）是面向业务人员和 QR 码扫码的可读标识，必须全局唯一。
 * dataHash 按 BatchService.computeBatchHash() 规则计算，用于完整性校验，
 * 其中使用 product.id 而非 productName，保证产品改名不影响已有批次指纹。
 *
 * 关联关系：
 *   - Product（多对一）：每个批次归属一个产品，通过 product_id 外键引用。
 *   - ProductionRecord（一对多，非直接持有）：通过 ProductionRecordRepository 查询。
 *   - InspectionRecord（一对多，非直接持有）：通过 InspectionRecordRepository 查询。
 *   - LogisticsRecord（一对多，非直接持有）：通过 LogisticsRecordRepository 查询。
 *   - BatchService 负责 CRUD、哈希计算和审计日志写入。
 *   - BatchRepository 提供 JPA 查询，多处使用 @EntityGraph 预加载 product 避免懒加载异常。
 */
@Getter
@Setter
@Entity
@Table(name = "batch")
public class Batch {

    /**
     * 批次唯一主键，格式为 batch_N（如 batch_1、batch_2）。
     * 由 BatchService.nextBatchId() 按库中最大序号 +1 生成，保证可读性和字典序一致。
     * 参与 dataHash 指纹计算，必须在计算前确定。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 批次号，全局唯一，最长 64 字符，不能为空。
     * 对应数据库唯一约束，批次号重复时数据库会抛 DataIntegrityViolationException，
     * 由 GlobalExceptionHandler 转换为友好提示。
     * BatchService.ensureBatchNoAvailable() 在写库前做应用层预检验，提前给出友好提示。
     * 参与 dataHash 指纹计算。
     */
    @Column(name = "batch_no", nullable = false, unique = true, length = 64)
    private String batchNo;

    /**
     * 所属产品，懒加载（LAZY）。
     * 查询批次列表或溯源详情时，BatchRepository 方法使用 @EntityGraph(attributePaths="product")
     * 提前加载，避免在 Service/Controller 层访问 product 时触发懒加载异常。
     * 参与 dataHash 指纹计算（只用 product.id，不用 productName）。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 生产日期（年月日），由 BatchService.ensureNotFutureProductionDate() 校验不能晚于今天。
     * 参与 dataHash 指纹计算（转换为 YYYY-MM-DD 字符串后拼接）。
     * 批次列表默认按生产日期倒序展示。
     */
    @Column(name = "production_date")
    private LocalDate productionDate;

    /**
     * 批次备注说明，最长 256 字符，可为空。
     * 溯源详情页（TraceDetail.vue）会展示此备注。
     * 参与 dataHash 指纹计算。
     */
    @Column(length = 256)
    private String remark;

    /**
     * 批次创建时间，格式 yyyy-MM-dd HH:mm:ss（TimeUtils.nowText() 生成）。
     * 参与 dataHash 指纹计算，保存后不再更新。
     */
    @Column(name = "create_time", length = 19)
    private String createTime;

    /**
     * 批次业务数据指纹（SHA-256，64 位十六进制）。
     * 由 BatchService.computeBatchHash() 按 id|batchNo|productId|productionDate|remark|createTime 顺序计算。
     * 用于完整性校验，发现绕过系统直接改库的篡改行为。
     */
    @Column(name = "data_hash", length = 64)
    private String dataHash;

    /**
     * 批次图片 URL 列表，JSON 数组字符串，与 Product.imageUrls 格式相同。
     * 不参与 dataHash 计算，可随时补充图片而不影响完整性校验。
     */
    @Column(name = "image_urls", columnDefinition = "text")
    private String imageUrls;
}
