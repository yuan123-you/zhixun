<template>
  <!-- 网络状态检测与提示组件（全局浮动提示） -->
  <Transition name="slide-down">
    <div
      v-if="showOfflineBar"
      class="fixed top-0 left-0 right-0 z-[9998] flex items-center justify-center py-2 px-4 bg-amber-500 text-white text-sm font-medium shadow-md"
    >
      <svg class="w-4 h-4 mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 5.636a9 9 0 010 12.728m-3.536-3.536a4 4 0 010-5.656m-7.072 9.192a9 9 0 010-12.728m3.536 3.536a4 4 0 010 5.656" />
      </svg>
      <span>网络连接已断开，请检查网络设置</span>
    </div>
  </Transition>
</template>

<script setup lang="ts">
/**
 * 网络状态检测组件
 *
 * 使用方式：在 layouts/default.vue 中引入即可全局生效
 * - 网络断开时显示顶部提示条
 * - 网络恢复时自动隐藏并显示恢复提示
 */

const isOnline = ref(true)
const showOfflineBar = ref(false)
const isMounted = ref(false)
let wasOffline = false

const updateOnlineStatus = () => {
  const online = navigator.onLine
  isOnline.value = online

  if (!online) {
    showOfflineBar.value = true
    wasOffline = true
  } else if (wasOffline) {
    // 网络恢复，短暂显示恢复提示后隐藏
    showOfflineBar.value = false
    wasOffline = false
    showRestoreToast()
  }
}

const showRestoreToast = () => {
  if (!import.meta.client) return
  const toast = document.createElement('div')
  toast.className = 'fixed top-4 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl shadow-lg text-white text-sm font-medium transition-all duration-300 transform bg-green-500 flex items-center gap-2'
  toast.innerHTML = `
    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
    </svg>
    <span>网络已恢复</span>
  `
  document.body.appendChild(toast)

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translate(-50%, -20px)'
    setTimeout(() => toast.remove(), 300)
  }, 2000)
}

onMounted(() => {
  if (!import.meta.client) return
  isMounted.value = true
  isOnline.value = navigator.onLine
  // 如果客户端离网，立即显示提示条
  if (!isOnline.value) {
    showOfflineBar.value = true
    wasOffline = true
  }
  window.addEventListener('online', updateOnlineStatus)
  window.addEventListener('offline', updateOnlineStatus)
})

onUnmounted(() => {
  if (!import.meta.client) return
  window.removeEventListener('online', updateOnlineStatus)
  window.removeEventListener('offline', updateOnlineStatus)
})
</script>

<style scoped>
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}
.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
