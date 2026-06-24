<template>
  <!-- 错误状态与重试组件 -->
  <div class="flex flex-col items-center justify-center py-8 px-4">
    <!-- 错误图标 -->
    <div class="w-16 h-16 mb-4 text-gray-300 dark:text-gray-600">
      <svg class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.5"
          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
        />
      </svg>
    </div>

    <!-- 错误信息 -->
    <p class="text-sm text-gray-500 dark:text-gray-400 mb-4 text-center max-w-xs">
      {{ message || '加载失败，请稍后重试' }}
    </p>

    <!-- 重试按钮 -->
    <button
      class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors"
      :disabled="retrying"
      @click="handleRetry"
    >
      <svg
        class="w-4 h-4"
        :class="{ 'animate-spin': retrying }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
        />
      </svg>
      <span>{{ retrying ? '重试中...' : '点击重试' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
/** 错误状态与重试组件 */

const props = withDefaults(defineProps<{
  /** 错误信息 */
  message?: string
  /** 重试回调 */
  onRetry?: () => Promise<void> | void
}>(), {
  message: '加载失败，请稍后重试',
})

const retrying = ref(false)

const handleRetry = async () => {
  if (retrying.value || !props.onRetry) return
  retrying.value = true
  try {
    await props.onRetry()
  } finally {
    retrying.value = false
  }
}
</script>
