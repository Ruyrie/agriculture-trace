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
import { computed } from 'vue' 
 
const props = defineProps({ 
  page: { type: Number, default: 1 }, 
  pageSize: { type: Number, default: 10 }, 
  total: { type: Number, default: 0 } 
}) 
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
