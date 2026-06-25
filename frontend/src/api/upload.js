import request from '@/utils/request'

// 上传溯源展示图片，返回 /uploads/** 相对 URL。
export const uploadTraceImage = (data) => {
  return request.post('/upload/image', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
