<template>
  <div class="min-h-dvh-screen flex items-center justify-center bg-[var(--color-bg)]">
    <p class="text-[var(--zh-text-secondary)]">正在跳转到管理后台...</p>
  </div>
</template>

<script setup lang="ts">
// 管理后台：在 Nuxt 开发模式下重定向到独立运行的 admin 开发服务器
// 生产环境中由 Nginx 统一代理，此路由不会被访问
if (true) {
  const adminPort = 3001
  const path = window.location.pathname + window.location.search + window.location.hash
  window.location.href = `http://localhost:${adminPort}${path}`
}
</script>
