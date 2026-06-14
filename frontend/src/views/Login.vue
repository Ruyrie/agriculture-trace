<template>
  <div class="login-page">
    <!-- 装饰性背景 -->
    <div class="bg-decor" aria-hidden="true">
      <span class="orb orb-1"></span>
      <span class="orb orb-2"></span>
      <span class="orb orb-3"></span>
      <span class="grid-overlay"></span>
    </div>

    <div class="login-shell">
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
              <span class="feature-icon"><el-icon><CircleCheck /></el-icon></span>
              <div class="feature-text">
                <strong>全程追溯</strong>
                <small>安全透明，来源可查</small>
              </div>
            </li>
            <li class="feature-item">
              <span class="feature-icon"><el-icon><TrendCharts /></el-icon></span>
              <div class="feature-text">
                <strong>数据可视化</strong>
                <small>一目了然，洞察全局</small>
              </div>
            </li>
            <li class="feature-item">
              <span class="feature-icon"><el-icon><Lock /></el-icon></span>
              <div class="feature-text">
                <strong>权限管理</strong>
                <small>分级管控，安全可靠</small>
              </div>
            </li>
          </ul>

          <div class="brand-stats">
            <div class="stat">
              <span class="stat-num">100%</span>
              <span class="stat-label">可追溯</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat">
              <span class="stat-num">24h</span>
              <span class="stat-label">实时监测</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat">
              <span class="stat-num">3级</span>
              <span class="stat-label">权限体系</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-form-area">
        <div class="login-card">
          <!-- 移动端品牌头（仅窄屏显示） -->
          <div class="login-brand-mobile">
            <span class="mobile-logo"><el-icon><Goods /></el-icon></span>
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
            <el-icon class="tips-icon"><InfoFilled /></el-icon>
            <div class="tips-text">
              <p>测试账号：<b>admin</b> / <b>farmer</b> / <b>inspector</b></p>
              <p>密码均为：<b>123456</b></p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CircleCheck, Goods, InfoFilled, Lock, TrendCharts, User } from '@element-plus/icons-vue'
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
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #0f3d1c 0%, #1a6b2a 45%, #2d9e4f 100%);
}

/* ===== 装饰背景 ===== */
.bg-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.5;
}

.orb-1 {
  width: 520px;
  height: 520px;
  top: -180px;
  left: -160px;
  background: radial-gradient(circle at 30% 30%, rgba(129, 199, 132, 0.55), transparent 70%);
  animation: float 14s ease-in-out infinite;
}

.orb-2 {
  width: 420px;
  height: 420px;
  bottom: -140px;
  right: -120px;
  background: radial-gradient(circle at 70% 70%, rgba(76, 175, 80, 0.5), transparent 70%);
  animation: float 18s ease-in-out infinite reverse;
}

.orb-3 {
  width: 300px;
  height: 300px;
  top: 55%;
  left: 8%;
  background: radial-gradient(circle, rgba(255, 235, 153, 0.35), transparent 70%);
  animation: float 16s ease-in-out infinite 2s;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, #000 35%, transparent 80%);
}

/* ===== 主体卡壳 ===== */
.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 960px;
  min-height: 580px;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  animation: fade-up 0.6s ease both;
}

/* ===== 左侧品牌区 ===== */
.login-brand {
  flex: 1.1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 44px;
  background:
    radial-gradient(circle at 20% 15%, rgba(255, 255, 255, 0.12), transparent 45%),
    linear-gradient(150deg, #1a6b2a 0%, #2d9e4f 55%, #43b05a 100%);
  color: #fff;
}

.brand-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 320px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 20px;
  font-size: 38px;
  margin-bottom: 22px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.brand-title {
  font-size: 30px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.brand-subtitle {
  font-size: 15px;
  opacity: 0.92;
  margin: 0 0 36px;
  letter-spacing: 0.5px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 0 0 36px;
  padding: 0;
  list-style: none;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 12px;
  font-size: 20px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.feature-text {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.feature-text strong {
  font-size: 15px;
  font-weight: 600;
}

.feature-text small {
  font-size: 12px;
  opacity: 0.82;
}

.brand-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-num {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.82;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
}

/* ===== 右侧登录区 ===== */
.login-form-area {
  width: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.96);
  padding: 48px 44px;
}

.login-card {
  width: 100%;
  max-width: 340px;
}

/* 移动端品牌头：默认隐藏，窄屏显示 */
.login-brand-mobile {
  display: none;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 28px;
  color: #2d9e4f;
}

.mobile-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  font-size: 24px;
  background: #f0f9f0;
}

.mobile-name {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
}

.login-header {
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 27px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px #e4e7ed;
  background: #f8fafb;
  transition: box-shadow 0.2s, background 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4caf50;
  background: #fff;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.35);
}

.login-form :deep(.el-input__prefix) {
  color: #9aa3a8;
}

.login-options {
  min-height: 28px;
  margin: -4px 0 18px;
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
  border-radius: 10px;
  font-size: 16px;
  letter-spacing: 4px;
  font-weight: 600;
  background: linear-gradient(135deg, #2d9e4f, #4caf50);
  border: none;
  height: 46px;
  box-shadow: 0 6px 16px rgba(45, 158, 79, 0.32);
  transition: all 0.25s;
}

.login-btn:hover {
  background: linear-gradient(135deg, #1a6b2a, #2d9e4f);
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(45, 158, 79, 0.42);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:focus-visible {
  outline: 2px solid #1a6b2a;
  outline-offset: 2px;
}

.login-tips {
  margin-top: 26px;
  padding: 14px 16px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: linear-gradient(135deg, #f0f9f0, #f3fbf4);
  border-radius: 12px;
  border: 1px solid #d9efdb;
}

.tips-icon {
  font-size: 18px;
  color: #4caf50;
  margin-top: 1px;
  flex-shrink: 0;
}

.tips-text {
  text-align: left;
}

.tips-text p {
  font-size: 12.5px;
  color: #5a8a5e;
  margin: 0;
  line-height: 1.7;
}

.tips-text b {
  color: #2d9e4f;
  font-weight: 700;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -24px) scale(1.06);
  }
}

/* 平板：收窄右侧栏 */
@media (max-width: 992px) {
  .login-shell {
    max-width: 760px;
    min-height: 540px;
  }

  .login-form-area {
    width: 380px;
    padding: 40px 32px;
  }

  .login-brand {
    padding: 40px 32px;
  }
}

/* 移动端：隐藏左侧品牌区，登录卡片占满 */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
  }

  .login-shell {
    max-width: 440px;
    min-height: auto;
    background: rgba(255, 255, 255, 0.98);
    border: none;
  }

  .login-brand {
    display: none;
  }

  .login-form-area {
    width: 100%;
    padding: 36px 28px;
    background: transparent;
  }

  .login-brand-mobile {
    display: flex;
  }
}

/* 小屏手机：进一步压缩留白 */
@media (max-width: 360px) {
  .login-form-area {
    padding: 28px 20px;
  }

  .login-header {
    margin-bottom: 24px;
  }
}

/* 尊重用户的减弱动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .login-shell,
  .orb {
    animation: none;
  }

  .login-btn,
  .login-form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
