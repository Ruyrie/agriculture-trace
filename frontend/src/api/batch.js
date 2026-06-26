/**
 * 批次管理 API 模块。
 *
 * 封装对后端 BatchController（/api/batch/*）的所有请求，供 BatchList.vue 调用。
 * 批次是溯源系统的核心业务单元，生产/质检/物流三类明细均通过 batch_id 关联。
 * 所有增删改操作会触发 BatchService 重算 dataHash 并写入区块链审计日志。
 */
import request from '@/utils/request'

/**
 * 分页查询批次列表。
 * 对应后端 GET /api/batch/list，支持多条件组合筛选。
 * BatchService.list() 根据参数的有无选择不同的 Repository 查询方法，数据库完成过滤。
 *
 * @param {{
 *   page: number,
 *   pageSize: number,
 *   productId?: string,
 *   productName?: string,
 *   batchNo?: string
 * }} params
 *   - productId 优先于 productName（有 productId 时忽略 productName）。
 *   - productId / productName / batchNo 可单独使用，也可组合使用。
 * @returns {Promise<{code: 200, data: { records: BatchRow[], total: number, page: number, pageSize: number }}>}
 *   records 中每条为 BatchRow（含 productId、productName 等展示字段，由 BatchService.toRow() 生成）。
 */
export const getBatchList = (params) => {
  return request.get('/batch/list', { params })
}

/**
 * 新增批次，并可同时提交生产、质检、物流明细数组。
 * 对应后端 POST /api/batch，整体在一个事务中执行（@Transactional）。
 * 后端 BatchService 会生成 batch_N 主键、创建时间和 dataHash，写入 CREATE 审计日志，
 * 并由 TraceDataService 保存三类溯源明细。
 *
 * @param {{
 *   batchNo: string,
 *   productId: string,
 *   productionDate: string,
 *   remark?: string,
 *   imageUrls?: string,
 *   productionRecords?: Array,
 *   inspectionRecords?: Array,
 *   logisticsRecords?: Array
 * }} data
 * @returns {Promise<{code: 200, data: BatchRow}>}
 */
export const addBatch = (data) => {
  return request.post('/batch', data)
}

/**
 * 更新批次基础信息；当 data 携带三类明细数组时，后端用覆盖式（先删后写）更新溯源明细。
 * 对应后端 PUT /api/batch，整体在一个事务中执行（@Transactional）。
 * 若请求体中不含 productionRecords/inspectionRecords/logisticsRecords 字段，则只更新批次基础字段，
 * 不影响已有溯源明细——这是 BatchController.update() 的 containsKey 判断逻辑所保证的。
 *
 * @param {{
 *   id: string,
 *   batchNo: string,
 *   productId: string,
 *   productionDate: string,
 *   remark?: string,
 *   imageUrls?: string,
 *   productionRecords?: Array,
 *   inspectionRecords?: Array,
 *   logisticsRecords?: Array
 * }} data
 * @returns {Promise<{code: 200, data: BatchRow}>}
 */
export const updateBatch = (data) => {
  return request.put('/batch', data)
}

/**
 * 删除指定批次。
 * 对应后端 DELETE /api/batch/{id}，删除前快照写入 DELETE 审计日志。
 * 关联的生产/质检/物流明细由 JPA 级联或数据库外键约束处理（具体行为见数据库 DDL）。
 *
 * @param {string} id - 批次 ID（格式 batch_N）。
 * @returns {Promise<{code: 200, data: null}>}
 */
export const deleteBatch = (id) => {
  return request.delete(`/batch/${id}`)
}
