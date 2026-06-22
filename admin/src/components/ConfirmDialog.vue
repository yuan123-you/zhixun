<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    @close="handleClose"
  >
    <p class="confirm-message">{{ message }}</p>
    <template #footer>
      <el-button @click="handleCancel">{{ cancelText }}</el-button>
      <el-button :type="confirmType" :loading="loading" @click="handleConfirm">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

/** 确认对话框组件 */

const props = withDefaults(
  defineProps<{
    /** 是否显示 */
    modelValue: boolean
    /** 标题 */
    title?: string
    /** 提示信息 */
    message?: string
    /** 确认按钮文本 */
    confirmText?: string
    /** 取消按钮文本 */
    cancelText?: string
    /** 确认按钮类型 */
    confirmType?: string
    /** 对话框宽度 */
    width?: string
    /** 加载状态 */
    loading?: boolean
  }>(),
  {
    title: '确认操作',
    message: '确定要执行此操作吗？',
    confirmText: '确定',
    cancelText: '取消',
    confirmType: 'primary',
    width: '420px',
    loading: false,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  }
)

function handleClose() {
  emit('update:modelValue', false)
}

function handleCancel() {
  emit('update:modelValue', false)
  emit('cancel')
}

function handleConfirm() {
  emit('confirm')
}
</script>

<style scoped lang="scss">
.confirm-message {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
</style>
