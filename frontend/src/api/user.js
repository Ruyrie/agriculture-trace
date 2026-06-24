import request from '@/utils/request'

// 登录接口使用表单编码提交，便于 Spring Security formLogin 直接读取用户名、密码和 remember-me。
export const login = (data) => {
  const form = new URLSearchParams()
  form.append('username', data.username)
  form.append('password', data.password)
  form.append('remember-me', data.rememberMe ? 'true' : 'false')
  return request.post('/user/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 退出登录，后端清理 Session。
export const logout = () => {
  return request.post('/user/logout')
}

// 获取图形验证码（base64 图片），答案保存在后端 Session，找回密码时校验。
export const getCaptcha = () => {
  return request.get('/captcha')
}

// 忘记密码：凭用户名 + 手机号 + 图形验证码重置密码。
export const forgotPassword = (data) => {
  return request.post('/user/forgot-password', data)
}

// 获取当前 Session 用户信息。
export const getCurrentUser = () => {
  return request.get('/user/info')
}

// 更新个人中心资料。
export const updateProfile = (data) => {
  return request.put('/user/profile', data)
}

// 修改当前用户密码。
export const changePassword = (data) => {
  return request.put('/user/password', data)
}

// 上传头像文件，使用 multipart/form-data。
export const uploadAvatar = (data) => {
  return request.post('/user/avatar', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
