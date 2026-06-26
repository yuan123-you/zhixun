<template>
  <!--
    根组件：使用 NuxtLayout + NuxtPage 实现布局和页面路由
    pre-hydration 类由 server/plugins/pre-hydration.ts 在 SSR 阶段直接写入 <html> 标签，
    客户端水合完成且布局稳定后，由 plugins/hydration.client.ts 移除。
    挂到 <html>（而非根 div）的关键原因：根 div 的 .pre-hydration * 选择器无法覆盖
    <Teleport to="body"> 渲染到 body 外层的节点（汉堡菜单、分享面板等），
    而 Teleport 内容若带 transition，水合时同样会引发布局错乱。
  -->
  <div>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
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
