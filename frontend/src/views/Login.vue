<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon><Goods /></el-icon>
        </div>
        <h1 class="brand-title">农产品溯源系统</h1>
        <p class="brand-subtitle">从田间到餐桌，每一步都可追溯</p>
        <ul class="brand-features">
          <li class="feature-item">
            <el-icon class="feature-icon"><CircleCheck /></el-icon>
            <span>全程追溯，安全透明</span>
          </li>
          <li class="feature-item">
            <el-icon class="feature-icon"><TrendCharts /></el-icon>
            <span>数据可视化，一目了然</span>
          </li>
          <li class="feature-item">
            <el-icon class="feature-icon"><Lock /></el-icon>
            <span>权限管理，安全可靠</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-area">
      <div class="login-card">
        <!-- 移动端品牌头（仅窄屏显示） -->
        <div class="login-brand-mobile">
          <el-icon class="mobile-logo"><Goods /></el-icon>
          <span class="mobile-name">农产品溯源系统</span>
        </div>

        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>请输入您的账号信息</p>
        </div>

        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              ref="usernameRef"
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              name="username"
              autocomplete="username"
              autocapitalize="off"
              autocorrect="off"
              spellcheck="false"
              clearable
              aria-label="用户名"
              @keyup.enter="focusPassword"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              ref="passwordRef"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
              name="password"
              autocomplete="current-password"
              aria-label="密码"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="form.rememberMe">记住登录状态</el-checkbox>
            <span>7 天内自动登录</span>
          </div>
          <el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              class="login-btn"
              :loading="loading"
              size="large"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-tips">
          <p>测试账号：admin / farmer / inspector</p>
          <p>密码均为：123456</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CircleCheck, Goods, Lock, TrendCharts, User } from '@element-plus/icons-vue'
import { login } from '@/api/user'

const router = useRouter()
const formRef = ref()
const usernameRef = ref()
const passwordRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  // 进入页面自动聚焦用户名，减少一次点击
  usernameRef.value?.focus()
})

const focusPassword = () => {
  passwordRef.value?.focus()
}

const handleLogin = async () => {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    // 后端使用 Spring Security formLogin，因此这里提交 x-www-form-urlencoded。
    const res = await login(form)
    if (res.code === 200) {
      localStorage.setItem('sessionActive', '1')
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      localStorage.setItem('userRole', res.data.role)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (!error.__handled) {
      ElMessage.error('登录请求失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  overflow: hidden;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #1a6b2a 0%, #2d9e4f 40%, #4caf50 70%, #81c784 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  top: -200px;
  left: -200px;
}

.login-brand::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -100px;
  right: -100px;
}

.brand-content {
  text-align: center;
  color: #fff;
  z-index: 1;
  padding: 40px;
  animation: fade-up 0.6s ease both;
}

.brand-icon {
  font-size: 72px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.brand-icon :deep(.el-icon) {
  width: 72px;
  height: 72px;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 40px;
  letter-spacing: 1px;
}

.brand-features {
  display: inline-flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
  margin: 0;
  padding: 0;
  list-style: none;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  opacity: 0.95;
}

.feature-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* 右侧登录区 */
.login-form-area {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  animation: fade-up 0.5s ease both;
}

/* 移动端品牌头：默认隐藏，窄屏显示 */
.login-brand-mobile {
  display: none;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
  color: #2d9e4f;
}

.mobile-logo {
  font-size: 30px;
}

.mobile-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dcdfe6;
  transition: box-shadow 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4caf50;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.3);
}

.login-options {
  min-height: 28px;
  margin: -6px 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.login-options :deep(.el-checkbox) {
  height: 24px;
}

.login-options :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #606266;
}

.login-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #2d9e4f, #4caf50);
  border: none;
  height: 44px;
  transition: all 0.3s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #1a6b2a, #2d9e4f);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(45, 158, 79, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:focus-visible {
  outline: 2px solid #1a6b2a;
  outline-offset: 2px;
}

.login-tips {
  margin-top: 24px;
  padding: 12px 16px;
  background: #f0f9f0;
  border-radius: 8px;
  border-left: 3px solid #4caf50;
  text-align: center;
}

.login-tips p {
  font-size: 12px;
  color: #67c23a;
  margin: 2px 0;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 平板：收窄右侧栏，避免品牌区被过度压缩 */
@media (max-width: 992px) {
  .login-form-area {
    width: 420px;
    padding: 32px 24px;
  }
}

/* 移动端：隐藏左侧品牌区，登录卡片占满 */
@media (max-width: 768px) {
  .login-brand {
    display: none;
  }

  .login-form-area {
    width: 100%;
    padding: 24px 16px;
    background: linear-gradient(160deg, #f0f9f0 0%, #f5f7fa 60%);
  }

  .login-card {
    max-width: 420px;
    padding: 32px 24px;
    border-radius: 14px;
  }

  .login-brand-mobile {
    display: flex;
  }
}

/* 小屏手机：进一步压缩留白 */
@media (max-width: 360px) {
  .login-card {
    padding: 24px 18px;
  }

  .login-header {
    margin-bottom: 24px;
  }
}

/* 尊重用户的减弱动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .brand-content,
  .login-card {
    animation: none;
  }

  .login-btn,
  .login-form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
