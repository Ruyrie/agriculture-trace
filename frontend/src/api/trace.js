/**
 * 产品溯源详情 API 模块。
 *
 * 封装对后端 TraceController（/api/trace/*）的请求，供 TraceDetail.vue 调用。
 * 溯源接口无需登录（SecurityConfig 中 /api/trace/** 路径已 permitAll），
 * 消费者扫描产品 QR 码可直接访问，不强制要求账号。
 * 每次访问后端会写入 trace_record，用于统计溯源访问次数和趋势。
 */
import request from '@/utils/request'

/**
 * 按产品 ID 获取产品基础详情（仅产品字段，不含溯源记录）。
 * 对应后端 GET /api/product/{id}，供编辑弹窗预填和辅助信息展示使用。
 *
 * @param {string} id - 产品 ID（格式 prod_N）。
 * @returns {Promise<{code: 200, data: Product}>}
 */
export const getProductDetail = (id) => {
  return request.get(`/product/${id}`)
}

/**
 * 按产品 ID 获取完整公开溯源详情。
 * 对应后端 GET /api/trace/{productId}，返回该产品所有批次及其三类溯源记录。
 * 每次调用都会在 trace_record 表写入一条访问记录（TraceController.recordVisit）。
 *
 * @param {string} id - 产品 ID（格式 prod_N）。
 * @returns {Promise<{code: 200, data: {
 *   product: Product,
 *   batches: Array<{batchNo, productionDate, remark, imageUrls}>,
 *   productionRecords: Array,
 *   inspectionReports: Array,
 *   logistics: Array
 * }}>}
 */
export const getTraceInfo = (id) => {
  return request.get(`/trace/${id}`)
}

/**
 * 按批次 ID 获取公开溯源详情（只含当前批次的记录，不含其他批次）。
 * 对应后端 GET /api/trace/batch/{batchId}，适用于 QR 码直接指向批次维度的场景。
 * 每次调用同样写入 trace_record 访问记录（以批次所属产品为维度统计）。
 *
 * @param {string} batchId - 批次 ID（格式 batch_N）。
 * @returns {Promise<{code: 200, data: {
 *   product: Product,
 *   batches: Array<{batchNo, productionDate, remark, imageUrls}>,
 *   productionRecords: Array,
 *   inspectionReports: Array,
 *   logistics: Array
 * }}>}
 */
export const getBatchTraceInfo = (batchId) => {
  return request.get(`/trace/batch/${batchId}`)
}
