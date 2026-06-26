<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>你好，{{ username }}</h2>
        <p>今天是 {{ today }}，欢迎使用农产品溯源系统</p>
      </div>
      <div class="banner-actions">
        <el-button type="primary" @click="$router.push('/products')" size="large">
          <el-icon><Box /></el-icon> 产品管理
        </el-button>
        <el-button @click="$router.push('/batches')" size="large" plain>
          <el-icon><List /></el-icon> 批次管理
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <div class="stat-card stat-card--green">
          <div class="stat-icon"><el-icon><Goods /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.productCount ?? '--' }}</div>
            <div class="stat-label">产品总数</div>
          </div>
          <div class="stat-trend">↑ 较上周</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card stat-card--blue">
          <div class="stat-icon"><el-icon><Tickets /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.batchCount ?? '--' }}</div>
            <div class="stat-label">批次总数</div>
          </div>
          <div class="stat-trend">↑ 较上周</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card stat-card--orange">
          <div class="stat-icon"><el-icon><Search /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.traceCount ?? '--' }}</div>
            <div class="stat-label">溯源查询次数</div>
          </div>
          <div class="stat-trend">↑ 较昨日</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">产品类别分布</span>
              <el-tag type="success" size="small">实时</el-tag>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">近一周溯源趋势</span>
              <el-tag type="primary" size="small">近7天</el-tag>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">链上操作类型概览</span>
              <el-tag type="warning" size="small">审计日志</el-tag>
            </div>
          </template>
          <div ref="blockchainChartRef" class="chart-box chart-box--wide"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading" size="32"><Loading /></el-icon>
      <span>数据加载中...</span>
    </div>
  </div>
</template>

<script setup>
/**
 * Dashboard.vue — 数据概览页（仪表盘首页）。
 *
 * 展示内容：
 *   1. 欢迎横幅：显示当前登录用户名和今日日期，提供产品/批次管理快捷入口。
 *   2. 统计卡片：产品总数、批次总数、溯源查询次数（来自 getStatistics）。
 *   3. 饼图：产品类别分布（来自 getCategoryDistribution）。
 *   4. 折线图：近 7 天溯源访问趋势（来自 getTraceTrend，后端保证连续 7 天补零）。
 *   5. 柱状图：审计日志操作类型分布——CREATE/UPDATE/DELETE（来自 getOverviewCharts）。
 *
 * 关联：
 *   - api/dashboard.js（所有数据接口）
 *   - utils/echarts.js（按需引入的 ECharts 实例，减少打包体积）
 *   - Layout.vue（本页嵌套在 <router-view> 中）
 */
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, Goods, List, Loading, Search, Tickets } from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { getCategoryDistribution, getOverviewCharts, getStatistics, getTraceTrend } from '@/api/dashboard'

const router = useRouter()
// 全局加载遮罩，Promise.all 全部完成后置 false。
const loading = ref(true)
// 顶部三张统计卡片数据；初始值 0 避免 ?? '--' 短路出现空白。
const stats = ref({ productCount: 0, batchCount: 0, traceCount: 0 })
// 饼图挂载的 DOM 节点 ref；echarts.init(pieChartRef.value) 后赋值给 pieChart。
const pieChartRef = ref(null)
// 折线图挂载的 DOM 节点 ref。
const lineChartRef = ref(null)
// 柱状图挂载的 DOM 节点 ref。
const blockchainChartRef = ref(null)
// ECharts 实例；使用 ||= 确保同一节点只 init 一次，resize 时复用。
let pieChart = null
let lineChart = null
let blockchainChart = null
// 链上操作类型分布数据，blockchainActionMix 数组格式 [{name, value}]。
const overviewCharts = ref({
  blockchainActionMix: []
})

// 从 localStorage 读取登录时缓存的用户名，显示在欢迎横幅。
const username = computed(() => {
  const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return info.username || '用户'
})

// 将当前日期格式化为中文完整日期，用于欢迎横幅。
const today = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// 获取顶部统计卡片数据。
const fetchStatistics = async () => {
  try {
    const res = await getStatistics()
    if (res.code === 200) stats.value = res.data
  } catch { ElMessage.error('获取统计数据失败') }
}

// 获取类别分布并初始化/更新饼图。
const fetchCategoryDistribution = async () => {
  try {
    const res = await getCategoryDistribution()
    if (res.code === 200) {
      const data = res.data || []
      pieChart ||= echarts.init(pieChartRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, itemWidth: 12, itemHeight: 12 },
        color: ['#4caf50', '#81c784', '#a5d6a7', '#ff9800', '#64b5f6', '#f06292'],
        series: [{
          name: '产品类别',
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['50%', '45%'],
          data,
          label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
          emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
        }]
      })
    }
  } catch { ElMessage.error('获取类别分布失败') }
}

// 获取近 7 天溯源趋势并初始化/更新折线图。  ||= 是 逻辑或赋值运算符。如果 lineChart 为空，就给它赋值，如果 lineChart 已经有值，就保持原来的值
const fetchTraceTrend = async () => {
  try {
    const res = await getTraceTrend()
    if (res.code === 200) {
      const { dates = [], counts = [] } = res.data || {}
      lineChart ||= echarts.init(lineChartRef.value)
      lineChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { top: 20, right: 20, bottom: 40, left: 50 },
        xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e0e0e0' } } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } } },
        series: [{
          name: '溯源查询次数',
          type: 'line',
          data: counts,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#4caf50', width: 3 },
          itemStyle: { color: '#4caf50' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(76, 175, 80, 0.3)' },
              { offset: 1, color: 'rgba(76, 175, 80, 0.02)' }
            ])
          }
        }]
      })
    }
  } catch { ElMessage.error('获取趋势数据失败') }
}

// 渲染区块链审计日志中的操作类型分布。
const renderBlockchainChart = () => {
  if (!blockchainChartRef.value) return
  blockchainChart ||= echarts.init(blockchainChartRef.value)
  const compact = blockchainChartRef.value.clientWidth < 320
  const rows = overviewCharts.value.blockchainActionMix || []
  blockchainChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 20, right: 36, bottom: 36, left: 48 },
    xAxis: { type: 'category', data: rows.map(item => item.name), axisLine: { lineStyle: { color: '#dcdfe6' } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      name: '日志数量',
      type: 'bar',
      data: rows.map(item => Number(item.value || 0)),
      barMaxWidth: 44,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#64b5f6' },
          { offset: 1, color: '#1976d2' }
        ])
      },
      label: { show: !compact, position: 'top', color: '#606266' }
    }]
  })
}

const renderOverviewCharts = async () => {
  await nextTick()
  renderBlockchainChart()
}

// 获取数据概览下方的链上操作类型图表。
const fetchOverviewCharts = async () => {
  try {
    const res = await getOverviewCharts()
    if (res.code === 200) {
      overviewCharts.value = {
        blockchainActionMix: res.data?.blockchainActionMix || []
      }
      await renderOverviewCharts()
    }
  } catch { ElMessage.error('获取链上操作图表失败') }
}

// 并发加载首页所有数据，最后关闭加载遮罩。
const loadDashboardData = async () => {
  loading.value = true
  try {
    await Promise.all([fetchStatistics(), fetchCategoryDistribution(), fetchTraceTrend(), fetchOverviewCharts()])
  } finally {
    loading.value = false
  }
}

const resizeCharts = () => {
  pieChart?.resize()
  lineChart?.resize()
  blockchainChart?.resize()
}

onMounted(() => {
  loadDashboardData()
  window.addEventListener('resize', resizeCharts)
})
onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  pieChart?.dispose()
  lineChart?.dispose()
  blockchainChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
  position: relative;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #1b5e20, #2e7d32);
  border-radius: 12px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  color: #fff;
}

.welcome-text h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
  color: #fff;
}

.welcome-text p {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}

.banner-actions {
  display: flex;
  gap: 10px;
}

.banner-actions .el-button {
  border-radius: 8px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  border-radius: 12px 0 0 12px;
}

.stat-card--green::before { background: #4caf50; }
.stat-card--blue::before { background: #2196f3; }
.stat-card--orange::before { background: #ff9800; }

.stat-icon {
  font-size: 36px;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon :deep(.el-icon) {
  width: 34px;
  height: 34px;
}

.stat-card--green .stat-icon { background: rgba(76, 175, 80, 0.1); }
.stat-card--blue .stat-icon { background: rgba(33, 150, 243, 0.1); }
.stat-card--orange .stat-icon { background: rgba(255, 152, 0, 0.1); }

.stat-body {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-trend {
  font-size: 12px;
  color: #67c23a;
  white-space: nowrap;
}

/* 图表卡片 */
.charts-row {
  margin-bottom: 20px;
}

.chart-box {
  height: 280px;
}

.chart-box--wide {
  height: 300px;
}

.chart-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.chart-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 加载遮罩 */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 1000;
  font-size: 14px;
  color: #4caf50;
  backdrop-filter: blur(2px);
}

@media (max-width: 960px) {
  .welcome-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .banner-actions {
    width: 100%;
  }

  .banner-actions .el-button {
    flex: 1;
  }

  .stats-row :deep(.el-col),
  .charts-row :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
    margin-bottom: 16px;
  }

  .charts-row {
    margin-bottom: 4px;
  }
}
</style>
