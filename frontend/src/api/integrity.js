/**
 * 数据指纹与完整性校验 API 模块。
 *
 * 封装对后端 IntegrityController（/api/integrity/*）的请求，供 IntegrityReport.vue 调用。
 * 所有接口均受 SecurityConfig 限制，仅 ROLE_ADMIN 和 ROLE_INSPECTOR 可访问。
 *
 * 数据指纹原理：
 *   每条产品/批次记录保存一个 dataHash（SHA-256 指纹），由固定字段拼接后计算。
 *   完整性校验接口对当前数据库记录重新计算指纹，若与 dataHash 不符，
 *   说明该记录被绕过系统（直接改数据库）篡改过。
 *   与审计日志链校验互补：日志链检测"操作记录是否完整"，指纹检测"业务数据本身是否被改"。
 */
import request from '@/utils/request'

/**
 * 获取所有产品的数据指纹列表和全局根哈希（rootHash）。
 * 对应后端 GET /api/integrity/fingerprints（别名 /api/integrity/products）。
 * 每行包含 storedHash（数据库保存值）、currentHash（即时重算值）和 valid（是否一致）。
 * rootHash 是将所有产品 currentHash 按固定顺序串联后再 SHA-256 的全局摘要。
 *
 * @returns {Promise<{code: 200, data: {
 *   records: Array<{id, name, category, origin, storedHash, currentHash, valid}>,
 *   total: number,
 *   rootHash: string,
 *   generatedAt: string
 * }}>}
 */
export const getProductFingerprints = () => {
  return request.get('/integrity/fingerprints')
}

/**
 * 只获取全局根哈希摘要，不返回逐条产品指纹。
 * 供需要轻量校验全局产品集合是否有变动的场景使用。
 *
 * @returns {Promise<{code: 200, data: { rootHash: string, total: number, generatedAt: string }}>}
 */
export const getRootHash = () => {
  return request.get('/integrity/root-hash')
}

/**
 * 校验单个产品的当前字段哈希是否与数据库 storedHash 一致。
 * 对应后端 GET /api/integrity/verify/{id}（别名 /api/integrity/product/{id}/verify）。
 * 用于产品表格"验证"按钮的单条校验。
 *
 * @param {string} id - 产品 ID（格式 prod_N）。
 * @returns {Promise<{code: 200, data: { id, name, storedHash, currentHash, valid: boolean }}>}
 */
export const verifyProductHash = (id) => {
  return request.get(`/integrity/verify/${id}`)
}

/**
 * 批量校验所有产品指纹，只返回异常项。
 * 对应后端 GET /api/integrity/products/verify，用于产品管理页"一键验证"按钮。
 *
 * @returns {Promise<{code: 200, data: {
 *   valid: boolean,
 *   total: number,
 *   invalidCount: number,
 *   invalidItems: Array<{id, name, category, origin, storedHash, currentHash}>
 * }}>}
 */
export const verifyAllProductHashes = () => {
  return request.get('/integrity/products/verify')
}

/**
 * 校验单个批次的当前字段哈希是否与数据库 storedHash 一致。
 * 对应后端 GET /api/integrity/batch/{id}/verify，用于批次表格"验证"按钮。
 *
 * @param {string} id - 批次 ID（格式 batch_N）。
 * @returns {Promise<{code: 200, data: { id, batchNo, storedHash, currentHash, valid: boolean }}>}
 */
export const verifyBatchHash = (id) => {
  return request.get(`/integrity/batch/${id}/verify`)
}

/**
 * 批量校验所有批次指纹，只返回异常项。
 * 对应后端 GET /api/integrity/batches/verify。
 *
 * @returns {Promise<{code: 200, data: {
 *   valid: boolean,
 *   total: number,
 *   invalidCount: number,
 *   invalidItems: Array<{id, batchNo, productName, productionDate, storedHash, currentHash}>
 * }}>}
 */
export const verifyAllBatchHashes = () => {
  return request.get('/integrity/batches/verify')
}
