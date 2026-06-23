<template>
  <!-- 通知页面 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 页面头部 -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">通知中心</h1>
      <div class="flex items-center gap-3">
        <!-- 批量操作按钮 -->
        <template v-if="batchMode">
          <button
            class="btn btn-sm btn-ghost text-gray-600 dark:text-gray-300"
            @click="exitBatchMode"
          >
            取消
          </button>
          <button
            class="btn btn-sm btn-ghost text-primary"
            :disabled="selectedIds.length === 0"
            @click="batchRead"
          >
            标记已读({{ selectedIds.length }})
          </button>
          <button
            class="btn btn-sm btn-ghost text-danger"
            :disabled="selectedIds.length === 0"
            @click="batchDelete"
          >
            删除({{ selectedIds.length }})
          </button>
        </template>
        <template v-else>
          <button
            v-if="notifications.length > 0"
            class="btn btn-sm btn-ghost text-gray-600 dark:text-gray-300"
            @click="enterBatchMode"
          >
            批量管理
          </button>
          <button
            v-if="notificationStore.unreadCount > 0"
            class="btn btn-sm btn-primary"
            @click="markAllRead"
          >
            全部已读
          </button>
        </template>
      </div>
    </div>

    <!-- 类型筛选 Tab -->
    <div class="flex items-center gap-1 mb-6 overflow-x-auto pb-2 border-b border-gray-200 dark:border-gray-700">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="shrink-0 px-4 py-2 text-sm font-medium rounded-t-lg transition-colors whitespace-nowrap"
        :class="[
          activeTab === tab.value
            ? 'text-primary border-b-2 border-primary bg-primary-50/50 dark:bg-primary-900/10'
            : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
        ]"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 通知列表 -->
    <div class="card">
      <!-- 加载状态 -->
      <div v-if="loading && notifications.length === 0" class="flex items-center justify-center py-16">
        <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="notifications.length === 0"
        title="暂无通知"
        description="还没有收到任何通知"
      />

      <!-- 通知列表 -->
      <div v-else>
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="flex items-start px-4 sm:px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors group border-b border-gray-100 dark:border-gray-700/50 last:border-b-0"
          :class="{ 'bg-primary-50/40 dark:bg-primary-900/5': !notification.isRead }"
        >
          <!-- 批量选择复选框 -->
          <div v-if="batchMode" class="shrink-0 mr-3 mt-1">
            <input
              type="checkbox"
              :checked="selectedIds.includes(notification.id)"
              class="w-4 h-4 rounded border-gray-300 dark:border-gray-600 text-primary focus:ring-primary"
              @change="toggleSelect(notification.id)"
            />
          </div>

          <!-- 通知类型图标 -->
          <div class="shrink-0 mr-3 mt-0.5">
            <!-- 系统通知 -->
            <div v-if="notification.type === NotificationType.System" class="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <!-- 审核通知 -->
            <div v-else-if="notification.type === NotificationType.Audit" class="w-10 h-10 rounded-full bg-yellow-100 dark:bg-yellow-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-yellow-600 dark:text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <!-- 互动通知 -->
            <div v-else-if="notification.type === NotificationType.Interact" class="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
            <!-- 关注 -->
            <div v-else-if="notification.type === NotificationType.Follow" class="w-10 h-10 rounded-full bg-purple-100 dark:bg-purple-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-purple-600 dark:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
            </div>
            <!-- 私信 -->
            <div v-else-if="notification.type === NotificationType.Message" class="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <!-- 评论回复 -->
            <div v-else-if="notification.type === NotificationType.CommentReply" class="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
              </svg>
            </div>
            <!-- @提及 -->
            <div v-else-if="notification.type === NotificationType.Mention" class="w-10 h-10 rounded-full bg-orange-100 dark:bg-orange-900/40 flex items-center justify-center">
              <svg class="w-5 h-5 text-orange-600 dark:text-orange-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
              </svg>
            </div>
          </div>

          <!-- 通知内容 -->
          <div
            class="flex-1 min-w-0 cursor-pointer"
            @click="handleClick(notification)"
          >
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0 flex-1">
                <p
                  class="text-sm leading-snug"
                  :class="notification.isRead ? 'text-gray-700 dark:text-gray-300' : 'text-gray-900 dark:text-white font-medium'"
                >
                  {{ notification.title || notification.content }}
                </p>
                <p v-if="notification.title && notification.content" class="text-sm text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
                  {{ notification.content }}
                </p>
              </div>
              <!-- 未读标记 -->
              <span v-if="!notification.isRead" class="shrink-0 w-2 h-2 bg-primary rounded-full mt-2"></span>
            </div>
            <div class="flex items-center gap-3 mt-2">
              <span class="text-xs text-gray-400 dark:text-gray-500">{{ formatTime(notification.createdAt) }}</span>
              <span class="text-xs text-gray-400 dark:text-gray-500">{{ getTypeLabel(notification.type) }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div v-if="!batchMode" class="shrink-0 ml-2 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              v-if="!notification.isRead"
              class="p-1.5 text-gray-400 hover:text-primary rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              title="标记已读"
              @click.stop="markAsRead(notification)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </button>
            <button
              class="p-1.5 text-gray-400 hover:text-danger rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              title="删除"
              @click.stop="deleteNotification(notification)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="flex items-center justify-center py-4 border-t border-gray-100 dark:border-gray-700/50">
          <button
            v-if="!loadingMore"
            class="text-sm text-primary hover:text-primary-600 transition-colors"
            @click="loadMore"
          >
            加载更多
          </button>
          <div v-else class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 通知中心页面 */
import type { Notification } from '~/types'
import { NotificationType } from '~/types'
import { notificationApi } from '~/api/notification'

definePageMeta({
  middleware: 'auth',
})

const notificationStore = useNotificationStore()

// 筛选 Tab 配置
const tabs = [
  { label: '全部', value: 0 },
  { label: '系统通知', value: NotificationType.System },
  { label: '审核通知', value: NotificationType.Audit },
  { label: '互动通知', value: NotificationType.Interact },
  { label: '关注通知', value: NotificationType.Follow },
  { label: '私信通知', value: NotificationType.Message },
]

// 状态
const activeTab = ref(0)
const notifications = ref<Notification[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const batchMode = ref(false)
const selectedIds = ref<number[]>([])

// 是否还有更多
const hasMore = computed(() => notifications.value.length < total.value)

// 切换 Tab
const switchTab = (type: number) => {
  activeTab.value = type
  page.value = 1
  notifications.value = []
  selectedIds.value = []
  fetchNotifications()
}

// 获取通知列表
const fetchNotifications = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, pageSize }
    if (activeTab.value) params.type = activeTab.value
    const response = await notificationApi.getNotifications(params)
    const data = response.data.data
    notifications.value = data?.list || []
    total.value = data?.total || 0
  } catch {
    notifications.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  loadingMore.value = true
  page.value++
  try {
    const params: Record<string, any> = { page: page.value, pageSize }
    if (activeTab.value) params.type = activeTab.value
    const response = await notificationApi.getNotifications(params)
    const data = response.data.data
    const newItems = data?.list || []
    notifications.value.push(...newItems)
    total.value = data?.total || 0
  } catch {
    page.value--
  } finally {
    loadingMore.value = false
  }
}

// 点击通知 - 标记已读
const handleClick = async (notification: Notification) => {
  if (batchMode.value) {
    toggleSelect(notification.id)
    return
  }
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(notification.id)
      notificationStore.markAsRead(notification.id)
      notification.isRead = true
    } catch {
      // 忽略错误
    }
  }
}

// 单条标记已读
const markAsRead = async (notification: Notification) => {
  if (notification.isRead) return
  try {
    await notificationApi.markAsRead(notification.id)
    notificationStore.markAsRead(notification.id)
    notification.isRead = true
  } catch {
    // 忽略错误
  }
}

// 全部标记已读
const markAllRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notificationStore.markAllAsRead()
    notifications.value.forEach(n => { n.isRead = true })
  } catch {
    // 忽略错误
  }
}

// 删除通知
const deleteNotification = async (notification: Notification) => {
  try {
    await notificationApi.deleteNotification(notification.id)
    notifications.value = notifications.value.filter(n => n.id !== notification.id)
    total.value--
    if (!notification.isRead) {
      notificationStore.decrementUnread()
    }
  } catch {
    // 忽略错误
  }
}

// 进入批量模式
const enterBatchMode = () => {
  batchMode.value = true
  selectedIds.value = []
}

// 退出批量模式
const exitBatchMode = () => {
  batchMode.value = false
  selectedIds.value = []
}

// 切换选择
const toggleSelect = (id: number) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

// 批量标记已读
const batchRead = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await notificationApi.batchMarkAsRead(selectedIds.value)
    selectedIds.value.forEach(id => {
      const n = notifications.value.find(item => item.id === id)
      if (n && !n.isRead) {
        n.isRead = true
        notificationStore.markAsRead(id)
      }
    })
    selectedIds.value = []
  } catch {
    // 忽略错误
  }
}

// 批量删除
const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await notificationApi.batchDeleteNotifications(selectedIds.value)
    const deleteSet = new Set(selectedIds.value)
    const removed = notifications.value.filter(n => deleteSet.has(n.id))
    notifications.value = notifications.value.filter(n => !deleteSet.has(n.id))
    total.value -= deleteSet.size
    removed.forEach(n => {
      if (!n.isRead) notificationStore.decrementUnread()
    })
    selectedIds.value = []
    if (notifications.value.length === 0 && page.value > 1) {
      page.value--
      fetchNotifications()
    }
  } catch {
    // 忽略错误
  }
}

// 获取类型标签
const getTypeLabel = (type: NotificationType) => {
  const map: Record<number, string> = {
    [NotificationType.System]: '系统通知',
    [NotificationType.Audit]: '审核通知',
    [NotificationType.Interact]: '互动通知',
    [NotificationType.Follow]: '关注通知',
    [NotificationType.Message]: '私信通知',
    [NotificationType.CommentReply]: '评论回复',
    [NotificationType.Mention]: '@提及',
  }
  return map[type] || '通知'
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
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 页面加载
onMounted(() => {
  fetchNotifications()
})

useHead({
  title: '通知中心 - 知讯',
})
</script>
