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
 * 溯源访问记录实体，对应数据库 trace_record 表。
 *
 * 每次消费者或监管人员访问溯源详情页（TraceDetail.vue 或 /api/trace/{productId}），
 * TraceController.recordVisit() 就会创建一条本实体记录。
 * 这些记录被 StatisticsMapper 聚合为：
 *   - 近 7 天溯源趋势（traceTrend，用于仪表盘折线图）
 *   - 总溯源访问次数（countTraceRecords，用于仪表盘数字卡片）
 *   - 溯源访问产品排行（traceRanking，用于统计分析页）
 *
 * 不做分页管理和删除功能——这是只增不删的操作日志，数据量会持续增长。
 *
 * 关联关系：
 *   - Product（多对一，懒加载）：记录哪个产品被访问，通过 product_id 外键引用。
 *   - TraceController 在每次 /api/trace/{productId} 和 /api/trace/batch/{batchId} 请求时创建本实体。
 *   - TraceRecordRepository 仅继承 JpaRepository，无额外自定义查询（聚合交给 MyBatis StatisticsMapper）。
 */
@Getter
@Setter
@Entity
@Table(name = "trace_record")
public class TraceRecord {

    /**
     * 访问记录唯一主键，32 位去横线 UUID，由 Ids.uuid32() 在 TraceController.recordVisit() 中生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 被访问的产品，懒加载（TraceController 写入时已持有 Product 实体，无需再查）。
     * 通过 product_id 外键关联 product 表，product 被删除时需注意此表的孤儿记录。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 溯源访问时间，格式 yyyy-MM-dd HH:mm:ss，由 TimeUtils.nowText() 生成。
     * StatisticsMapper.traceTrend() 按此字段的日期部分聚合近 7 天访问量。
     */
    @Column(name = "trace_time", length = 19)
    private String traceTime;

    /**
     * 访问者客户端 IP 地址，由 HttpServletRequest.getRemoteAddr() 获取，最长 64 字符。
     * 当前仅记录，未做限频或分析使用。反向代理部署时可能获取到代理 IP，需留意。
     */
    @Column(length = 64)
    private String ip;
}
