<template>
  <div class="login-log-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">登录日志</h3>
        <span class="page-subtitle">记录每一次登录尝试，用于安全审计与异常登录排查</span>
      </div>
    </div>

    <div class="summary-cards">
      <div class="summary-card" :class="{ active: filter.status === '' }" @click="filterByStatus('')">
        <div class="summary-num">{{ summary.total }}</div>
        <div class="summary-label">总登录次数</div>
      </div>
      <div class="summary-card success" :class="{ active: filter.status === 'SUCCESS' }" @click="filterByStatus('SUCCESS')">
        <div class="summary-num">{{ summary.success }}</div>
        <div class="summary-label">成功</div>
      </div>
      <div class="summary-card danger" :class="{ active: filter.status === 'FAILURE' }" @click="filterByStatus('FAILURE')">
        <div class="summary-num">{{ summary.failure }}</div>
        <div class="summary-label">失败</div>
      </div>
    </div>

    <el-card shadow="never" class="content-card">
      <div class="toolbar">
        <el-input
          v-model="filter.keyword"
          placeholder="搜索用户名或 IP"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          @change="search"
        />
        <el-button type="primary" plain @click="search">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="ip" label="IP 地址" min-width="130" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="userAgent" label="客户端" min-width="180" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无登录日志" />

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
  </div>
</template>

<script setup>
/**
 * LoginLog.vue — 登录日志页面（管理员 / 监管员）。
 *
 * 顶部汇总卡片展示总/成功/失败次数（可点击按结果筛选），
 * 下方表格支持按用户名/IP 关键字和时间范围检索。
 * 数据来源 src/api/loginLog.js，对应后端 LoginLogController。
 */
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getLoginLogs, getLoginLogSummary } from '@/api/loginLog'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 时间范围由 el-date-picker 维护为 [start, end] 字符串数组，查询时拆分到 filter。
const timeRange = ref([])
const filter = reactive({ status: '', keyword: '' })
const summary = reactive({ total: 0, success: 0, failure: 0 })

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: filter.status,
      keyword: filter.keyword,
      startTime: timeRange.value?.[0] || '',
      endTime: timeRange.value?.[1] || ''
    }
    const res = await getLoginLogs(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const fetchSummary = async () => {
  const res = await getLoginLogSummary()
  if (res.code === 200) {
    Object.assign(summary, res.data)
  }
}

const search = () => {
  page.value = 1
  fetchList()
}

const filterByStatus = (status) => {
  filter.status = status
  search()
}

onMounted(() => {
  fetchList()
  fetchSummary()
})
</script>

<style scoped>
.login-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.summary-card.success { border-left-color: #67c23a; }
.summary-card.danger { border-left-color: #f56c6c; }

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
</style>
