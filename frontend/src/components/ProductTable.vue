<template> 
  <el-table :data="data" border stripe> 
    <el-table-column prop="id" label="ID" width="80" /> 
    <el-table-column prop="name" label="产品名称" /> 
    <el-table-column prop="category" label="类别" /> 
    <el-table-column prop="origin" label="产地" /> 
    <el-table-column prop="price" label="价格(元/kg)" /> 
    <el-table-column label="数据指纹" min-width="150">
      <template #default="{ row }">
        <el-tag v-if="row.dataHash" type="success" size="small">{{ shortHash(row.dataHash) }}</el-tag>
        <el-tag v-else type="warning" size="small">未生成</el-tag>
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
defineProps({ 
  data: { 
    type: Array, 
    default: () => [] 
  } 
}) 
const emit = defineEmits(['edit', 'delete', 'trace', 'verify'])
const shortHash = (hash) => `${hash.slice(0, 8)}...${hash.slice(-6)}`
</script>
