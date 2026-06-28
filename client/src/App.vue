<template>
  <div v-if="appReady">
    <!-- 路由切换加载进度条 -->
    <div v-if="isNavigating" class="fixed top-0 left-0 right-0 z-[10000] h-0.5 bg-[var(--zh-primary)] animate-pulse">
      <div class="h-full bg-[var(--zh-primary)] animate-[splash-shimmer_1.4s_ease-in-out_infinite]"></div>
    </div>
    <router-view />
  </div>
  <div v-else class="flex items-center justify-center min-h-screen bg-[var(--zh-bg)]">
    <div class="text-gray-400 text-sm">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { initApiPlugin } from '@/plugins/api'

const router = useRouter()
const appReady = ref(false)
const isNavigating = ref(false)

// 初始化认证插件：从 localStorage 恢复 token + 注册自动刷新
initApiPlugin()

// 等路由就绪后再显示页面，避免闪烁
router.isReady().then(() => {
  appReady.value = true
})

// 监听路由导航，显示加载进度条
router.beforeEach((_to, _from, next) => {
  isNavigating.value = true
  next()
})
router.afterEach(() => {
  // 延迟一点隐藏，确保新页面已经开始渲染
  setTimeout(() => {
    isNavigating.value = false
  }, 300)
})
</script>

<style>
/* ============================================================
   页面切换过渡（router-view 顶层）
   使用 CSS absolute 定位确保新旧页面重叠，避免 out-in 模式的白屏间隙。
   实际生效位置：layouts/default.vue 与 layouts/blank.vue 中
   用 <router-view v-slot> + <Transition> 包裹 <component> 的结构。
   这里定义全局 CSS（不带 scoped），确保 transition name="page-fade" 在任意位置使用都能命中。
   ============================================================ */
.page-fade-enter-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
  position: absolute;
  width: 100%;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 兼容旧 class 名称（page-），避免删除后产生级联问题 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease;
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
}
</style>
