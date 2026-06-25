<template>
  <div class="user-management">
    <div class="page-header">
      <div>
        <h3 class="page-title">用户管理</h3>
        <span class="page-subtitle">管理员维护账号、角色和启用状态</span>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名或手机号"
          clearable
          prefix-icon="Search"
          @keyup.enter="fetchUsers"
          @clear="fetchUsers"
        />
        <el-button type="primary" plain @click="fetchUsers">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tooltip
              :disabled="!isSelf(row)"
              content="不能禁用当前登录账号"
              placement="top"
            >
              <span class="status-switch-wrapper">
                <el-switch
                  v-model="row.enabled"
                  :before-change="() => updateStatus(row)"
                  :disabled="isSelf(row)"
                  active-text="启用"
                  inactive-text="禁用"
                  inline-prompt
                />
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button link type="warning" @click="resetPassword(row)">
              <el-icon><Refresh /></el-icon> 重置密码
            </el-button>
            <el-button link type="danger" :disabled="isSelf(row)" @click="deleteUser(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ROLE_ADMIN" />
            <el-option label="农户" value="ROLE_FARMER" />
            <el-option label="监管员" value="ROLE_INSPECTOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-tooltip
            :disabled="!isEditingSelf"
            content="不能禁用当前登录账号"
            placement="top"
          >
            <span class="status-switch-wrapper">
              <el-switch
                v-model="form.enabled"
                :disabled="isEditingSelf"
                active-text="启用"
                inactive-text="禁用"
              />
            </span>
          </el-tooltip>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref()

const form = reactive({
  id: '',
  username: '',
  password: '',
  nickname: '',
  phone: '',
  role: 'ROLE_FARMER',
  enabled: true
})

const usernamePattern = /^[A-Za-z][A-Za-z0-9_]{2,31}$/

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: usernamePattern, message: '用户名需为3-32位英文、数字或下划线，并以英文字母开头', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 从本地缓存读取当前登录用户 ID，用于防止禁用/删除自己。
const currentUserId = () => JSON.parse(localStorage.getItem('userInfo') || '{}').id

// 判断某一行用户是否为当前登录用户。
const isSelf = (row) => row.id === currentUserId()

// 编辑弹窗是否正在编辑当前登录用户，用于禁用危险操作。
const isEditingSelf = computed(() => form.id && form.id === currentUserId())

// 将角色编码转换为中文标签。
const roleLabel = (role) => ({
  ROLE_ADMIN: '管理员',
  ROLE_FARMER: '农户',
  ROLE_INSPECTOR: '监管员'
})[role] || role

// 用户列表读取后端分页结果，字段与 Result.data.records 对齐。
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/users', {
      params: { page: page.value, pageSize: pageSize.value, keyword: keyword.value }
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 重置用户表单到新增默认值。
const resetForm = () => {
  Object.assign(form, {
    id: '',
    username: '',
    password: '',
    nickname: '',
    phone: '',
    role: 'ROLE_FARMER',
    enabled: true
  })
}

// 打开新增用户弹窗。
const openCreate = () => {
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

// 打开编辑用户弹窗，并把当前行数据回填到表单。
const openEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    phone: row.phone,
    role: row.role,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

// 提交新增或编辑用户表单，后端负责密码哈希和角色绑定。
const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    const res = form.id
      ? await request.put(`/users/${form.id}`, payload)
      : await request.post('/users', payload)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchUsers()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    submitting.value = false
  }
}

// 启用/禁用用户；当前登录账号不允许被自己禁用。
const updateStatus = async (row) => {
  if (isSelf(row)) {
    ElMessage.warning('不能禁用当前登录账号')
    return false
  }
  const nextEnabled = !row.enabled
  const res = await request.put(`/users/${row.id}/status`, { enabled: nextEnabled })
  if (res.code === 200) {
    ElMessage.success(nextEnabled ? '已启用' : '已禁用')
    return true
  }
  ElMessage.error(res.message || '状态更新失败')
  return false
}

// 管理员将指定用户密码重置为默认 123456。
const resetPassword = (row) => {
  ElMessageBox.confirm(`确定将 ${row.username} 的密码重置为 123456 吗？`, '重置密码', {
    type: 'warning'
  }).then(async () => {
    const res = await request.put(`/users/${row.id}/password`, { password: '123456' })
    if (res.code === 200) {
      ElMessage.success('密码已重置为 123456')
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  })
}

// 删除用户前二次确认，并禁止删除当前登录账号。
const deleteUser = (row) => {
  if (isSelf(row)) {
    ElMessage.warning('不能删除当前登录账号')
    return
  }
  ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？删除后不可恢复。`, '删除用户', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    const res = await request.delete(`/users/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (tableData.value.length === 1 && page.value > 1) {
        page.value -= 1
      }
      fetchUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
}

.content-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.toolbar {
  display: flex;
  gap: 10px;
  width: 420px;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.status-switch-wrapper {
  display: inline-flex;
}
</style>
