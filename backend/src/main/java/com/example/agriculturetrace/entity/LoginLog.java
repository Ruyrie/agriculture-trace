package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录日志实体，对应数据库 login_log 表。
 *
 * 记录每一次登录尝试（成功或失败），用于安全审计与异常登录排查。
 * 与 blockchain_log（业务变更链）不同，这里只记录认证事件，不参与防篡改链式校验。
 *
 * 写入时机：
 *   - SecurityConfig 的 successHandler 在登录成功后写入一条 SUCCESS 记录。
 *   - SecurityConfig 的 failureHandler 在登录失败后写入一条 FAILURE 记录（含失败原因）。
 *
 * 关联关系：
 *   - LoginLogService 负责记录与分页查询。
 *   - LoginLogController 暴露管理员/监管员查看接口（/api/login-logs）。
 *   - LoginLogSchemaInitializer 在启动时建表（项目关闭了 Hibernate 自动建表）。
 */
@Getter
@Setter
@Entity
@Table(name = "login_log")
public class LoginLog {

    /**
     * 日志主键，32 位去横线 UUID，由 Ids.uuid32() 生成。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 登录使用的用户名，最长 64 字符。
     * 失败时也记录用户输入的用户名（即使该账号不存在），便于排查撞库/猜测攻击。
     */
    @Column(length = 64)
    private String username;

    /**
     * 客户端 IP 地址，最长 64 字符（兼容 IPv6）。
     * 优先取 X-Forwarded-For 首个地址，回退到 request.getRemoteAddr()。
     */
    @Column(length = 64)
    private String ip;

    /**
     * 登录结果，固定值 SUCCESS（成功）/ FAILURE（失败），最长 16 字符。
     */
    @Column(length = 16)
    private String status;

    /**
     * 结果说明：成功时为"登录成功"，失败时为具体原因（密码错误/账号禁用/账号不存在等），最长 128 字符。
     */
    @Column(length = 128)
    private String message;

    /**
     * 浏览器 User-Agent 摘要，最长 256 字符，可为空。
     * 辅助识别登录设备/客户端类型。
     */
    @Column(name = "user_agent", length = 256)
    private String userAgent;

    /**
     * 登录时间，格式 yyyy-MM-dd HH:mm:ss，由 TimeUtils.nowText() 生成，最长 19 字符。
     * 列表默认按此字段倒序展示，最新登录在最上方。
     */
    @Column(name = "login_time", length = 19)
    private String loginTime;
}
