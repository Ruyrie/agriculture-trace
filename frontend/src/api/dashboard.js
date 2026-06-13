import request from '@/utils/request' 
 
export const getStatistics = () => { 
  return request.get('/dashboard/statistics') 
} 
 
export const getCategoryDistribution = () => { 
  return request.get('/dashboard/categoryDistribution') 
} 
 
export const getTraceTrend = () => { 
  return request.get('/dashboard/traceTrend') 
}

export const getReports = () => {
  return request.get('/dashboard/reports')
}
