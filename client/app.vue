<template>
  <!-- 根组件：使用 NuxtLayout + NuxtPage 实现布局和页面路由 -->
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
// 页面切换时滚动到顶部
const nuxtApp = useNuxtApp()
nuxtApp.hook('page:finish', () => {
  if (import.meta.client) {
    window.scrollTo(0, 0)
  }
})
</script>
