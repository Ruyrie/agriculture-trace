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
    const userInfo = sessionChecked && sessionActive && cachedUser.username ? cachedUser : await ensureSession()
    if (!userInfo) {
      return '/login'
    }
    const role = userInfo.role
    if (to.meta.roles && !to.meta.roles.includes(role)) {
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
