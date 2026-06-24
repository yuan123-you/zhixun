<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center px-4">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-primary mb-4">{{ error?.statusCode || 404 }}</h1>
      <p class="text-xl text-gray-600 dark:text-gray-400 mb-8">{{ error?.message || $t('error.pageNotFound') }}</p>
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
  title: $t('error.pageNotFound') + ' - 知讯',
})
</script>
