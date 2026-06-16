import request from '@/utils/request' 
 
// 查询批次分页列表，params 可包含 page、pageSize、productId/productName、batchNo。
export const getBatchList = (params) => { 
  return request.get('/batch/list', { params }) 
} 
 
// 新增批次，并可同时提交生产、质检、物流明细数组。
export const addBatch = (data) => { 
  return request.post('/batch', data) 
} 
 
// 更新批次基础信息；当 data 带三类明细数组时后端会覆盖保存。
export const updateBatch = (data) => { 
  return request.put('/batch', data) 
} 
 
// 删除指定批次，后端会记录审计日志。
export const deleteBatch = (id) => { 
  return request.delete(`/batch/${id}`) 
}
