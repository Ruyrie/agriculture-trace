<template>
  <div class="profile-page">
    <!-- 顶部资料横幅：头像 + 昵称 + 角色 -->
    <div class="profile-hero">
      <el-upload
        class="hero-avatar-upload"
        action="#"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        :http-request="handleAvatarUpload"
      >
        <div class="hero-avatar">
          <img v-if="profileForm.avatar" :src="profileForm.avatar" class="hero-avatar-img" alt="头像" />
          <el-icon v-else class="hero-avatar-icon"><UserFilled /></el-icon>
          <div class="hero-avatar-mask">
            <el-icon><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </div>
      </el-upload>

      <div class="hero-info">
        <h1 class="hero-name">{{ profileForm.nickname || profileForm.username || '未命名用户' }}</h1>
        <div class="hero-meta">
          <el-tag class="hero-role" effect="light" round>{{ profileForm.role || '用户' }}</el-tag>
          <span class="hero-username">@{{ profileForm.username }}</span>
        </div>
        <p class="hero-phone">
          <el-icon><Iphone /></el-icon>
          {{ profileForm.phone || '未绑定手机号' }}
        </p>
      </div>
    </div>

    <!-- 资料编辑卡片：左侧表单 + 右侧说明，充分利用横向空间 -->
    <el-card class="profile-card" shadow="never">
      <div class="card-grid">
        <div class="card-main">
          <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
            <!-- 基本信息选项卡 -->
            <el-tab-pane name="info">
              <template #label>
                <span class="tab-label"><el-icon><User /></el-icon> 基本信息</span>
              </template>
              <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px" class="profile-form">
                <el-form-item label="用户名">
                  <el-input v-model="profileForm.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" :prefix-icon="EditPen" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" :prefix-icon="Iphone" maxlength="11" />
                </el-form-item>
                <el-form-item label="角色">
                  <el-input v-model="profileForm.role" disabled />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="updateProfile" :loading="saving">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 修改密码选项卡 -->
            <el-tab-pane name="password">
              <template #label>
                <span class="tab-label"><el-icon><Lock /></el-icon> 修改密码</span>
              </template>
              <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" class="profile-form">
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入 6-20 位新密码（不能与原密码相同）" :prefix-icon="Key" show-password />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" :prefix-icon="Key" show-password />
                </el-form-item>
                <el-form-item label="图形验证码" prop="captcha">
                  <div class="captcha-row">
                    <el-input v-model="pwdForm.captcha" placeholder="请输入图中字符" :prefix-icon="Picture" clearable />
                    <el-tooltip content="看不清？点击刷新" placement="top">
                      <img
                        v-if="captchaImg"
                        :src="captchaImg"
                        class="captcha-img"
                        alt="图形验证码"
                        @click="refreshPwdCaptcha"
                      />
                      <div v-else class="captcha-img captcha-img--loading" @click="refreshPwdCaptcha">加载中…</div>
                    </el-tooltip>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="changePassword" :loading="changing">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>

        <aside class="card-side">
          <h3 class="side-title"><el-icon><InfoFilled /></el-icon> 账号与安全</h3>
          <ul class="side-list">
            <li><span class="dot"></span>用户名与角色由管理员分配，无法自行修改。</li>
            <li><span class="dot"></span>建议绑定常用手机号，便于忘记密码时找回。</li>
            <li><span class="dot"></span>新密码长度 6-20 位，且不能与原密码相同。</li>
            <li><span class="dot"></span>修改密码成功后需要重新登录。</li>
          </ul>
          <div class="side-tip">
            忘记原密码？可在登录页通过「忘记密码」+ 图形验证码重置。
          </div>
        </aside>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, EditPen, InfoFilled, Iphone, Key, Lock, Picture, User, UserFilled } from '@element-plus/icons-vue'
import { updateProfile as updateProfileApi, changePassword as changePasswordApi, uploadAvatar as uploadAvatarApi, getCaptcha } from '@/api/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('info')
const saving = ref(false)
const changing = ref(false)

// 基本信息表单
const profileFormRef = ref()
const profileForm = reactive({
  id: null,
  username: '',
  nickname: '',
  phone: '',
  avatar: '',
  role: ''
})

const profileRules = {
  nickname: [{ min: 2, max: 20, message: '昵称长度2-20个字符', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// 密码表单
const pwdFormRef = ref()
const captchaImg = ref('')
const pwdForm = reactive({
  newPassword: '',
  confirmPassword: '',
  captcha: ''
})

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }]
}

// 设置默认头像
const defaultAvatar =
'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjAiIGhlaWdodD0iMTIwIiB2aWV3Qm94PSIwIDAgMTIwIDEyMCI+PHJlY3Qgd2lkdGg9IjEyMCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiNFMEUwRTAiLz48Y2lyY2xlIGN4PSI2MCIgY3k9IjQ1IiByPSIyMCIgZmlsbD0iIzlFOUU5RSIvPjxwYXRoIGZpbGw9IiM5RTlFOUUiIGQ9Ik0zMCA4MCBMOTAgODAgTDgwIDY1IEw3MCA2NSBMNzAgNzUgTDUwIDc1IEw1MCA2NSBMNDAgNjVaIi8+PC9zdmc+'

const resolveAssetUrl = (url) => {
  if (!url || url.startsWith('http') || url.startsWith('data:')) return url
  return import.meta.env.DEV ? url : `${import.meta.env.VITE_API_BASE_URL}${url}`
}

// 从 localStorage 加载用户信息，并把后端相对头像路径转换成浏览器可访问地址。
const loadUserInfo = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  profileForm.id = userInfo.id || null
  profileForm.username = userInfo.username || ''
  profileForm.nickname = userInfo.nickname || userInfo.username || ''
  profileForm.phone = userInfo.phone || ''
  const avatarPath = userInfo.avatar
  if (avatarPath) {
    profileForm.avatar = resolveAssetUrl(avatarPath)
  } else {
    profileForm.avatar = defaultAvatar
  }
  profileForm.role = roleLabel(userInfo.role)
}

// 将后端角色编码转换成页面展示文案。
const roleLabel = (role) => ({
  ROLE_ADMIN: '管理员',
  ROLE_FARMER: '农户',
  ROLE_INSPECTOR: '监管员'
})[role] || role || ''

// 保存基本资料；成功后刷新 localStorage 并通知 Layout 同步右上角头像/用户名。
const updateProfile = async () => {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: profileForm.id,
      nickname: profileForm.nickname,
      phone: profileForm.phone
    }
    // 注意：如果有上传新头像，因为已经是绝对路径，我们需要截取相对路径传给后端
    if (profileForm.avatar && profileForm.avatar.includes('/uploads/')) {
        payload.avatar = profileForm.avatar.substring(profileForm.avatar.indexOf('/uploads/'));
    }
    const res = await updateProfileApi(payload)
    if (res.code === 200) {
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      // 通知 Layout 顶部立即同步头像和用户名
      window.dispatchEvent(new CustomEvent('userInfoUpdated'))
      ElMessage.success('保存成功')
      loadUserInfo() // 重新加载整理后的数据
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('保存异常，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 请求一张新的图形验证码；答案保存在后端 Session，前端只拿到图片。
const refreshPwdCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.code === 200) captchaImg.value = res.data.image
  } catch {
    // 网络异常已由拦截器统一提示。
  }
}

// 切到“修改密码”标签时按需加载验证码，避免进页面就请求。
const handleTabChange = (name) => {
  if (name === 'password' && !captchaImg.value) {
    refreshPwdCaptcha()
  }
}

// 修改密码；验证码为一次性，成功后清本地登录态并跳回登录页，失败则刷新验证码重试。
const changePassword = async () => {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  changing.value = true
  try {
    const res = await changePasswordApi({
      id: profileForm.id,
      newPassword: pwdForm.newPassword,
      captcha: pwdForm.captcha
    })

    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('sessionActive')
      localStorage.removeItem('userInfo')
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(res.message || '修改失败')
      pwdForm.captcha = ''
      refreshPwdCaptcha()
    }
  } catch (error) {
    if (!error.__handled) ElMessage.error('修改异常，请稍后重试')
    pwdForm.captcha = ''
    refreshPwdCaptcha()
  } finally {
    changing.value = false
  }
}

// 上传头像前校验文件类型和大小，避免把非图片或超大文件发给后端。
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt50M = file.size / 1024 / 1024 <= 50
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt50M) {
    ElMessage.error('图片大小不能超过 50MB')
    return false
  }
  return true
}

// 使用 multipart/form-data 上传头像，并把后端返回的头像 URL 同步到表单和本地用户信息。
const handleAvatarUpload = async (options) => {
  const file = options.file
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await uploadAvatarApi(formData)
    if (res.code === 200) {
      profileForm.avatar = resolveAssetUrl(res.data.url)
      if (res.data.user) {
        localStorage.setItem('userInfo', JSON.stringify(res.data.user))
        window.dispatchEvent(new CustomEvent('userInfoUpdated'))
      }
      ElMessage.success('头像已更新')
    } else {
      ElMessage.error(res.message || '头像上传失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('头像上传异常')
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

/* ===== 顶部资料横幅 ===== */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 88% 12%, rgba(255, 255, 255, 0.18), transparent 42%),
    linear-gradient(135deg, #1b5e20 0%, #2e7d32 55%, #43a047 100%);
  color: #fff;
  box-shadow: 0 10px 28px rgba(46, 125, 50, 0.28);
}

.hero-avatar-upload :deep(.el-upload) {
  border-radius: 50%;
}

.hero-avatar {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.18);
  border: 3px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-avatar-icon {
  font-size: 46px;
  color: rgba(255, 255, 255, 0.85);
}

.hero-avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.hero-avatar-mask .el-icon {
  font-size: 20px;
}

.hero-avatar:hover .hero-avatar-mask {
  opacity: 1;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-name {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hero-role {
  border: none;
  font-weight: 600;
}

.hero-username {
  font-size: 13px;
  opacity: 0.85;
}

.hero-phone {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

/* ===== 资料编辑卡片 ===== */
.profile-card {
  border-radius: 14px;
  border: 1px solid #eef0f3;
}

.profile-card :deep(.el-card__body) {
  padding: 8px 24px 24px;
}

/* 左表单 + 右说明的两列布局，填满卡片横向空间 */
.card-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(240px, 1fr);
  gap: 32px;
}

.card-main {
  min-width: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.profile-form {
  max-width: 520px;
  margin-top: 18px;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 22px;
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
  border-color: #409eff;
}

.captcha-img--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
  background: #f5f7fa;
}

/* 右侧账号与安全说明 */
.card-side {
  margin-top: 18px;
  padding: 20px;
  border-radius: 12px;
  background: linear-gradient(160deg, #f4f9f4, #eef5ff);
  border: 1px solid #eef0f3;
}

.side-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.side-list {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-list li {
  position: relative;
  padding-left: 16px;
  font-size: 13px;
  line-height: 1.6;
  color: #5a6570;
}

.side-list .dot {
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2e7d32;
}

.side-tip {
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px dashed #cfe0d2;
  font-size: 12.5px;
  line-height: 1.7;
  color: #5a8a5e;
}

/* 窄屏回落为单列 */
@media (max-width: 900px) {
  .card-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
