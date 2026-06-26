/**
 * 图片 URL 工具函数模块。
 *
 * 系统中产品、批次和生产记录的图片列表在数据库中以 JSON 数组字符串存储
 * （如 '["/uploads/trace-images/xxx.jpg","/uploads/trace-images/yyy.png"]'），
 * 上传组件（ImageUploadGrid.vue）和后端接口（UploadController）均使用此格式。
 * 本模块统一处理解析、序列化和 URL 拼接，避免各组件重复实现兼容逻辑。
 */

/**
 * 将数据库存储的图片值解析为 URL 字符串数组。
 * 兼容三种输入形式：
 *   1. 已经是数组（组件内部状态）→ 直接过滤空值并截取前 9 张。
 *   2. JSON 字符串（数据库值）→ JSON.parse 后取数组元素，截取前 9 张。
 *   3. 逗号分隔字符串（旧数据格式兼容）→ split(',') 后截取前 9 张。
 *
 * @param {string|string[]|null|undefined} value - 来自数据库的 imageUrls 字段值，或组件内部数组。
 * @returns {string[]} 图片 URL 字符串数组，最多 9 个元素，不含空字符串。
 *
 * 使用场景：
 *   - ImageUploadGrid.vue 初始化时将 props.modelValue（数据库字符串）转为数组供 el-upload 展示。
 *   - TraceDetail.vue 展示溯源详情中的产品/批次/生产记录图片。
 */
export const parseImageUrls = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean).slice(0, 9)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter(Boolean).slice(0, 9) : []
  } catch {
    // 兼容旧版逗号分隔格式，解析失败时不抛出异常。
    return String(value)
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
      .slice(0, 9)
  }
}

/**
 * 将图片 URL 数组（或任意可被 parseImageUrls 解析的值）序列化为 JSON 字符串，
 * 用于提交表单时还原为数据库存储格式。
 * 空数组时返回空字符串，后端 Service 将空字符串存为 null 或空列。
 *
 * @param {string|string[]|null|undefined} value - 待序列化的图片 URL 集合。
 * @returns {string} JSON 数组字符串（如 '["url1","url2"]'），或空字符串。
 *
 * 使用场景：
 *   - ImageUploadGrid.vue 中 emit('update:modelValue') 时将内部数组转回 JSON 字符串。
 *   - ProductFormDialog.vue / BatchList.vue 提交表单时序列化 imageUrls 字段。
 */
export const stringifyImageUrls = (value) => {
  const urls = parseImageUrls(value)
  return urls.length > 0 ? JSON.stringify(urls) : ''
}

/**
 * 将后端返回的相对图片路径（如 /uploads/trace-images/xxx.jpg）拼接为浏览器可访问的完整 URL。
 *
 * @param {string|null|undefined} url - 相对路径或已是绝对 URL / data URI 的字符串。
 * @returns {string|undefined} 可直接用于 <img src> 的完整 URL，或原值（已是绝对地址时）。
 *
 * 规则：
 *   - 开发环境（import.meta.env.DEV）：Vite 代理已将 /uploads/** 转发到后端，直接返回相对路径。
 *   - 生产环境：拼接 VITE_API_BASE_URL 环境变量（在 .env.production 中配置后端域名）。
 *   - http/https 开头或 data: 开头的 URL 直接返回，不做处理。
 *
 * 使用场景：
 *   - Layout.vue 中展示用户头像。
 *   - TraceDetail.vue / ImageUploadGrid.vue 展示产品、批次图片时统一调用。
 */
export const resolveImageUrl = (url) => {
  if (!url || url.startsWith('http') || url.startsWith('data:')) return url
  return import.meta.env.DEV ? url : `${import.meta.env.VITE_API_BASE_URL}${url}`
}
