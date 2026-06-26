<template> 
  <el-pagination 
    v-model:current-page="currentPage" 
    v-model:page-size="currentPageSize" 
    :total="total" 
    :page-sizes="[5, 10, 20]" 
    layout="total, sizes, prev, pager, next, jumper" 
    @size-change="handleSizeChange" 
    @current-change="handleCurrentChange" 
  /> 
</template> 
 
<script setup>
/**
 * Pagination.vue — 通用分页组件。
 *
 * 封装 el-pagination，通过 v-model:page / v-model:pageSize 与父组件双向绑定，
 * 避免各列表页重复写分页事件处理逻辑。
 * 当前在 AuditLog.vue 中使用，其他页面（如 BatchList.vue）直接使用原生 el-pagination。
 *
 * Props:
 *   page     — 当前页码（v-model:page）
 *   pageSize — 每页条数（v-model:pageSize）
 *   total    — 数据总条数，来自后端分页接口 res.data.total
 *
 * Emits:
 *   update:page     — 页码变化时通知父组件
 *   update:pageSize — 页大小变化时通知父组件
 */
import { computed } from 'vue'

// page / pageSize / total 均由父组件绑定，Pagination 内部不存储任何业务数据。
const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})
// update:page 和 update:pageSize 实现双 v-model，父组件使用 v-model:page 和 v-model:page-size 绑定。
const emit = defineEmits(['update:page', 'update:pageSize']) 
 
// currentPage 桥接父组件的 v-model:page，Element Plus 改页时会同步回父组件。
const currentPage = computed({ 
  get: () => props.page, 
  set: (val) => emit('update:page', val) 
}) 
// currentPageSize 桥接父组件的 v-model:pageSize。
const currentPageSize = computed({ 
  get: () => props.pageSize, 
  set: (val) => emit('update:pageSize', val) 
}) 
 
// 页大小变化时通知父组件重新设置 pageSize。
const handleSizeChange = (size) => { 
  emit('update:pageSize', size) 
} 
// 当前页变化时通知父组件重新设置 page。
const handleCurrentChange = (page) => { 
  emit('update:page', page) 
} 
</script>
