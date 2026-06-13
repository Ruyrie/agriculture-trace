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
 
const currentPage = computed({ 
  get: () => props.page, 
  set: (val) => emit('update:page', val) 
}) 
const currentPageSize = computed({ 
  get: () => props.pageSize, 
  set: (val) => emit('update:pageSize', val) 
}) 
 
const handleSizeChange = (size) => { 
  emit('update:pageSize', size) 
} 
const handleCurrentChange = (page) => { 
  emit('update:page', page) 
} 
</script>
