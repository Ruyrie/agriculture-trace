<template>
  <el-popover
    v-if="hash"
    placement="top"
    :width="460"
    trigger="click"
    popper-class="hash-tag-popover"
  >
    <template #reference>
      <el-tag :type="type" size="small" class="hash-tag-trigger" title="点击查看完整数据指纹">
        {{ shortHash }}
      </el-tag>
    </template>
    <div class="hash-tag-content">
      <div class="hash-tag-label">完整数据指纹（SHA-256）</div>
      <div class="hash-tag-full">{{ hash }}</div>
      <div class="hash-tag-actions">
        <el-button type="primary" size="small" :icon="CopyDocument" @click="copy">复制</el-button>
      </div>
    </div>
  </el-popover>
  <el-tag v-else type="warning" size="small">未生成</el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

// 列表里数据指纹只展示首尾缩略，点击后用 popover 展示完整哈希并提供复制按钮，
// 避免用户为了拿到完整指纹去翻接口或手动拼接。
const props = defineProps({
  hash: { type: String, default: '' },
  type: { type: String, default: 'success' }
})

const shortHash = computed(() =>
  props.hash ? `${props.hash.slice(0, 8)}...${props.hash.slice(-6)}` : ''
)

const copy = async () => {
  try {
    await navigator.clipboard.writeText(props.hash)
    ElMessage.success('已复制完整数据指纹')
  } catch {
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}
</script>

<style scoped>
.hash-tag-trigger {
  cursor: pointer;
}

.hash-tag-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hash-tag-label {
  font-size: 12px;
  color: #909399;
}

.hash-tag-full {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: #235c2f;
  line-height: 1.5;
  word-break: break-all;
}

.hash-tag-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
