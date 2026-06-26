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
 * 生产环节记录实体，对应数据库 production_record 表。
 *
 * 一个批次可以包含多条生产活动（如播种→施肥→采摘→入库），每条记录描述一个具体活动节点。
 * 记录按 sortOrder 字段排序，保留前端录入顺序，以便溯源详情页按真实业务顺序展示。
 * activityDate 存储 yyyy-MM-dd HH:mm:ss 字符串（由 BlockchainSchemaInitializer 从旧 date 类型迁移），
 * TraceDataService.notFutureDateTime() 校验其不能晚于当前服务器时间。
 *
 * 关联关系：
 *   - Batch（多对一，懒加载）：通过 batch_id 外键归属一个批次。
 *   - TraceDataService.saveProductionRecords() 负责批量创建本实体。
 *   - TraceDataService.toProductionRow() 将本实体转换为溯源详情展示格式。
 *   - ProductionRecordRepository 提供按 productId / batchId 查询，并支持覆盖式删除。
 */
@Getter
@Setter
@Entity
@Table(name = "production_record")
public class ProductionRecord {

    /**
     * 记录唯一主键，32 位去横线 UUID，由 Ids.uuid32() 在 TraceDataService 中生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 所属批次，懒加载。
     * ProductionRecordRepository 的查询方法使用 @EntityGraph(attributePaths="batch") 预加载，
     * 确保 TraceDataService.toProductionRow() 访问 batch.batchNo 时不触发懒加载异常。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * 生产活动名称，如"播种"、"灌溉"、"采摘"，不能为空，最长 128 字符。
     * 溯源详情页（TraceDetail.vue）在时间线上展示此字段。
     */
    @Column(name = "activity_name", nullable = false, length = 128)
    private String activityName;

    /**
     * 操作人姓名或工号，最长 64 字符，可为空。
     * 不与 User 实体关联（溯源记录允许记录非系统用户的外部人员）。
     */
    @Column(length = 64)
    private String operator;

    /**
     * 活动发生时间，格式 yyyy-MM-dd HH:mm:ss，最长 19 字符。
     * TraceDataService.notFutureDateTime() 校验此时间不能晚于当前服务器时间。
     * BlockchainSchemaInitializer 在启动时将旧 date 列（YYYY-MM-DD）补全为 HH:mm:ss 格式。
     */
    @Column(name = "activity_date", length = 19)
    private String activityDate;

    /**
     * 活动备注说明，最长 256 字符，可为空。
     * 溯源详情页展示此字段为补充信息。
     */
    @Column(length = 256)
    private String remark;

    /**
     * 前端录入顺序（从 1 开始），用于溯源详情页按录入顺序排列活动节点。
     * 由 TraceDataService.saveProductionRecords() 按列表索引 i+1 赋值。
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 生产活动现场图片 URL 列表，JSON 数组字符串，可为空。
     * 格式与 Product.imageUrls 相同，由 images.js 的 parseImageUrls() 解析展示。
     */
    @Column(name = "image_urls", columnDefinition = "text")
    private String imageUrls;
}
