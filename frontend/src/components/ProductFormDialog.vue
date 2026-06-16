<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="min(1040px, calc(100vw - 32px))"
    class="product-form-dialog"
    align-center
    @update:model-value="handleClose"
    :close-on-click-modal="false"
  >
    <el-form
      :model="mergedForm"
      :rules="allRules"
      ref="formRef"
      label-position="top"
      class="product-form"
      status-icon
    >
      <!-- ===== 基本信息 ===== -->
      <div class="trace-section">
        <div class="section-title">
          <span class="section-mark">01</span>
          <span>基本信息</span>
        </div>
        <div class="form-grid form-grid-basic">
          <el-form-item label="产品名称" prop="name">
            <el-input v-model="mergedForm.name" placeholder="请输入产品名称" />
          </el-form-item>
          <el-form-item label="类别" prop="category">
            <el-input v-model="mergedForm.category" placeholder="如：水果、茶叶、粮食" />
          </el-form-item>
          <el-form-item label="产地" prop="origin" class="span-2">
            <el-select v-model="mergedForm.origin" filterable placeholder="请选择或搜索产地" :loading="originsLoading">
              <el-option-group label="国际产地">
                <el-option v-for="item in internationalOrigins" :key="item" :label="item" :value="item" />
              </el-option-group>
              <el-option-group label="国内省份">
                <el-option v-for="item in domesticOrigins" :key="item" :label="item" :value="item" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item label="价格(元/kg)" prop="price">
            <el-input-number v-model="mergedForm.price" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </div>
      </div>

      <!-- ===== 批次与溯源信息 ===== -->
      <div class="trace-section">
        <div class="section-title">
          <span class="section-mark">02</span>
          <span>批次信息</span>
        </div>
        <div v-if="mergedForm.id" class="form-grid form-grid-two">
          <el-form-item label="选择维护批次" prop="batchId">
            <el-select
              v-model="mergedForm.batchId"
              placeholder="请选择要维护的批次"
              :loading="batchLoading"
              clearable
              @change="handleBatchChange"
            >
              <el-option
                v-for="batch in batchOptions"
                :key="batch.id"
                :label="`${batch.batchNo} / ${batch.productionDate || '未填日期'}`"
                :value="batch.id"
              />
            </el-select>
          </el-form-item>
          <div class="batch-help">
            当前产品共有 {{ batchOptions.length }} 个批次，可选择其中一个维护生产、质检和物流记录。
          </div>
        </div>
        <div v-if="!mergedForm.id || mergedForm.batchId" class="form-grid form-grid-two">
          <el-form-item label="批次号" prop="batchNo">
            <el-input v-model="mergedForm.batchNo" placeholder="如：BATCH2026008" />
          </el-form-item>
          <el-form-item label="生产日期" prop="productionDate">
            <el-date-picker
              v-model="mergedForm.productionDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择生产日期"
            />
          </el-form-item>
          <el-form-item label="批次备注" prop="batchRemark" class="span-all">
            <el-input v-model="mergedForm.batchRemark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="填写批次备注" />
          </el-form-item>
        </div>
        <div v-else class="empty-hint neutral">当前产品暂无批次，请先在批次管理中新增批次。</div>
      </div>

      <template v-if="!mergedForm.id || mergedForm.batchId">
        <!-- 生产记录 -->
        <div class="trace-section">
          <div class="section-title section-title-action">
            <div class="title-group">
              <span class="section-mark">03</span>
              <span>生产记录</span>
              <span class="required-hint">至少1条</span>
            </div>
            <el-button size="small" type="success" plain :icon="Plus" @click="addProduction">添加</el-button>
          </div>
          <div class="record-list">
            <div v-for="(rec, idx) in mergedForm.productionRecords" :key="idx" class="record-row production-grid">
              <span class="row-index">{{ idx + 1 }}</span>
              <el-form-item :prop="`productionRecords.${idx}.activityName`" :rules="rowRequired('活动名称')">
                <el-input v-model="rec.activityName" placeholder="活动名称" />
              </el-form-item>
              <el-form-item :prop="`productionRecords.${idx}.operator`" :rules="rowRequired('操作员')">
                <el-input v-model="rec.operator" placeholder="操作员" />
              </el-form-item>
              <el-form-item :prop="`productionRecords.${idx}.activityDate`" :rules="rowRequired('日期')">
                <el-date-picker v-model="rec.activityDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" />
              </el-form-item>
              <el-input v-model="rec.remark" placeholder="备注" />
              <el-button class="delete-btn" type="danger" :icon="Delete" circle plain @click="removeProduction(idx)" />
            </div>
          </div>
          <div v-if="mergedForm.productionRecords.length === 0" class="empty-hint">请至少添加一条生产记录</div>
        </div>

        <!-- 质检记录 -->
        <div class="trace-section">
          <div class="section-title section-title-action">
            <div class="title-group">
              <span class="section-mark">04</span>
              <span>质检记录</span>
              <span class="required-hint">至少1条</span>
            </div>
            <el-button size="small" type="primary" plain :icon="Plus" @click="addInspection">添加</el-button>
          </div>
          <div class="record-list">
            <div v-for="(rec, idx) in mergedForm.inspectionRecords" :key="idx" class="record-row inspection-grid">
              <span class="row-index">{{ idx + 1 }}</span>
              <el-form-item :prop="`inspectionRecords.${idx}.inspectionItem`" :rules="rowRequired('检测项目')">
                <el-input v-model="rec.inspectionItem" placeholder="检测项目" />
              </el-form-item>
              <el-form-item :prop="`inspectionRecords.${idx}.result`" :rules="rowRequired('检测结果')">
                <el-input v-model="rec.result" placeholder="检测结果" />
              </el-form-item>
              <el-form-item :prop="`inspectionRecords.${idx}.inspector`" :rules="rowRequired('检测员')">
                <el-input v-model="rec.inspector" placeholder="检测员" />
              </el-form-item>
              <el-form-item :prop="`inspectionRecords.${idx}.inspectionDate`" :rules="rowRequired('日期')">
                <el-date-picker v-model="rec.inspectionDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" />
              </el-form-item>
              <el-button class="delete-btn" type="danger" :icon="Delete" circle plain @click="removeInspection(idx)" />
            </div>
          </div>
          <div v-if="mergedForm.inspectionRecords.length === 0" class="empty-hint">请至少添加一条质检记录</div>
        </div>

        <!-- 物流记录 -->
        <div class="trace-section">
          <div class="section-title section-title-action">
            <div class="title-group">
              <span class="section-mark">05</span>
              <span>物流轨迹</span>
              <span class="required-hint">至少1条</span>
            </div>
            <el-button size="small" type="warning" plain :icon="Plus" @click="addLogistics">添加</el-button>
          </div>
          <div class="record-list">
            <div v-for="(rec, idx) in mergedForm.logisticsRecords" :key="idx" class="record-row logistics-grid">
              <span class="row-index">{{ idx + 1 }}</span>
              <el-form-item :prop="`logisticsRecords.${idx}.nodeName`" :rules="rowRequired('节点名称')">
                <el-input v-model="rec.nodeName" placeholder="节点名称" />
              </el-form-item>
              <el-form-item :prop="`logisticsRecords.${idx}.location`" :rules="rowRequired('地点')">
                <el-input v-model="rec.location" placeholder="地点" />
              </el-form-item>
              <el-form-item :prop="`logisticsRecords.${idx}.operator`" :rules="rowRequired('操作员')">
                <el-input v-model="rec.operator" placeholder="操作员" />
              </el-form-item>
              <el-form-item :prop="`logisticsRecords.${idx}.updateTime`" :rules="rowRequired('时间')">
                <el-date-picker v-model="rec.updateTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="时间" />
              </el-form-item>
              <el-button class="delete-btn" type="danger" :icon="Delete" circle plain @click="removeLogistics(idx)" />
            </div>
          </div>
          <div v-if="mergedForm.logisticsRecords.length === 0" class="empty-hint">请至少添加一条物流记录</div>
        </div>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getBatchList } from '@/api/batch'
import { getBatchTraceInfo } from '@/api/trace'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '表单' },
  initialData: {
    type: Object,
    default: () => ({ id: null, name: '', category: '', origin: '', price: 0 })
  }
})
const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const submitting = ref(false)

// 每次打开/关闭弹窗自增，异步加载结果若与当前会话不一致则丢弃，
// 避免上一次编辑的批次/溯源数据在弹窗已重置后回填，造成“数据没清空”。
let loadSession = 0

// 合并表单：基本信息 + 溯源信息放在同一个 reactive 对象
const mergedForm = reactive({
  id: null, name: '', category: '', origin: '', price: 0,
  batchId: '', batchNo: '', productionDate: '', batchRemark: '',
  productionRecords: [],
  inspectionRecords: [],
  logisticsRecords: []
})

const internationalOrigins = ref([])
const domesticOrigins = ref([])
const originsLoading = ref(false)
const batchOptions = ref([])
const batchLoading = ref(false)

// 生成动态表格行的必填校验规则，field 用于拼接友好错误消息。
const rowRequired = (field) => [{ required: true, message: `${field}不能为空`, trigger: ['blur', 'change'] }]

// 新增日期默认当天；物流时间默认当前日期时间（含时分），与系统当前时间同步。
const pad = (value) => String(value).padStart(2, '0')
const currentDateText = () => {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
const currentDateTimeText = () => {
  const now = new Date()
  return `${currentDateText()} ${pad(now.getHours())}:${pad(now.getMinutes())}:00`
}

// 根据当前是否在维护批次动态追加批次号、生产日期校验规则。
const allRules = computed(() => {
  const base = {
    name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
    category: [{ required: true, message: '请输入类别', trigger: 'blur' }],
    origin: [{ required: true, message: '请选择产地', trigger: 'change' }],
    price: [{ required: true, type: 'number', message: '请输入价格', trigger: 'blur' }]
  }
  if (!mergedForm.id || mergedForm.batchId) {
    base.batchNo = [{ required: true, message: '请输入批次号', trigger: 'blur' }]
    base.productionDate = [{ required: true, message: '请选择生产日期', trigger: 'change' }]
  }
  return base
})

// 加载产地候选项，供产品产地下拉框分组展示。
const fetchOrigins = async () => {
  originsLoading.value = true
  try {
    const res = await request.get('/product/origins')
    if (res.code === 200) {
      internationalOrigins.value = res.data.international || []
      domesticOrigins.value = res.data.domestic || []
    }
  } catch (e) {
    ElMessage.error('产地列表加载失败')
  } finally {
    originsLoading.value = false
  }
}

// 重置弹窗表单到初始空状态，同时清空可选批次列表。
const resetForm = () => {
  Object.assign(mergedForm, {
    id: null, name: '', category: '', origin: '', price: 0,
    batchId: '', batchNo: '', productionDate: '', batchRemark: '',
    productionRecords: [],
    inspectionRecords: [],
    logisticsRecords: []
  })
  batchOptions.value = []
}

// 新增产品时默认补齐三类溯源记录各一行，减少用户首次录入成本。
const ensureDefaultTraceRows = () => {
  if (mergedForm.productionRecords.length === 0) addProduction()
  if (mergedForm.inspectionRecords.length === 0) addInspection()
  if (mergedForm.logisticsRecords.length === 0) addLogistics()
}

watch(() => props.visible, async (val) => {
  if (val) {
    const session = ++loadSession
    resetForm()
    Object.assign(mergedForm, props.initialData)
    if (!mergedForm.id) {
      // 新增产品时，生产日期默认当天，避免每次手动选择。
      if (!mergedForm.productionDate) mergedForm.productionDate = currentDateText()
      ensureDefaultTraceRows()
    } else {
      await loadProductBatches(mergedForm.id, session)
    }
    if (formRef.value) formRef.value.clearValidate()
  } else {
    // 关闭时让所有在途请求作废，防止其回填到下一次打开的表单
    loadSession++
  }
})

const clearTraceRows = () => {
  mergedForm.productionRecords.splice(0)
  mergedForm.inspectionRecords.splice(0)
  mergedForm.logisticsRecords.splice(0)
}

// 将批次基础字段填入弹窗表单；传空值时清空批次字段。
const fillBatchBase = (batch) => {
  Object.assign(mergedForm, {
    batchId: batch?.id || '',
    batchNo: batch?.batchNo || '',
    productionDate: batch?.productionDate || '',
    batchRemark: batch?.remark || ''
  })
}

// 加载当前产品的批次列表，默认选中第一个批次并拉取其溯源记录。
const loadProductBatches = async (productId, session = loadSession) => {
  batchLoading.value = true
  try {
    const res = await getBatchList({ page: 1, pageSize: 100, productId })
    if (session !== loadSession) return
    if (res.code === 200) {
      batchOptions.value = res.data.records || []
      if (batchOptions.value.length > 0) {
        fillBatchBase(batchOptions.value[0])
        await loadBatchTraceRows(mergedForm.batchId, session)
      } else {
        fillBatchBase(null)
        clearTraceRows()
      }
    }
  } catch {
    if (session === loadSession) ElMessage.error('批次列表加载失败')
  } finally {
    if (session === loadSession) batchLoading.value = false
  }
}

// 切换维护批次时同步批次基础信息，并重新加载该批次的三类溯源记录。
const handleBatchChange = async (batchId) => {
  const session = loadSession
  const batch = batchOptions.value.find(item => item.id === batchId)
  fillBatchBase(batch)
  clearTraceRows()
  if (batchId) await loadBatchTraceRows(batchId, session)
}

// 拉取某个批次的溯源详情，并回填到生产、质检、物流三个动态表格。
const loadBatchTraceRows = async (batchId, session = loadSession) => {
  try {
    const res = await getBatchTraceInfo(batchId)
    if (session !== loadSession) return
    if (res.code !== 200) {
      ElMessage.error(res.message || '批次溯源记录加载失败')
      return
    }
    // 编辑产品时加载选中批次的三类溯源记录，提交时会随批次一起保存。
    mergedForm.productionRecords.splice(
      0,
      mergedForm.productionRecords.length,
      ...(res.data.productionRecords || []).map(record => ({
        activityName: record.activityName || '',
        operator: record.operator || '',
        activityDate: record.activityDate || '',
        remark: record.remark || ''
      }))
    )
    mergedForm.inspectionRecords.splice(
      0,
      mergedForm.inspectionRecords.length,
      ...(res.data.inspectionReports || []).map(record => ({
        inspectionItem: record.inspectionItem || record.item || '',
        result: record.result || '',
        inspector: record.inspector || '',
        inspectionDate: record.inspectionDate || record.date || ''
      }))
    )
    mergedForm.logisticsRecords.splice(
      0,
      mergedForm.logisticsRecords.length,
      ...(res.data.logistics || []).map(record => ({
        nodeName: record.nodeName || record.node || '',
        location: record.location || '',
        operator: record.operator || '',
        updateTime: record.updateTime || record.time || ''
      }))
    )
  } catch {
    if (session === loadSession) ElMessage.error('批次溯源记录加载失败')
  }
}

// 新增一行生产记录，日期默认当天。
const addProduction = () => mergedForm.productionRecords.push({ activityName: '', operator: '', activityDate: currentDateText(), remark: '' })
// 删除指定生产记录行。
const removeProduction = (i) => mergedForm.productionRecords.splice(i, 1)
// 新增一行质检记录，日期默认当天。
const addInspection = () => mergedForm.inspectionRecords.push({ inspectionItem: '', result: '', inspector: '', inspectionDate: currentDateText() })
// 删除指定质检记录行。
const removeInspection = (i) => mergedForm.inspectionRecords.splice(i, 1)
// 新增一行物流记录，时间默认当前日期时间。
const addLogistics = () => mergedForm.logisticsRecords.push({ nodeName: '', location: '', operator: '', updateTime: currentDateTimeText() })
// 删除指定物流记录行。
const removeLogistics = (i) => mergedForm.logisticsRecords.splice(i, 1)

// 通知父组件关闭弹窗。
const handleClose = () => emit('update:visible', false)

// 校验并提交弹窗数据；根据新增/编辑场景分别构造 _traceData 或 _batchTraceData。
const handleSubmit = async () => {
  // 新增产品或维护选中批次时，三类溯源记录都必须完整。
  if (!mergedForm.id || mergedForm.batchId) {
    if (mergedForm.productionRecords.length === 0) {
      ElMessage.warning('请至少添加一条生产记录')
      return
    }
    if (mergedForm.inspectionRecords.length === 0) {
      ElMessage.warning('请至少添加一条质检记录')
      return
    }
    if (mergedForm.logisticsRecords.length === 0) {
      ElMessage.warning('请至少添加一条物流记录')
      return
    }
  }

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查必填项是否填写完整')
    return
  }

  submitting.value = true
  try {
    const { batchId, batchNo, productionDate, batchRemark, productionRecords, inspectionRecords, logisticsRecords, ...productData } = mergedForm
    const isNew = !mergedForm.id
    emit('submit', {
      ...productData,
      _withTrace: isNew,
      _traceData: isNew ? { batchNo, productionDate, remark: batchRemark, productionRecords: [...productionRecords], inspectionRecords: [...inspectionRecords], logisticsRecords: [...logisticsRecords] } : null,
      _batchTraceData: !isNew && batchId ? { id: batchId, productId: mergedForm.id, batchNo, productionDate, remark: batchRemark, productionRecords: [...productionRecords], inspectionRecords: [...inspectionRecords], logisticsRecords: [...logisticsRecords] } : null
    })
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchOrigins()
})
</script>

<style scoped>
.product-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: min(72vh, 760px);
  overflow-y: auto;
  padding: 2px 4px 4px;
}

.trace-section {
  border: 1px solid #e7edf5;
  border-radius: 8px;
  padding: 16px;
  background: #fbfcfe;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title-action {
  justify-content: space-between;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.section-mark {
  display: inline-flex;
  width: 26px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #eef6ec;
  color: #2f7d32;
  font-size: 12px;
  font-weight: 700;
}

.required-hint {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #fff1f0;
  color: #f56c6c;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.form-grid {
  display: grid;
  gap: 14px 16px;
}

.form-grid-basic {
  grid-template-columns: 1.3fr 1fr 1.35fr 160px;
}

.form-grid-two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.span-2 {
  min-width: 0;
}

.span-all {
  grid-column: 1 / -1;
}

.batch-help {
  align-self: end;
  min-height: 38px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 7px;
  background: #f6fbf5;
  color: #4b8063;
  font-size: 13px;
}

.product-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.product-form :deep(.el-form-item__label) {
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  margin-bottom: 6px;
}

.product-form :deep(.el-input__wrapper),
.product-form :deep(.el-select__wrapper),
.product-form :deep(.el-input-number .el-input__wrapper) {
  min-height: 38px;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #d9e1ec inset;
}

.product-form :deep(.el-input-number),
.product-form :deep(.el-select),
.product-form :deep(.el-date-editor) {
  width: 100%;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-row {
  display: grid;
  align-items: start;
  gap: 10px;
  padding: 10px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #ffffff;
}

.production-grid {
  grid-template-columns: 30px minmax(150px, 1.3fr) minmax(100px, 0.8fr) minmax(150px, 1fr) minmax(140px, 1fr) 34px;
}

.inspection-grid {
  grid-template-columns: 30px minmax(150px, 1.3fr) minmax(110px, 0.8fr) minmax(110px, 0.8fr) minmax(150px, 1fr) 34px;
}

.logistics-grid {
  grid-template-columns: 30px minmax(130px, 1fr) minmax(180px, 1.4fr) minmax(100px, 0.8fr) minmax(190px, 1.35fr) 34px;
}

.row-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-top: 6px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.delete-btn {
  margin-top: 2px;
}

.record-row :deep(.el-form-item__error) {
  padding-top: 4px;
}

.empty-hint {
  text-align: center;
  color: #f56c6c;
  font-size: 13px;
  padding: 14px 0 2px;
}

.empty-hint.neutral {
  color: #909399;
  padding: 4px 0;
}

:global(.product-form-dialog .el-dialog__header) {
  padding: 20px 24px 12px;
  margin-right: 0;
  border-bottom: 1px solid #eef2f7;
}

:global(.product-form-dialog .el-dialog__title) {
  color: #111827;
  font-size: 20px;
  font-weight: 700;
}

:global(.product-form-dialog .el-dialog__body) {
  padding: 16px 24px;
}

:global(.product-form-dialog .el-dialog__footer) {
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
}

@media (max-width: 960px) {
  .form-grid-basic,
  .form-grid-two,
  .record-row {
    grid-template-columns: 1fr;
  }

  .section-title-action {
    align-items: flex-start;
    gap: 10px;
  }

  .row-index,
  .delete-btn {
    margin-top: 0;
  }

  .delete-btn {
    justify-self: end;
  }
}
</style>
