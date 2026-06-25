<template>
  <!-- 通知铃铛组件 - 仅客户端渲染，避免 hydration 不匹配 -->
  <ClientOnly>
    <div class="relative" ref="bellRef">
      <!-- 铃铛按钮 -->
      <button
        class="relative p-2 text-slate-600 hover:bg-slate-50 rounded-lg transition-colors"
        @click="togglePanel"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <!-- 未读数量徽章 -->
        <span
          v-if="notificationStore.unreadCount > 0"
          class="absolute -top-0.5 -right-0.5 min-w-[1rem] h-4 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-1"
        >
          {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
        </span>
      </button>

    <!-- 通知下拉面板 -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 scale-95 -translate-y-1"
      enter-to-class="opacity-100 scale-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 scale-100 translate-y-0"
      leave-to-class="opacity-0 scale-95 -translate-y-1"
    >
      <div
        v-if="showPanel"
        class="absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white rounded-lg shadow-xl border border-slate-200/60 z-50 overflow-hidden"
      >
        <!-- 面板头部 -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-slate-200/60">
          <h3 class="text-sm font-semibold text-slate-900">通知</h3>
          <button
            v-if="notificationStore.unreadCount > 0"
            class="text-xs text-primary hover:text-primary-600 transition-colors"
            @click="markAllRead"
          >
            全部已读
          </button>
        </div>

        <!-- 通知列表 -->
        <div class="max-h-80 overflow-y-auto">
          <!-- 加载状态 -->
          <div v-if="loading" class="flex items-center justify-center py-8">
            <div class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="notifications.length === 0" class="py-8 text-center text-slate-400">
            <svg class="w-10 h-10 mx-auto mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <p class="text-sm">暂无通知</p>
          </div>

          <!-- 通知项 -->
          <div v-else>
            <div
              v-for="notification in notifications"
              :key="notification.id"
              class="flex items-start px-4 py-3 hover:bg-slate-50 transition-colors cursor-pointer"
              :class="{ 'bg-primary-50/50': !notification.isRead }"
              @click="handleNotificationClick(notification)"
            >
              <!-- 通知类型图标 -->
              <div class="shrink-0 mr-3 mt-0.5">
                <!-- 系统通知 -->
                <div v-if="notification.type === NotificationType.System" class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <!-- 审核通知 -->
                <div v-else-if="notification.type === NotificationType.Audit" class="w-8 h-8 rounded-full bg-yellow-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <!-- 互动通知（点赞/评论/收藏） -->
                <div v-else-if="notification.type === NotificationType.Interact" class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </div>
                <!-- 关注 -->
                <div v-else-if="notification.type === NotificationType.Follow" class="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                  </svg>
                </div>
                <!-- 私信 -->
                <div v-else-if="notification.type === NotificationType.Message" class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <!-- 评论回复 -->
                <div v-else-if="notification.type === NotificationType.CommentReply" class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                  </svg>
                </div>
                <!-- @提及 -->
                <div v-else-if="notification.type === NotificationType.Mention" class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center">
                  <svg class="w-4 h-4 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
                  </svg>
                </div>
              </div>

              <!-- 通知内容 -->
              <div class="flex-1 min-w-0">
                <p class="text-sm text-slate-900 leading-snug">
                  {{ notification.content || notification.title }}
                </p>
                <p class="text-xs text-slate-400 mt-1">{{ formatTime(notification.createdAt) }}</p>
              </div>

              <!-- 未读标记 -->
              <span v-if="!notification.isRead" class="shrink-0 ml-2 w-2 h-2 bg-primary rounded-full mt-1.5"></span>
            </div>
          </div>
        </div>

        <!-- 面板底部 -->
        <div v-if="notifications.length > 0" class="border-t border-slate-200/60">
          <NuxtLink
            to="/notifications"
            class="block text-center py-2.5 text-sm text-primary hover:text-primary-600 transition-colors"
            @click="showPanel = false"
          >
            查看全部通知
          </NuxtLink>
        </div>
      </div>
    </Transition>
  </div>
  </ClientOnly>
</template>

<script setup lang="ts">
/** 通知铃铛组件 */
import type { Notification } from '~/types'
import { NotificationType } from '~/types'

const notificationStore = useNotificationStore()
const userStore = useUserStore()

const showPanel = ref(false)
const loading = ref(false)
const notifications = ref<Notification[]>([])
const bellRef = ref<HTMLElement | null>(null)

// 切换面板显示
const togglePanel = async () => {
  showPanel.value = !showPanel.value
  if (showPanel.value) {
    await fetchNotifications()
  }
}

// 获取通知列表
const fetchNotifications = async () => {
  loading.value = true
  try {
    const { notificationApi } = await import('~/api/notification')
    const response = await notificationApi.getNotifications({ page: 1, pageSize: 10 })
    const data = response.data.data
    notifications.value = data?.items || data?.list || []
  } catch {
    // 忽略错误
  } finally {
    loading.value = false
  }
}

// 点击单条通知 - 标记已读
const handleNotificationClick = async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      const { notificationApi } = await import('~/api/notification')
      await notificationApi.markAsRead(notification.id)
      notificationStore.markAsRead(notification.id)
      notification.isRead = true
    } catch {
      // 忽略错误
    }
  }
}

// 全部标记已读
const markAllRead = async () => {
  try {
    const { notificationApi } = await import('~/api/notification')
    await notificationApi.markAllAsRead()
    notificationStore.markAllAsRead()
    notifications.value.forEach(n => { n.isRead = true })
  } catch {
    // 忽略错误
  }
}

// 格式化时间
const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 点击外部关闭面板
const handleClickOutside = (event: MouseEvent) => {
  if (bellRef.value && !bellRef.value.contains(event.target as Node)) {
    showPanel.value = false
  }
}

// 初始化未读数量
onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const { notificationApi } = await import('~/api/notification')
      const response = await notificationApi.getUnreadCount()
      // 后端返回字段名为 unread_count，对齐 NotificationController
      notificationStore.setUnreadCount(response.data.data?.unread_count ?? 0)
    } catch {
      // 忽略错误
    }
    document.addEventListener('click', handleClickOutside)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
