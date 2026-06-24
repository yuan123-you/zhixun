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

<style>
/* 应用加载画面 - 在JS水合前通过CSS显示，水合后由Vue接管 */
.app-loading-screen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.dark .app-loading-screen {
  background: #111827;
}

.app-loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: app-spin 0.8s linear infinite;
}

.dark .app-loading-spinner {
  border-color: #374151;
  border-top-color: #6366f1;
}

.app-loading-text {
  margin-top: 16px;
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.dark .app-loading-text {
  color: #9ca3af;
}

@keyframes app-spin {
  to { transform: rotate(360deg); }
}
</style>
