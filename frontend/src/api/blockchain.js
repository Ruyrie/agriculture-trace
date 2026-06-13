import request from '@/utils/request'

export const getAuditLogs = (params) => {
  return request.get('/blockchain/logs', { params })
}

export const verifyAuditLogChain = () => {
  return request.get('/blockchain/logs/verify')
}
