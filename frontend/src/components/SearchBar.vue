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
/**
 * SearchBar.vue — 产品名称搜索栏组件。
 *
 * 一个轻量的输入框 + 搜索按钮组件，通过 v-model 与父组件双向绑定 keyword 值，
 * 并在用户点击"搜索"或清空输入框时 emit 'search' 事件通知父组件发起请求。
 * 当前在 ProductList.vue 中使用，若搜索条件更复杂的页面（如 BatchList.vue）则内联实现。
 *
 * Props:
 *   modelValue — 搜索关键词字符串（v-model 双向绑定）
 *
 * Emits:
 *   update:modelValue — 输入变化时同步到父组件
 *   search            — 用户触发搜索（点击按钮或清空）时通知父组件拉取数据
 */
import { ref, watch } from 'vue'

// modelValue：来自父组件 v-model 绑定的关键词字符串。
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})
// update:modelValue：实现 v-model 的写端，更新父组件搜索关键词。
// search：通知父组件执行搜索请求。
const emit = defineEmits(['update:modelValue', 'search'])

// 内部 keyword 与 modelValue 双向同步，使输入框既受控又可本地编辑。
const keyword = ref(props.modelValue) 
 
// 监听外部传入值的变化（v-model 双向绑定） 
watch(() => props.modelValue, (val) => { 
  keyword.value = val 
}) 
 
// 监听内部输入变化，同步到父组件 
watch(keyword, (val) => { 
  emit('update:modelValue', val) 
}) 
 
// 点击搜索按钮时通知父组件按当前 keyword 拉取数据。
const handleSearch = () => { 
  emit('search') 
} 
 
// 清空输入框后同步清空父组件 keyword，并立即重新搜索。
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
