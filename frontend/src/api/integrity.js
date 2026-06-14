import request from '@/utils/request'

export const getProductFingerprints = () => {
  return request.get('/integrity/fingerprints')
}

export const getRootHash = () => {
  return request.get('/integrity/root-hash')
}

export const verifyProductHash = (id) => {
  return request.get(`/integrity/verify/${id}`)
}

export const verifyAllProductHashes = () => {
  return request.get('/integrity/products/verify')
}

export const verifyBatchHash = (id) => {
  return request.get(`/integrity/batch/${id}/verify`)
}

export const verifyAllBatchHashes = () => {
  return request.get('/integrity/batches/verify')
}
