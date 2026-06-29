/**
 * 登录日志 API 模块。
 *
 * 封装对后端 LoginLogController（/api/login-logs/*）的请求，供 LoginLog.vue 调用。
 * 日志在后端 SecurityConfig 的登录成功/失败处理器中写入；
 * 查询接口受 ROLE_ADMIN / ROLE_INSPECTOR 保护（见 SecurityConfig）。
 */
import request from '@/utils/request'

/**
 * 分页查询登录日志，支持按结果、关键字（用户名/IP）、时间范围筛选。
 *
 * @param {{
 *   page: number, pageSize: number,
 *   status?: string,    // SUCCESS / FAILURE
 *   keyword?: string,   // 用户名或 IP 模糊匹配
 *   startTime?: string, // yyyy-MM-dd HH:mm:ss
 *   endTime?: string
 * }} params
 * @returns {Promise<{code: 200, data: { records: LoginLogRow[], total, page, pageSize }}>}
 */
export const getLoginLogs = (params) => {
  return request.get('/login-logs', { params })
}

/**
 * 登录日志汇总：总次数、成功次数、失败次数。
 *
 * @returns {Promise<{code: 200, data: { total: number, success: number, failure: number }}>}
 */
export const getLoginLogSummary = () => {
  return request.get('/login-logs/summary')
}
