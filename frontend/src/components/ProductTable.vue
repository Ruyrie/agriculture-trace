<template> 
  <el-table :data="data" border stripe> 
    <el-table-column prop="id" label="ID" width="80" /> 
    <el-table-column prop="name" label="产品名称" /> 
    <el-table-column prop="category" label="类别" /> 
    <el-table-column prop="origin" label="产地" /> 
    <el-table-column prop="price" label="价格(元/kg)" /> 
    <el-table-column label="操作" width="200"> 
      <template #default="{ row }"> 
        <el-button type="primary" link @click="emit('edit', row)">编辑</el-button> 
        <el-button type="danger" link @click="emit('delete', row)">删除</el-button> 
        <el-button type="info" link @click="emit('trace', row)">溯源</el-button> 
      </template> 
    </el-table-column> 
  </el-table> 
</template> 
 
<script setup> 
defineProps({ 
  data: { 
    type: Array, 
    default: () => [] 
  } 
}) 
const emit = defineEmits(['edit', 'delete', 'trace']) 
</script>
