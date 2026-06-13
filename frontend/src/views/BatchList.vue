<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">批次管理</h3>
        <span class="page-subtitle">共 {{ total }} 条记录</span>
      </div>
      <el-button type="primary" @click="handleAdd" size="large">
        <el-icon><Plus /></el-icon> 新增批次
      </el-button>
    </div>

    <!-- 主内容卡片 -->
    <el-card shadow="never" class="content-card">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-select v-model="searchProductId" placeholder="按产品筛选" clearable style="width: 200px">
          <el-option
            v-for="product in productOptions"
            :key="product.id"
            :label="product.name"
            :value="product.id"
          />
        </el-select>
        <el-input
          v-model="searchBatchNo"
          placeholder="搜索批次号..."
          prefix-icon="Search"
          clearable
          style="width: 220px"
          @keyup.enter="fetchData"
        />
        <el-button type="primary" plain @click="fetchData">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <!-- 批次表格 -->
      <el-table :data="tableData" stripe class="batch-table">
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (page - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" min-width="140">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.batchNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="productionDate" label="生产日期" width="120" align="center" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="success" link size="small" @click="viewTrace(row)">
              <el-icon><View /></el-icon> 溯源
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(980px, calc(100vw - 32px))"
      class="batch-form-dialog"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="batch-dialog-form">
        <el-tabs v-model="activeTab" class="batch-tabs">
          <el-tab-pane label="基础信息" name="base">
            <div class="tab-panel">
              <div class="panel-hint">先建立批次基础档案，生产、质检和物流记录可以现在填写，也可以后续在编辑里继续补充。</div>
              <div class="batch-form-grid">
                <el-form-item label="产品" prop="productId">
                  <el-select v-model="form.productId" placeholder="请选择产品" style="width: 100%" @change="onProductChange">
                    <el-option
                      v-for="product in productOptions"
                      :key="product.id"
                      :label="product.name"
                      :value="product.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="批次号" prop="batchNo">
                  <el-input v-model="form.batchNo" placeholder="请输入批次号（5-20位字母数字）" />
                </el-form-item>
                <el-form-item label="生产日期" prop="productionDate">
                  <el-date-picker
                    v-model="form.productionDate"
                    type="date"
                    placeholder="默认今天，可修改"
                    value-format="YYYY-MM-DD"
                    :disabled-date="disabledDate"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
              <el-form-item label="备注" prop="remark">
                <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="可选备注信息" />
              </el-form-item>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`生产记录 ${form.productionRecords.length}`" name="production">
            <div class="tab-panel">
              <div class="panel-toolbar">
                <span class="panel-hint">记录采收、加工、包装等生产动作；不确定的信息可先不添加，后续编辑补充。</span>
                <el-button size="small" type="success" plain :icon="Plus" @click="addProduction">添加生产记录</el-button>
              </div>
              <div v-for="(record, index) in form.productionRecords" :key="index" class="record-row production-grid">
                <span class="row-index">{{ index + 1 }}</span>
                <el-input v-model="record.activityName" placeholder="生产活动" />
                <el-input v-model="record.operator" placeholder="操作员" />
                <el-date-picker v-model="record.activityDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天，可修改" />
                <el-input v-model="record.remark" placeholder="备注" />
                <el-button type="danger" plain circle :icon="Delete" @click="removeProduction(index)" />
              </div>
              <div v-if="form.productionRecords.length === 0" class="empty-trace-hint">暂无生产记录，保存后也可以从编辑入口继续添加</div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`质检记录 ${form.inspectionRecords.length}`" name="inspection">
            <div class="tab-panel">
              <div class="panel-toolbar">
                <span class="panel-hint">记录检测项目、结果和检测员；只保存已填写完整的记录。</span>
                <el-button size="small" type="primary" plain :icon="Plus" @click="addInspection">添加质检记录</el-button>
              </div>
              <div v-for="(record, index) in form.inspectionRecords" :key="index" class="record-row inspection-grid">
                <span class="row-index">{{ index + 1 }}</span>
                <el-input v-model="record.inspectionItem" placeholder="检测项目" />
                <el-input v-model="record.result" placeholder="检测结果" />
                <el-input v-model="record.inspector" placeholder="检测员" />
                <el-date-picker v-model="record.inspectionDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天，可修改" />
                <el-button type="danger" plain circle :icon="Delete" @click="removeInspection(index)" />
              </div>
              <div v-if="form.inspectionRecords.length === 0" class="empty-trace-hint">暂无质检记录，检测完成后可回到编辑继续补充</div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`物流轨迹 ${form.logisticsRecords.length}`" name="logistics">
            <div class="tab-panel">
              <div class="panel-toolbar">
                <span class="panel-hint">物流节点通常会逐步产生，可先保存已有节点，后续继续添加运输和签收节点。</span>
                <el-button size="small" type="warning" plain :icon="Plus" @click="addLogistics">添加物流节点</el-button>
              </div>
              <div v-for="(record, index) in form.logisticsRecords" :key="index" class="record-row logistics-grid">
                <span class="row-index">{{ index + 1 }}</span>
                <el-input v-model="record.nodeName" placeholder="节点名称" />
                <el-input v-model="record.location" placeholder="地点" />
                <el-input v-model="record.operator" placeholder="操作员" />
                <el-date-picker v-model="record.updateTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="默认当前时间，可修改" />
                <el-button type="danger" plain circle :icon="Delete" @click="removeLogistics(index)" />
              </div>
              <div v-if="form.logisticsRecords.length === 0" class="empty-trace-hint">暂无物流轨迹，运输开始后可回到编辑继续补充</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, View } from '@element-plus/icons-vue'
import { getBatchList, addBatch, updateBatch, deleteBatch } from '@/api/batch'
import { getProductList } from '@/api/product'
import { getBatchTraceInfo } from '@/api/trace'

const router = useRouter()
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchProductId = ref('')
const searchBatchNo = ref('')
const productOptions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增批次')
const submitting = ref(false)
const formRef = ref()
const activeTab = ref('base')
const form = reactive({
  id: null,
  productId: '',
  batchNo: '',
  productionDate: '',
  remark: '',
  productionRecords: [],
  inspectionRecords: [],
  logisticsRecords: []
})

const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  batchNo: [
    { required: true, message: '请输入批次号', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: '批次号只能包含字母和数字', trigger: 'blur' }
  ],
  productionDate: [{ required: true, message: '请选择生产日期', trigger: 'change' }]
}

const disabledDate = (time) => time.getTime() > Date.now()

const currentDateText = () => new Date().toISOString().slice(0, 10)

const currentDateTimeText = () => {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:00`
}

const onProductChange = (productId) => {
  const product = productOptions.value.find(p => p.id === productId)
  if (product && !form.id) {
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const randomNo = Math.floor(Math.random() * 900 + 100)
    form.batchNo = `B${dateStr}${randomNo}`
  }
}

const fetchData = async () => {
  try {
    const res = await getBatchList({ page: page.value, pageSize: pageSize.value, productId: searchProductId.value, batchNo: searchBatchNo.value })
    if (res.code === 200) {
      // 后端分页接口统一返回 records/total，空值兜底避免页面被异常响应卡住。
      tableData.value = res.data?.records || []
      total.value = Number(res.data?.total || 0)
    } else {
      tableData.value = []
      total.value = 0
      ElMessage.error(res.message || '批次数据加载失败')
    }
  } catch (error) {
    tableData.value = []
    total.value = 0
    if (!error.__handled) {
      ElMessage.error('批次数据加载失败，请确认后端服务已启动')
    }
  }
}

const fetchProductOptions = async () => {
  const res = await getProductList({ page: 1, pageSize: 100 })
  if (res.code === 200) productOptions.value = res.data.records
}

const handleSizeChange = (val) => { pageSize.value = val; fetchData() }
const handleCurrentChange = (val) => { page.value = val; fetchData() }

const addProduction = () => form.productionRecords.push({ activityName: '', operator: '', activityDate: currentDateText(), remark: '' })
const removeProduction = (index) => form.productionRecords.splice(index, 1)
const addInspection = () => form.inspectionRecords.push({ inspectionItem: '', result: '', inspector: '', inspectionDate: currentDateText() })
const removeInspection = (index) => form.inspectionRecords.splice(index, 1)
const addLogistics = () => form.logisticsRecords.push({ nodeName: '', location: '', operator: '', updateTime: currentDateTimeText() })
const removeLogistics = (index) => form.logisticsRecords.splice(index, 1)

const clearTraceRows = () => {
  form.productionRecords.splice(0)
  form.inspectionRecords.splice(0)
  form.logisticsRecords.splice(0)
}

const resetForm = () => {
  Object.assign(form, { id: null, productId: '', batchNo: '', productionDate: currentDateText(), remark: '' })
  clearTraceRows()
  activeTab.value = 'base'
}

const closeDialog = () => {
  dialogVisible.value = false
}

const handleDialogClosed = () => {
  resetForm()
  nextTick(() => formRef.value?.clearValidate())
}

const handleAdd = async () => {
  dialogTitle.value = '新增批次'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const loadTraceRows = async (batchId) => {
  clearTraceRows()
  try {
    const res = await getBatchTraceInfo(batchId)
    if (res.code === 200) {
      form.productionRecords.splice(
        0,
        form.productionRecords.length,
        ...(res.data.productionRecords || []).map(record => ({
          activityName: record.activityName || '',
          operator: record.operator || '',
          activityDate: record.activityDate || '',
          remark: record.remark || ''
        }))
      )
      form.inspectionRecords.splice(
        0,
        form.inspectionRecords.length,
        ...(res.data.inspectionReports || []).map(record => ({
          inspectionItem: record.inspectionItem || record.item || '',
          result: record.result || '',
          inspector: record.inspector || '',
          inspectionDate: record.inspectionDate || record.date || ''
        }))
      )
      form.logisticsRecords.splice(
        0,
        form.logisticsRecords.length,
        ...(res.data.logistics || []).map(record => ({
          nodeName: record.nodeName || record.node || '',
          location: record.location || '',
          operator: record.operator || '',
          updateTime: record.updateTime || record.time || ''
        }))
      )
    } else {
      ElMessage.error(res.message || '溯源记录加载失败')
    }
  } catch (error) {
    if (!error.__handled) {
      ElMessage.error('溯源记录加载失败')
    }
  }
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑批次'
  resetForm()
  Object.assign(form, { id: row.id, productId: row.productId, batchNo: row.batchNo, productionDate: row.productionDate, remark: row.remark })
  activeTab.value = 'base'
  dialogVisible.value = true
  await loadTraceRows(row.id)
  await nextTick()
  formRef.value?.clearValidate()
}

const compactProductionRows = () => form.productionRecords.filter(record => record.activityName || record.operator || record.activityDate || record.remark)
const compactInspectionRows = () => form.inspectionRecords.filter(record => record.inspectionItem || record.result || record.inspector || record.inspectionDate)
const compactLogisticsRows = () => form.logisticsRecords.filter(record => record.nodeName || record.location || record.operator || record.updateTime)

const submitForm = async () => {
  await formRef.value.validate()
  const productionRecords = compactProductionRows()
  const inspectionRecords = compactInspectionRows()
  const logisticsRecords = compactLogisticsRows()
  const missingProduction = productionRecords.some(record => !record.activityName || !record.operator || !record.activityDate)
  const missingInspection = inspectionRecords.some(record => !record.inspectionItem || !record.result || !record.inspector || !record.inspectionDate)
  const missingLogistics = logisticsRecords.some(record => !record.nodeName || !record.location || !record.operator || !record.updateTime)
  if (missingProduction || missingInspection || missingLogistics) {
    ElMessage.warning('已填写的生产、质检或物流记录需要补完整；暂时没有的信息可以删除该行后先保存')
    return
  }
  submitting.value = true
  try {
    const payload = {
      id: form.id,
      productId: form.productId,
      batchNo: form.batchNo,
      productionDate: form.productionDate,
      remark: form.remark,
      productionRecords,
      inspectionRecords,
      logisticsRecords
    }
    const res = form.id ? await updateBatch(payload) : await addBatch(payload)
    if (res.code === 200) {
      ElMessage.success(res.message || '操作成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该批次吗？', '删除确认', { type: 'warning' }).then(async () => {
    const res = await deleteBatch(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

const viewTrace = (row) => {
  if (!row.id) { ElMessage.warning('无法获取批次信息'); return }
  router.push(`/trace/batch/${row.id}`)
}

onMounted(() => {
  fetchProductOptions()
  fetchData()
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

.page-subtitle {
  font-size: 13px;
  color: #909399;
}

.content-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.content-card :deep(.el-card__body) {
  padding: 20px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.batch-table {
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.batch-form-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) minmax(180px, 1fr);
  gap: 12px;
}

.batch-dialog-form {
  max-height: min(68vh, 660px);
  overflow-y: auto;
  padding: 0 2px 4px;
}

.batch-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.batch-tabs :deep(.el-tabs__item) {
  font-weight: 600;
}

.tab-panel {
  min-height: 300px;
  padding: 14px;
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #fbfcfe;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-hint {
  color: #1f2937;
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.72;
}

.tab-panel > .panel-hint {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 7px;
  background: #f4f8f3;
  color: #2f6f3a;
}

.record-row {
  display: grid;
  gap: 8px;
  align-items: start;
  margin-bottom: 8px;
  padding: 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #ffffff;
}

.row-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-top: 6px;
  border-radius: 50%;
  background: #eef6ec;
  color: #2f7d32;
  font-size: 12px;
  font-weight: 700;
}

.empty-trace-hint {
  padding: 10px 0;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.production-grid {
  grid-template-columns: 30px minmax(130px, 1fr) minmax(100px, 0.8fr) minmax(150px, 1fr) minmax(140px, 1fr) 34px;
}

.inspection-grid {
  grid-template-columns: 30px minmax(130px, 1fr) minmax(110px, 0.8fr) minmax(100px, 0.8fr) minmax(150px, 1fr) 34px;
}

.logistics-grid {
  grid-template-columns: 30px minmax(130px, 1fr) minmax(150px, 1fr) minmax(100px, 0.8fr) minmax(190px, 1.2fr) 34px;
}

:global(.batch-form-dialog .el-dialog__header) {
  padding: 18px 22px 10px;
  margin-right: 0;
  border-bottom: 1px solid #eef2f7;
}

:global(.batch-form-dialog .el-dialog__body) {
  padding: 14px 22px;
}

:global(.batch-form-dialog .el-dialog__footer) {
  padding: 12px 22px 18px;
  border-top: 1px solid #eef2f7;
}

@media (max-width: 900px) {
  .batch-form-grid,
  .record-row {
    grid-template-columns: 1fr;
  }

  .panel-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .row-index {
    margin-top: 0;
  }
}
</style>
