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

export const verifyBatchHash = (id) => {
  return request.get(`/integrity/batch/${id}/verify`)
}
