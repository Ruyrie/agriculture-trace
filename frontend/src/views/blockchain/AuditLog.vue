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
        <el-tag
          v-if="verifyResult && verifyResult.logChainValid"
          :type="verifyResult.tailValid === false ? 'danger' : 'success'"
          effect="dark"
          size="small"
        >
          链尾锚点：{{ verifyResult.tailValid === false ? '异常' : '正常' }}
        </el-tag>
      </div>
      <div class="page-actions">
        <el-button type="success" plain @click="exportAuditLogs" :loading="exporting" :disabled="loading || total === 0">
          <el-icon><Download /></el-icon> 导出日志
        </el-button>
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
          <div v-if="tailInfo" class="verify-broken">{{ tailInfo }}</div>
          <div v-if="verifyDescription" class="verify-items">{{ verifyDescription }}</div>
        </div>
      </template>
    </el-alert>

    <el-card shadow="never" class="filter-card">
      <div class="filter-header">
        <div class="filter-title">筛选条件</div>
        <el-tag v-if="activeFilterCount" type="info" size="small">已选 {{ activeFilterCount }} 项</el-tag>
      </div>
      <el-form :model="filters" class="filter-form" label-position="top">
        <div class="filter-grid">
          <el-form-item label="操作类型">
            <el-select v-model="filters.actionType" clearable placeholder="全部" class="filter-control">
              <el-option label="新增" value="CREATE" />
              <el-option label="更新" value="UPDATE" />
              <el-option label="删除" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item label="对象类型">
            <el-select v-model="filters.targetType" clearable placeholder="全部" class="filter-control">
              <el-option label="产品" value="PRODUCT" />
              <el-option label="批次" value="BATCH" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model.trim="filters.operator" clearable placeholder="输入操作人" class="filter-control" />
          </el-form-item>
          <el-form-item label="对象ID">
            <el-input v-model.trim="filters.targetId" clearable placeholder="输入对象ID" class="filter-control" />
          </el-form-item>
          <el-form-item label="时间范围" class="time-form-item">
            <el-date-picker
              v-model="filterTimeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="time-range"
            />
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button type="primary" plain @click="searchLogs" :loading="loading">
            <el-icon><Search /></el-icon> 筛选
          </el-button>
          <el-button plain @click="resetFilters" :disabled="loading || activeFilterCount === 0">
            <el-icon><Delete /></el-icon> 重置
          </el-button>
        </div>
      </el-form>
    </el-card>

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
/**
 * AuditLog.vue — 区块链审计日志页面（仅 ROLE_ADMIN / ROLE_INSPECTOR 可访问）。
 *
 * 展示内容：
 *   - 审计日志分页列表：id / 操作类型 / 对象 / 操作人 / 时间戳 / dataHash。
 *   - 多字段筛选：操作类型（CREATE/UPDATE/DELETE）、对象类型（PRODUCT/BATCH）、
 *     操作人、对象 ID、时间范围。
 *   - "验证链条完整性"：调用 verifyAuditLogChain() 触发三层校验，结果以 el-alert 展示。
 *     头部同时显示链条完整性 Tag 和链尾锚点 Tag 的实时状态。
 *   - "查看"按钮：弹出日志详情弹窗，对比操作前（dataBefore）和操作后（dataAfter）字段，
 *     其中 dataHash 字段通过 HashTag 组件展示（可展开完整哈希）。
 *   - "导出日志"：将当前筛选条件下所有日志导出为 CSV 文件。
 *
 * 关联：
 *   - api/blockchain.js（getAuditLogs / verifyAuditLogChain）
 *   - components/Pagination.vue（自定义分页组件）
 *   - components/HashTag.vue（日志详情中的 dataHash 字段展示）
 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Delete, Download, Refresh, Search } from '@element-plus/icons-vue'
import { getAuditLogs, verifyAuditLogChain } from '@/api/blockchain'
import Pagination from '@/components/Pagination.vue'
import HashTag from '@/components/HashTag.vue'

// 当前页审计日志数据，绑定到 el-table :data。
const logs = ref([])
// 当前分页页码。
const page = ref(1)
// 每页条数。
const pageSize = ref(10)
// 日志总条数，来自后端 res.data.total。
const total = ref(0)
// 列表加载状态，控制"刷新"按钮 :loading 和表格 v-loading。
const loading = ref(false)
// 导出日志时的加载状态，防止重复点击。
const exporting = ref(false)
// 链条验证请求的加载状态，控制"验证链条完整性"按钮 :loading。
const verifying = ref(false)
// 验证结果；null 表示尚未验证，有值时在页面显示 el-alert 和状态 Tag。
const verifyResult = ref(null)
// 日志详情弹窗是否显示。
const detailVisible = ref(false)
// 当前查看详情的日志对象；弹窗使用 beforeFields / afterFields 解析其 dataBefore / dataAfter。
const currentLog = ref(null)
// 时间范围筛选的日期区间 [startDate, endDate]，由 el-date-picker daterange 绑定。
const filterTimeRange = ref([])
// 筛选条件对象，传给 getAuditLogs({ ...filters, startTime, endTime })。
const filters = ref({
  actionType: '',   // CREATE / UPDATE / DELETE，空字符串表示不筛选
  targetType: '',   // PRODUCT / BATCH，空字符串表示不筛选
  operator: '',     // 操作人用户名，模糊匹配
  targetId: ''      // 对象 ID，模糊匹配（如 prod_1 或 batch_3）
})

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

// 链尾锚点异常时，给出期望条数 vs 当前条数，直观说明尾部被删了多少。
const tailInfo = computed(() => {
  const result = verifyResult.value
  if (!result || result.tailValid !== false) return ''
  if (result.expectedTotal == null) return ''
  return `链尾锚点：期望 ${result.expectedTotal} 条，当前 ${result.total} 条`
})

// 业务数据指纹异常时，把前几项异常目标拼成简洁描述，避免提示内容过长。
const verifyDescription = computed(() => {
  const items = verifyResult.value?.invalidItems || []
  if (!items.length) return ''
  const prefix = `指纹异常 ${items.length} 项：`
  return prefix + items
    .slice(0, 5)
    .map(item => `${item.targetType} ${item.targetId}${item.name ? `（${item.name}）` : ''}`)
    .join('；')
})

const activeFilterCount = computed(() => {
  return [
    filters.value.actionType,
    filters.value.targetType,
    filters.value.operator,
    filters.value.targetId,
    filterTimeRange.value?.length ? 'timeRange' : ''
  ].filter(Boolean).length
})

const buildLogParams = () => {
  const [startTime, endTime] = filterTimeRange.value || []
  const params = {
    page: page.value,
    pageSize: pageSize.value,
    actionType: filters.value.actionType,
    targetType: filters.value.targetType,
    operator: filters.value.operator,
    targetId: filters.value.targetId,
    startTime,
    endTime
  }
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

// 分页拉取审计日志列表。
const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs(buildLogParams())
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

const searchLogs = () => {
  page.value = 1
  fetchLogs()
}

const resetFilters = () => {
  filters.value = {
    actionType: '',
    targetType: '',
    operator: '',
    targetId: ''
  }
  filterTimeRange.value = []
  page.value = 1
  fetchLogs()
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

const actionLabel = (type) => {
  if (type === 'CREATE') return '新增'
  if (type === 'UPDATE') return '更新'
  if (type === 'DELETE') return '删除'
  return type || ''
}

const targetLabel = (type) => {
  if (type === 'PRODUCT') return '产品'
  if (type === 'BATCH') return '批次'
  return type || ''
}

const filterSummaryRows = () => {
  const [startTime, endTime] = filterTimeRange.value || []
  return [
    ['导出范围', activeFilterCount.value ? '当前筛选结果' : '全部日志'],
    ['操作类型', actionLabel(filters.value.actionType) || '全部'],
    ['对象类型', targetLabel(filters.value.targetType) || '全部'],
    ['操作人', filters.value.operator || '全部'],
    ['对象ID', filters.value.targetId || '全部'],
    ['开始时间', startTime || '不限'],
    ['结束时间', endTime || '不限'],
    ['匹配总数', total.value]
  ]
}

const exportAuditLogs = async () => {
  if (total.value === 0) {
    ElMessage.warning('暂无可导出的审计日志')
    return
  }
  exporting.value = true
  try {
    const res = await getAuditLogs({
      ...buildLogParams(),
      page: 1,
      pageSize: Math.max(total.value, 1)
    })
    if (res.code !== 200) {
      ElMessage.error(res.message || '审计日志导出失败')
      return
    }
    const exportRows = res.data?.records || []
    if (exportRows.length === 0) {
      ElMessage.warning('暂无可导出的审计日志')
      return
    }

    const summaryRows = [
      ['报告名称', '审计日志导出'],
      ['导出时间', new Date().toLocaleString()],
      ...filterSummaryRows(),
      [],
      ['日志ID', '时间', '操作人', '操作类型', '对象类型', '对象ID', '上一哈希', '本条哈希', '操作前', '操作后']
    ]
    const dataRows = exportRows.map(item => [
      item.id,
      item.timestamp,
      item.operator,
      actionLabel(item.actionType),
      targetLabel(item.targetType),
      item.targetId,
      item.previousHash,
      item.dataHash,
      item.dataBefore || '',
      item.dataAfter || ''
    ])
    const csv = [...summaryRows, ...dataRows]
      .map(row => row.map(csvCell).join(','))
      .join('\r\n')

    const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `审计日志_${reportTimestamp()}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${exportRows.length} 条审计日志`)
  } finally {
    exporting.value = false
  }
}

// 修改每页条数后回到第一页并重新拉取日志。
const handlePageSizeChange = () => {
  page.value = 1
  fetchLogs()
}

// 调用后端完整校验：日志链、链尾锚点、业务数据指纹都会被检查。
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

// 打开审计日志详情弹窗，展示操作前后 JSON 字段。
const openDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

// 根据操作类型选择 Element Plus 标签颜色。
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

// 将审计日志里存储的 JSON 字符串解析成“标签-值”数组，供详情弹窗循环展示。
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

// 操作前字段列表。
const beforeFields = computed(() => parseFields(currentLog.value?.dataBefore))
// 操作后字段列表。
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

.filter-card,
.content-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  min-width: 0;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 14px;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.filter-title {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
  min-width: 0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 14px;
  min-width: 0;
}

.filter-grid :deep(.el-form-item__label) {
  line-height: 20px;
  padding-bottom: 6px;
  color: #606266;
}

.time-form-item {
  grid-column: span 2;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 14px;
}

.filter-control {
  width: 100%;
  min-width: 0;
}

.time-range {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.filter-card :deep(.el-date-editor),
.filter-card :deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
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

  .filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .time-form-item {
    grid-column: auto;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
