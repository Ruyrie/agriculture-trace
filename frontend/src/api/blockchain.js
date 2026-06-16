import request from '@/utils/request'

// 分页获取区块链式审计日志，供审计日志页面表格展示。
export const getAuditLogs = (params) => {
  return request.get('/blockchain/logs', { params })
}

// 触发后端校验日志链、链尾锚点和业务数据指纹。
export const verifyAuditLogChain = () => {
  return request.get('/blockchain/logs/verify')
}
