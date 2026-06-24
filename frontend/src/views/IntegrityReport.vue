<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">数据指纹</h3>
        <span class="page-subtitle">共 {{ total }} 个产品</span>
      </div>
      <el-button type="primary" plain @click="fetchData" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import { getProductFingerprints, verifyProductHash } from '@/api/integrity'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const rootHash = ref('')
const generatedAt = ref('')
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
}

.page-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
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
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
