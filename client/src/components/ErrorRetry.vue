<template>
  <!-- 错误状态 - 友好重试 -->
  <div class="error-state">
    <div class="error-illustration">
      <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="2" fill="none" opacity="0.15"/>
        <path d="M35 35l30 30M65 35l-30 30" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.4"/>
      </svg>
    </div>
    <h3 class="error-title">加载失败</h3>
    <p class="error-desc">{{ message || '请检查网络后重试' }}</p>
    <el-button type="primary" size="default" :loading="retrying" @click="handleRetry" round>
      {{ retrying ? '重试中...' : '点击重试' }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{ message?: string; onRetry?: () => Promise<unknown> | unknown }>(), {
  message: '加载失败，请稍后重试',
})

const retrying = ref(false)

const handleRetry = async () => {
  if (retrying.value || !props.onRetry) return
  retrying.value = true
  try { await props.onRetry() }
  finally { retrying.value = false }
}
</script>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.error-illustration {
  width: 100px;
  height: 100px;
  color: var(--zh-text-placeholder);
  margin-bottom: 16px;
  animation: fade-in-up 0.4s var(--zh-transition-base) both;
}
.error-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--zh-text);
  margin-bottom: 8px;
}
.error-desc {
  font-size: 14px;
  color: var(--zh-text-tertiary);
  margin-bottom: 24px;
  line-height: 1.6;
}
</style>
