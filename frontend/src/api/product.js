import request from '@/utils/request' 
 
export const getProductList = (params) => { 
    return request.get('/product/list', { params }) 
} 
 
export const addProduct = (data) => { 
    return request.post('/product', data) 
} 
 
export const updateProduct = (data) => { 
    return request.put('/product', data) 
} 
 
export const deleteProduct = (id) => {
    return request.delete(`/product/${id}`)
}

export const addProductWithTrace = (data) => {
    return request.post('/product/create-with-trace', data)
}
