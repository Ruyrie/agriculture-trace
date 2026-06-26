<template>
  <div class="statistics-page">
    <!-- 顶部渐变横幅 -->
    <div class="page-hero">
      <div class="hero-text">
        <h1>统计分析</h1>
        <p>面向管理员与监管员的生产批次、产品维度和溯源访问多维报表</p>
      </div>
      <el-button class="hero-refresh" :loading="loading" round @click="loadReports">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 内容主体：刷新时通过 key 整体重挂载，重放入场动画，观感等同 F5。 -->
    <div class="page-body" :key="refreshKey">
    <!-- 概览卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6" v-for="item in summaryCards" :key="item.label">
        <div class="summary-card" :class="`summary-card--${item.theme}`">
          <div class="summary-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="summary-body">
            <div class="summary-value">{{ item.value }}</div>
            <div class="summary-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表行 1：月度产量 + 溯源趋势 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="report-card">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><Histogram /></el-icon> 月度批次产量</span>
              <el-tag type="success" size="small" effect="light">批次 / 产品</el-tag>
            </div>
          </template>
          <div ref="monthlyChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="report-card">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><TrendCharts /></el-icon> 近 7 天溯源趋势</span>
              <el-tag type="primary" size="small" effect="light">近 7 天</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 2：产地分布 + 类别批次占比 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><Location /></el-icon> 产地分布</span>
              <el-tag size="small" effect="light">产品数</el-tag>
            </div>
          </template>
          <div ref="originChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><PieChart /></el-icon> 类别批次占比</span>
              <el-tag type="warning" size="small" effect="light">批次数</el-tag>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 3：溯源热度 Top 10 横向条形 -->
    <el-card shadow="never" class="report-card">
      <template #header>
        <div class="card-head">
          <span class="card-title"><el-icon><DataLine /></el-icon> 溯源热度 Top 10</span>
          <el-tag type="danger" size="small" effect="light">查询次数</el-tag>
        </div>
      </template>
      <div ref="rankingChartRef" class="chart-box chart-box--tall"></div>
    </el-card>

    <!-- 明细表格 -->
    <el-row :gutter="16" class="table-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span class="card-title"><el-icon><Trophy /></el-icon> 溯源排行榜</span>
          </template>
          <el-table class="trace-ranking-table" :data="reports.traceRanking" height="320" empty-text="暂无溯源记录" stripe>
            <el-table-column type="index" label="#" width="56" />
            <el-table-column prop="productName" label="产品" min-width="120" show-overflow-tooltip />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column prop="traceCount" label="查询次数" width="112" sortable />
            <el-table-column prop="lastTraceTime" label="最近查询" min-width="170" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span class="card-title"><el-icon><Tickets /></el-icon> 产品批次统计</span>
          </template>
          <el-table :data="reports.productBatchOutput" height="320" empty-text="暂无批次数据" stripe>
            <el-table-column prop="productName" label="产品" min-width="120" show-overflow-tooltip />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column prop="batchCount" label="批次数" width="90" sortable />
            <el-table-column prop="lastProductionDate" label="最近生产日期" min-width="130" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    </div>
  </div>
</template>

<script setup>
/**
 * Statistics.vue — 统计分析页面（仅 ROLE_ADMIN / ROLE_INSPECTOR 可访问）。
 *
 * 展示内容：
 *   1. 顶部概览卡片：月度批次合计、溯源查询合计、有批次产品数、产地数量（计算属性汇总）。
 *   2. 月度批次产量图：柱状（批次数）+ 折线（产品数）复合图，数据来自 getReports。
 *   3. 近 7 天溯源趋势面积图，数据来自 getTraceTrend（与 Dashboard.vue 共用同一接口）。
 *   4. 产地分布环形饼图，数据来自 getReports.originDistribution。
 *   5. 类别批次占比环形图，由 productBatchOutput 按类别聚合计算（前端 computed）。
 *   6. 溯源热度 Top 10 横向条形图 + 溯源排行榜明细表 + 产品批次统计明细表。
 *
 * 刷新机制：
 *   点击"刷新数据"按钮时 refreshKey 自增，触发内容主体 <div :key="refreshKey"> 整体
 *   重挂载，从而重放 CSS 入场动画，视觉上等同 F5 刷新但不重载整个页面。
 *
 * 关联：
 *   - api/dashboard.js（getReports / getTraceTrend）
 *   - utils/echarts.js（按需引入的 ECharts 实例）
 */
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh, Histogram, TrendCharts, Location, PieChart, DataLine, Trophy, Tickets,
  Box, Search, Goods
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { getReports, getTraceTrend } from '@/api/dashboard'

// 全局加载状态，控制"刷新数据"按钮 :loading 以及图表渲染时机。
const loading = ref(false)
// 各图表 DOM 节点 ref；echarts.init() 后赋值给对应实例变量，resize 时复用。
const monthlyChartRef = ref(null)
const trendChartRef = ref(null)
const originChartRef = ref(null)
const categoryChartRef = ref(null)
const rankingChartRef = ref(null)
// ECharts 实例，模块级变量（非响应式），||= 确保同一节点只 init 一次。
let monthlyChart = null
let trendChart = null
let originChart = null
let categoryChart = null
let rankingChart = null

// 统一的图表配色，绿色为主基调，与系统整体风格保持一致。
const PALETTE = ['#2e7d32', '#66bb6a', '#ffa726', '#42a5f5', '#ab47bc', '#26a69a', '#ef5350', '#78909c']

// 每次刷新自增，作为内容主体的 key，触发整体重挂载以重放入场动画。
const refreshKey = ref(0)

// 后端 getReports() 返回的四组报表数据；reactive 展开后各字段可直接作为图表数据源。
const reports = reactive({
  monthlyBatchOutput: [],   // 近 12 个月批次产量（month, batchCount, productCount）
  traceRanking: [],         // 溯源访问 Top N（productName, category, traceCount, lastTraceTime）
  originDistribution: [],   // 产地产品分布（name, value）
  productBatchOutput: []    // 各产品批次统计（productName, category, batchCount, lastProductionDate）
})
// 近 7 天溯源访问趋势，来自 getTraceTrend()，后端保证 7 天连续（无访问日填 0）。
const traceTrend = reactive({ dates: [], counts: [] })

// 根据报表数据汇总顶部四张概览卡片。
const summaryCards = computed(() => {
  const totalBatches = reports.monthlyBatchOutput.reduce((sum, item) => sum + Number(item.batchCount || 0), 0)
  const totalTraces = reports.traceRanking.reduce((sum, item) => sum + Number(item.traceCount || 0), 0)
  const activeProducts = reports.productBatchOutput.filter(item => Number(item.batchCount || 0) > 0).length
  const origins = reports.originDistribution.length
  return [
    { label: '月度批次合计', value: totalBatches, icon: Box, theme: 'green' },
    { label: '溯源查询合计', value: totalTraces, icon: Search, theme: 'blue' },
    { label: '有批次产品', value: activeProducts, icon: Goods, theme: 'orange' },
    { label: '产地数量', value: origins, icon: Location, theme: 'purple' }
  ]
})

// 按类别聚合各产品批次数，得到“类别批次占比”饼图数据。
const categoryBatchData = computed(() => {
  const map = {}
  reports.productBatchOutput.forEach(item => {
    const category = item.category || '未分类'
    map[category] = (map[category] || 0) + Number(item.batchCount || 0)
  })
  return Object.entries(map)
    .map(([name, value]) => ({ name, value }))
    .filter(item => item.value > 0)
    .sort((a, b) => b.value - a.value)
})

// 渲染月度批次产出图：柱状展示批次数，折线展示涉及产品数。
const renderMonthlyChart = () => {
  if (!monthlyChartRef.value) return
  monthlyChart ||= echarts.init(monthlyChartRef.value)
  const months = reports.monthlyBatchOutput.map(item => item.month)
  const batchCounts = reports.monthlyBatchOutput.map(item => Number(item.batchCount || 0))
  const productCounts = reports.monthlyBatchOutput.map(item => Number(item.productCount || 0))
  monthlyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 24, left: 42, right: 20, bottom: 44 },
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#dcdfe6' } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [
      {
        name: '批次数', type: 'bar', data: batchCounts, barMaxWidth: 34,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#66bb6a' },
            { offset: 1, color: '#2e7d32' }
          ])
        }
      },
      { name: '产品数', type: 'line', data: productCounts, smooth: true, symbol: 'circle', symbolSize: 7, itemStyle: { color: '#1976d2' }, lineStyle: { width: 3 } }
    ]
  }, true)
}

// 渲染近 7 天溯源访问趋势面积图。
const renderTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart ||= echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 24, left: 40, right: 20, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: false, data: traceTrend.dates, axisLine: { lineStyle: { color: '#dcdfe6' } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      name: '溯源查询次数', type: 'line', data: traceTrend.counts,
      smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#2e7d32', width: 3 }, itemStyle: { color: '#2e7d32' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(46, 125, 50, 0.30)' },
          { offset: 1, color: 'rgba(46, 125, 50, 0.02)' }
        ])
      }
    }]
  }, true)
}

// 渲染产地分布饼图。
const renderOriginChart = () => {
  if (!originChartRef.value) return
  originChart ||= echarts.init(originChartRef.value)
  originChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    color: PALETTE,
    series: [{
      name: '产地产品数', type: 'pie', radius: ['38%', '62%'], center: ['50%', '44%'],
      data: reports.originDistribution,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  }, true)
}

// 渲染类别批次占比环形图。
const renderCategoryChart = () => {
  if (!categoryChartRef.value) return
  categoryChart ||= echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 批 ({d}%)' },
    legend: { bottom: 0 },
    color: PALETTE,
    series: [{
      name: '类别批次数', type: 'pie', radius: ['40%', '64%'], center: ['50%', '44%'],
      data: categoryBatchData.value,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
    }]
  }, true)
}

// 渲染溯源热度 Top 10 横向条形图。
const renderRankingChart = () => {
  if (!rankingChartRef.value) return
  rankingChart ||= echarts.init(rankingChartRef.value)
  // 取前 10 名；横向条形图轴从下往上，故反转使最高值显示在顶部。
  const top = reports.traceRanking
    .filter(item => Number(item.traceCount || 0) > 0)
    .slice(0, 10)
    .reverse()
  rankingChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 16, left: 12, right: 30, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    yAxis: { type: 'category', data: top.map(item => item.productName), axisLine: { lineStyle: { color: '#dcdfe6' } } },
    series: [{
      name: '查询次数', type: 'bar', data: top.map(item => Number(item.traceCount || 0)),
      barMaxWidth: 20,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#a5d6a7' },
          { offset: 1, color: '#2e7d32' }
        ])
      },
      label: { show: true, position: 'right', color: '#606266' }
    }]
  }, true)
}

// 销毁所有图表实例并置空，使下次渲染重新 init 并重放入场动画。
const disposeCharts = () => {
  monthlyChart?.dispose(); monthlyChart = null
  trendChart?.dispose(); trendChart = null
  originChart?.dispose(); originChart = null
  categoryChart?.dispose(); categoryChart = null
  rankingChart?.dispose(); rankingChart = null
}

// 等 DOM 更新后再初始化/更新 ECharts，避免 ref 还没挂载。
const renderCharts = async () => {
  await nextTick()
  renderMonthlyChart()
  renderTrendChart()
  renderOriginChart()
  renderCategoryChart()
  renderRankingChart()
}

// 拉取统计分析页所有报表数据，并刷新图表。
const loadReports = async () => {
  loading.value = true
  try {
    // 报表数据与溯源趋势分属两个接口，并发拉取减少等待。
    const [reportRes, trendRes] = await Promise.all([getReports(), getTraceTrend()])
    if (reportRes.code === 200) {
      Object.assign(reports, reportRes.data)
    } else {
      ElMessage.error(reportRes.message || '统计报表加载失败')
    }
    if (trendRes.code === 200) {
      traceTrend.dates = trendRes.data.dates || []
      traceTrend.counts = trendRes.data.counts || []
    }
    // 整体重挂载内容主体（重放 CSS 入场动画），并丢弃旧图表实例使其重新 init 重放动画。
    refreshKey.value++
    disposeCharts()
    await renderCharts()
  } catch {
    ElMessage.error('统计报表加载失败')
  } finally {
    loading.value = false
  }
}

// 窗口尺寸变化时同步调整图表画布。
const resizeCharts = () => {
  monthlyChart?.resize()
  trendChart?.resize()
  originChart?.resize()
  categoryChart?.resize()
  rankingChart?.resize()
}

onMounted(() => {
  loadReports()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 内容主体：保持原有行间距，并在（重）挂载时重放入场动画。 */
.page-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: page-enter 0.45s ease;
}

@keyframes page-enter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 顶部渐变横幅 */
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 26px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1b5e20 0%, #2e7d32 55%, #43a047 100%);
  color: #fff;
  box-shadow: 0 6px 18px rgba(46, 125, 50, 0.25);
}

.hero-text h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.hero-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.88;
}

.hero-refresh {
  color: #2e7d32;
  border: none;
  font-weight: 600;
}

/* 概览卡片 */
.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 96px;
  padding: 0 22px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
  transition: transform 0.2s, box-shadow 0.2s;
}

.summary-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.1);
}

.summary-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #fff;
  flex-shrink: 0;
}

.summary-card--green .summary-icon { background: linear-gradient(135deg, #66bb6a, #2e7d32); }
.summary-card--blue .summary-icon { background: linear-gradient(135deg, #64b5f6, #1976d2); }
.summary-card--orange .summary-icon { background: linear-gradient(135deg, #ffb74d, #f57c00); }
.summary-card--purple .summary-icon { background: linear-gradient(135deg, #ba68c8, #8e24aa); }

.summary-value {
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.summary-label {
  font-size: 13px;
  color: #7a8794;
}

/* 图表卡片 */
.report-card {
  border-radius: 12px;
  border: 1px solid #eef0f3;
  transition: box-shadow 0.2s;
}

.report-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.report-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #f3f4f6;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-title .el-icon {
  color: #2e7d32;
}

.chart-box {
  height: 320px;
}

.chart-box--tall {
  height: 360px;
}

.summary-row,
.chart-row,
.table-row {
  width: 100%;
}

.trace-ranking-table :deep(th .cell) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
  white-space: nowrap;
}

.trace-ranking-table :deep(th .caret-wrapper) {
  flex: 0 0 auto;
}
</style>
