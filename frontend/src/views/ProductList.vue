<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">产品管理</h3>
        <span class="page-subtitle">共 {{ total }} 条记录</span>
      </div>
      <el-button type="primary" @click="handleAdd" size="large">
        <el-icon><Plus /></el-icon> 新增产品
      </el-button>
    </div>

    <!-- 主内容卡片 -->
    <el-card shadow="never" class="content-card">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索产品名称..."
          prefix-icon="Search"
          clearable
          style="width: 300px"
          @keyup.enter="fetchData"
          @clear="fetchData"
        />
        <el-button type="primary" plain @click="fetchData">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <!-- 表格 -->
      <ProductTable :data="tableData" @edit="handleEdit" @delete="handleDelete" @trace="viewTrace" />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <Pagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @update:page="fetchData"
          @update:page-size="fetchData"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <ProductFormDialog
      v-model:visible="dialogVisible"
      :title="dialogTitle"
      :initial-data="formData"
      @submit="submitForm"
    />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { getProductList, addProduct, updateProduct, deleteProduct, addProductWithTrace } from '@/api/product'
import { updateBatch } from '@/api/batch'
import SearchBar from '@/components/SearchBar.vue'
import ProductTable from '@/components/ProductTable.vue'
import Pagination from '@/components/Pagination.vue'
import ProductFormDialog from '@/components/ProductFormDialog.vue'

const router = useRouter()
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增产品')
const formData = reactive({ id: null, name: '', category: '', origin: '', price: 0 })

const fetchData = async () => {
  const res = await getProductList({ page: page.value, pageSize: pageSize.value, keyword: searchKeyword.value })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增产品'
  Object.assign(formData, { id: null, name: '', category: '', origin: '', price: 0 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑产品'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const submitForm = async (data) => {
  let res
  if (data.id) {
    const { _withTrace, _traceData, _batchTraceData, ...productData } = data
    res = await updateProduct(productData)
    if (res.code === 200 && _batchTraceData) {
      // 产品编辑页允许顺手维护一个关联批次的完整溯源记录，最终仍走批次更新接口落库。
      res = await updateBatch(_batchTraceData)
    }
  } else if (data._withTrace) {
    const { _withTrace, _traceData, ...productData } = data
    res = await addProductWithTrace({
      product: productData,
      batch: { batchNo: _traceData.batchNo, productionDate: _traceData.productionDate, remark: _traceData.remark },
      productionRecords: _traceData.productionRecords,
      inspectionRecords: _traceData.inspectionRecords,
      logisticsRecords: _traceData.logisticsRecords
    })
  } else {
    const { _withTrace, _traceData, ...productData } = data
    res = await addProduct(productData)
  }
  if (res.code === 200) {
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } else {
    ElMessage.error(res.message || '操作失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该产品吗？此操作不可逆。', '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    const res = await deleteProduct(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  })
}

const viewTrace = (row) => {
  router.push(`/trace/${row.id}`)
}

fetchData()
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
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
