import request from '@/utils/request'

export const getProductDetail = (id) => {
  return request.get(`/product/${id}`)
}

export const getTraceInfo = (id) => {
  return request.get(`/trace/${id}`)
}

export const getBatchTraceInfo = (batchId) => {
  return request.get(`/trace/batch/${batchId}`)
}
