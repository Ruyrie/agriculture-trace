/**
 * 意见反馈 API 模块。
 *
 * 封装对后端 FeedbackController（/api/feedback/*）的请求，供 Feedback.vue 调用。
 *
 * 权限说明（与后端 SecurityConfig 一致）：
 *   - submitFeedback / getMyFeedback：任意已登录用户（农户/监管员/管理员）。
 *   - getFeedbackList / getFeedbackSummary / replyFeedback / closeFeedback / deleteFeedback：
 *     仅管理员（/api/feedback/admin/** 受 hasRole('ADMIN') 保护）。
 */
import request from '@/utils/request'

/**
 * 提交一条意见反馈。提交人身份由后端从登录态解析，无需前端传入。
 *
 * @param {{ type: string, title: string, content: string }} data
 *   type 取值 BUG（问题报告）/ SUGGESTION（功能建议）/ OTHER（其他）。
 * @returns {Promise<{code: 200, data: FeedbackRow}>}
 */
export const submitFeedback = (data) => {
  return request.post('/feedback', data)
}

/**
 * 获取当前用户自己提交过的反馈（含管理员回复），分页倒序。
 *
 * @param {{ page: number, pageSize: number }} params
 * @returns {Promise<{code: 200, data: { records: FeedbackRow[], total, page, pageSize }}>}
 */
export const getMyFeedback = (params) => {
  return request.get('/feedback/mine', { params })
}

/**
 * 管理员分页查询全部反馈，支持按状态、类型、关键字筛选。
 *
 * @param {{
 *   page: number, pageSize: number,
 *   status?: string,   // PENDING / REPLIED / CLOSED
 *   type?: string,     // BUG / SUGGESTION / OTHER
 *   keyword?: string   // 标题或用户名模糊匹配
 * }} params
 * @returns {Promise<{code: 200, data: { records: FeedbackRow[], total, page, pageSize }}>}
 */
export const getFeedbackList = (params) => {
  return request.get('/feedback/admin/list', { params })
}

/**
 * 管理员获取反馈汇总统计。
 *
 * @returns {Promise<{code: 200, data: {
 *   total: number, pending: number, replied: number, closed: number,
 *   byType: { BUG: number, SUGGESTION: number, OTHER: number }
 * }}>}
 */
export const getFeedbackSummary = () => {
  return request.get('/feedback/admin/summary')
}

/**
 * 管理员回复指定反馈。
 *
 * @param {string} id - 反馈 ID。
 * @param {string} reply - 回复内容。
 * @returns {Promise<{code: 200, data: FeedbackRow}>}
 */
export const replyFeedback = (id, reply) => {
  return request.put(`/feedback/admin/${id}/reply`, { reply })
}

/**
 * 管理员关闭指定反馈（保留记录，仅置为已关闭）。
 *
 * @param {string} id - 反馈 ID。
 * @returns {Promise<{code: 200, data: FeedbackRow}>}
 */
export const closeFeedback = (id) => {
  return request.put(`/feedback/admin/${id}/close`)
}

/**
 * 管理员删除指定反馈。
 *
 * @param {string} id - 反馈 ID。
 * @returns {Promise<{code: 200, data: null}>}
 */
export const deleteFeedback = (id) => {
  return request.delete(`/feedback/admin/${id}`)
}
