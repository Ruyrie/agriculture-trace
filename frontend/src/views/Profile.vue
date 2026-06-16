<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <span>个人中心</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基本信息选项卡 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px">
            <el-form-item label="头像">
              <el-upload
                  class="avatar-uploader"
                  action="#"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="handleAvatarUpload"
              >
                <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
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
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changing">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { updateProfile as updateProfileApi, changePassword as changePasswordApi, uploadAvatar as uploadAvatarApi } from '@/api/user'
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
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
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
  ]
}

// 设置默认头像
const defaultAvatar =
'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjAiIGhlaWdodD0iMTIwIiB2aWV3Qm94PSIwIDAgMTIwIDEyMCI+PHJlY3Qgd2lkdGg9IjEyMCIgaGVpZ2h0PSIxMjAiIGZpbGw9IiNFMEUwRTAiLz48Y2lyY2xlIGN4PSI2MCIgY3k9IjQ1IiByPSIyMCIgZmlsbD0iIzlFOUU5RSIvPjxwYXRoIGZpbGw9IiM5RTlFOUUiIGQ9Ik0zMCA4MCBMOTAgODAgTDgwIDY1IEw3MCA2NSBMNzAgNzUgTDUwIDc1IEw1MCA2NSBMNDAgNjVaIi8+PC9zdmc+'

// 从 localStorage 加载用户信息，并把后端相对头像路径转换成浏览器可访问的完整 URL。
const loadUserInfo = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  profileForm.id = userInfo.id || null
  profileForm.username = userInfo.username || ''
  profileForm.nickname = userInfo.nickname || userInfo.username || ''
  profileForm.phone = userInfo.phone || ''
  // 处理头像相对路径，需要拼上后端域名。
  const avatarPath = userInfo.avatar
  if (avatarPath) {
    profileForm.avatar = avatarPath.startsWith('http') ? avatarPath : `${import.meta.env.VITE_API_BASE_URL}${avatarPath}`
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

// 修改密码；成功后清空本地登录态并跳回登录页，要求用户重新登录。
const changePassword = async () => {
  await pwdFormRef.value.validate()
  changing.value = true
  try {
    const res = await changePasswordApi({
      id: profileForm.id,
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('sessionActive')
      localStorage.removeItem('userInfo')
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(res.message || '原密码错误')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('修改异常，请稍后重试')
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
      const avatarUrl = `${import.meta.env.VITE_API_BASE_URL}${res.data.url}`
      profileForm.avatar = avatarUrl
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
.profile-container {
  padding: 20px;
}
.avatar-uploader {
  display: inline-block;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}
.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}
</style>
