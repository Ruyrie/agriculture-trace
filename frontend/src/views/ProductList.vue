<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-left">
        <h3 class="page-title">产品管理</h3>
        <span class="page-subtitle">共 {{ total }} 条记录</span>
      </div>
      <div class="page-actions">
        <el-button type="success" plain @click="handleVerifyAll" :loading="verifyingAll" size="large">
          <el-icon><CircleCheck /></el-icon> 一键验证
        </el-button>
        <el-button type="primary" @click="handleAdd" size="large">
          <el-icon><Plus /></el-icon> 新增产品
        </el-button>
      </div>
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
      <ProductTable :data="tableData" @edit="handleEdit" @delete="handleDelete" @trace="viewTrace" @verify="handleVerify" />

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

    <el-dialog v-model="verifyDialogVisible" title="异常产品明细" width="900px">
      <el-table :data="verifyResult.invalidItems || []" stripe>
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="name" label="产品名称" min-width="120" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column prop="origin" label="产地" min-width="120" />
        <el-table-column label="存储指纹" min-width="220">
          <template #default="{ row }">
            <code class="hash-code">{{ row.storedHash || '未生成' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="当前指纹" min-width="220">
          <template #default="{ row }">
            <code class="hash-code">{{ row.currentHash }}</code>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { CircleCheck, Plus, Search } from '@element-plus/icons-vue'
import { getProductList, addProduct, updateProduct, deleteProduct, addProductWithTrace } from '@/api/product'
import { updateBatch } from '@/api/batch'
import { verifyAllProductHashes, verifyProductHash } from '@/api/integrity'
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
const formData = reactive({ id: null, name: '', category: '', origin: '', price: 0, imageUrls: '' })
const verifyingAll = ref(false)
const verifyDialogVisible = ref(false)
const verifyResult = ref({ total: 0, invalidCount: 0, invalidItems: [] })

// 拉取产品分页数据，并同步总数供分页器和标题展示。
const fetchData = async () => {
  const res = await getProductList({ page: page.value, pageSize: pageSize.value, keyword: searchKeyword.value })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

// 打开新增产品弹窗，并清空表单基础字段。
const handleAdd = () => {
  dialogTitle.value = '新增产品'
  Object.assign(formData, { id: null, name: '', category: '', origin: '', price: 0, imageUrls: '' })
  dialogVisible.value = true
}

// 打开编辑产品弹窗，把当前行数据作为初始值传入 ProductFormDialog。
const handleEdit = (row) => {
  dialogTitle.value = '编辑产品'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 处理产品弹窗提交：新增可走复合溯源接口，编辑时可顺带更新一个批次的溯源明细。
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
      batch: { batchNo: _traceData.batchNo, productionDate: _traceData.productionDate, remark: _traceData.remark, imageUrls: _traceData.imageUrls },
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

// 删除产品前二次确认，成功后刷新列表。
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

// 跳转公开溯源详情页，页面内会基于当前 URL 生成二维码。
const viewTrace = (row) => {
  router.push(`/trace/${row.id}`)
}

// 校验单个产品数据指纹，提示是否可能被绕过系统改动。
const handleVerify = async (row) => {
  const res = await verifyProductHash(row.id)
  if (res.code === 200 && res.data?.valid) {
    ElMessage.success('产品哈希一致，数据未发现篡改')
  } else {
    ElMessage.warning('产品哈希不一致，数据可能被改动')
  }
}

// 批量校验所有产品指纹；存在异常时打开明细弹窗。
const handleVerifyAll = async () => {
  verifyingAll.value = true
  try {
    const res = await verifyAllProductHashes()
    if (res.code === 200) {
      verifyResult.value = res.data || { total: 0, invalidCount: 0, invalidItems: [] }
      if (verifyResult.value.valid) {
        ElMessage.success('全部产品哈希一致，数据未发现篡改')
      } else {
        ElMessage.warning(`发现 ${verifyResult.value.invalidCount} 项产品数据异常`)
        verifyDialogVisible.value = true
      }
    } else {
      ElMessage.error(res.message || '产品数据验证失败')
    }
  } finally {
    verifyingAll.value = false
  }
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

.page-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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

.hash-code {
  display: inline-block;
  max-width: 100%;
  color: #374151;
  font-size: 12px;
  overflow-wrap: anywhere;
}
</style>
