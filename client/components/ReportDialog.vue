<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="$emit('close')">
      <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
        <h3 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">举报{{ type === 'article' ? '内容' : '用户' }}</h3>
        <div class="space-y-3 mb-4">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">举报原因</label>
          <div class="space-y-2">
            <button v-for="reason in reasons" :key="reason" @click="selectedReason = reason"
              :class="selectedReason === reason ? 'bg-blue-100 dark:bg-blue-900/30 border-blue-500 text-blue-700 dark:text-blue-300' : 'border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400'"
              class="w-full text-left px-3 py-2 rounded-lg border text-sm transition hover:bg-gray-50 dark:hover:bg-gray-700">{{ reason }}</button>
          </div>
        </div>
        <textarea v-model="description" placeholder="补充说明（选填）" maxlength="500" rows="2"
          class="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-sm mb-4 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"></textarea>
        <div class="flex justify-end gap-3">
          <button @click="$emit('close')" class="px-4 py-2 text-gray-600 dark:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-sm">取消</button>
          <button @click="doReport" :disabled="!selectedReason || submitting"
            class="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 text-sm transition">{{ submitting ? '提交中...' : '提交举报' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { reportApi } from '~/api/report'

const props = defineProps<{
  visible: boolean
  type: 'article' | 'user'
  targetId: number
}>()

const emit = defineEmits(['close', 'reported'])

const reasons = props.type === 'article'
  ? ['色情低俗', '政治敏感', '违法违规', '虚假信息', '侵权内容', '垃圾广告', '其他']
  : ['骚扰谩骂', '冒充他人', '发布违法信息', '恶意刷屏', '其他']

const selectedReason = ref('')
const description = ref('')
const submitting = ref(false)

async function doReport() {
  if (!selectedReason.value) return
  submitting.value = true
  try {
    if (props.type === 'article') {
      await reportApi.reportArticle(props.targetId, selectedReason.value, description.value)
    } else {
      await reportApi.reportUser(props.targetId, selectedReason.value, description.value)
    }
    emit('reported')
    emit('close')
  } catch (e) { /* ignore */ }
  submitting.value = false
}
</script>
