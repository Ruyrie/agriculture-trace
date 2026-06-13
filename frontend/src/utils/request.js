import axios from 'axios'
import { ElMessage } from 'element-plus'

// 全局 axios 实例：baseURL 指向后端 /api，withCredentials 让浏览器携带 JSESSIONID。
const request = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api`,
  timeout: 10000,
  withCredentials: true
})

request.interceptors.request.use(config => {
  return config
})

// 统一拆包 Result，并在 Session 失效时清理本地状态。
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data?.code === 401) {
      localStorage.removeItem('sessionActive')
      localStorage.removeItem('userInfo')
      ElMessage.error(data.message || '登录已过期')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return data
  },
  error => {
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    error.__handled = true
    return Promise.reject(error)
  }
)

export default request
