<template>
  <div class="min-h-screen bg-[var(--color-bg)] flex items-center justify-center px-4">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-primary mb-4">{{ error?.statusCode || 404 }}</h1>
      <p class="text-xl text-slate-600 mb-8">{{ error?.message || '页面未找到' }}</p>
      <NuxtLink to="/" class="btn-primary inline-block">返回首页</NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  error?: {
    statusCode?: number
    message?: string
  }
}>()

useHead({
  title: '页面未找到 - 知讯',
})
</script>
