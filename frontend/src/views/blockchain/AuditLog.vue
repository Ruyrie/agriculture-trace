<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">审计日志</h3>
        <span class="page-subtitle">共 {{ total }} 条记录</span>
        <el-tag
          v-if="verifyResult"
          :type="verifyResult.logChainValid ? 'success' : 'danger'"
          effect="dark"
          size="small"
        >
          链条完整性：{{ verifyResult.logChainValid ? '正常' : '异常' }}
        </el-tag>
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
      :title="verifyTitle"
      show-icon
      :closable="false"
    >
      <template #default>
        <div class="verify-detail">
          <div>{{ verifyResult.message }}</div>
          <div v-if="brokenInfo" class="verify-broken">{{ brokenInfo }}</div>
          <div v-if="verifyDescription" class="verify-items">{{ verifyDescription }}</div>
        </div>
      </template>
    </el-alert>

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
        <div class="detail-col">
          <div class="detail-title">操作前</div>
          <div v-if="beforeFields.length" class="field-list">
            <div v-for="f in beforeFields" :key="f.label" class="field-row">
              <span class="field-label">{{ f.label }}</span>
              <span class="field-value">
                <HashTag v-if="f.isHash" :hash="f.value" />
                <template v-else>{{ f.value }}</template>
              </span>
            </div>
          </div>
          <div v-else class="field-empty">无（新增操作没有操作前数据）</div>
        </div>
        <div class="detail-col">
          <div class="detail-title">操作后</div>
          <div v-if="afterFields.length" class="field-list">
            <div v-for="f in afterFields" :key="f.label" class="field-row">
              <span class="field-label">{{ f.label }}</span>
              <span class="field-value">
                <HashTag v-if="f.isHash" :hash="f.value" />
                <template v-else>{{ f.value }}</template>
              </span>
            </div>
          </div>
          <div v-else class="field-empty">无（删除操作没有操作后数据）</div>
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
import HashTag from '@/components/HashTag.vue'

const logs = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const verifying = ref(false)
const verifyResult = ref(null)
const detailVisible = ref(false)
const currentLog = ref(null)

// 顶部标题直接给出“正常/异常”结论，便于一眼判断链条状态。
const verifyTitle = computed(() => {
  if (!verifyResult.value) return ''
  return verifyResult.value.valid ? '验证结果：正常' : '验证结果：异常'
})

// 链条断裂时明确指出断裂位置（第几条 + 日志ID），方便定位问题。
const brokenInfo = computed(() => {
  const result = verifyResult.value
  if (!result || result.logChainValid !== false) return ''
  if (!result.brokenIndex) return ''
  return `断裂位置：第 ${result.brokenIndex} 条日志（日志ID：${result.brokenLogId || '未知'}）`
})

const verifyDescription = computed(() => {
  const items = verifyResult.value?.invalidItems || []
  if (!items.length) return ''
  const prefix = `指纹异常 ${items.length} 项：`
  return prefix + items
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

// 审计详情用业务字段+中文标签展示；数据指纹（dataHash）改用可点击展开+复制的缩略样式，
// 既能查看对应的完整哈希，又不会让长串代码铺满详情。
const FIELD_LABELS = {
  id: 'ID',
  name: '产品名称',
  category: '类别',
  origin: '产地',
  price: '价格(元/kg)',
  createTime: '创建时间',
  batchNo: '批次号',
  productId: '所属产品',
  productName: '产品名称',
  productionDate: '生产日期',
  remark: '备注',
  dataHash: '数据指纹'
}

const parseFields = (value) => {
  if (!value) return []
  let obj
  try {
    obj = JSON.parse(value)
  } catch {
    return []
  }
  if (!obj || typeof obj !== 'object') return []
  return Object.entries(obj)
    .filter(([, val]) => val !== null && val !== '')
    .map(([key, val]) => ({
      label: FIELD_LABELS[key] || key,
      value: String(val),
      isHash: key === 'dataHash'
    }))
}

const beforeFields = computed(() => parseFields(currentLog.value?.dataBefore))
const afterFields = computed(() => parseFields(currentLog.value?.dataAfter))

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

.verify-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.verify-broken {
  font-weight: 600;
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

.field-list {
  min-height: 220px;
  max-height: 420px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 8px 12px;
}

.field-row {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #e5e7eb;
  font-size: 13px;
  line-height: 1.5;
}

.field-row:last-child {
  border-bottom: none;
}

.field-label {
  flex: 0 0 96px;
  color: #6b7280;
  font-weight: 600;
}

.field-value {
  flex: 1;
  color: #1f2937;
  word-break: break-word;
}

.field-empty {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #9ca3af;
  font-size: 13px;
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
