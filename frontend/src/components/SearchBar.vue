<template> 
  <div class="search-bar"> 
    <el-input 
      v-model="keyword" 
      placeholder="产品名称" 
      style="width: 200px" 
      clearable 
      @clear="handleClear" 
    /> 
    <el-button type="primary" @click="handleSearch">搜索</el-button> 
  </div> 
</template> 
 
<script setup> 
import { ref, watch } from 'vue' 
 
const props = defineProps({ 
  modelValue: { 
    type: String, 
    default: '' 
  } 
}) 
const emit = defineEmits(['update:modelValue', 'search']) 
 
const keyword = ref(props.modelValue) 
 
// 监听外部传入值的变化（v-model 双向绑定） 
watch(() => props.modelValue, (val) => { 
  keyword.value = val 
}) 
 
// 监听内部输入变化，同步到父组件 
watch(keyword, (val) => { 
  emit('update:modelValue', val) 
}) 
 
const handleSearch = () => { 
  emit('search') 
} 
 
const handleClear = () => { 
  emit('update:modelValue', '') 
  emit('search') 
} 
</script> 
 
<style scoped> 
.search-bar { 
  display: flex; 
  gap: 10px; 
  margin-bottom: 20px; 
} 
</style>
