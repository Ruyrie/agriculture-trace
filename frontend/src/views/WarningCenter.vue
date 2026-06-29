<template>
  <div class="warning-page">
    <div class="page-header">
      <div>
        <div class="title-row">
          <h3 class="page-title">批次预警中心</h3>
          <el-popover placement="bottom-start" :width="360" trigger="hover">
            <template #reference>
              <el-icon class="title-help"><QuestionFilled /></el-icon>
            </template>
            <div class="rule-popover">
              <div class="rule-popover-title">批次预警级别说明</div>
              <div class="rule-line"><el-tag type="danger" size="small" effect="dark">高危</el-tag><span>质检结果<strong>不合格</strong>（命中 不合格/异常/阳性/检出超标 等关键字），产品质量存在实际问题，最需优先处置。</span></div>
              <div class="rule-line"><el-tag type="warning" size="small" effect="dark">中风险</el-tag><span>批次<strong>缺少质检记录</strong>，或质检结果<strong>存疑</strong>（既非明确合格也非不合格，如"随意/测试/空"），需补检或人工复核。</span></div>
              <div class="rule-line"><el-tag type="info" size="small" effect="dark">低风险</el-tag><span>批次<strong>缺少物流轨迹</strong>，流向无法追溯，属于完整性缺口。</span></div>
            </div>
          </el-popover>
        </div>
        <span class="page-subtitle">以批次为单位，实时扫描质检不合格、缺少质检、缺少物流等风险，及时处置</span>
      </div>
      <el-button :loading="loading" @click="refresh">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <div class="summary-cards">
      <el-tooltip content="实时扫描出的全部风险，点击查看完整列表" placement="top">
        <div class="summary-card" :class="{ active: level === '' }" @click="filterByLevel('')">
          <div class="summary-num">{{ summary.total }}</div>
          <div class="summary-label">全部预警</div>
          <div class="summary-hint">预警批次合计</div>
        </div>
      </el-tooltip>
      <el-tooltip content="质检结果命中不合格关键字（不合格/异常/阳性/检出超标 等），产品质量存在实际问题，最优先处置" placement="top">
        <div class="summary-card danger" :class="{ active: level === 'HIGH' }" @click="filterByLevel('HIGH')">
          <div class="summary-num">{{ summary.high }}</div>
          <div class="summary-label">高危</div>
          <div class="summary-hint">质检不合格</div>
        </div>
      </el-tooltip>
      <el-tooltip content="批次缺少质检记录，或质检结果存疑（既非明确合格也非不合格，如随意/测试/空），需补检或人工复核" placement="top">
        <div class="summary-card warning" :class="{ active: level === 'MEDIUM' }" @click="filterByLevel('MEDIUM')">
          <div class="summary-num">{{ summary.medium }}</div>
          <div class="summary-label">中风险</div>
          <div class="summary-hint">缺质检 / 结果存疑</div>
        </div>
      </el-tooltip>
      <el-tooltip content="批次没有任何物流记录，流向无法追溯，属于追溯完整性缺口" placement="top">
        <div class="summary-card info" :class="{ active: level === 'LOW' }" @click="filterByLevel('LOW')">
          <div class="summary-num">{{ summary.low }}</div>
          <div class="summary-label">低风险</div>
          <div class="summary-hint">缺少物流轨迹</div>
        </div>
      </el-tooltip>
    </div>

    <el-card shadow="never" class="content-card">
      <el-table :data="pagedData" stripe v-loading="loading">
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" effect="dark">{{ levelMap[row.level] || row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题" width="190">
          <template #default="{ row }">
            <div class="issue-tags">
              <el-tag
                v-for="(issue, idx) in issuesOf(row)"
                :key="idx"
                :type="levelTagType(issue.level)"
                effect="plain"
                size="small"
              >{{ issue.typeLabel }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="关联批次" min-width="180" show-overflow-tooltip />
        <el-table-column label="预警说明" min-width="260">
          <template #default="{ row }">
            <div v-for="(issue, idx) in issuesOf(row)" :key="idx" class="issue-msg">{{ issue.message }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.targetId" link type="primary" @click="viewTrace(row)">
              <el-icon><View /></el-icon> 查看溯源
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && warnings.length === 0" description="太好了，当前没有任何预警" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="warnings.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * WarningCenter.vue — 预警中心（管理员 / 监管员）。
 *
 * 顶部汇总卡片按级别（高危/中/低）统计，点击卡片按级别筛选；
 * 下方表格展示每条预警的级别、类型、关联批次、说明与时间，并可跳转批次溯源详情。
 * 预警由后端实时计算（src/api/warning.js → WarningController），列表整体返回、前端分页。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, View, QuestionFilled } from '@element-plus/icons-vue'
import { getWarnings, getWarningSummary } from '@/api/warning'

const router = useRouter()

const levelMap = { HIGH: '高危', MEDIUM: '中风险', LOW: '低风险' }
const levelTagType = (lvl) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }[lvl] || 'info')

// 兼容两种后端返回：聚合后每行带 issues 子问题数组；
// 旧版（未聚合/未重启）每行是单条问题，这里回退包装成单元素数组，避免表格列空白。
const issuesOf = (row) => {
  if (Array.isArray(row.issues) && row.issues.length) return row.issues
  return [{ level: row.level, type: row.type, typeLabel: row.typeLabel, message: row.message }]
}

const loading = ref(false)
const warnings = ref([])
const summary = reactive({ total: 0, high: 0, medium: 0, low: 0 })
const level = ref('')

const page = ref(1)
const pageSize = ref(10)
// 后端整体返回预警列表，这里做客户端分页切片。
const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return warnings.value.slice(start, start + pageSize.value)
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getWarnings(level.value ? { level: level.value } : {})
    if (res.code === 200) {
      warnings.value = res.data
      page.value = 1
    }
  } finally {
    loading.value = false
  }
}

const fetchSummary = async () => {
  const res = await getWarningSummary()
  if (res.code === 200) {
    Object.assign(summary, res.data)
  }
}

const filterByLevel = (lvl) => {
  level.value = lvl
  fetchList()
}

const refresh = () => {
  fetchList()
  fetchSummary()
}

const viewTrace = (row) => {
  // 批次溯源详情页为公开路由 /trace/batch/:batchId，新标签打开便于对照排查。
  const route = router.resolve({ name: 'BatchTraceDetail', params: { batchId: row.targetId } })
  window.open(route.href, '_blank')
}

onMounted(refresh)
</script>

<style scoped>
.warning-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.title-help {
  font-size: 16px;
  color: #c0c4cc;
  cursor: help;
  transition: color 0.2s;
}

.title-help:hover {
  color: #409eff;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
}

/* 级别说明气泡 */
.rule-popover-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.rule-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.rule-line:last-child {
  margin-bottom: 0;
}

.rule-line .el-tag {
  flex-shrink: 0;
  margin-top: 1px;
}

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

.summary-card.danger { border-left-color: #f56c6c; }
.summary-card.warning { border-left-color: #e6a23c; }
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

.summary-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.content-card {
  border-radius: 10px;
}

/* 同一批次的多个问题：标签竖向堆叠，逐条说明分行展示 */
.issue-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.issue-msg {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.issue-msg + .issue-msg {
  margin-top: 2px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
