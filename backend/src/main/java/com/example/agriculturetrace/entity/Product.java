package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 农产品基础信息实体，对应数据库 product 表。
 *
 * 每条产品记录包含业务字段（名称、类别、产地、价格）和两个系统字段（创建时间、数据指纹）。
 * dataHash 是由 ProductService.computeProductHash() 按固定字段顺序计算的 SHA-256 摘要，
 * 用于后续通过 IntegrityController / BlockchainLogController 的校验接口检测是否被直接改库篡改。
 * imageUrls 存储 JSON 数组字符串，不参与指纹计算，避免补图影响历史哈希一致性。
 *
 * 关联关系：
 *   - Batch 通过 product_id 外键引用本实体（@ManyToOne）。
 *   - TraceRecord 通过 product_id 外键记录每次溯源访问。
 *   - ProductService 负责创建、更新、删除本实体，并在每次变更时更新 dataHash 并写审计日志。
 *   - ProductRepository 提供 JPA 查询支持。
 */
@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {

    /**
     * 产品唯一主键，格式为 prod_N（如 prod_1、prod_2）。
     * 由 ProductService.nextProductId() 按库中最大序号 +1 生成，保证可读性。
     * 长度 32 位，与数据库 varchar(32) 对应。
     * 该字段参与 dataHash 计算，必须在计算前确定。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 产品名称，不能为空，最长 128 字符。
     * 产品列表搜索（ProductRepository.findByNameContainingIgnoreCase）按此字段模糊匹配。
     * 参与 dataHash 指纹计算（ProductService.computeProductHash）。
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * 产品类别，如"蔬菜"、"水果"、"粮食"等，最长 64 字符。
     * DashboardController.categoryDistribution() 按此字段聚合统计供首页饼图使用。
     * 参与 dataHash 指纹计算。
     */
    @Column(length = 64)
    private String category;

    /**
     * 产品产地，如"山东烟台"、"云南昆明"等，最长 128 字符。
     * ProductController 提供 /api/product/origins 接口返回国内外产地候选列表。
     * 参与 dataHash 指纹计算。
     */
    @Column(length = 128)
    private String origin;

    /**
     * 产品参考价格，单位由前端展示层决定（通常为元/斤）。
     * 参与 dataHash 指纹计算，计算时用 stripTrailingZeros().toPlainString() 标准化，
     * 确保 12.50 与 12.5 产生相同哈希。
     */
    private BigDecimal price;

    /**
     * 产品创建时间，格式固定为 yyyy-MM-dd HH:mm:ss（由 TimeUtils.nowText() 生成）。
     * 长度 19 字符对应该格式。
     * 参与 dataHash 指纹计算，保存后不再更新。
     */
    @Column(name = "create_time", length = 19)
    private String createTime;

    /**
     * 产品业务数据指纹（SHA-256，64 位十六进制）。
     * 由 ProductService.computeProductHash() 按 id|name|category|origin|price|createTime 顺序计算。
     * BlockchainSchemaInitializer 启动时对历史记录用 SQL 的 SHA2 函数回填相同结果。
     * IntegrityController 和 BlockchainLogController 通过重新计算并比较该字段来检测篡改。
     */
    @Column(name = "data_hash", length = 64)
    private String dataHash;

    /**
     * 产品图片 URL 列表，JSON 数组字符串，最多 9 张（由 parseImageUrls 工具函数限制）。
     * 格式示例：["/uploads/trace-images/xxx.jpg", ...]
     * 不参与 dataHash 指纹计算，可随时补图而不影响历史完整性校验。
     * 前端通过 images.js 的 parseImageUrls / resolveImageUrl 解析和展示。
     */
    @Column(name = "image_urls", columnDefinition = "text")
    private String imageUrls;
}
