import request from '@/utils/request'

// 获取产品基础详情。
export const getProductDetail = (id) => {
  return request.get(`/product/${id}`)
}

// 按产品 ID 获取公开溯源详情，包含产品、批次和三类记录。
export const getTraceInfo = (id) => {
  return request.get(`/trace/${id}`)
}

// 按批次 ID 获取公开溯源详情，只聚焦一个批次。
export const getBatchTraceInfo = (batchId) => {
  return request.get(`/trace/batch/${batchId}`)
}
