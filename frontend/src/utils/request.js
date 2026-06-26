/**
 * 全局 axios 请求实例配置模块。
 *
 * 所有 API 调用（src/api/*.js）都通过此实例发起请求，统一处理：
 *   1. 基础 URL：开发环境走 Vite 代理（/api），生产环境走 VITE_API_BASE_URL 环境变量。
 *   2. Cookie 凭证：withCredentials: true 确保跨域请求携带 JSESSIONID，维持 Spring Security Session。
 *   3. 响应拆包：后端统一返回 Result<T>（{code, message, data}），拦截器直接返回整个 Result，
 *      各 api/*.js 函数拿到的就是 Result 对象，无需再解 response.data。
 *   4. Session 失效处理：code===401 时清理 localStorage 并跳转登录页。
 *   5. 网络错误提示：HTTP 非 2xx 统一弹出 ElMessage，并在 error 上标记 __handled，
 *      避免页面组件的 catch 块二次弹出。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 动态计算后端 API 基础路径。
 * 开发时 Vite 配置了 '/api' → 'http://localhost:8080/api' 的代理（vite.config.js proxy），
 * 生产构建后由 VITE_API_BASE_URL 环境变量（如 'https://api.example.com'）指向真实后端。
 */
const apiBaseURL = import.meta.env.DEV
  ? '/api'
  : `${import.meta.env.VITE_API_BASE_URL}/api`

/**
 * 全局 axios 实例。
 * - baseURL：所有请求路径自动拼接（如 request.get('/product/list') → GET /api/product/list）。
 * - timeout：10 秒超时，超时后 error 走响应错误拦截器统一提示"网络异常"。
 * - withCredentials：携带 Cookie，保证 Spring Security Session 认证正常工作。
 */
const request = axios.create({
  baseURL: apiBaseURL,
  timeout: 10000,
  withCredentials: true
})

/**
 * 请求拦截器。
 * 当前仅作为统一扩展点存在，不修改请求内容。
 * 若未来需要添加追踪 ID（如 X-Request-Id 请求头）、Authorization Bearer 或请求签名，在此处统一注入。
 *
 * @param {import('axios').InternalAxiosRequestConfig} config - axios 请求配置对象。
 * @returns {import('axios').InternalAxiosRequestConfig} 原样返回的配置。
 */
request.interceptors.request.use(config => {
  return config
})

/**
 * 响应拦截器（成功回调）。
 * 后端所有接口统一返回 HTTP 200 + Result JSON 体（不依赖 HTTP 状态码区分业务错误），
 * 因此这里从 response.data（即 Result 对象）读取 code 字段判断业务状态。
 *
 * @param {import('axios').AxiosResponse} response - axios 原始响应对象。
 * @returns {object} response.data，即后端 Result<T> 对象，包含 code/message/data 三个字段。
 *
 * 特殊处理：
 *   - code===401：Session 失效或权限不足。
 *       → 清理 localStorage 的 sessionActive 和 userInfo（router/index.js 的 ensureSession 会重新验证）。
 *       → 非登录请求才弹出"登录已过期"提示（登录失败时后端也返回 401，由登录页自己处理错误文案）。
 *       → 非登录请求且当前不在 /login 页时，强制跳转 /login。
 */
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data?.code === 401) {
      const isLoginRequest = response.config?.url?.includes('/user/login')
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
  /**
   * 响应拦截器（错误回调）。
   * 处理 HTTP 层面的错误（如 404、500、网络断连、超时等）。
   * 统一弹出 ElMessage 错误提示后，在 error 对象上设置 __handled = true，
   * 告知调用方的 catch 块该错误已被处理、无需再次弹出提示。
   *
   * @param {Error} error - axios 抛出的错误对象。
   * @returns {Promise<never>} 始终 reject，让调用方 catch 能感知请求失败。
   */
  error => {
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    error.__handled = true
    return Promise.reject(error)
  }
)

export default request
