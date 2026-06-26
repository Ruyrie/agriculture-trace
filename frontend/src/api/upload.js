/**
 * 文件上传 API 模块。
 *
 * 封装对后端 UploadController（/api/upload/*）的请求。
 * 上传的文件保存在后端 uploads/ 目录，并由 WebMvcConfig 的静态资源映射
 * 以 /uploads/** 路径对外暴露。前端通过 resolveImageUrl()（utils/images.js）
 * 将相对路径转换为可访问的完整 URL。
 *
 * 关联：ImageUploadGrid.vue（调用方）、resolveImageUrl（URL 构建）、
 *       ProductFormDialog.vue / BatchList.vue（图片上传入口）
 */
import request from '@/utils/request'

/**
 * 上传一张溯源展示图片，后端保存后返回相对路径。
 * 对应后端 POST /api/upload/image，由 UploadController 处理文件存储。
 * 请求必须以 multipart/form-data 格式发送（FormData 对象），
 * 文件字段名为 "file"（与后端 @RequestParam("file") 一致）。
 * 返回的 url 为相对路径（如 /uploads/abc123.jpg），
 * 存储到数据库时调用 stringifyImageUrls() 序列化为 JSON 数组。
 *
 * @param {FormData} data - 包含 file 字段的 FormData 对象。
 * @returns {Promise<{code: 200, data: { url: string }}>}
 *   data.url 为 /uploads/ 开头的相对路径，供 ImageUploadGrid 追加到图片列表。
 */
export const uploadTraceImage = (data) => {
  return request.post('/upload/image', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
