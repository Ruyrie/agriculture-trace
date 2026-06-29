package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户意见反馈实体，对应数据库 feedback 表。
 *
 * 普通用户（农户/监管员/管理员）提交对系统的意见或问题，管理员统一查看并回复。
 * 反馈本身不属于业务溯源数据，因此不参与区块链指纹/审计链校验，独立成表管理。
 *
 * 状态流转：
 *   PENDING（待处理）── 管理员回复 ──▶ REPLIED（已回复）
 *   PENDING / REPLIED ── 管理员关闭 ──▶ CLOSED（已关闭）
 *
 * 关联关系：
 *   - FeedbackService 负责创建、查询、回复等业务逻辑。
 *   - FeedbackController 暴露普通用户提交/查看自己反馈、管理员管理全部反馈的接口。
 *   - FeedbackSchemaInitializer 在应用启动时建表（项目关闭了 Hibernate 自动建表）。
 */
@Getter
@Setter
@Entity
@Table(name = "feedback")
public class Feedback {

    /**
     * 反馈唯一主键，32 位去横线 UUID，由 Ids.uuid32() 生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 提交反馈的用户 ID，关联 user 表主键，不能为空。
     * 用于"我的反馈"列表只展示当前用户自己提交的记录。
     */
    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    /**
     * 提交人用户名，冗余存储便于管理员列表直接展示，避免每行再查 user 表。
     */
    @Column(length = 64)
    private String username;

    /**
     * 反馈类型，固定值 BUG（问题报告）/ SUGGESTION（功能建议）/ OTHER（其他），最长 20 字符。
     * 供管理员按类型筛选，以及汇总统计各类反馈数量。
     */
    @Column(length = 20)
    private String type;

    /**
     * 反馈标题（简述），最长 128 字符，不能为空。
     * 列表页和汇总卡片展示标题，便于管理员快速扫读。
     */
    @Column(nullable = false, length = 128)
    private String title;

    /**
     * 反馈正文详情（TEXT 类型），不能为空。
     * 详情弹窗展示完整内容。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 处理状态，固定值 PENDING（待处理）/ REPLIED（已回复）/ CLOSED（已关闭），默认 PENDING。
     * 管理员回复后置为 REPLIED，关闭后置为 CLOSED。
     */
    @Column(length = 20)
    private String status = "PENDING";

    /**
     * 管理员回复内容（TEXT 类型），未回复时为 null。
     * 提交用户在"我的反馈"中可查看管理员的回复。
     */
    @Column(columnDefinition = "TEXT")
    private String reply;

    /**
     * 回复人用户名（管理员），未回复时为 null，最长 64 字符。
     */
    @Column(name = "reply_by", length = 64)
    private String replyBy;

    /**
     * 回复时间，格式 yyyy-MM-dd HH:mm:ss，未回复时为 null，最长 19 字符。
     */
    @Column(name = "reply_time", length = 19)
    private String replyTime;

    /**
     * 提交时间，格式 yyyy-MM-dd HH:mm:ss，由 TimeUtils.nowText() 生成，最长 19 字符。
     * 列表默认按此字段倒序展示，最新反馈在最上方。
     */
    @Column(name = "create_time", length = 19)
    private String createTime;
}
