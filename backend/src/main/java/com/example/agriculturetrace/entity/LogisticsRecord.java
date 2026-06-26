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
 * 物流轨迹记录实体，对应数据库 logistics_record 表。
 *
 * 每条记录代表批次从田间到餐桌流转过程中的一个节点（如产地入库→冷链装车→运输→门店签收）。
 * 记录按 updateTime 升序排列（最新版本 LogisticsRecordRepository.findRowsByBatchId 使用升序），
 * 在溯源详情页形成物流时间线。
 *
 * 关联关系：
 *   - Batch（多对一，懒加载）：通过 batch_id 外键归属一个批次。
 *   - TraceDataService.saveLogisticsRecords() 批量创建。
 *   - TraceDataService.toLogisticsRow() 转换为溯源展示格式，保留 node/time 兼容别名。
 *   - LogisticsRecordRepository 提供按 productId / batchId 查询，支持覆盖式删除。
 */
@Getter
@Setter
@Entity
@Table(name = "logistics_record")
public class LogisticsRecord {

    /**
     * 记录唯一主键，32 位去横线 UUID，由 Ids.uuid32() 在 TraceDataService 中生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 所属批次，懒加载；Repository 查询时用 @EntityGraph 预加载以读取 batch.batchNo。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * 物流节点名称，如"产地冷库入库"、"运输车发车"，不能为空，最长 128 字符。
     * toLogisticsRow() 同时以 nodeName 和兼容别名 node 输出。
     */
    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;

    /**
     * 节点所在地点描述，如"山东烟台仓库"，最长 128 字符，可为空。
     * 溯源详情页（TraceDetail.vue）在物流时间线上展示此字段。
     */
    @Column(length = 128)
    private String location;

    /**
     * 操作人姓名或司机/签收人信息，最长 64 字符，可为空。
     */
    @Column(length = 64)
    private String operator;

    /**
     * 节点发生时间，格式 yyyy-MM-dd HH:mm:ss，最长 19 字符。
     * toLogisticsRow() 同时以 updateTime 和兼容别名 time 输出。
     * TraceDataService.notFutureDateTime() 校验此时间不能晚于当前服务器时间。
     * findRowsByBatchId() 按 updateTime 升序排列，形成从旧到新的物流时间线。
     */
    @Column(name = "update_time", length = 19)
    private String updateTime;

    /**
     * 前端录入顺序（从 1 开始），作为 updateTime 相同时的次级排序依据。
     * 由 TraceDataService.saveLogisticsRecords() 按列表索引 i+1 赋值。
     */
    @Column(name = "sort_order")
    private Integer sortOrder;
}
