<template>
  <!-- 根组件：使用 NuxtLayout + NuxtPage 实现布局和页面路由 -->
  <!-- pre-hydration 直接写在 class 中，避免 ref 导致 SSR/客户端不一致 -->
  <div ref="rootEl" class="pre-hydration">
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

// 水合完成后移除 pre-hydration 类，恢复正常过渡效果
const rootEl = ref<HTMLElement | null>(null)
onMounted(() => {
  nextTick(() => {
    requestAnimationFrame(() => {
      rootEl.value?.classList.remove('pre-hydration')
    })
  })
})
</script>
