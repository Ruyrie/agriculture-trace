<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">审计日志</h3>
        <span class="page-subtitle">共 {{ total }} 条记录</span>
      </div>
      <div class="page-actions">
        <el-button type="success" plain @click="verifyChain" :loading="verifying">
          <el-icon><CircleCheck /></el-icon> 验证链条完整性
        </el-button>
        <el-button type="primary" plain @click="fetchLogs" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="verifyResult"
      :type="verifyResult.valid ? 'success' : 'error'"
      :title="verifyResult.message"
      :description="verifyDescription"
      show-icon
      :closable="false"
    />

    <el-card shadow="never" class="content-card">
      <el-table :data="logs" stripe class="log-table" v-loading="loading">
        <el-table-column prop="timestamp" label="时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="actionType(row.actionType)" size="small">{{ row.actionType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象" width="110" align="center" />
        <el-table-column prop="targetId" label="对象ID" min-width="160" />
        <el-table-column label="上一哈希" min-width="190">
          <template #default="{ row }">
            <code class="hash-code">{{ row.previousHash }}</code>
          </template>
        </el-table-column>
        <el-table-column label="本条哈希" min-width="210">
          <template #default="{ row }">
            <code class="hash-code">{{ row.dataHash }}</code>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <Pagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @update:page="fetchLogs"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="min(760px, calc(100vw - 32px))">
      <div class="detail-grid" v-if="currentLog">
        <div>
          <div class="detail-title">操作前</div>
          <pre>{{ formatJson(currentLog.dataBefore) }}</pre>
        </div>
        <div>
          <div class="detail-title">操作后</div>
          <pre>{{ formatJson(currentLog.dataAfter) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Refresh } from '@element-plus/icons-vue'
import { getAuditLogs, verifyAuditLogChain } from '@/api/blockchain'
import Pagination from '@/components/Pagination.vue'

const logs = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const verifying = ref(false)
const verifyResult = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

const verifyDescription = computed(() => {
  const items = verifyResult.value?.invalidItems || []
  if (!items.length) return ''
  return items
    .slice(0, 5)
    .map(item => `${item.targetType} ${item.targetId}${item.name ? `（${item.name}）` : ''}`)
    .join('；')
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs({ page: page.value, pageSize: pageSize.value })
    if (res.code === 200) {
      logs.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '审计日志加载失败')
    }
  } finally {
    loading.value = false
  }
}

const handlePageSizeChange = () => {
  page.value = 1
  fetchLogs()
}

const verifyChain = async () => {
  verifying.value = true
  try {
    const res = await verifyAuditLogChain()
    if (res.code === 200) {
      verifyResult.value = res.data
      if (res.data.valid) {
        ElMessage.success(res.data.message)
      } else {
        ElMessage.error(res.data.message)
      }
    }
  } finally {
    verifying.value = false
  }
}

const openDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const actionType = (type) => {
  if (type === 'CREATE') return 'success'
  if (type === 'UPDATE') return 'warning'
  if (type === 'DELETE') return 'danger'
  return 'info'
}

const formatJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

fetchLogs()
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.page-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.page-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
}

.content-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.log-table {
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.hash-code {
  display: inline-block;
  max-width: 100%;
  padding: 2px 0;
  background: transparent;
  color: #374151;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.detail-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-weight: 700;
}

pre {
  min-height: 220px;
  max-height: 420px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #374151;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 860px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
