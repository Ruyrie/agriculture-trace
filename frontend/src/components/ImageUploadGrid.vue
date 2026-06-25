<template>
  <div class="image-upload-grid">
    <el-upload
      action="#"
      list-type="picture-card"
      :file-list="fileList"
      :limit="limit"
      :accept="accept"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      :on-remove="handleRemove"
      :on-exceed="handleExceed"
      :on-preview="handlePreview"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
    <el-dialog v-model="previewVisible" width="min(720px, calc(100vw - 32px))" append-to-body>
      <img :src="previewUrl" class="preview-image" alt="图片预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadTraceImage } from '@/api/upload'
import { parseImageUrls, resolveImageUrl } from '@/utils/images'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 9 }
})
const emit = defineEmits(['update:modelValue'])

const accept = 'image/png,image/jpeg,image/jpg,image/gif,image/webp'
const previewVisible = ref(false)
const previewUrl = ref('')

const fileList = computed(() => parseImageUrls(props.modelValue).map((url, index) => ({
  name: `image-${index + 1}`,
  url: resolveImageUrl(url),
  rawUrl: url
})))

const updateUrls = (urls) => {
  emit('update:modelValue', parseImageUrls(urls))
}

const beforeUpload = (file) => {
  if (parseImageUrls(props.modelValue).length >= props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return false
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 50) {
    ElMessage.error('单张图片不能超过 50MB')
    return false
  }
  return true
}

const handleUpload = async ({ file, onSuccess, onError }) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadTraceImage(formData)
    if (res.code === 200) {
      updateUrls([...parseImageUrls(props.modelValue), res.data.url])
      onSuccess(res)
    } else {
      ElMessage.error(res.message || '图片上传失败')
      onError(new Error(res.message || 'upload failed'))
    }
  } catch (error) {
    onError(error)
  }
}

const handleRemove = (file) => {
  const url = file.rawUrl || file.response?.data?.url || file.url
  updateUrls(parseImageUrls(props.modelValue).filter(item => resolveImageUrl(item) !== url && item !== url))
}

const handleExceed = () => {
  ElMessage.warning(`最多上传 ${props.limit} 张图片`)
}

const handlePreview = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}
</script>

<style scoped>
.image-upload-grid {
  width: 100%;
}

.image-upload-grid :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
}

.image-upload-grid :deep(.el-upload--picture-card),
.image-upload-grid :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 96px;
  height: 72px;
  margin: 0;
  border-radius: 8px;
}

.image-upload-grid :deep(.el-upload--picture-card) {
  border-style: dashed;
  background: #fbfcfe;
  color: #8b95a1;
}

.image-upload-grid :deep(.el-upload-list--picture-card .el-upload-list__item-thumbnail) {
  object-fit: cover;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
</style>
