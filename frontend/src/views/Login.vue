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
              <el-link type="primary" :underline="false" class="forgot-link" @click="openForgot">忘记密码？</el-link>
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

    <!-- 忘记密码：图形验证码 + 用户名 + 手机号 校验后重置 -->
    <el-dialog
      v-model="forgotVisible"
      title="找回密码"
      width="420px"
      append-to-body
      class="forgot-dialog"
      @closed="resetForgot"
    >
      <p class="forgot-hint">通过注册时填写的手机号验证身份，校验图形验证码后即可重置密码。</p>
      <el-form :model="forgotForm" :rules="forgotRules" ref="forgotRef" label-position="top" class="forgot-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item label="注册手机号" prop="phone">
          <el-input v-model="forgotForm.phone" placeholder="请输入注册手机号" :prefix-icon="Iphone" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="图形验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="forgotForm.captcha" placeholder="请输入图中字符" :prefix-icon="Key" clearable @keyup.enter="submitForgot" />
            <el-tooltip content="看不清？点击刷新" placement="top">
              <img
                v-if="captchaImg"
                :src="captchaImg"
                class="captcha-img"
                alt="图形验证码"
                @click="refreshCaptcha"
              />
              <div v-else class="captcha-img captcha-img--loading" @click="refreshCaptcha">加载中…</div>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入 6-20 位新密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" :prefix-icon="Lock" show-password @keyup.enter="submitForgot" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="submitForgot">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Login.vue — 系统登录页。
 *
 * 功能：
 *   - 用户名 + 密码表单登录（Spring Security formLogin，x-www-form-urlencoded 格式）。
 *   - "记住我"勾选后后端设置持久 Cookie，关闭浏览器再打开仍保持登录。
 *   - "忘记密码"：图形验证码 + 用户名 + 手机号三重校验后重置密码。
 *   - 登录成功后将用户信息存入 localStorage（sessionActive/userInfo/userRole），
 *     供 router/index.js 导航守卫和 Layout.vue 菜单过滤使用。
 *
 * 关联：
 *   - api/user.js（login / getCaptcha / forgotPassword）
 *   - router/index.js（beforeEach 守卫读取 sessionActive/userRole）
 *   - Layout.vue（读取 userInfo 展示用户名和头像）
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CircleCheck, Goods, InfoFilled, Iphone, Key, Lock, TrendCharts, User } from '@element-plus/icons-vue'
import { forgotPassword, getCaptcha, login } from '@/api/user'

// 路由实例，登录成功后用 router.push('/dashboard') 跳转。
const router = useRouter()
// 主登录表单的 el-form ref，用于调用 .validate() 触发全字段校验。
const formRef = ref()
// 用户名输入框 ref，页面挂载后自动 focus() 减少一次点击。
const usernameRef = ref()
// 密码输入框 ref，用户名按回车后 focusPassword() 将焦点移过来。
const passwordRef = ref()
// 登录按钮加载状态；置 true 时按钮显示"登录中…"并禁用重复点击。
const loading = ref(false)
// 登录表单数据，双向绑定到模板 :model="form"。
// rememberMe 传入 login() 后由后端决定 Session 过期时间和是否设置持久 Cookie。
const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 主登录表单校验规则；传给 el-form :rules="rules"。
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  // 进入页面自动聚焦用户名，减少一次点击。
  usernameRef.value?.focus()
})

// 用户名输入框回车后聚焦密码框，提升键盘登录体验。
const focusPassword = () => {
  passwordRef.value?.focus()
}

/* ===== 忘记密码弹窗 ===== */
// 弹窗是否可见；openForgot() 置 true，取消按钮 / 成功提交后置 false。
const forgotVisible = ref(false)
// 提交找回密码请求的加载状态；防止重复提交。
const forgotLoading = ref(false)
// Base64 图形验证码图片数据（src="data:image/png;base64,…"），来自 getCaptcha()。
const captchaImg = ref('')
// 忘记密码 el-form ref，用于 .validate() 和 .resetFields()。
const forgotRef = ref()
// 忘记密码表单数据；username 会预填为登录框中已输入的用户名。
const forgotForm = reactive({
  username: '',       // 用户名，传给后端做数据库查询
  phone: '',          // 注册时填写的手机号，与数据库记录比对
  captcha: '',        // 图形验证码答案，与后端 Session 存储的答案比对
  newPassword: '',    // 新密码（6-20 位）
  confirmPassword: '' // 前端二次确认，不发送给后端
})

const forgotRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入注册手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 向后端请求一张新的图形验证码；答案保存在后端 Session，前端只拿到图片。
const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.code === 200) captchaImg.value = res.data.image
  } catch {
    // 网络异常已由拦截器统一提示，这里保持安静即可。
  }
}

// 打开找回密码弹窗：用当前登录框用户名预填，并加载验证码。
const openForgot = () => {
  forgotForm.username = form.username
  forgotVisible.value = true
  nextTick(() => {
    forgotRef.value?.clearValidate()
    refreshCaptcha()
  })
}

// 弹窗关闭后清空表单，避免下次打开残留上次输入。
const resetForgot = () => {
  forgotRef.value?.resetFields()
  forgotForm.captcha = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
}

// 提交找回密码；验证码为一次性，任何失败都刷新验证码再让用户重试。
const submitForgot = async () => {
  if (forgotLoading.value) return
  try {
    await forgotRef.value.validate()
  } catch {
    return
  }
  forgotLoading.value = true
  try {
    const res = await forgotPassword({
      username: forgotForm.username,
      phone: forgotForm.phone,
      captcha: forgotForm.captcha,
      newPassword: forgotForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码重置成功，请使用新密码登录')
      form.username = forgotForm.username
      forgotVisible.value = false
    } else {
      ElMessage.error(res.message || '重置失败')
      forgotForm.captcha = ''
      refreshCaptcha()
    }
  } catch (error) {
    if (!error.__handled) ElMessage.error('重置请求失败')
    forgotForm.captcha = ''
    refreshCaptcha()
  } finally {
    forgotLoading.value = false
  }
}

// 校验表单并发起登录；成功后缓存用户信息和角色，再进入仪表盘。
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

.forgot-link {
  font-size: 13px;
}

/* ===== 找回密码弹窗 ===== */
.forgot-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.forgot-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  flex-shrink: 0;
  object-fit: cover;
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: #4caf50;
}

.captcha-img--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
  background: #f5f7fa;
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
