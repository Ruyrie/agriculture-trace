import { createRouter, createWebHistory } from 'vue-router' 
import { ElMessage } from 'element-plus' 
import Layout from '@/layout/Layout.vue' 
import { getCurrentUser } from '@/api/user'
 
/** 
 * 路由表定义 
 * 注意：Layout 作为父路由，其 children 中的组件会渲染在 Layout 的 <router-view /> 中 
 */ 
const routes = [ 
  { 
    path: '/login', 
    name: 'Login', 
    component: () => import('@/views/Login.vue') 
  }, 
  { 
    path: '/', 
    component: Layout, 
    redirect: '/dashboard',    // 默认重定向到仪表盘 
    children: [ 
      { 
        path: 'dashboard', 
        name: 'Dashboard', 
        component: () => import('@/views/Dashboard.vue'), 
        meta: { requiresAuth: true, title: '仪表盘', icon: 'PieChart' } 
      }, 
      { 
        path: 'products', 
        name: 'ProductList', 
        component: () => import('@/views/ProductList.vue'), 
        meta: { requiresAuth: true, title: '产品管理', icon: 'Goods' } 
      }, 
      { 
        path: 'batches', 
        name: 'BatchList', 
        component: () => import('@/views/BatchList.vue'), 
        meta: { requiresAuth: true, title: '批次管理', icon: 'List' } 
      }, 
      {
        path: 'integrity',
        name: 'IntegrityReport',
        component: () => import('@/views/IntegrityReport.vue'),
        meta: { requiresAuth: true, title: '数据指纹', icon: 'Key', roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR'] }
      },
      {
        path: 'blockchain/audit-log',
        name: 'AuditLog',
        component: () => import('@/views/blockchain/AuditLog.vue'),
        meta: { requiresAuth: true, title: '审计日志', icon: 'Lock', roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR'] }
      },
      { 
        path: 'users', 
        name: 'UserManagement', 
        component: () => import('@/views/UserManagement.vue'), 
        meta: { requiresAuth: true, title: '用户管理', icon: 'User', roles: ['ROLE_ADMIN'] } 
      }, 
      { 
        path: 'statistics', 
        name: 'Statistics', 
        component: () => import('@/views/Statistics.vue'), 
        meta: { requiresAuth: true, title: '统计分析', icon: 'DataAnalysis', roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR'] } 
      },
      {
        path: 'trace/batch/:batchId',
        name: 'BatchTraceDetail',
        component: () => import('@/views/TraceDetail.vue'),
        meta: { requiresAuth: false, title: '批次溯源详情' }
      },
      {
        path: 'trace/:id',
        name: 'TraceDetail',
        component: () => import('@/views/TraceDetail.vue'),
        meta: { requiresAuth: false, title: '溯源详情' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { requiresAuth: true, title: '个人中心' }
      }
    ]
  }
] 
 
const router = createRouter({ 
  history: createWebHistory(), 
  routes 
}) 
 
let sessionChecked = false

const clearSession = () => {
  localStorage.removeItem('sessionActive')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userRole')
  sessionChecked = false
}

// 使用后端 Session Cookie 校验登录状态，避免只依赖 localStorage 造成假登录。
const ensureSession = async () => {
  try {
    const res = await getCurrentUser()
    if (res.code === 200 && res.data) {
      localStorage.setItem('sessionActive', '1')
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      localStorage.setItem('userRole', res.data.role)
      // 本轮页面生命周期内已向后端确认过 Session，后续路由跳转可先使用缓存用户信息。
      sessionChecked = true
      return res.data
    }
  } catch {
    clearSession()
  }
  return null
}

/**
 * 全局前置守卫：由 JSESSIONID 对应的后端 Session 确认登录状态，再按角色控制页面权限。
 */
router.beforeEach(async (to, from) => {
  const sessionActive = localStorage.getItem('sessionActive')
  const cachedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')

  if (to.meta.requiresAuth) {
    // 受保护页面必须先确认后端 Session，再检查 meta.roles。
    // 这样即使用户手工改 localStorage，也无法绕过后端 Cookie 校验。
    const userInfo = sessionChecked && sessionActive && cachedUser.username ? cachedUser : await ensureSession()
    if (!userInfo) {
      return '/login'
    }
    const role = userInfo.role
    if (to.meta.roles && !to.meta.roles.includes(role)) {
      // 前端角色守卫提供友好跳转；真正的接口权限仍由 SecurityConfig 兜底。
      ElMessage.error('无权访问该页面')
      return '/dashboard'
    }
    return true
  }

  if (to.path === '/login' && sessionActive) {
    const userInfo = await ensureSession()
    if (userInfo) return '/dashboard'
  }
  return true
}) 
 
export default router 
