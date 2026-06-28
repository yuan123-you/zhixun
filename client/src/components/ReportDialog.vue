<template>
  <!-- 举报弹窗（Element Plus 风格） -->
  <el-dialog
    :model-value="visible"
    :title="type === 'article' ? '举报内容' : '举报用户'"
    width="400px"
    :close-on-click-modal="true"
    @close="$emit('close')"
  >
    <div class="report-body">
      <el-radio-group v-model="selectedReason" class="report-reasons">
        <el-radio v-for="reason in reasons" :key="reason" :value="reason" class="report-reason-radio">
          {{ reason }}
        </el-radio>
      </el-radio-group>
      <el-input
        v-model="description"
        type="textarea"
        :rows="2"
        placeholder="补充说明（选填）"
        maxlength="500"
        show-word-limit
        class="report-desc"
      />
    </div>
    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="danger" :disabled="!selectedReason || submitting" :loading="submitting" @click="doReport">
        {{ submitting ? '提交中...' : '提交举报' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reportApi } from '@/api/report'

const props = withDefaults(defineProps<{ visible: boolean; type: 'article' | 'user'; targetId: number }>(), {
  visible: false,
  targetId: 0,
})
const emit = defineEmits(['close', 'reported'])

const userStore = useUserStore()
const router = useRouter()

const reasons = props.type === 'article'
  ? ['色情低俗', '政治敏感', '违法违规', '虚假信息', '侵权内容', '垃圾广告', '其他']
  : ['骚扰谩骂', '冒充他人', '发布违法信息', '恶意刷屏', '其他']

const selectedReason = ref('')
const description = ref('')
const submitting = ref(false)

async function doReport() {
  if (!selectedReason.value) return
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  submitting.value = true
  try {
    if (props.type === 'article')
      await reportApi.reportArticle(props.targetId, selectedReason.value, description.value)
    else
      await reportApi.reportUser(props.targetId, selectedReason.value, description.value)
    emit('reported')
    emit('close')
  } catch (e) { /* ignore */ }
  submitting.value = false
}
</script>

<style scoped>
.report-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.report-reasons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.report-reason-radio {
  margin-right: 0;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  transition: border-color 0.2s;
}
.report-reason-radio:hover {
  border-color: var(--el-color-primary);
}
.report-desc {
  margin-top: 4px;
}
</style>
