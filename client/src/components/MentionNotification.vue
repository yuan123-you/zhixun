<template>
  <!-- @提及通知弹窗 - QQ风格滑入通知 -->
  <Teleport to="body">
    <div class="mention-toast-container">
      <Transition name="mention-toast">
        <div
          v-if="currentNotification"
          class="mention-toast"
          @click="handleClick"
        >
          <div class="mention-toast-icon">
            <img
              v-if="currentNotification.sender?.avatar"
              :src="currentNotification.sender.avatar"
              class="mention-toast-avatar"
              alt=""
            />
            <div v-else class="mention-toast-avatar-placeholder">
              @
            </div>
          </div>
          <div class="mention-toast-body">
            <p class="mention-toast-title">{{ currentNotification.title }}</p>
            <p class="mention-toast-content">{{ currentNotification.content }}</p>
          </div>
          <button class="mention-toast-close" @click.stop="dismiss">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </Transition>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * @提及通知弹窗组件
 *
 * 监听通知 store，当收到 MENTION 类型通知时弹出滑入式提示。
 * 3 秒后自动消失，点击跳转到通知页面。
 */
import { ref, watch, onUnmounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { NotificationType } from '@/types'

const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const currentNotification = ref<any>(null)
let dismissTimer: ReturnType<typeof setTimeout> | null = null

// 记录已展示过的通知数量，避免初始加载时弹出旧通知
let seenCount = notificationStore.notifications.length

watch(
  () => notificationStore.notifications.length,
  (newLen) => {
    // 仅当新通知到来（长度增加）且超过已见数量时才弹出
    if (newLen > seenCount) {
      const latest = notificationStore.notifications[0]
      if (latest && latest.type === NotificationType.Mention) {
        show(latest)
      }
      seenCount = newLen
    }
  }
)

function show(notification: any) {
  if (dismissTimer) clearTimeout(dismissTimer)
  currentNotification.value = notification
  dismissTimer = setTimeout(() => {
    currentNotification.value = null
  }, 3500)
}

function dismiss() {
  if (dismissTimer) clearTimeout(dismissTimer)
  currentNotification.value = null
}

function handleClick() {
  dismiss()
  // 如果已在通知页则不跳转
  if (route.path === '/notifications') return
  router.push('/notifications')
}

onUnmounted(() => {
  if (dismissTimer) clearTimeout(dismissTimer)
})
</script>

<style scoped>
.mention-toast-container {
  position: fixed;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  pointer-events: none;
}

.mention-toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-primary, #6366f1);
  border-left: 3px solid var(--zh-primary, #6366f1);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.15), 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  pointer-events: auto;
  max-width: 380px;
  min-width: 260px;
}

.mention-toast:hover {
  box-shadow: 0 6px 28px rgba(99, 102, 241, 0.2), 0 2px 6px rgba(0, 0, 0, 0.08);
}

.mention-toast-icon {
  flex-shrink: 0;
}

.mention-toast-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.mention-toast-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.mention-toast-body {
  flex: 1;
  min-width: 0;
}

.mention-toast-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--zh-text, #1e293b);
  margin: 0 0 2px;
  line-height: 1.4;
}

.mention-toast-content {
  font-size: 12px;
  color: var(--zh-text-secondary, #64748b);
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mention-toast-close {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s, color 0.15s;
  min-height: 0;
  min-width: 0;
}

.mention-toast-close:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}

/* 滑入/滑出动画 */
.mention-toast-enter-active {
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease;
}
.mention-toast-leave-active {
  transition: transform 0.25s ease, opacity 0.2s ease;
}
.mention-toast-enter-from {
  transform: translateY(-20px);
  opacity: 0;
}
.mention-toast-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>
