<template>
  <div class="min-h-screen bg-[var(--color-bg)] flex items-center justify-center px-4">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-primary mb-4">{{ error?.statusCode || 404 }}</h1>
      <p class="text-xl text-slate-600 mb-4">{{ friendlyMessage }}</p>
      <p v-if="error?.message !== friendlyMessage" class="text-sm text-slate-400 mb-6">{{ error?.message }}</p>
      <div class="flex items-center justify-center gap-3">
        <NuxtLink to="/" class="btn-primary inline-block">返回首页</NuxtLink>
        <button v-if="error?.statusCode !== 404" class="btn-secondary inline-block" @click="handleRetry">重新加载</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  error?: {
    statusCode?: number
    message?: string
  }
}>()

// 将 Nuxt 内部错误信息映射为用户友好提示
const friendlyMessage = computed(() => {
  const code = props.error?.statusCode
  const msg = props.error?.message || ''

  // 按状态码映射
  switch (code) {
    case 404: return '页面未找到'
    case 500: return '服务器繁忙，请稍后重试'
    case 502: return '服务暂时不可用'
    case 503: return '服务正在维护中'
  }

  // 捕获 Nuxt 内部错误，如 [nuxt] instance unavailable
  if (msg.includes('instance unavailable') || msg.includes('instanceunavailable')) {
    return '页面加载异常，请刷新后重试'
  }
  if (msg.includes('Component is missing template') || msg.includes('render')) {
    return '页面加载异常，请刷新后重试'
  }
  if (msg.includes('Failed to fetch') || msg.includes('Network')) {
    return '网络连接失败，请检查网络后重试'
  }
  if (msg.includes('timeout')) {
    return '请求超时，请稍后重试'
  }

  return msg || '页面加载失败，请稍后重试'
})

const handleRetry = () => {
  if (import.meta.client) {
    window.location.reload()
  }
}

useHead({
  title: computed(() => `${friendlyMessage.value} - 知讯`),
})
</script>
