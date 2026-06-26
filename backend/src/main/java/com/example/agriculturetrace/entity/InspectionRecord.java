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
 * 质检记录实体，对应数据库 inspection_record 表。
 *
 * 记录批次产品在生产过程中接受的质量检验事项，包括检验项目、检验结果和检验人员。
 * 一个批次可包含多条质检记录（如农残检测、重金属检测、感官检验等）。
 * inspectionDate 为 varchar(19) 格式 yyyy-MM-dd HH:mm:ss，
 * BlockchainSchemaInitializer 在启动时将旧 date 列补全时分秒。
 *
 * 关联关系：
 *   - Batch（多对一，懒加载）：通过 batch_id 外键归属一个批次。
 *   - TraceDataService.saveInspectionRecords() 批量创建。
 *   - TraceDataService.toInspectionRow() 转换为溯源展示格式，保留 item/date 兼容别名。
 *   - InspectionRecordRepository 提供按 productId / batchId 查询，支持覆盖式删除。
 */
@Getter
@Setter
@Entity
@Table(name = "inspection_record")
public class InspectionRecord {

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
     * 检验项目名称，如"农药残留"、"重金属含量"，不能为空，最长 128 字符。
     * toInspectionRow() 同时以 inspectionItem 和兼容别名 item 输出，供新旧前端字段名共用。
     */
    @Column(name = "inspection_item", nullable = false, length = 128)
    private String inspectionItem;

    /**
     * 检验结论，如"合格"、"未检出超标"，最长 128 字符，可为空。
     * 溯源详情页（TraceDetail.vue）在质检时间线上展示此字段。
     */
    @Column(length = 128)
    private String result;

    /**
     * 检验人员姓名或机构名，最长 64 字符，可为空。
     */
    @Column(length = 64)
    private String inspector;

    /**
     * 检验时间，格式 yyyy-MM-dd HH:mm:ss，最长 19 字符，可为空。
     * toInspectionRow() 同时以 inspectionDate 和兼容别名 date 输出。
     * TraceDataService.notFutureDateTime() 校验此时间不能晚于当前服务器时间。
     */
    @Column(name = "inspection_date", length = 19)
    private String inspectionDate;

    /**
     * 质检现场图片 URL 列表，JSON 数组字符串，格式与生产记录 imageUrls 相同。
     * 前端通过 ImageUploadGrid 上传，后端存为 TEXT；不参与数据指纹计算。
     */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    /**
     * 前端录入顺序（从 1 开始），控制溯源详情页质检记录的展示顺序。
     * 由 TraceDataService.saveInspectionRecords() 按列表索引 i+1 赋值。
     */
    @Column(name = "sort_order")
    private Integer sortOrder;
}
