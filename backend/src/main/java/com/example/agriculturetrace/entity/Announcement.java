package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统公告实体，对应数据库 announcement 表。
 *
 * 管理员发布面向全体用户的系统公告（维护通知、规则变更等）。
 * 普通用户只能浏览"已发布"的公告，管理员可管理草稿与发布、置顶、下线。
 *
 * 状态：DRAFT（草稿，仅管理员可见）/ PUBLISHED（已发布，全员可见）。
 *
 * 关联关系：
 *   - AnnouncementService 负责 CRUD、发布/下线和置顶。
 *   - AnnouncementController 暴露公告浏览（全员）与公告管理（仅管理员）接口。
 *   - AnnouncementSchemaInitializer 在启动时建表。
 */
@Getter
@Setter
@Entity
@Table(name = "announcement")
public class Announcement {

    /**
     * 公告唯一主键，32 位去横线 UUID，由 Ids.uuid32() 生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 公告标题，最长 128 字符，不能为空。
     */
    @Column(nullable = false, length = 128)
    private String title;

    /**
     * 公告正文（TEXT 类型），不能为空。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 发布状态，固定值 DRAFT（草稿）/ PUBLISHED（已发布），默认 PUBLISHED。
     * 普通用户列表只返回 PUBLISHED；DRAFT 仅管理端可见。
     */
    @Column(length = 20)
    private String status = "PUBLISHED";

    /**
     * 是否置顶，默认 false。置顶公告在列表中排在最前。
     */
    private Boolean pinned = false;

    /**
     * 发布人用户名（管理员），最长 64 字符。
     */
    @Column(length = 64)
    private String creator;

    /**
     * 创建时间，格式 yyyy-MM-dd HH:mm:ss，由 TimeUtils.nowText() 生成，最长 19 字符。
     */
    @Column(name = "create_time", length = 19)
    private String createTime;

    /**
     * 最近更新时间，格式 yyyy-MM-dd HH:mm:ss，每次编辑时刷新，最长 19 字符。
     */
    @Column(name = "update_time", length = 19)
    private String updateTime;
}
