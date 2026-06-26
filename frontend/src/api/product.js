/**
 * 产品管理 API 模块。
 *
 * 封装对后端 ProductController（/api/product/*）的所有请求，供 ProductList.vue 调用。
 * 所有增删改操作会触发后端 ProductService 重算 dataHash 并写入区块链审计日志。
 */
import request from '@/utils/request'

/**
 * 分页查询产品列表。
 * 对应后端 GET /api/product/list，支持关键字模糊搜索。
 *
 * @param {{ page: number, pageSize: number, keyword?: string }} params
 *   - page：当前页码，从 1 开始（后端内部转为 0-based）。
 *   - pageSize：每页条数，默认 10。
 *   - keyword：产品名称关键字，为空时查全部。
 * @returns {Promise<{code: 200, data: { records: Product[], total: number, page: number, pageSize: number }}>}
 */
export const getProductList = (params) => {
  return request.get('/product/list', { params })
}

/**
 * 新增产品基础信息。
 * 对应后端 POST /api/product，ProductService 会生成 prod_N 主键、创建时间和 dataHash，
 * 并写入 CREATE 审计日志。
 *
 * @param {{ name: string, category: string, origin: string, price: number, imageUrls: string }} data
 * @returns {Promise<{code: 200, data: Product}>} data 为带完整字段（含 id、dataHash）的产品实体。
 */
export const addProduct = (data) => {
  return request.post('/product', data)
}

/**
 * 更新产品基础信息。
 * 对应后端 PUT /api/product，若字段有变化则重算 dataHash 并写入 UPDATE 审计日志。
 * 无变化时后端直接返回原实体，不产生审计日志。
 *
 * @param {{ id: string, name: string, category: string, origin: string, price: number, imageUrls: string }} data
 * @returns {Promise<{code: 200, data: Product}>}
 */
export const updateProduct = (data) => {
  return request.put('/product', data)
}

/**
 * 删除产品。
 * 对应后端 DELETE /api/product/{id}，删除前先快照数据写入 DELETE 审计日志，
 * 再从数据库删除，方便事后审计查看被删对象原始内容。
 *
 * @param {string} id - 产品 ID（格式 prod_N）。
 * @returns {Promise<{code: 200, data: null}>}
 */
export const deleteProduct = (id) => {
  return request.delete(`/product/${id}`)
}

/**
 * 复合新增：一次性创建产品、首个批次和三类溯源明细。
 * 对应后端 POST /api/product/create-with-trace，整体在一个事务中执行，
 * 任一环节失败都会整体回滚，不会产生"产品有了但溯源不完整"的半成品。
 *
 * @param {{
 *   product: { name, category, origin, price, imageUrls },
 *   batch: { batchNo, productionDate, remark, imageUrls },
 *   productionRecords: Array<{ activityName, operator, activityDate, remark, imageUrls }>,
 *   inspectionRecords: Array<{ inspectionItem, result, inspector, inspectionDate }>,
 *   logisticsRecords: Array<{ nodeName, location, operator, updateTime }>
 * }} data
 * @returns {Promise<{code: 200, data: Product}>} data 为创建成功的产品实体。
 */
export const addProductWithTrace = (data) => {
  return request.post('/product/create-with-trace', data)
}
