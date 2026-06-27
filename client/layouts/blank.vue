<template>
  <!-- 空白布局：登录/注册页使用，无导航栏和底部Tab -->
  <div class="min-h-screen bg-[var(--color-bg)] flex flex-col items-center justify-center relative">
    <!-- 返回按钮 -->
    <button class="absolute top-3 left-3 p-1.5 text-slate-600 hover:bg-slate-50 rounded-lg transition-colors z-10" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>
    <slot />
  </div>
</template>

<script setup lang="ts">
const router = useRouter()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}
</script>
