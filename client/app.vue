<template>
  <!-- 根组件：使用 NuxtLayout + NuxtPage 实现布局和页面路由 -->
  <div :class="{ 'pre-hydration': !isHydrated }">
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
const isHydrated = ref(true)
if (import.meta.client) {
  isHydrated.value = false
  onMounted(() => {
    // 等待一帧确保布局计算完成后再启用过渡
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        isHydrated.value = true
      })
    })
  })
}
</script>
