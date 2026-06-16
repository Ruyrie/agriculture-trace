import axios from 'axios'
import { ElMessage } from 'element-plus'

// 全局 axios 实例：baseURL 指向后端 /api，withCredentials 让浏览器携带 JSESSIONID。
const request = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api`,
  timeout: 10000,
  withCredentials: true
})

// 请求拦截器暂不改写请求，只保留统一扩展点，后续可在这里添加请求头或追踪 ID。
request.interceptors.request.use(config => {
  return config
})

// 统一拆包 Result，并在 Session 失效时清理本地状态。
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data?.code === 401) {
      const isLoginRequest = response.config?.url?.includes('/user/login')
      // 后端统一用 HTTP 200 + Result.code 表示业务状态，所以这里要检查 data.code。
      // Session 失效时同步清理本地缓存，并把用户带回登录页。
      localStorage.removeItem('sessionActive')
      localStorage.removeItem('userInfo')
      if (!isLoginRequest) {
        ElMessage.error(data.message || '登录已过期')
      }
      if (!isLoginRequest && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return data
  },
  // 网络错误或非 2xx 响应统一弹出错误，并标记 __handled 避免页面层重复提示。
  error => {
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    error.__handled = true
    return Promise.reject(error)
  }
)

export default request
