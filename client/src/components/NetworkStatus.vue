<template>
  <!-- 网络状态检测组件 -->
  <Transition name="slide-down">
    <div v-if="showOfflineBar" class="offline-bar">
      <el-icon :size="16"><Warning /></el-icon>
      <span>网络连接已断开，请检查网络设置</span>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue'

const isOnline = ref(true)
const showOfflineBar = ref(false)
let wasOffline = false

const updateOnlineStatus = () => {
  const online = navigator.onLine
  isOnline.value = online
  if (!online) { showOfflineBar.value = true; wasOffline = true }
  else if (wasOffline) { showOfflineBar.value = false; wasOffline = false; showRestoreToast() }
}

const showRestoreToast = () => {
  const toast = document.createElement('div')
  Object.assign(toast.style, {
    position: 'fixed', top: '16px', left: '50%', transform: 'translateX(-50%)', zIndex: '9999',
    padding: '12px 20px', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    background: '#67C23A', color: '#fff', fontSize: '14px', fontWeight: '500',
    display: 'flex', alignItems: 'center', gap: '8px', transition: 'opacity 0.3s, transform 0.3s'
  })
  toast.innerHTML = '<span>✓</span><span>网络已恢复</span>'
  document.body.appendChild(toast)
  setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translate(-50%, -20px)'; setTimeout(() => toast.remove(), 300) }, 2000)
}

onMounted(() => {
  isOnline.value = navigator.onLine
  if (!isOnline.value) { showOfflineBar.value = true; wasOffline = true }
  window.addEventListener('online', updateOnlineStatus)
  window.addEventListener('offline', updateOnlineStatus)
})

onUnmounted(() => {
  window.removeEventListener('online', updateOnlineStatus)
  window.removeEventListener('offline', updateOnlineStatus)
})
</script>

<style scoped>
.offline-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9998;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px;
  background: #E6A23C;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}
.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from,
.slide-down-leave-to { transform: translateY(-100%); opacity: 0; }
</style>
