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
/**
 * HashTag.vue — 数据指纹标签组件。
 *
 * 在 BatchList.vue 和 IntegrityReport.vue 的表格列中内联使用。
 * 列表里只展示首 8 + 末 6 的缩略形式（shortHash）；点击后通过 el-popover
 * 弹出完整 SHA-256（64 位十六进制）并提供"复制"按钮，
 * 避免用户为核对完整指纹而去翻接口或拼接字符串。
 * hash 为空时降级展示"未生成"警告标签。
 *
 * Props:
 *   hash — 完整 SHA-256 指纹字符串（64 字符）
 *   type — el-tag type 颜色值（默认 'success'，异常时外部传 'danger'）
 */
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

// hash：完整 SHA-256 指纹，来自后端 product.dataHash 或 batch.dataHash 字段。
// type：el-tag 颜色类型，由外部根据 valid 字段动态传入（'success'/'danger'）。
const props = defineProps({
  hash: { type: String, default: '' },
  type: { type: String, default: 'success' }
})

const shortHash = computed(() =>
  props.hash ? `${props.hash.slice(0, 8)}...${props.hash.slice(-6)}` : ''
)

// 复制完整 SHA-256 指纹到剪贴板，便于用户核对或留存证据。
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
