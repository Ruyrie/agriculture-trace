<template>
  <div class="announcement-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">{{ isAdmin ? '公告管理' : '系统公告' }}</h3>
        <span class="page-subtitle">
          {{ isAdmin ? '发布、编辑系统公告，向全体用户通知重要信息' : '查看系统发布的最新通知与公告' }}
        </span>
      </div>
      <el-button v-if="isAdmin" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 发布公告
      </el-button>
    </div>

    <el-card shadow="never" class="content-card">
      <!-- 管理端筛选 -->
      <div v-if="isAdmin" class="toolbar">
        <el-select v-model="filter.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-input
          v-model="filter.keyword"
          placeholder="搜索标题"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" plain @click="search">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <!-- 普通用户：卡片式公告流 -->
      <div v-if="!isAdmin" v-loading="loading" class="notice-list">
        <div v-for="row in tableData" :key="row.id" class="notice-item" @click="openDetail(row)">
          <div class="notice-head">
            <el-tag v-if="row.pinned" type="danger" size="small" effect="plain">置顶</el-tag>
            <span class="notice-title">{{ row.title }}</span>
            <span class="notice-time">{{ row.createTime }}</span>
          </div>
          <div class="notice-preview">{{ row.content }}</div>
        </div>
        <el-empty v-if="!loading && tableData.length === 0" description="暂无公告" />
      </div>

      <!-- 管理员：表格 -->
      <el-table v-else :data="tableData" stripe v-loading="loading">
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" type="danger" size="small" effect="plain" style="margin-right: 6px">置顶</el-tag>
            <span>{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="发布人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              <el-icon><View /></el-icon> 查看
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              <el-icon><component :is="row.status === 'PUBLISHED' ? 'Hide' : 'Promotion'" /></el-icon>
              {{ row.status === 'PUBLISHED' ? '下线' : '发布' }}
            </el-button>
            <el-button link type="danger" @click="removeOne(row)">
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
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗（管理员） -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑公告' : '发布公告'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="5000"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="PUBLISHED">立即发布</el-radio>
            <el-radio value="DRAFT">存为草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="current?.title" width="600px">
      <div v-if="current" class="detail">
        <div class="detail-meta">
          <el-tag v-if="current.pinned" type="danger" size="small" effect="plain">置顶</el-tag>
          <span>发布人：{{ current.creator || '系统' }}</span>
          <span>{{ current.createTime }}</span>
        </div>
        <div class="detail-content">{{ current.content }}</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Announcement.vue — 系统公告页面。
 *
 * 按角色呈现两种形态：
 *   - 管理员：表格化管理（发布/编辑/下线/删除/置顶），可按状态、标题筛选。
 *   - 普通用户：只读卡片流，仅展示已发布公告，点击查看详情。
 *
 * 角色来源：localStorage.userInfo.role。接口来源：src/api/announcement.js。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Edit, Delete } from '@element-plus/icons-vue'
import {
  getPublishedAnnouncements, getAnnouncementList,
  createAnnouncement, updateAnnouncement,
  changeAnnouncementStatus, deleteAnnouncement
} from '@/api/announcement'

const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const isAdmin = computed(() => userInfo.role === 'ROLE_ADMIN')

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filter = reactive({ status: '', keyword: '' })

const fetchList = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    const res = isAdmin.value
      ? await getAnnouncementList({ ...params, status: filter.status, keyword: filter.keyword })
      : await getPublishedAnnouncements(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  fetchList()
}

/* ---------- 新增 / 编辑 ---------- */
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: '', title: '', content: '', pinned: false, status: 'PUBLISHED' })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const resetForm = (row) => {
  form.id = row?.id || ''
  form.title = row?.title || ''
  form.content = row?.content || ''
  form.pinned = row?.pinned || false
  form.status = row?.status || 'PUBLISHED'
}

const openCreate = () => {
  resetForm(null)
  editVisible.value = true
}

const openEdit = (row) => {
  resetForm(row)
  editVisible.value = true
}

const doSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { title: form.title, content: form.content, pinned: form.pinned, status: form.status }
    const res = form.id
      ? await updateAnnouncement(form.id, payload)
      : await createAnnouncement(payload)
    if (res.code === 200) {
      ElMessage.success(form.id ? '已保存' : '已发布')
      editVisible.value = false
      fetchList()
    }
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const next = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  const res = await changeAnnouncementStatus(row.id, next)
  if (res.code === 200) {
    ElMessage.success(next === 'PUBLISHED' ? '已发布' : '已下线')
    fetchList()
  }
}

const removeOne = async (row) => {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？删除后不可恢复。`, '提示', { type: 'warning' })
  const res = await deleteAnnouncement(row.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    fetchList()
  }
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const current = ref(null)
const openDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

onMounted(fetchList)
</script>

<style scoped>
.announcement-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
}

.content-card {
  border-radius: 10px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 用户端卡片流 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.notice-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.notice-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #c6e2ff;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.notice-time {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.notice-preview {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 详情 */
.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
