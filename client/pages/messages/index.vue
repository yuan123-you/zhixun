<template>
  <div class="flex items-center justify-center h-[calc(100vh-8rem)]">
    <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: 'auth' })
// 重定向到消息中心（QQ风格统一页面）
onMounted(() => navigateTo('/notifications', { replace: true }))
</script>
