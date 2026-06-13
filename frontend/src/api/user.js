import request from '@/utils/request'

export const login = (data) => {
  const form = new URLSearchParams()
  form.append('username', data.username)
  form.append('password', data.password)
  form.append('remember-me', data.rememberMe ? 'true' : 'false')
  return request.post('/user/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export const logout = () => {
  return request.post('/user/logout')
}

export const getCurrentUser = () => {
  return request.get('/user/info')
}

export const updateProfile = (data) => {
  return request.put('/user/profile', data)
}

export const changePassword = (data) => {
  return request.put('/user/password', data)
}

export const uploadAvatar = (data) => {
  return request.post('/user/avatar', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
