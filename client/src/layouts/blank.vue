<template>
  <!-- 空白布局：登录/注册页使用，无导航栏和底部Tab -->
  <div class="min-h-dvh bg-[var(--zh-bg)] flex flex-col items-center justify-center px-2 py-3 sm:p-3 relative">
    <!-- 返回按钮 -->
    <button class="absolute top-3 left-3 p-1.5 text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] rounded-lg transition-colors z-10" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>
    <!-- 同样使用 <Transition> 包裹 router-view 以实现登录/注册/找回密码间的平滑切换，无白屏间隙。
         使用 :key="r.path" 避免 query/hash 变化时重载页面（与 default.vue 一致的修复）。 -->
    <router-view v-slot="{ Component, route: r }">
      <Transition name="page-fade">
        <component :is="Component" :key="r.path" />
      </Transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
const router = useRouter()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>
