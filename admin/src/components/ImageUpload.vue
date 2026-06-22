<template>
  <div class="image-upload">
    <!-- 已上传图片预览 -->
    <div v-if="imageUrl" class="image-preview">
      <el-image :src="imageUrl" fit="cover" class="preview-img" />
      <div class="image-actions">
        <el-icon class="action-icon" @click="handlePreview"><ZoomIn /></el-icon>
        <el-icon class="action-icon" @click="handleRemove"><Delete /></el-icon>
      </div>
    </div>
    <!-- 上传按钮 -->
    <el-upload
      v-else
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      accept="image/*"
      class="upload-trigger"
    >
      <el-icon class="upload-icon"><Plus /></el-icon>
    </el-upload>
    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="600px">
      <el-image :src="imageUrl" fit="contain" style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/file'

/** 图片上传组件 */

const props = withDefaults(
  defineProps<{
    /** 图片URL（v-model） */
    modelValue?: string
    /** 最大文件大小（MB） */
    maxSize?: number
  }>(),
  {
    modelValue: '',
    maxSize: 5,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const imageUrl = ref(props.modelValue)
const previewVisible = ref(false)

watch(() => props.modelValue, (val) => {
  imageUrl.value = val
})

/** 上传前校验 */
function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

/** 自定义上传 */
async function handleUpload(options: { file: File }) {
  try {
    const res = await uploadImage(options.file)
    imageUrl.value = res.data.url
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
}

/** 预览图片 */
function handlePreview() {
  previewVisible.value = true
}

/** 移除图片 */
function handleRemove() {
  imageUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped lang="scss">
.image-upload {
  .image-preview {
    position: relative;
    width: 148px;
    height: 148px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    overflow: hidden;

    .preview-img {
      width: 100%;
      height: 100%;
    }

    .image-actions {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      background: rgba(0, 0, 0, 0.5);
      opacity: 0;
      transition: opacity 0.3s;

      .action-icon {
        font-size: 20px;
        color: #fff;
        cursor: pointer;
      }
    }

    &:hover .image-actions {
      opacity: 1;
    }
  }

  .upload-trigger {
    :deep(.el-upload) {
      width: 148px;
      height: 148px;
      border: 1px dashed #dcdfe6;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }

    .upload-icon {
      font-size: 28px;
      color: #8c939d;
    }
  }
}
</style>
