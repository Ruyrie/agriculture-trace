import request from '@/utils/request' 
 
// 查询产品分页列表，params 通常包含 page、pageSize、keyword。
export const getProductList = (params) => { 
    return request.get('/product/list', { params }) 
} 
 
// 新增产品基础信息。
export const addProduct = (data) => { 
    return request.post('/product', data) 
} 
 
// 更新产品基础信息。
export const updateProduct = (data) => { 
    return request.put('/product', data) 
} 
 
// 删除产品。
export const deleteProduct = (id) => {
    return request.delete(`/product/${id}`)
}

// 复合新增：一次性创建产品、批次和溯源记录。
export const addProductWithTrace = (data) => {
    return request.post('/product/create-with-trace', data)
}
