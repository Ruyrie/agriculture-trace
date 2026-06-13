import request from '@/utils/request' 
 
export const getBatchList = (params) => { 
  return request.get('/batch/list', { params }) 
} 
 
export const addBatch = (data) => { 
  return request.post('/batch', data) 
} 
 
export const updateBatch = (data) => { 
  return request.put('/batch', data) 
} 
 
export const deleteBatch = (id) => { 
  return request.delete(`/batch/${id}`) 
}
