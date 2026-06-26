package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 区块链审计日志实体，对应数据库 blockchain_log 表。
 *
 * 每条日志通过 previousHash → dataHash 构成链式结构，模拟区块链不可篡改日志：
 *   - 插入中间日志：链条在插入位置哈希不连续，校验即发现。
 *   - 修改已有日志：重算 dataHash 与存储值不符，校验即发现。
 *   - 从链尾截断：哈希链本身仍连续，但 blockchain_anchor 表记录的期望条数/链尾哈希会揭露。
 *
 * 主键 id 使用 Ids.logId() 生成（毫秒时间戳前缀 + 自增序列 + 随机后缀），
 * 保证按 id 字典序排序等价于写入时序，是链式校验遍历的标准顺序。
 *
 * 关联关系：
 *   - ProductService.recordLog() 在产品 CREATE/UPDATE/DELETE 时写入。
 *   - BatchService.recordLog() 在批次 CREATE/UPDATE/DELETE 时写入。
 *   - BlockchainLogController.verify() 遍历全量日志做链式校验。
 *   - BlockchainAnchorService 在每次合法写入后刷新链尾锚点。
 *   - BlockchainLogRepository 提供分页查询（含 JpaSpecificationExecutor 动态过滤）和链尾查询。
 */
@Getter
@Setter
@Entity
@Table(name = "blockchain_log")
public class BlockchainLog {

    /**
     * 日志主键，由 Ids.logId() 生成：13位毫秒时间戳 + 6位进程内自增序列 + 13位随机十六进制。
     * 按此字段升序排序等价于按写入时序排序，链式校验遍历依赖这一特性。
     * 不能使用随机 UUID，否则同一毫秒内的日志会排序歧义导致误报链条断裂。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 操作类型，固定值为 CREATE / UPDATE / DELETE，长度最大 20 字符，不能为空。
     * BlockchainLogController 前端筛选框、BlockchainLogController.verify() 哈希计算均使用此字段。
     */
    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    /**
     * 操作目标类型，固定值为 PRODUCT / BATCH，长度最大 20 字符，不能为空。
     * 用于前端按目标类型筛选日志，也是日志 dataHash 计算的输入之一（间接通过 targetId 区分）。
     */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    /**
     * 操作目标的业务 ID（产品 ID 或批次 ID），最长 32 字符，不能为空。
     * BlockchainLogController.verify() 在重算 dataHash 时使用此字段。
     * 前端可按 targetId 过滤查看某一产品/批次的全部操作历史。
     */
    @Column(name = "target_id", nullable = false, length = 32)
    private String targetId;

    /**
     * 操作人用户名，最长 64 字符，不能为空。
     * 由 ProductService/BatchService 的 currentOperator() 从 Spring Security 上下文读取。
     * 系统启动初始化日志时使用固定值 "system"。
     * 参与 dataHash 计算。
     */
    @Column(nullable = false, length = 64)
    private String operator;

    /**
     * 操作前业务数据的 JSON 快照（TEXT 类型），CREATE 时为 null。
     * 由 ProductService.toAuditRow() / BatchService.toAuditRow() 序列化生成。
     * 不参与 dataHash 计算（只用 dataAfter 参与）。
     */
    @Column(name = "data_before", columnDefinition = "TEXT")
    private String dataBefore;

    /**
     * 操作后业务数据的 JSON 快照（TEXT 类型），DELETE 时为 null。
     * 参与 dataHash 计算：content = actionType + targetId + operator + timestamp + previousHash + dataAfter。
     * 前端审计日志详情弹窗展示 dataAfter 内容，供管理员核查每次变更的具体值。
     */
    @Column(name = "data_after", columnDefinition = "TEXT")
    private String dataAfter;

    /**
     * 本条日志的数据哈希（SHA-256，64 位十六进制），不能为空。
     * 由 HashUtil.sha256(actionType + targetId + operator + timestamp + previousHash + dataAfter) 计算。
     * 校验时重新计算并与此值比较：不一致说明本条日志被直接改库篡改。
     * 同时作为下一条日志的 previousHash，构成链式结构。
     */
    @Column(name = "data_hash", nullable = false, length = 64)
    private String dataHash;

    /**
     * 上一条日志的 dataHash，64 位十六进制；第一条日志（创世块）固定为 "0"。
     * 校验时将"已校验到的上一条日志 dataHash"与此字段比较：
     * 不一致说明中间有日志被删除、插入或顺序被破坏。
     */
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    /**
     * 操作时间，格式 yyyy-MM-dd HH:mm:ss，不能为空，长度 19 字符。
     * 按字典序比较等价于按时间顺序比较，供前端时间范围筛选使用。
     * 参与 dataHash 计算。
     * 注意：不能用此字段排序日志（本地墙钟，跨时区/夏令时可能乱序），必须用 id 排序。
     */
    @Column(nullable = false, length = 19)
    private String timestamp;
}
