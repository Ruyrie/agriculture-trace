<template>
  <div class="feedback-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">意见反馈</h3>
        <span class="page-subtitle">
          {{ isAdmin ? '查看并回复用户对系统的意见与问题' : '提交您对系统的意见或问题，管理员会及时回复' }}
        </span>
      </div>
      <el-button v-if="!isAdmin" type="primary" @click="openSubmit">
        <el-icon><EditPen /></el-icon> 我要反馈
      </el-button>
    </div>

    <!-- 管理员：汇总卡片，反馈过多时先看全局再点进明细 -->
    <div v-if="isAdmin" class="summary-cards">
      <div class="summary-card" :class="{ active: filter.status === '' }" @click="filterByStatus('')">
        <div class="summary-num">{{ summary.total }}</div>
        <div class="summary-label">全部反馈</div>
      </div>
      <div class="summary-card warning" :class="{ active: filter.status === 'PENDING' }" @click="filterByStatus('PENDING')">
        <div class="summary-num">{{ summary.pending }}</div>
        <div class="summary-label">待处理</div>
      </div>
      <div class="summary-card success" :class="{ active: filter.status === 'REPLIED' }" @click="filterByStatus('REPLIED')">
        <div class="summary-num">{{ summary.replied }}</div>
        <div class="summary-label">已回复</div>
      </div>
      <div class="summary-card info" :class="{ active: filter.status === 'CLOSED' }" @click="filterByStatus('CLOSED')">
        <div class="summary-num">{{ summary.closed }}</div>
        <div class="summary-label">已关闭</div>
      </div>
    </div>

    <el-card shadow="never" class="content-card">
      <!-- 管理员工具栏：类型 / 关键字筛选 -->
      <div v-if="isAdmin" class="toolbar">
        <el-select v-model="filter.type" placeholder="全部类型" clearable style="width: 150px" @change="fetchList">
          <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="value" />
        </el-select>
        <el-input
          v-model="filter.keyword"
          placeholder="搜索标题或提交人"
          clearable
          prefix-icon="Search"
          style="width: 240px"
          @keyup.enter="fetchList"
          @clear="fetchList"
        />
        <el-button type="primary" plain @click="fetchList">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="plain">{{ typeMap[row.type] || '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" prop="username" label="提交人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" :width="isAdmin ? 260 : 90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
            <template v-if="isAdmin">
              <el-button link type="success" :disabled="row.status === 'CLOSED'" @click="openReply(row)">
                <el-icon><ChatLineRound /></el-icon> 回复
              </el-button>
              <el-button link type="info" :disabled="row.status === 'CLOSED'" @click="closeOne(row)">
                <el-icon><CircleClose /></el-icon> 关闭
              </el-button>
              <el-button link type="danger" @click="removeOne(row)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" :description="isAdmin ? '暂无反馈' : '您还没有提交过反馈'" />

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

    <!-- 提交反馈弹窗（普通用户） -->
    <el-dialog v-model="submitVisible" title="提交意见反馈" width="520px">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="submitForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="submitForm.title" maxlength="128" show-word-limit placeholder="一句话概述您的反馈" />
        </el-form-item>
        <el-form-item label="详细描述" prop="content">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="请尽量详细描述问题或建议，便于我们处理"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="560px">
      <div v-if="current" class="detail">
        <div class="detail-row">
          <span class="detail-label">标题</span>
          <span class="detail-value">{{ current.title }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <el-tag :type="typeTagType(current.type)" effect="plain">{{ typeMap[current.type] || '其他' }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="statusTagType(current.status)">{{ statusMap[current.status] || current.status }}</el-tag>
        </div>
        <div v-if="isAdmin" class="detail-row">
          <span class="detail-label">提交人</span>
          <span class="detail-value">{{ current.username }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间</span>
          <span class="detail-value">{{ current.createTime }}</span>
        </div>
        <div class="detail-row column">
          <span class="detail-label">详细描述</span>
          <div class="detail-content">{{ current.content }}</div>
        </div>
        <div v-if="current.reply" class="reply-box">
          <div class="reply-head">
            <el-icon><ChatLineRound /></el-icon>
            管理员回复（{{ current.replyBy }} · {{ current.replyTime }}）
          </div>
          <div class="reply-content">{{ current.reply }}</div>
        </div>
        <el-empty v-else description="管理员尚未回复" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="isAdmin && current && current.status !== 'CLOSED'"
          type="primary"
          @click="openReply(current)"
        >
          回复
        </el-button>
      </template>
    </el-dialog>

    <!-- 回复弹窗（管理员） -->
    <el-dialog v-model="replyVisible" title="回复反馈" width="520px">
      <div v-if="current" class="reply-quote">
        <div class="quote-title">{{ current.title }}</div>
        <div class="quote-content">{{ current.content }}</div>
      </div>
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="0">
        <el-form-item prop="reply">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="doReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * Feedback.vue — 意见反馈页面。
 *
 * 一个页面按角色呈现两种形态：
 *   - 普通用户（农户/监管员）：右上角"我要反馈"提交意见，表格只展示自己提交过的反馈及回复。
 *   - 管理员：顶部汇总卡片（总量/待处理/已回复/已关闭，可点击筛选）+ 全量反馈列表，
 *     支持按类型、关键字筛选，并对每条反馈进行回复 / 关闭 / 删除。
 *
 * 角色来源：localStorage.userInfo.role（与 Layout.vue / 路由守卫保持一致）。
 * 接口来源：src/api/feedback.js，对应后端 FeedbackController。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  EditPen, Search, View, ChatLineRound, CircleClose, Delete
} from '@element-plus/icons-vue'
import {
  submitFeedback, getMyFeedback,
  getFeedbackList, getFeedbackSummary,
  replyFeedback, closeFeedback, deleteFeedback
} from '@/api/feedback'

// 类型与状态的中文映射，提交、筛选、表格、详情统一复用，避免散落硬编码。
const typeMap = { SUGGESTION: '功能建议', BUG: '问题报告', OTHER: '其他' }
const statusMap = { PENDING: '待处理', REPLIED: '已回复', CLOSED: '已关闭' }

// 从登录态读取当前用户角色，决定页面以"管理"还是"提交"形态呈现。
const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const isAdmin = computed(() => userInfo.role === 'ROLE_ADMIN')

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 管理员列表筛选条件（状态由汇总卡片点击切换，类型/关键字由工具栏控制）。
const filter = reactive({ status: '', type: '', keyword: '' })
// 管理员汇总统计，反馈过多时先看卡片掌握全局。
const summary = reactive({ total: 0, pending: 0, replied: 0, closed: 0 })

const typeTagType = (type) => ({ BUG: 'danger', SUGGESTION: 'success', OTHER: 'info' }[type] || 'info')
const statusTagType = (status) => ({ PENDING: 'warning', REPLIED: 'success', CLOSED: 'info' }[status] || 'info')

// 拉取列表：管理员走全量+筛选接口，普通用户走"我的反馈"接口。
const fetchList = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    let res
    if (isAdmin.value) {
      res = await getFeedbackList({ ...params, status: filter.status, type: filter.type, keyword: filter.keyword })
    } else {
      res = await getMyFeedback(params)
    }
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 管理员汇总统计。
const fetchSummary = async () => {
  const res = await getFeedbackSummary()
  if (res.code === 200) {
    Object.assign(summary, res.data)
  }
}

// 点击汇总卡片按状态筛选，回到第一页重新查询。
const filterByStatus = (status) => {
  filter.status = status
  page.value = 1
  fetchList()
}

/* ---------- 提交反馈（普通用户） ---------- */
const submitVisible = ref(false)
const submitting = ref(false)
const submitFormRef = ref()
const submitForm = reactive({ type: 'SUGGESTION', title: '', content: '' })
const submitRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

const openSubmit = () => {
  submitForm.type = 'SUGGESTION'
  submitForm.title = ''
  submitForm.content = ''
  submitVisible.value = true
}

const doSubmit = async () => {
  await submitFormRef.value.validate()
  submitting.value = true
  try {
    const res = await submitFeedback({ ...submitForm })
    if (res.code === 200) {
      ElMessage.success('反馈已提交，感谢您的意见')
      submitVisible.value = false
      page.value = 1
      fetchList()
    }
  } finally {
    submitting.value = false
  }
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const current = ref(null)
const openDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

/* ---------- 回复 / 关闭 / 删除（管理员） ---------- */
const replyVisible = ref(false)
const replying = ref(false)
const replyFormRef = ref()
const replyForm = reactive({ reply: '' })
const replyRules = { reply: [{ required: true, message: '请输入回复内容', trigger: 'blur' }] }

const openReply = (row) => {
  current.value = row
  replyForm.reply = row.reply || ''
  detailVisible.value = false
  replyVisible.value = true
}

const doReply = async () => {
  await replyFormRef.value.validate()
  replying.value = true
  try {
    const res = await replyFeedback(current.value.id, replyForm.reply)
    if (res.code === 200) {
      ElMessage.success('回复成功')
      replyVisible.value = false
      fetchList()
      fetchSummary()
    }
  } finally {
    replying.value = false
  }
}

const closeOne = async (row) => {
  await ElMessageBox.confirm(`确定关闭反馈「${row.title}」吗？`, '提示', { type: 'warning' })
  const res = await closeFeedback(row.id)
  if (res.code === 200) {
    ElMessage.success('已关闭')
    fetchList()
    fetchSummary()
  }
}

const removeOne = async (row) => {
  await ElMessageBox.confirm(`确定删除反馈「${row.title}」吗？删除后不可恢复。`, '提示', { type: 'warning' })
  const res = await deleteFeedback(row.id)
  if (res.code === 200) {
    ElMessage.success('已删除')
    fetchList()
    fetchSummary()
  }
}

onMounted(() => {
  fetchList()
  if (isAdmin.value) {
    fetchSummary()
  }
})
</script>

<style scoped>
.feedback-page {
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

/* 汇总卡片 */
.summary-cards {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 130px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid #c0c4cc;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.summary-card.active {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.summary-card.warning { border-left-color: #e6a23c; }
.summary-card.success { border-left-color: #67c23a; }
.summary-card.info { border-left-color: #909399; }

.summary-num {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
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

/* 详情 */
.detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-row.column {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.detail-label {
  font-size: 13px;
  color: #909399;
  min-width: 64px;
}

.detail-value {
  font-size: 14px;
  color: #303133;
}

.detail-content {
  width: 100%;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-box {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 12px 14px;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #67c23a;
  font-weight: 600;
  margin-bottom: 8px;
}

.reply-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-quote {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
}

.quote-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.quote-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
