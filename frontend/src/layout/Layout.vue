<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <!-- Logo区域 -->
      <div class="logo" :class="{ 'logo-collapse': isCollapse }">
        <el-icon class="logo-icon"><Goods /></el-icon>
        <span v-if="!isCollapse" class="logo-text">农产品溯源系统</span>
      </div>

      <!-- 动态菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="side-menu"
        :class="{ 'side-menu--collapse': isCollapse }"
      >
        <template v-for="item in menuList" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.meta.icon" /></el-icon>
              <span>{{ child.meta.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <!-- 折叠按钮 -->
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        <span v-if="!isCollapse" class="collapse-text">收起菜单</span>
      </div>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <div class="breadcrumb-area">
            <span class="page-title">{{ currentPageTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <template v-if="showHeaderRefresh">
            <el-tooltip content="刷新页面" placement="bottom">
              <div class="header-action" @click="refreshPage">
                <el-icon><Refresh /></el-icon>
              </div>
            </el-tooltip>
            <div class="divider"></div>
          </template>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="34" :src="userAvatar" class="user-avatar" />
              <div class="user-meta">
                <span class="username">{{ username }}</span>
                <span class="user-role">{{ userRoleLabel }}</span>
              </div>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主要内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Fold, Expand, ArrowDown, Refresh, User, SwitchButton, Goods } from '@element-plus/icons-vue'
import { logout } from '@/api/user'

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjAiIGhlaWdodD0iMTIwIiB2aWV3Qm94PSIwIDAgMTIwIDEyMCI+PHJlY3Qgd2lkdGg9IjEyMCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiNFMEUwRTAiLz48Y2lyY2xlIGN4PSI2MCIgY3k9IjQ1IiByPSIyMCIgZmlsbD0iIzlFOUU5RSIvPjxwYXRoIGZpbGw9IiM5RTlFOUUiIGQ9Ik0zMCA4MCBMOTAgODAgTDgwIDY1IEw3MCA2NSBMNzAgNzUgTDUwIDc1IEw1MCA2NSBMNDAgNjVaIi8+PC9zdmc+'

const username = ref('')
const userAvatar = ref(defaultAvatar)
const menuList = ref([])

const router = useRouter()
const route = useRoute()

const roleMap = { ROLE_ADMIN: '管理员', ROLE_FARMER: '农户', ROLE_INSPECTOR: '监管员' }
const userRoleLabel = computed(() => {
  const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return roleMap[info.role] || '用户'
})

const currentPageTitle = computed(() => {
  const matched = menuList.value.flatMap(m => m.children ? m.children : [m])
  const current = matched.find(m => m.path === route.path)
  return current?.meta?.title || '首页'
})

const showHeaderRefresh = computed(() => route.path !== '/statistics')

// 从 localStorage 读取当前用户资料，并把相对头像 URL 拼成后端完整地址。
const getUserInfo = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    const { username: name, avatar } = JSON.parse(userInfo)
    username.value = name || '用户'
    if (avatar) {
      // 后端返回 /uploads/xxx 时，补齐后端域名用于头像展示。
      const apiBase = import.meta.env.VITE_API_BASE_URL
      userAvatar.value = avatar.startsWith('http') ? avatar : `${apiBase}${avatar}`
    } else {
      userAvatar.value = defaultAvatar
    }
  }
}

const allMenus = [
  {
    path: '/dashboard',
    meta: { title: '数据概览', icon: 'DataLine' },
    roles: ['ROLE_ADMIN', 'ROLE_FARMER', 'ROLE_INSPECTOR']
  },
  {
    path: '/products',
    meta: { title: '产品管理', icon: 'Box' },
    roles: ['ROLE_ADMIN', 'ROLE_FARMER', 'ROLE_INSPECTOR']
  },
  {
    path: '/batches',
    meta: { title: '批次管理', icon: 'List' },
    roles: ['ROLE_ADMIN', 'ROLE_FARMER', 'ROLE_INSPECTOR']
  },
  {
    path: '/integrity',
    meta: { title: '数据指纹', icon: 'Key' },
    roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR']
  },
  {
    path: '/blockchain/audit-log',
    meta: { title: '审计日志', icon: 'Lock' },
    roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR']
  },
  {
    path: '/statistics',
    meta: { title: '统计分析', icon: 'DataAnalysis' },
    roles: ['ROLE_ADMIN', 'ROLE_INSPECTOR']
  },
  {
    path: '/users',
    meta: { title: '用户管理', icon: 'User' },
    roles: ['ROLE_ADMIN']
  }
]

// 根据当前用户角色筛选侧边栏菜单，前端展示与路由 meta.roles 保持一致。
const fetchMenu = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const role = userInfo.role
  menuList.value = allMenus.filter(item => !item.roles || item.roles.includes(role))
}

// 折叠/展开左侧菜单。
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 刷新当前页面，用于重新拉取当前路由组件中的数据。
const refreshPage = () => {
  window.location.reload()
}

// 处理右上角用户菜单命令：退出登录或进入个人中心。
const handleCommand = async (command) => {
  if (command === 'logout') {
    await logout().catch(() => {})
    localStorage.removeItem('sessionActive')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    router.push('/login')
    ElMessage.success('已退出登录')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  getUserInfo()
  fetchMenu()
  // 监听个人中心保存头像/信息后派发的事件，实时同步右上角头像
  window.addEventListener('userInfoUpdated', getUserInfo)
})

onUnmounted(() => {
  window.removeEventListener('userInfoUpdated', getUserInfo)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #f0f2f5;
}

/* 侧边栏 */
.aside {
  background: linear-gradient(180deg, #1b5e20 0%, #2e7d32 50%, #388e3c 100%);
  transition: width 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

/* Logo */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.15);
  white-space: nowrap;
  overflow: hidden;
  flex-shrink: 0;
}

.logo-collapse {
  justify-content: center;
}

.logo-icon {
  font-size: 24px;
  flex-shrink: 0;
  color: #fff;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

/* 菜单 */
.side-menu {
  flex: 1;
  border: none !important;
  background: transparent !important;
  overflow-y: auto;
  overflow-x: hidden;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: transparent !important;
  border-radius: 0 !important;
  margin: 2px 8px !important;
  border-radius: 8px !important;
  transition: all 0.2s !important;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.25) !important;
  font-weight: 600;
}

.side-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #a5d6a7;
  border-radius: 0 2px 2px 0;
}

.side-menu :deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.1) !important;
}

.side-menu--collapse :deep(.el-menu-item),
.side-menu--collapse :deep(.el-sub-menu__title) {
  width: 48px !important;
  height: 48px !important;
  margin: 4px 8px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.side-menu--collapse :deep(.el-icon) {
  margin: 0 !important;
  width: 22px !important;
  height: 22px !important;
  font-size: 20px !important;
}

.side-menu--collapse :deep(.el-menu-tooltip__trigger) {
  width: 48px !important;
  height: 48px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 折叠按钮 */
.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  background: rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: #fff;
  background: rgba(0, 0, 0, 0.2);
}

.collapse-text {
  font-size: 13px;
}

/* 主容器 */
.main-container {
  overflow: hidden;
}

/* 顶部导航 */
.header {
  background: #fff;
  border-bottom: 1px solid #e8eaec;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  height: 60px !important;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-action {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}

.header-action:hover {
  background: #f5f7fa;
  color: #2e7d32;
}

.divider {
  width: 1px;
  height: 24px;
  background: #e8eaec;
  margin: 0 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.arrow-icon {
  color: #909399;
  font-size: 12px;
}

/* 内容区 */
.main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
