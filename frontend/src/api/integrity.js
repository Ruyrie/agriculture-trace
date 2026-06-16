import request from '@/utils/request'

// 获取产品数据指纹列表和全局 rootHash。
export const getProductFingerprints = () => {
  return request.get('/integrity/fingerprints')
}

// 只获取全局 rootHash 摘要。
export const getRootHash = () => {
  return request.get('/integrity/root-hash')
}

// 校验单个产品当前哈希是否与数据库 storedHash 一致。
export const verifyProductHash = (id) => {
  return request.get(`/integrity/verify/${id}`)
}

// 批量校验所有产品指纹。
export const verifyAllProductHashes = () => {
  return request.get('/integrity/products/verify')
}

// 校验单个批次当前哈希。
export const verifyBatchHash = (id) => {
  return request.get(`/integrity/batch/${id}/verify`)
}

// 批量校验所有批次指纹。
export const verifyAllBatchHashes = () => {
  return request.get('/integrity/batches/verify')
}
