/**
 * 仪表盘与统计分析 API 模块。
 *
 * 封装对后端 DashboardController（/api/dashboard/*）的所有请求。
 * 后端使用 MyBatis StatisticsMapper 执行聚合 SQL，数据实时来自数据库。
 *
 * 关联视图：
 *   - Dashboard.vue：getStatistics / getCategoryDistribution / getTraceTrend / getOverviewCharts
 *   - Statistics.vue：getReports（仅 ADMIN/INSPECTOR 可访问）
 */
import request from '@/utils/request'

/**
 * 获取仪表盘顶部三项核心统计数字。
 * 对应后端 GET /api/dashboard/statistics，用 StatisticsMapper 聚合 COUNT 查询。
 *
 * @returns {Promise<{code: 200, data: {
 *   productCount: number,  // product 表总行数
 *   batchCount: number,    // batch 表总行数
 *   traceCount: number     // trace_record 表总行数（溯源访问总次数）
 * }}>}
 */
export const getStatistics = () => {
  return request.get('/dashboard/statistics')
}

/**
 * 获取产品类别分布，用于仪表盘首页饼图。
 * 对应后端 GET /api/dashboard/categoryDistribution，
 * 返回格式 [{name: '蔬菜', value: 3}, ...] 可直接作为 ECharts 饼图 series.data。
 *
 * @returns {Promise<{code: 200, data: Array<{name: string, value: number}>}>}
 */
export const getCategoryDistribution = () => {
  return request.get('/dashboard/categoryDistribution')
}

/**
 * 获取近 7 天溯源访问趋势，用于仪表盘折线图。
 * 后端 DashboardController.traceTrend() 会补零保证 7 天连续，不会出现缺日期的情况。
 *
 * @returns {Promise<{code: 200, data: {
 *   dates: string[],   // ['2025-06-19', '2025-06-20', ..., '2025-06-25']（7 天连续日期）
 *   counts: number[]   // 对应每天的溯源访问次数，无访问日为 0
 * }}>}
 */
export const getTraceTrend = () => {
  return request.get('/dashboard/traceTrend')
}

/**
 * 获取统计分析页的多组报表数据（需 ROLE_ADMIN 或 ROLE_INSPECTOR）。
 * 对应后端 GET /api/dashboard/reports，一次请求返回四组图表数据。
 *
 * @returns {Promise<{code: 200, data: {
 *   monthlyBatchOutput: Array,    // 近 12 个月批次产出量（月份 + 数量）
 *   traceRanking: Array,          // 溯源访问量 TOP N 产品（产品名 + 访问次数）
 *   originDistribution: Array,    // 产品产地分布（产地 + 数量）
 *   productBatchOutput: Array     // 各产品的批次数量（产品名 + 批次数）
 * }}>}
 */
export const getReports = () => {
  return request.get('/dashboard/reports')
}

/**
 * 获取仪表盘数据概览页下方的链上操作类型图表数据。
 * 对应后端 GET /api/dashboard/overviewCharts，聚合 blockchain_log 的 actionType 分布。
 *
 * @returns {Promise<{code: 200, data: {
 *   blockchainActionMix: Array<{name: string, value: number}>
 *   // name 为 CREATE/UPDATE/DELETE，value 为对应日志条数
 * }}>}
 */
export const getOverviewCharts = () => {
  return request.get('/dashboard/overviewCharts')
}
