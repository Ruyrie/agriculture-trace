<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">数据指纹</h3>
        <span class="page-subtitle">共 {{ total }} 个产品</span>
      </div>
      <div class="page-actions">
        <el-button type="success" plain @click="exportFingerprintReport" :disabled="loading || records.length === 0">
          <el-icon><Download /></el-icon> 导出报告
        </el-button>
        <el-button type="primary" plain @click="fetchData" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <div class="summary-grid">
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">全局根哈希</div>
        <div class="root-hash-row">
          <div class="hash-line">{{ rootHash || '-' }}</div>
          <el-button type="primary" plain size="small" @click="copyRootHash" :disabled="!rootHash">
            <el-icon><CopyDocument /></el-icon>
            复制
          </el-button>
        </div>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">生成时间</div>
        <div class="summary-value">{{ generatedAt || '-' }}</div>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">校验状态</div>
        <el-tag :type="invalidCount === 0 ? 'success' : 'danger'" size="large">
          {{ invalidCount === 0 ? '全部一致' : `${invalidCount} 项异常` }}
        </el-tag>
      </el-card>
    </div>

    <el-card shadow="never" class="content-card">
      <el-table :data="records" stripe class="integrity-table" v-loading="loading">
        <el-table-column prop="name" label="产品名称" min-width="130" />
        <el-table-column prop="category" label="类别" width="110" />
        <el-table-column prop="origin" label="产地" min-width="140" />
        <el-table-column label="存储指纹" min-width="240">
          <template #default="{ row }">
            <code class="hash-code">{{ row.storedHash || '未生成' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="当前指纹" min-width="240">
          <template #default="{ row }">
            <code class="hash-code">{{ row.currentHash }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.valid ? 'success' : 'danger'" size="small">
              {{ row.valid ? '一致' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="verify(row)">验证</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * IntegrityReport.vue — 数据指纹（产品完整性校验）页面。
 *
 * 功能：
 *   - 展示全局根哈希（rootHash）和所有产品的 storedHash vs currentHash 对比结果。
 *   - 每条记录显示"一致"或"异常"状态标签。
 *   - "验证"按钮触发单条产品指纹重算对比，刷新该行状态。
 *   - "导出报告"按钮将当前指纹列表导出为 CSV 文件，方便留存证据。
 *   - 页面停留时间期间实时更新"生成时间"时钟（前端 setInterval，不重新请求接口）。
 *
 * 关联：
 *   - api/integrity.js（getProductFingerprints / verifyProductHash）
 *   - 仅 ROLE_ADMIN 和 ROLE_INSPECTOR 可访问（router meta.roles + 后端 SecurityConfig）
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Download, Refresh } from '@element-plus/icons-vue'
import { getProductFingerprints, verifyProductHash } from '@/api/integrity'

// 指纹列表加载状态，控制表格 v-loading 和刷新按钮 :loading。
const loading = ref(false)
// 产品指纹记录列表，每项含 id/name/category/origin/storedHash/currentHash/valid。
const records = ref([])
// 产品总数，来自后端 res.data.total，显示在页面标题行。
const total = ref(0)
// 全局根哈希，将所有产品的 currentHash 串联后再 SHA-256 得到，显示在摘要卡片中。
const rootHash = ref('')
// 指纹生成时间文本；首次加载时由 updateClock() 设置，之后每秒由 clockTimer 刷新。
const generatedAt = ref('')
// setInterval 定时器句柄，组件卸载时在 onUnmounted 中 clearInterval 防止内存泄漏。
let clockTimer = null

// 统计当前指纹列表中不一致的记录数，用于页面头部风险提示。
const invalidCount = computed(() => records.value.filter(item => !item.valid).length)

// 与页面停留时间保持一致的实时时钟，不依赖刷新接口。
const formatDateTime = (date) => {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const updateClock = () => {
  generatedAt.value = formatDateTime(new Date())
}

// 拉取产品指纹列表、总数、根哈希和生成时间。
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProductFingerprints()
    if (res.code === 200) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
      rootHash.value = res.data.rootHash || ''
      updateClock()
    } else {
      ElMessage.error(res.message || '数据指纹加载失败')
    }
  } finally {
    loading.value = false
  }
}

// 校验单个产品指纹；完成后刷新列表以展示最新 currentHash。
const verify = async (row) => {
  const res = await verifyProductHash(row.id)
  if (res.code === 200 && res.data?.valid) {
    ElMessage.success('哈希一致，数据未发现篡改')
  } else {
    ElMessage.warning('哈希不一致，数据可能被改动')
  }
  fetchData()
}

// 将全局 rootHash 复制到剪贴板，方便对外校验或留档。
const copyRootHash = async () => {
  try {
    await navigator.clipboard.writeText(rootHash.value)
    ElMessage.success('复制成功')
  } catch {
    ElMessage.error('复制失败')
  }
}

const csvCell = (value) => {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

const reportTimestamp = () => {
  const now = new Date()
  const pad = value => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

// 导出当前数据指纹报告，包含摘要和每条产品的存储/当前指纹。
const exportFingerprintReport = () => {
  if (records.value.length === 0) {
    ElMessage.warning('暂无可导出的数据指纹')
    return
  }

  const summaryRows = [
    ['报告名称', '数据指纹报告'],
    ['生成时间', generatedAt.value || formatDateTime(new Date())],
    ['全局根哈希', rootHash.value || '-'],
    ['产品总数', total.value],
    ['异常数量', invalidCount.value],
    [],
    ['产品ID', '产品名称', '类别', '产地', '存储指纹', '当前指纹', '校验状态']
  ]
  const dataRows = records.value.map(item => [
    item.id,
    item.name,
    item.category,
    item.origin,
    item.storedHash || '未生成',
    item.currentHash || '',
    item.valid ? '一致' : '异常'
  ])
  const csv = [...summaryRows, ...dataRows]
    .map(row => row.map(csvCell).join(','))
    .join('\r\n')

  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `数据指纹报告_${reportTimestamp()}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报告导出成功')
}

onMounted(() => {
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)
  fetchData()
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})
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

.page-subtitle,
.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(180px, 0.7fr) minmax(160px, 0.5fr);
  gap: 12px;
}

.summary-card,
.content-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.summary-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 72px;
}

.root-hash-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.summary-value {
  color: #1f2937;
  font-weight: 700;
}

.hash-line {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  color: #235c2f;
  font-size: 13px;
  overflow-wrap: anywhere;
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

.integrity-table {
  border-radius: 8px;
  overflow: hidden;
}

@media (max-width: 980px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
