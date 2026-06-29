/**
 * 预警中心 API 模块。
 *
 * 封装对后端 WarningController（/api/warnings/*）的请求，供 WarningCenter.vue 调用。
 * 预警由后端实时计算（质检不合格 / 缺少质检 / 缺少物流），不依赖独立预警表。
 * 接口受 ROLE_ADMIN / ROLE_INSPECTOR 保护（见 SecurityConfig）。
 */
import request from '@/utils/request'

/**
 * 获取全部预警，可按级别筛选。
 *
 * @param {{ level?: string }} [params] - level 取值 HIGH / MEDIUM / LOW。
 * @returns {Promise<{code: 200, data: WarningRow[]}>}
 *   每条 WarningRow 含 level / type / typeLabel / targetType / targetId / targetName / message / time。
 */
export const getWarnings = (params) => {
  return request.get('/warnings', { params })
}

/**
 * 获取各级别预警数量汇总。
 *
 * @returns {Promise<{code: 200, data: { total: number, high: number, medium: number, low: number }}>}
 */
export const getWarningSummary = () => {
  return request.get('/warnings/summary')
}
