<template>
  <div class="trace-detail">
    <!-- 产品信息 + 二维码 -->
    <el-card v-loading="loadingProduct" class="info-card">
      <div class="info-layout">
        <div class="product-info-panel" v-if="productInfo.name">
          <h2>{{ productInfo.name }}</h2>
          <el-descriptions :column="1" border class="product-desc">
            <el-descriptions-item label="类别">{{ productInfo.category }}</el-descriptions-item>
            <el-descriptions-item label="产地">{{ productInfo.origin }}</el-descriptions-item>
            <el-descriptions-item label="参考价格">{{ productInfo.price }} 元/kg</el-descriptions-item>
          </el-descriptions>
          <div v-if="imageList(productInfo.imageUrls).length" class="image-gallery product-gallery">
            <el-image
              v-for="(url, index) in imageList(productInfo.imageUrls)"
              :key="url"
              :src="url"
              :preview-src-list="imageList(productInfo.imageUrls)"
              :initial-index="index"
              preview-teleported
              fit="cover"
              class="gallery-image"
            />
          </div>
        </div>
        <div v-else-if="!loadingProduct" class="no-product">暂无产品信息</div>
        <div class="qrcode-panel">
          <h3 class="qrcode-title">产品溯源二维码</h3>
          <p class="qrcode-tip">扫码可查看该产品完整溯源信息</p>
          <canvas ref="qrcodeCanvas"></canvas>
          <el-button type="primary" size="small" @click="downloadQRCode" style="margin-top: 12px">
            <el-icon><Download /></el-icon> 下载二维码
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选与导出 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <div class="filter-left">
          <el-icon color="#409eff" size="16"><Filter /></el-icon>
          <span class="filter-label">按日期筛选：</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 280px"
          />
          <el-input
            v-model="searchBatchNo"
            placeholder="输入批次号查询"
            prefix-icon="Search"
            clearable
            style="width: 200px; margin-left: 8px"
          />
          <el-button plain @click="clearFilter" style="margin-left: 8px">重置</el-button>
        </div>
        <div class="filter-right">
          <el-dropdown @command="handleExportCommand">
            <el-button type="primary">
              <el-icon style="margin-right:4px"><Download /></el-icon>
              导出数据
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="current">
                  {{ activeBatch === 'all' ? '导出当前显示批次' : `导出批次 ${activeBatch}` }}
                </el-dropdown-item>
                <el-dropdown-item command="all" divided>导出全部批次</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 批次选择标签 -->
      <div class="batch-selector" v-if="allBatches.length > 0">
        <span class="batch-selector-label">批次选择：</span>
        <div class="batch-tags-wrap">
          <el-check-tag
            :checked="activeBatch === 'all'"
            type="primary"
            @change="activeBatch = 'all'"
            class="batch-tag"
          >
            全部批次（{{ filteredBatches.length }}）
          </el-check-tag>
          <el-check-tag
            v-for="b in filteredBatches"
            :key="b.batchNo"
            :checked="activeBatch === b.batchNo"
            type="primary"
            @change="selectBatch(b.batchNo)"
            class="batch-tag"
          >
            {{ b.batchNo }}
            <span class="tag-date" v-if="b.date">{{ b.date }}</span>
          </el-check-tag>
          <span v-if="filteredBatches.length === 0 && (dateRange || searchBatchNo)" class="no-batch-tip">
            未找到匹配的批次
          </span>
        </div>
      </div>
    </el-card>

    <!-- 批次溯源记录 -->
    <div class="records-area" v-loading="loadingTrace">
      <el-empty v-if="!loadingTrace && displayBatchData.length === 0" description="暂无溯源记录" />

      <el-collapse v-else v-model="expandedBatches" class="batch-collapse">
        <el-collapse-item
          v-for="batch in displayBatchData"
          :key="batch.batchNo"
          :name="batch.batchNo"
          class="batch-collapse-item"
        >
          <template #title>
            <div class="collapse-title">
              <el-tag effect="dark" round class="batch-no-tag">{{ batch.batchNo }}</el-tag>
              <span class="batch-date-text" v-if="batch.date">{{ batch.date }}</span>
              <div class="batch-count-tags">
                <el-tag size="small" type="success" effect="plain">生产 {{ batch.production.length }} 条</el-tag>
                <el-tag size="small" type="primary" effect="plain">质检 {{ batch.inspection.length }} 条</el-tag>
                <el-tag size="small" type="warning" effect="plain">物流 {{ batch.logistics.length }} 节点</el-tag>
              </div>
            </div>
          </template>

          <div class="batch-content">
            <!-- 批次说明 -->
            <div v-if="imageList(batch.imageUrls).length" class="sub-section batch-note-section">
              <div class="sub-header">
                <el-icon color="#909399"><Document /></el-icon>
                <span class="sub-title">批次说明</span>
              </div>
              <div class="image-gallery batch-gallery">
                <el-image
                  v-for="(url, index) in imageList(batch.imageUrls)"
                  :key="url"
                  :src="url"
                  :preview-src-list="imageList(batch.imageUrls)"
                  :initial-index="index"
                  preview-teleported
                  fit="cover"
                  class="gallery-image"
                />
              </div>
            </div>

            <!-- 生产记录 -->
            <div class="sub-section">
              <div class="sub-header">
                <el-icon color="#67c23a"><Tools /></el-icon>
                <span class="sub-title">生产记录</span>
              </div>
              <el-empty v-if="batch.production.length === 0" description="暂无生产记录" :image-size="50" />
              <el-table v-else :data="batch.production" border stripe>
                <el-table-column prop="activityName" label="生产活动" min-width="160" />
                <el-table-column prop="activityDate" label="操作时间" width="170" />
                <el-table-column prop="operator" label="操作员" width="120" />
                <el-table-column prop="remark" label="备注" min-width="140">
                  <template #default="{ row }">{{ row.remark || '-' }}</template>
                </el-table-column>
                <el-table-column label="记录图片" width="120" align="center">
                  <template #default="{ row }">
                    <div v-if="imageList(row.imageUrls).length" class="inspection-image-cell">
                      <el-image
                        v-for="(url, imgIndex) in imageList(row.imageUrls)"
                        :key="url"
                        :src="url"
                        :preview-src-list="imageList(row.imageUrls)"
                        :initial-index="imgIndex"
                        preview-teleported
                        fit="cover"
                        class="record-image"
                      />
                    </div>
                    <span v-else class="no-image">—</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 质检报告 -->
            <div class="sub-section">
              <div class="sub-header">
                <el-icon color="#409eff"><CircleCheck /></el-icon>
                <span class="sub-title">质检报告</span>
              </div>
              <el-empty v-if="batch.inspection.length === 0" description="暂无质检记录" :image-size="50" />
              <el-table v-else :data="batch.inspection" border stripe>
                <el-table-column prop="inspectionItem" label="检测项目" min-width="160" />
                <el-table-column prop="result" label="检测结果" width="130">
                  <template #default="{ row }">
                    <el-tag :type="isPassResult(row.result) ? 'success' : 'danger'">
                      {{ row.result }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="inspector" label="检测员" width="120" />
                <el-table-column prop="inspectionDate" label="检测时间" width="170" />
                <el-table-column label="质检图片" width="120" align="center">
                  <template #default="{ row }">
                    <div v-if="imageList(row.imageUrls).length" class="inspection-image-cell">
                      <el-image
                        v-for="(url, imgIndex) in imageList(row.imageUrls)"
                        :key="url"
                        :src="url"
                        :preview-src-list="imageList(row.imageUrls)"
                        :initial-index="imgIndex"
                        preview-teleported
                        fit="cover"
                        class="record-image"
                      />
                    </div>
                    <span v-else class="no-image">—</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 物流轨迹 -->
            <div class="sub-section">
              <div class="sub-header">
                <el-icon color="#e6a23c"><Promotion /></el-icon>
                <span class="sub-title">物流轨迹</span>
              </div>
              <el-empty v-if="batch.logistics.length === 0" description="暂无物流记录" :image-size="50" />
              <el-timeline v-else class="batch-timeline">
                <el-timeline-item
                  v-for="(log, i) in batch.logistics"
                  :key="i"
                  :timestamp="log.updateTime"
                  placement="top"
                  :type="i === 0 ? 'primary' : ''"
                >
                  <el-card shadow="never" class="logistics-card">
                    <strong>{{ log.nodeName }}</strong>
                    <span class="logistics-location">{{ log.location }}</span>
                    <span class="logistics-operator" v-if="log.operator">操作员：{{ log.operator }}</span>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 未关联批次的物流记录（兜底展示） -->
      <el-card v-if="unassignedLogistics.length > 0" class="unassigned-logistics-card" style="margin-top: 16px">
        <div class="sub-header">
          <el-icon color="#e6a23c"><Promotion /></el-icon>
          <span class="sub-title">物流轨迹（全览）</span>
          <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">未按批次分类</el-tag>
        </div>
        <el-timeline class="batch-timeline" style="margin-top: 12px">
          <el-timeline-item
            v-for="(log, i) in unassignedLogistics"
            :key="i"
            :timestamp="log.updateTime"
            placement="top"
            :type="i === 0 ? 'primary' : ''"
          >
            <el-card shadow="never" class="logistics-card">
              <strong>{{ log.nodeName }}</strong>
              <span class="logistics-location">{{ log.location }}</span>
              <span class="logistics-operator" v-if="log.operator">操作员：{{ log.operator }}</span>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup>
/**
 * TraceDetail.vue — 产品溯源详情页（公开页面，无需登录）。
 *
 * 功能：
 *   - 展示产品基本信息（名称、类别、产地、价格、产品图片）。
 *   - 生成产品溯源二维码（qrcode 库渲染到 <canvas>），支持下载。
 *   - 展示所有批次的生产记录、质检报告和物流时间线。
 *   - 支持按日期范围和批次号筛选，点击批次标签切换为单批次视图。
 *   - 导出功能：将当前展示的溯源数据导出为 CSV 或 JSON 文件。
 *
 * 路由参数：
 *   /trace/:id       — 按产品 ID 加载所有批次溯源数据（调用 getTraceInfo）
 *   /trace/batch/:batchId — 按批次 ID 加载单批次溯源数据（调用 getBatchTraceInfo）
 *
 * 关联：
 *   - api/trace.js（getTraceInfo / getBatchTraceInfo）
 *   - utils/images.js（parseImageUrls / resolveImageUrl）
 *   - qrcode 库（将溯源 URL 编码为二维码图像）
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import { Download, Tools, CircleCheck, Promotion, Filter, ArrowDown, Picture, Document } from '@element-plus/icons-vue'
import { getBatchTraceInfo, getTraceInfo } from '@/api/trace'
import { parseImageUrls, resolveImageUrl } from '@/utils/images'

const route = useRoute()
// 路由参数：产品 ID（/trace/:id 路由时有值）。
const productId = route.params.id
// 路由参数：批次 ID（/trace/batch/:batchId 路由时有值，优先级高于 productId）。
const batchId = route.params.batchId

// 产品基础信息，来自 getTraceInfo / getBatchTraceInfo 返回的 product 字段。
const productInfo = ref({})
// 全部生产活动记录，包含 batchNo、activityName、activityDate、operator、remark、imageUrls。
const productionRecords = ref([])
// 全部质检报告记录，包含 batchNo、inspectionItem、result、inspector、inspectionDate。
const inspectionReports = ref([])
// 全部物流节点记录，包含 batchNo、nodeName、location、operator、updateTime。
const logistics = ref([])
// 批次元数据列表，来自接口返回的 batches 字段，含 batchNo、productionDate、imageUrls。
const batchMetas = ref([])
// 二维码 canvas DOM 节点 ref，qrcode.toCanvas() 将二维码渲染到此元素上。
const qrcodeCanvas = ref(null)
// 产品信息区块加载状态。
const loadingProduct = ref(false)
// 溯源记录区块加载状态。
const loadingTrace = ref(false)

// 日期范围筛选，[startDate, endDate] 格式，通过 filteredBatches 计算属性过滤批次。
const dateRange = ref(null)
// 批次号关键词搜索，模糊匹配 filteredBatches。
const searchBatchNo = ref('')
// 当前激活批次：'all' 表示全部批次，字符串值表示某个具体 batchNo。
const activeBatch = ref('all')
// el-collapse 展开的批次号列表，控制溯源详情的折叠/展开。
const expandedBatches = ref([])

// 判断质检结果是否可视为通过，用于给结果标签设置 success/danger 类型。
const isPassResult = (result) => {
  if (!result) return false
  return result.includes('合格') || result.includes('未检出') || result.includes('通过')
}

// 提取所有唯一批次，记录最早操作日期
const allBatches = computed(() => {
  const map = new Map()
  batchMetas.value.forEach(batch => {
    if (!batch.batchNo) return
    map.set(batch.batchNo, {
      batchNo: batch.batchNo,
      date: batch.productionDate || '',
      imageUrls: batch.imageUrls || ''
    })
  })
  const addRecord = (r, dateField) => {
    const no = r.batchNo
    if (!no) return
    const d = (r[dateField] || '').substring(0, 10)
    if (!map.has(no)) {
      map.set(no, { batchNo: no, date: d, imageUrls: '' })
    } else if (d && (!map.get(no).date || d < map.get(no).date)) {
      map.get(no).date = d
    }
  }
  productionRecords.value.forEach(r => addRecord(r, 'activityDate'))
  inspectionReports.value.forEach(r => addRecord(r, 'inspectionDate'))
  logistics.value.forEach(r => addRecord(r, 'updateTime'))
  return Array.from(map.values()).sort((a, b) => b.batchNo.localeCompare(a.batchNo))
})

// 日期范围 + 批次号关键字筛选后的批次列表
const filteredBatches = computed(() => {
  let list = allBatches.value
  if (dateRange.value && dateRange.value[0]) {
    const [start, end] = dateRange.value
    list = list.filter(b => !b.date || (b.date >= start && b.date <= end))
  }
  const keyword = searchBatchNo.value.trim().toLowerCase()
  if (keyword) {
    list = list.filter(b => b.batchNo.toLowerCase().includes(keyword))
  }
  return list
})

// 当前展示的批次（全部或单个）
const displayBatches = computed(() => {
  if (activeBatch.value === 'all') return filteredBatches.value
  return filteredBatches.value.filter(b => b.batchNo === activeBatch.value)
})

// 每个批次的完整数据（含三类记录）
const displayBatchData = computed(() => {
  return displayBatches.value.map(b => ({
    ...b,
    production: productionRecords.value.filter(r => r.batchNo === b.batchNo),
    inspection: inspectionReports.value.filter(r => r.batchNo === b.batchNo),
    logistics: logistics.value.filter(r => r.batchNo === b.batchNo)
  }))
})

const imageList = (value) => parseImageUrls(value).map(resolveImageUrl)
const firstImage = (value) => imageList(value)[0] || ''

// 没有 batchNo 的物流记录（兜底显示，防止数据丢失）
const unassignedLogistics = computed(() => {
  const hasAnyBatchNo = logistics.value.some(r => r.batchNo)
  if (!hasAnyBatchNo) return logistics.value
  return logistics.value.filter(r => !r.batchNo)
})

// 显示批次变化时自动展开所有
watch(displayBatches, (batches) => {
  expandedBatches.value = batches.map(b => b.batchNo)
}, { immediate: true })

// 日期筛选变化后，若当前选中批次不在结果中，重置为全部
watch(filteredBatches, (batches) => {
  if (activeBatch.value !== 'all' && !batches.find(b => b.batchNo === activeBatch.value)) {
    activeBatch.value = 'all'
  }
})

const selectBatch = (batchNo) => {
  activeBatch.value = batchNo
  expandedBatches.value = [batchNo]
}

// 清空日期和批次号筛选，并恢复展示全部批次。
const clearFilter = () => {
  dateRange.value = null
  searchBatchNo.value = ''
  activeBatch.value = 'all'
}

// 导出 CSV（含 UTF-8 BOM 确保中文兼容）
const exportToCSV = (batchDataList, filename) => {
  const escape = (v) => `"${(v || '').toString().replace(/"/g, '""')}"`
  const lines = ['﻿']

  batchDataList.forEach(batch => {
    lines.push(`批次号,${escape(batch.batchNo)},日期,${escape(batch.date)}`)
    lines.push('')
    lines.push('【生产记录】')
    lines.push('生产活动,操作时间,操作员,备注')
    batch.production.forEach(r =>
      lines.push([r.activityName, r.activityDate, r.operator, r.remark].map(escape).join(','))
    )
    lines.push('')
    lines.push('【质检报告】')
    lines.push('检测项目,检测结果,检测员,检测时间')
    batch.inspection.forEach(r =>
      lines.push([r.inspectionItem, r.result, r.inspector, r.inspectionDate].map(escape).join(','))
    )
    lines.push('')
    lines.push('【物流轨迹】')
    lines.push('节点,地点,操作员,时间')
    batch.logistics.forEach(r =>
      lines.push([r.nodeName, r.location, r.operator, r.updateTime].map(escape).join(','))
    )
    lines.push('')
    lines.push('================')
    lines.push('')
  })

  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

// 处理导出下拉菜单：可导出当前筛选/当前批次，也可导出全部批次。
const handleExportCommand = (command) => {
  const name = productInfo.value.name || productId || batchId || 'trace'
  const today = new Date().toISOString().substring(0, 10)

  if (command === 'all') {
    const allData = allBatches.value.map(b => ({
      ...b,
      production: productionRecords.value.filter(r => r.batchNo === b.batchNo),
      inspection: inspectionReports.value.filter(r => r.batchNo === b.batchNo),
      logistics: logistics.value.filter(r => r.batchNo === b.batchNo)
    }))
    if (allData.length === 0) { ElMessage.warning('暂无可导出数据'); return }
    exportToCSV(allData, `溯源记录_${name}_全部批次_${today}.csv`)
  } else {
    if (displayBatchData.value.length === 0) { ElMessage.warning('当前无可导出的数据'); return }
    const label = activeBatch.value === 'all' ? '当前显示' : activeBatch.value
    exportToCSV(displayBatchData.value, `溯源记录_${name}_${label}_${today}.csv`)
  }
}

// 使用 qrcode 库把当前页面完整 URL 绘制到 canvas，扫码即可回到同一溯源页。
const generateQRCode = async () => {
  const url = `${window.location.origin}${route.fullPath}`
  try {
    await QRCode.toCanvas(qrcodeCanvas.value, url, { width: 160, margin: 2 })
  } catch {
    ElMessage.error('二维码生成失败')
  }
}

// 将二维码 canvas 转成 PNG dataURL，并触发浏览器下载。
const downloadQRCode = () => {
  const canvas = qrcodeCanvas.value
  if (!canvas) return
  const a = document.createElement('a')
  a.download = `product_${productId || batchId}_qrcode.png`
  a.href = canvas.toDataURL()
  a.click()
}

// 根据路由参数选择产品维度或批次维度接口，加载产品信息和三类溯源记录。
const fetchTraceInfo = async () => {
  loadingProduct.value = true
  loadingTrace.value = true
  try {
    const res = batchId ? await getBatchTraceInfo(batchId) : await getTraceInfo(productId)
    if (res.code === 200) {
      productInfo.value = res.data.product || {}
      batchMetas.value = res.data.batches || []
      productionRecords.value = res.data.productionRecords || []
      inspectionReports.value = res.data.inspectionReports || []
      logistics.value = res.data.logistics || []
    }
  } catch {
    ElMessage.error('溯源信息加载失败')
  } finally {
    loadingProduct.value = false
    loadingTrace.value = false
  }
}

onMounted(() => {
  fetchTraceInfo()
  generateQRCode()
})
</script>

<style scoped>
.trace-detail {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 产品信息卡 */
.info-layout {
  display: flex;
  align-items: flex-start;
  gap: 32px;
}
.product-info-panel {
  flex: 1;
}
.product-info-panel h2 {
  font-size: 26px;
  margin: 0 0 16px;
  color: #303133;
}
.product-desc {
  max-width: 560px;
}
/* 提高选择器特异度，覆盖 Element Plus 内置的 .el-descriptions__cell 字号规则 */
.product-desc :deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
  font-size: 17px;
}
.product-desc :deep(.el-descriptions__body .el-descriptions__table.is-bordered .el-descriptions__cell) {
  padding: 14px 16px;
}
.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.product-gallery {
  max-width: 560px;
  margin-top: 16px;
}
.batch-gallery {
  margin: 0;
}
.gallery-image {
  width: 108px;
  height: 78px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e7edf5;
  background: #f8fafc;
}
.record-image {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  display: inline-block;
  overflow: hidden;
  border: 1px solid #e7edf5;
}
.inspection-image-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}
.no-image {
  color: #c0c4cc;
  font-size: 14px;
}
.no-product {
  color: #999;
  padding: 20px;
  flex: 1;
}
.qrcode-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  text-align: center;
}
.qrcode-title {
  margin: 0 0 4px;
  font-size: 17px;
  color: #303133;
}
.qrcode-tip {
  color: #999;
  font-size: 14px;
  margin: 0 0 12px;
}

/* 筛选卡 */
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-label {
  font-size: 15px;
  color: #606266;
  white-space: nowrap;
}
.filter-right {
  flex-shrink: 0;
}
.batch-selector {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
.batch-selector-label {
  font-size: 15px;
  color: #606266;
  white-space: nowrap;
  padding-top: 4px;
}
.batch-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.batch-tag {
  cursor: pointer;
}
.tag-date {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
.no-batch-tip {
  font-size: 14px;
  color: #909399;
  padding: 4px 0;
}

/* 记录区 */
.records-area {
  min-height: 200px;
}
.batch-collapse {
  border: none;
}
.batch-collapse-item {
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.collapse-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  padding-right: 16px;
  min-width: 0;
}
.batch-no-tag {
  font-size: 14px;
  flex-shrink: 0;
}
.batch-date-text {
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}
.batch-count-tags {
  display: flex;
  gap: 6px;
  margin-left: auto;
  flex-shrink: 0;
}

/* 批次内容 */
.batch-content {
  padding: 4px 20px 20px;
}
.sub-section {
  margin-top: 20px;
}
.sub-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}
.sub-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.batch-note-section {
  margin-top: 10px;
  padding: 12px;
  border-radius: 8px;
  background: #fbfcfe;
  border: 1px solid #eef2f7;
}


/* 物流时间线 */
.batch-timeline {
  padding-left: 12px;
  margin-top: 8px;
}
.logistics-card {
  background: #f9f9f9;
}
.logistics-location {
  color: #909399;
  font-size: 14px;
  margin-left: 12px;
}
.logistics-operator {
  color: #606266;
  font-size: 14px;
  margin-left: 12px;
}
.unassigned-logistics-card {
  border: 1px dashed #d9d9d9;
}

/* Element Plus 样式覆盖 */
:deep(.el-collapse-item__header) {
  padding: 0 16px;
  height: 60px;
  background-color: #f5f7fa;
  font-size: 15px;
  border-bottom: 1px solid #e4e7ed;
}
:deep(.el-collapse-item__content) {
  padding-bottom: 0;
}
:deep(.el-collapse-item__wrap) {
  border-bottom: none;
}
:deep(.el-collapse) {
  border: none;
}
:deep(.el-collapse-item) {
  border: none;
}
</style>
