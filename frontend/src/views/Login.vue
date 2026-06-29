<template>
  <div class="login-page">
    <!-- ===== 晨光田野背景 ===== -->
    <div class="bg-photo" aria-hidden="true"></div>
    <div class="bg-overlay" aria-hidden="true"></div>

    <!-- ===== 左上角品牌标识 ===== -->
    <div class="brand-mark">
      <span class="brand-word">农产品溯源</span>
    </div>

    <!-- ===== 左侧主标语 ===== -->
    <div class="hero">
      <span class="hero-accent"></span>
      <h1 class="hero-title">农产品全链路溯源平台</h1>
      <p class="hero-subtitle">从田间到餐桌，全程透明可追溯</p>
    </div>

    <!-- ===== 右侧登录卡 ===== -->
    <div class="login-card">
      <div class="login-header">
        <h2>欢迎回来</h2>
        <p>登录后继续管理您的溯源网络</p>
      </div>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        class="login-form"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
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
        <el-form-item label="密码" prop="password">
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
          <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
          <el-link type="primary" underline="never" class="forgot-link" @click="openForgot">忘记密码？</el-link>
        </div>
        <el-form-item class="submit-item">
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

      <p class="login-tips">
        测试账号 <b>admin</b> / <b>farmer</b> / <b>inspector</b>
        <span class="tips-dot">·</span> 密码 <b class="tips-pwd">123456</b>
      </p>
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
 * 视觉：
 *   - 全屏背景使用真实晨光农田图片 public/login-bg-real.png。
 *   - 左侧为品牌标语，右侧为玻璃拟态登录卡。
 *
 * 关联：
 *   - api/user.js（login / getCaptcha / forgotPassword）
 *   - router/index.js（beforeEach 守卫读取 sessionActive/userRole）
 *   - Layout.vue（读取 userInfo 展示用户名和头像）
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Iphone, Key, Lock, User } from '@element-plus/icons-vue'
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
  box-sizing: border-box;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 48px clamp(32px, 6vw, 110px);
  overflow: hidden;
  isolation: isolate;
  background: #17351f;
}

/* 真实农田摄影背景 */
.bg-photo {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image: url('/login-bg-real.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform: scale(1.01);
}

/* 聚焦中心内容，同时提升左侧白字和右侧卡片的对比度 */
.bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(90deg, rgba(5, 22, 12, 0.56) 0%, rgba(8, 26, 15, 0.23) 43%, rgba(55, 34, 8, 0.08) 100%),
    linear-gradient(180deg, rgba(7, 19, 12, 0.06), rgba(5, 20, 11, 0.2));
}

/* ===== 品牌标识 ===== */
.brand-mark {
  position: absolute;
  top: 40px;
  left: clamp(28px, 5vw, 64px);
  z-index: 2;
  display: flex;
  align-items: center;
  color: #fff;
}

.brand-word {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 3px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
}

/* ===== 左侧主标语 ===== */
.hero {
  position: absolute;
  left: clamp(28px, 6vw, 96px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  max-width: 760px;
  color: #fff;
  animation: fade-in 0.8s ease both;
}

.hero-accent {
  display: block;
  width: 64px;
  height: 5px;
  border-radius: 3px;
  margin-bottom: 26px;
  background: linear-gradient(90deg, #f0b73f, #e8c069);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.25);
}

.hero-title {
  font-size: clamp(34px, 3.4vw, 56px);
  font-weight: 800;
  line-height: 1.18;
  margin: 0 0 20px;
  letter-spacing: 2px;
  white-space: nowrap;
  text-shadow: 0 3px 18px rgba(0, 0, 0, 0.4);
}

.hero-subtitle {
  font-size: clamp(16px, 1.5vw, 20px);
  font-weight: 400;
  margin: 0;
  letter-spacing: 3px;
  opacity: 0.94;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

/* ===== 右侧登录卡（保留玻璃拟态） ===== */
.login-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 440px;
  padding: 44px 44px 30px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 30px 70px rgba(15, 40, 22, 0.32);
  animation: card-in 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-header {
  margin-bottom: 26px;
}

.login-header h2 {
  font-size: 30px;
  font-weight: 800;
  color: #16261a;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: #8a9a8e;
  margin: 0;
}

.login-form :deep(.el-form-item__label) {
  font-size: 13.5px;
  font-weight: 600;
  color: #44604b;
  padding-bottom: 6px;
  line-height: 1.2;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 11px;
  padding: 6px 14px;
  box-shadow: 0 0 0 1px #dfe5e0;
  background: #f7faf8;
  transition: box-shadow 0.2s, background 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #4caf50;
  background: #fff;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(45, 110, 60, 0.45);
}

.login-form :deep(.el-input__prefix) {
  color: #7d957f;
}

.login-options {
  min-height: 26px;
  margin: -2px 0 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-options :deep(.el-checkbox__label) {
  font-size: 13.5px;
  color: #44604b;
}

.forgot-link {
  font-size: 13.5px;
}

.submit-item {
  margin-bottom: 0 !important;
}

.login-btn {
  width: 100%;
  border-radius: 11px;
  font-size: 16px;
  letter-spacing: 6px;
  font-weight: 600;
  height: 50px;
  color: #fff;
  background: #245c30;
  border: none;
  box-shadow: 0 10px 24px rgba(28, 76, 40, 0.35);
  transition: all 0.25s;
}

.login-btn:hover {
  background: #1c4a26;
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(28, 76, 40, 0.45);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:focus-visible {
  outline: 2px solid #16331f;
  outline-offset: 2px;
}

.login-tips {
  margin: 22px 0 0;
  font-size: 13px;
  color: #8a9a8e;
  text-align: left;
  line-height: 1.6;
}

.login-tips b {
  color: #2f6b3a;
  font-weight: 700;
}

.login-tips .tips-pwd {
  color: #c98a2e;
}

.tips-dot {
  margin: 0 6px;
  color: #c7d2c9;
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

/* ===== 动画 ===== */
@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(22px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes fade-in {
  from { opacity: 0; transform: translate(0, calc(-50% + 14px)); }
  to { opacity: 1; transform: translate(0, -50%); }
}

/* 平板及以下：标语下移为顶部信息，卡片居中 */
@media (max-width: 900px) {
  .login-page {
    justify-content: center;
    padding: 96px 24px 40px;
  }

  .bg-overlay {
    background:
      linear-gradient(180deg, rgba(6, 24, 13, 0.46) 0%, rgba(8, 28, 14, 0.18) 44%, rgba(5, 20, 11, 0.55) 100%);
  }

  .hero {
    position: absolute;
    top: 96px;
    transform: none;
    left: 50%;
    margin-left: -50vw;
    width: 100vw;
    max-width: none;
    padding: 0 24px;
    text-align: center;
    animation: none;
  }

  .hero-title {
    white-space: normal;
  }

  .hero-accent {
    margin: 0 auto 18px;
  }
}

@media (max-width: 768px) {
  .hero {
    display: none;
  }

  .login-page {
    align-items: center;
    padding: 88px 20px 40px;
  }

  .brand-mark {
    top: 28px;
    left: 24px;
  }

  .brand-word {
    font-size: 19px;
  }
}

@media (max-width: 360px) {
  .login-card {
    padding: 32px 22px 24px;
  }

  .login-header h2 {
    font-size: 26px;
  }
}

/* 尊重用户的减弱动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .login-card,
  .hero {
    animation: none;
  }

  .login-btn,
  .login-form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
