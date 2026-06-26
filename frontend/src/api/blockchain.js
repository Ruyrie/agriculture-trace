/**
 * 区块链审计日志 API 模块。
 *
 * 封装对后端 BlockchainLogController（/api/blockchain/*）的请求，供 AuditLog.vue 调用。
 * 审计日志通过 previous_hash → data_hash 的链式结构模拟区块链不可篡改日志。
 * 日志权限受限于 ROLE_ADMIN 和 ROLE_INSPECTOR（见 SecurityConfig）。
 */
import request from '@/utils/request'

/**
 * 分页获取区块链式审计日志。
 * 对应后端 GET /api/blockchain/logs，支持多字段动态过滤。
 * 后端按 id（时间前缀单调递增主键）升序返回，展示顺序与链条写入顺序一致。
 *
 * @param {{
 *   page: number,
 *   pageSize: number,
 *   actionType?: string,    // CREATE / UPDATE / DELETE
 *   targetType?: string,    // PRODUCT / BATCH
 *   operator?: string,      // 操作人用户名，模糊匹配
 *   targetId?: string,      // 目标 ID，模糊匹配
 *   startTime?: string,     // 开始时间 yyyy-MM-dd HH:mm:ss，字典序过滤
 *   endTime?: string        // 结束时间 yyyy-MM-dd HH:mm:ss
 * }} params
 * @returns {Promise<{code: 200, data: {
 *   records: BlockchainLogRow[],
 *   total: number, page: number, pageSize: number
 * }}>}
 */
export const getAuditLogs = (params) => {
  return request.get('/blockchain/logs', { params })
}

/**
 * 触发后端三层完整性校验：
 *   1. 日志链校验：逐条遍历所有日志，验证 dataHash 和 previousHash 的连续性。
 *   2. 链尾锚点校验：将当前日志总数和链尾哈希与 blockchain_anchor 表的基线对比，
 *      检测"从链尾整段删除最新日志"的截断攻击。
 *   3. 业务数据指纹校验：对所有产品和批次重算当前指纹，与数据库 dataHash 字段比较，
 *      发现绕过系统直接改库的篡改行为。
 *
 * @returns {Promise<{code: 200, data: {
 *   valid: boolean,
 *   logChainValid: boolean,
 *   tailValid: boolean,
 *   expectedTotal: number | null,
 *   dataIntegrityValid: boolean,
 *   total: number,
 *   invalidCount: number,
 *   invalidItems: Array,
 *   brokenIndex?: number,
 *   brokenLogId?: string,
 *   message: string
 * }}>}
 */
export const verifyAuditLogChain = () => {
  return request.get('/blockchain/logs/verify')
}
