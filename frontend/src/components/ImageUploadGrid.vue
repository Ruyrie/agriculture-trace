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
/**
 * ImageUploadGrid.vue — 图片上传网格组件（v-model 绑定）。
 *
 * 以 el-upload picture-card 样式展示图片列表和"+"添加按钮。
 * 支持通过 v-model（modelValue/update:modelValue）双向绑定一个相对路径 URL 数组。
 * 上传成功后将新 URL 追加到已有列表；删除时从列表中移除对应项。
 * 点击图片缩略图触发全屏预览对话框。
 *
 * 关联：
 *   - api/upload.js（uploadTraceImage 执行实际文件上传）
 *   - utils/images.js（parseImageUrls 解析 URL 数组，resolveImageUrl 构建访问地址）
 *   - BatchList.vue、ProductFormDialog.vue（使用方，通过 v-model:imageUrls 绑定）
 *
 * Props:
 *   modelValue — 相对路径 URL 数组（如 ["/uploads/abc.jpg"]）
 *   limit      — 最大图片数量（默认 9）
 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadTraceImage } from '@/api/upload'
import { parseImageUrls, resolveImageUrl } from '@/utils/images'

// modelValue：外部绑定的图片 URL 数组（相对路径），通过 emit('update:modelValue') 更新。
// limit：最大图片数量，超出时触发 handleExceed 提示并阻止上传。
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 9 }
})
// update:modelValue：每次增删图片后向父组件同步最新 URL 数组。
const emit = defineEmits(['update:modelValue'])

// el-upload 的 accept 属性，限制只能选择图片文件（浏览器文件选择器层过滤，不替代 beforeUpload 校验）。
const accept = 'image/png,image/jpeg,image/jpg,image/gif,image/webp'
// 图片全屏预览弹窗是否可见。
const previewVisible = ref(false)
// 当前预览图片的完整可访问 URL，由 handlePreview 赋值。
const previewUrl = ref('')

// 将 modelValue（相对路径数组）转为 el-upload 需要的 { name, url, rawUrl } 格式列表。
// rawUrl 保存原始相对路径，用于 handleRemove 时过滤匹配。
const fileList = computed(() => parseImageUrls(props.modelValue).map((url, index) => ({
  name: `image-${index + 1}`,
  url: resolveImageUrl(url),  // 转为浏览器可访问的完整 URL
  rawUrl: url                 // 原始相对路径，用于删除时比对
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
