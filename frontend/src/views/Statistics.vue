<template>
  <div class="statistics-page">
    <div class="page-head">
      <div>
        <h1>统计分析</h1>
        <p>面向管理员与监管员的生产批次、产品维度和溯源访问报表</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadReports">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :span="6" v-for="item in summaryCards" :key="item.label">
        <div class="summary-card">
          <div class="summary-value">{{ item.value }}</div>
          <div class="summary-label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span>月度批次产量</span>
          </template>
          <div ref="monthlyChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span>产地分布</span>
          </template>
          <div ref="originChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="table-row">
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span>溯源排行榜</span>
          </template>
          <el-table :data="reports.traceRanking" height="320" empty-text="暂无溯源记录">
            <el-table-column type="index" label="#" width="56" />
            <el-table-column prop="productName" label="产品" min-width="120" />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column prop="traceCount" label="查询次数" width="100" />
            <el-table-column prop="lastTraceTime" label="最近查询" min-width="150" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="report-card">
          <template #header>
            <span>产品批次统计</span>
          </template>
          <el-table :data="reports.productBatchOutput" height="320" empty-text="暂无批次数据">
            <el-table-column prop="productName" label="产品" min-width="120" />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column prop="batchCount" label="批次数" width="90" />
            <el-table-column prop="lastProductionDate" label="最近生产日期" min-width="130" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getReports } from '@/api/dashboard'

const loading = ref(false)
const monthlyChartRef = ref(null)
const originChartRef = ref(null)
let monthlyChart = null
let originChart = null

const reports = reactive({
  monthlyBatchOutput: [],
  traceRanking: [],
  originDistribution: [],
  productBatchOutput: []
})

const summaryCards = computed(() => {
  const totalBatches = reports.monthlyBatchOutput.reduce((sum, item) => sum + Number(item.batchCount || 0), 0)
  const totalTraces = reports.traceRanking.reduce((sum, item) => sum + Number(item.traceCount || 0), 0)
  const activeProducts = reports.productBatchOutput.filter(item => Number(item.batchCount || 0) > 0).length
  const origins = reports.originDistribution.length
  return [
    { label: '月度批次合计', value: totalBatches },
    { label: '溯源查询合计', value: totalTraces },
    { label: '有批次产品', value: activeProducts },
    { label: '产地数量', value: origins }
  ]
})

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
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '批次数', type: 'bar', data: batchCounts, itemStyle: { color: '#2e7d32' }, barMaxWidth: 34 },
      { name: '产品数', type: 'line', data: productCounts, smooth: true, itemStyle: { color: '#1976d2' } }
    ]
  })
}

const renderOriginChart = () => {
  if (!originChartRef.value) return
  originChart ||= echarts.init(originChartRef.value)
  originChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    color: ['#2e7d32', '#66bb6a', '#ffa726', '#42a5f5', '#ab47bc', '#78909c'],
    series: [{
      name: '产地产品数',
      type: 'pie',
      radius: ['38%', '62%'],
      center: ['50%', '44%'],
      data: reports.originDistribution,
      label: { formatter: '{b}\n{c}' }
    }]
  })
}

const renderCharts = async () => {
  await nextTick()
  renderMonthlyChart()
  renderOriginChart()
}

const loadReports = async () => {
  loading.value = true
  try {
    const res = await getReports()
    if (res.code === 200) {
      Object.assign(reports, res.data)
      await renderCharts()
    } else {
      ElMessage.error(res.message || '统计报表加载失败')
    }
  } catch {
    ElMessage.error('统计报表加载失败')
  } finally {
    loading.value = false
  }
}

const resizeCharts = () => {
  monthlyChart?.resize()
  originChart?.resize()
}

onMounted(() => {
  loadReports()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  monthlyChart?.dispose()
  originChart?.dispose()
})
</script>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.page-head p {
  margin: 0;
  font-size: 14px;
  color: #7a8794;
}

.summary-card {
  height: 92px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-sizing: border-box;
}

.summary-value {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: #2e7d32;
  margin-bottom: 10px;
}

.summary-label {
  font-size: 13px;
  color: #7a8794;
}

.report-card {
  border-radius: 8px;
}

.chart-box {
  height: 320px;
}

.chart-row,
.table-row {
  width: 100%;
}
</style>
