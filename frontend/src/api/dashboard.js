import request from '@/utils/request' 
 
// 获取仪表盘核心数字：产品数、批次数、溯源访问次数。
export const getStatistics = () => { 
  return request.get('/dashboard/statistics') 
} 
 
// 获取产品类别分布，用于首页饼图。
export const getCategoryDistribution = () => { 
  return request.get('/dashboard/categoryDistribution') 
} 
 
// 获取近 7 天溯源访问趋势，用于首页折线图。
export const getTraceTrend = () => { 
  return request.get('/dashboard/traceTrend') 
}

// 获取统计分析页的多组报表数据。
export const getReports = () => {
  return request.get('/dashboard/reports')
}
