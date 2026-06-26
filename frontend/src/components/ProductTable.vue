<template> 
  <el-table :data="data" border stripe> 
    <el-table-column prop="id" label="ID" width="80" /> 
    <el-table-column prop="name" label="产品名称" /> 
    <el-table-column label="图片" width="92" align="center">
      <template #default="{ row }">
        <el-image
          v-if="firstImage(row)"
          :src="firstImage(row)"
          :preview-src-list="previewImages(row)"
          preview-teleported
          fit="cover"
          class="product-thumb"
        />
      </template>
    </el-table-column>
    <el-table-column prop="category" label="类别" /> 
    <el-table-column prop="origin" label="产地" /> 
    <el-table-column prop="price" label="价格(元/kg)" /> 
    <el-table-column label="数据指纹" min-width="150">
      <template #default="{ row }">
        <HashTag :hash="row.dataHash" />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="260"> 
      <template #default="{ row }"> 
        <el-button type="primary" link @click="emit('edit', row)">编辑</el-button> 
        <el-button type="danger" link @click="emit('delete', row)">删除</el-button> 
        <el-button type="info" link @click="emit('trace', row)">溯源</el-button> 
        <el-button type="success" link @click="emit('verify', row)">验证哈希</el-button>
      </template> 
    </el-table-column> 
  </el-table> 
</template> 
 
<script setup>
/**
 * ProductTable.vue — 产品列表表格纯展示组件。
 *
 * 纯展示组件（dumb component），不包含任何业务逻辑：
 *   - 接收产品数组 data，渲染 el-table（包含产品缩略图、数据指纹 HashTag、操作按钮）。
 *   - 操作按钮（编辑/删除/溯源/验证哈希）全部以 emit 事件抛出，由 ProductList.vue 处理。
 *   - 图片通过 parseImageUrls + resolveImageUrl 将存储格式转为可访问 URL。
 *
 * Props:
 *   data — 产品列表数组，每项含 id/name/category/origin/price/dataHash/imageUrls
 *
 * Emits:
 *   edit   — 编辑产品，产品 row 对象作参数
 *   delete — 删除产品，产品 row 对象作参数
 *   trace  — 打开溯源详情页，产品 row 对象作参数
 *   verify — 校验产品数据指纹，产品 row 对象作参数
 */
import HashTag from '@/components/HashTag.vue'
import { parseImageUrls, resolveImageUrl } from '@/utils/images'

// data：产品列表数组，来自 ProductList.vue 的 tableData 分页数据。
defineProps({
  data: {
    type: Array,
    default: () => []
  }
})
// edit/delete/trace/verify 分别对应编辑、删除、打开溯源页、校验产品哈希四个操作。
const emit = defineEmits(['edit', 'delete', 'trace', 'verify'])

const previewImages = (row) => parseImageUrls(row.imageUrls).map(resolveImageUrl)
const firstImage = (row) => previewImages(row)[0] || ''
</script>

<style scoped>
.product-thumb {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  display: block;
  overflow: hidden;
}
</style>
