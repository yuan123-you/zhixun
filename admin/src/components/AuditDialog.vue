<template>
  <el-dialog v-model="visible" title="作品审核" width="500px" @close="handleClose">
    <p class="audit-tip">请选择审核结果：</p>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="审核结果" prop="action">
        <el-radio-group v-model="form.action">
          <el-radio value="approve">通过</el-radio>
          <el-radio value="reject">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.action === 'reject'" label="驳回原因" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          placeholder="请输入驳回原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        :type="form.action === 'approve' ? 'success' : 'danger'"
        :loading="loading"
        @click="handleSubmit"
      >
        {{ form.action === 'approve' ? '通过' : '驳回' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { auditArticle } from '@/api/article'

/** 审核对话框组件 */

const props = defineProps<{
  /** 是否显示 */
  modelValue: boolean
  /** 作品ID */
  articleId?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'success'): void
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  action: 'approve' as 'approve' | 'reject',
  reason: '',
})

const rules: FormRules = {
  action: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }],
}

watch(() => props.modelValue, (val) => {
  visible.value = val
})

/** 关闭对话框 */
function handleClose() {
  form.action = 'approve'
  form.reason = ''
  emit('update:modelValue', false)
}

/** 提交审核 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!props.articleId) return

  loading.value = true
  try {
    await auditArticle({
      id: props.articleId,
      action: form.action,
      reason: form.action === 'reject' ? form.reason : undefined,
    })
    ElMessage.success(form.action === 'approve' ? '审核通过' : '已驳回')
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.audit-tip {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}
</style>
