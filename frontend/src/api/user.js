/**
 * 用户认证与个人中心 API 模块。
 *
 * 本模块封装所有与用户账号相关的后端请求，对应后端 UserController（/api/user/*）
 * 和 CaptchaController（/api/captcha）。
 * 所有请求通过 src/utils/request.js 中的全局 axios 实例发出，统一携带 Cookie 和超时设置。
 *
 * 关联视图：
 *   - Login.vue：login / logout / getCaptcha / forgotPassword
 *   - Profile.vue：getCurrentUser / updateProfile / changePassword / uploadAvatar
 *   - router/index.js：getCurrentUser（用于前置路由守卫验证 Session 有效性）
 *   - Layout.vue：logout
 */
import request from '@/utils/request'

/**
 * 用户登录。
 * 使用 application/x-www-form-urlencoded 编码提交，原因：Spring Security 的 formLogin
 * 处理器（loginProcessingUrl = /api/user/login）只能读取表单格式，不支持 JSON 请求体。
 * remember-me 字段值为 'true' 时，后端 Spring Security 会写入 AGRICULTURE_TRACE_REMEMBER_ME
 * 持久 Cookie，有效期 7 天，下次访问时可自动恢复 Session。
 *
 * @param {{ username: string, password: string, rememberMe: boolean }} data - 登录表单数据。
 * @returns {Promise<{code: number, message: string, data: UserInfo}>}
 *   成功时 data 包含 {id, username, nickname, phone, avatar, enabled, role}，
 *   Login.vue 将其存入 localStorage.userInfo 供全局使用。
 */
export const login = (data) => {
  const form = new URLSearchParams()
  form.append('username', data.username)
  form.append('password', data.password)
  form.append('remember-me', data.rememberMe ? 'true' : 'false')
  return request.post('/user/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

/**
 * 退出登录。
 * 后端 /api/user/logout 由 Spring Security 的 logout 处理器接收，
 * 使当前 JSESSIONID 对应的 Session 立即失效，并返回 Result.success(null)。
 * 调用方（Layout.vue handleCommand）会在此 Promise 结束后清理 localStorage 并跳转 /login。
 *
 * @returns {Promise<{code: 200, message: 'success', data: null}>}
 */
export const logout = () => {
  return request.post('/user/logout')
}

/**
 * 获取图形验证码。
 * 后端 CaptchaController 生成一张 4 位随机字符 PNG 图片，答案存入 HttpSession（5 分钟有效），
 * 仅把 base64 编码的图片数据返回给前端，答案不下发。
 * 前端将 data.image（'data:image/png;base64,...'）直接设为 <img src>。
 *
 * @returns {Promise<{code: 200, data: { image: string }}>}
 *   data.image 为可直接用于 img src 的 base64 data URI。
 */
export const getCaptcha = () => {
  return request.get('/captcha')
}

/**
 * 忘记密码重置。
 * 无需登录态，通过"用户名 + 注册手机号 + 图形验证码"双重核验身份后重置密码。
 * 后端 UserController.forgotPassword() 先验证 Session 中的验证码，
 * 再由 UserService.resetPasswordByIdentity() 核验用户名与手机号匹配关系。
 *
 * @param {{ username: string, phone: string, captcha: string, newPassword: string }} data
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export const forgotPassword = (data) => {
  return request.post('/user/forgot-password', data)
}

/**
 * 获取当前登录用户信息。
 * 后端 UserController.currentUser() 从 Spring Security Authentication 中取用户名，
 * 再查库返回 {id, username, nickname, phone, avatar, enabled, role}（不含密码）。
 * 路由守卫 router/index.js 的 ensureSession() 调用此接口向后端确认 Cookie Session 有效性。
 *
 * @returns {Promise<{code: number, data: UserInfo | null}>}
 *   若 Session 已失效则返回 code===401，请求拦截器会自动跳转 /login。
 */
export const getCurrentUser = () => {
  return request.get('/user/info')
}

/**
 * 更新当前登录用户的个人资料（昵称、手机号、头像）。
 * 用户名和角色不允许通过此接口修改（后端 UserService.updateProfile 只处理 nickname/phone/avatar）。
 * Profile.vue 保存成功后需 dispatch 'userInfoUpdated' 事件，通知 Layout.vue 刷新右上角头像。
 *
 * @param {{ nickname: string, phone: string, avatar?: string }} data - 更新后的资料。
 * @returns {Promise<{code: number, data: UserInfo}>}
 */
export const updateProfile = (data) => {
  return request.put('/user/profile', data)
}

/**
 * 当前用户修改自己的密码。
 * 后端通过图形验证码确认是本人操作，不再校验原密码。
 * Profile.vue 在提交前将验证码和新密码一起打包到请求体。
 *
 * @param {{ newPassword: string, captcha: string }} data - 新密码和图形验证码。
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export const changePassword = (data) => {
  return request.put('/user/password', data)
}

/**
 * 上传当前用户头像。
 * 使用 multipart/form-data，参数 key 为 'file'，对应后端 @RequestParam("file") MultipartFile。
 * 后端保存到 uploads/ 目录后返回相对 URL（如 /uploads/xxx.png），
 * 同时更新 User.avatar 字段并返回更新后的用户信息。
 *
 * @param {FormData} data - 包含 file 字段的 FormData 对象（由 el-upload http-request 回调构建）。
 * @returns {Promise<{code: number, data: { url: string, user: UserInfo }}>}
 *   data.url 为头像相对路径，data.user 为更新后的完整用户信息。
 */
export const uploadAvatar = (data) => {
  return request.post('/user/avatar', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
