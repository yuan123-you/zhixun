<template>
  <!-- 空白布局：登录/注册页使用，无导航栏和底部Tab -->
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col items-center justify-center relative">
    <!-- 返回按钮 -->
    <button class="absolute top-4 left-4 p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors z-10" @click="goBack">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
