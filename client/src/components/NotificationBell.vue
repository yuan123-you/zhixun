<template>
  <!-- 通知铃铛组件 -->
  <div ref="bellRef" class="notif-bell-wrap">
    <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount <= 0">
      <el-button text class="bell-btn" @click="togglePanel">
        <el-icon :size="20"><Bell /></el-icon>
      </el-button>
    </el-badge>

    <Transition name="panel-drop">
      <div v-if="showPanel" class="notif-panel">
        <div class="notif-panel-header">
          <h3>通知</h3>
          <el-button v-if="notificationStore.unreadCount > 0" text size="small" type="primary" @click="markAllRead">全部已读</el-button>
        </div>

        <div class="notif-panel-body">
          <div v-if="loading" class="notif-loading">
            <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          </div>
          <div v-else-if="notifications.length === 0" class="notif-empty">
            <el-icon :size="40"><Bell /></el-icon>
            <p>暂无通知</p>
          </div>
          <div v-else>
            <div v-for="notification in notifications" :key="notification.id" class="notif-item"
              :class="{ unread: !notification.isRead }" @click="handleNotificationClick(notification)">
              <div class="notif-icon" :class="notifTypeClass(notification.type)">
                <el-icon :size="16"><component :is="notifTypeIcon(notification.type)" /></el-icon>
              </div>
              <div class="notif-content">
                <p>{{ notification.content || notification.title }}</p>
                <span>{{ formatTime(notification.createdAt) }}</span>
              </div>
              <span v-if="!notification.isRead" class="notif-dot"></span>
            </div>
          </div>
        </div>

        <div v-if="notifications.length > 0" class="notif-panel-footer">
          <RouterLink to="/notifications" @click="showPanel = false">查看全部通知</RouterLink>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { Bell, Loading, UserFilled, CircleCheckFilled, Star, Check } from '@element-plus/icons-vue'
import type { Notification } from '@/types'
import { NotificationType } from '@/types'

const notificationStore = useNotificationStore()
const userStore = useUserStore()

const showPanel = ref(false)
const loading = ref(false)
const notifications = ref<Notification[]>([])
const bellRef = ref<HTMLElement | null>(null)

const notifTypeClass = (type: string) => {
  const map: Record<string, string> = {
    [NotificationType.System]: 'icon-system', [NotificationType.Audit]: 'icon-audit',
    [NotificationType.Interact]: 'icon-interact', [NotificationType.Follow]: 'icon-follow',
    [NotificationType.Message]: 'icon-message', [NotificationType.CommentReply]: 'icon-reply',
    [NotificationType.Mention]: 'icon-mention',
  }
  return map[type] || 'icon-system'
}

const notifTypeIcon = (type: string) => { return Star }

const togglePanel = async () => { showPanel.value = !showPanel.value; if (showPanel.value) await fetchNotifications() }

const fetchNotifications = async () => {
  loading.value = true
  try {
    const { notificationApi } = await import('@/api/notification')
    const response = await notificationApi.getNotifications({ page: 1, pageSize: 10 })
    const data = response.data.data
    notifications.value = data?.items || data?.list || []
  } catch { /* ignore */ }
  loading.value = false
}

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      const { notificationApi } = await import('@/api/notification')
      await notificationApi.markAsRead(notification.id)
      notificationStore.markAsRead(notification.id)
      notification.isRead = true
    } catch { /* ignore */ }
  }
}

const markAllRead = async () => {
  try {
    const { notificationApi } = await import('@/api/notification')
    await notificationApi.markAllAsRead()
    notificationStore.markAllAsRead()
    notifications.value.forEach(n => { n.isRead = true })
  } catch { /* ignore */ }
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr); const now = new Date()
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

const handleClickOutside = (event: MouseEvent) => {
  if (bellRef.value && !bellRef.value.contains(event.target as Node)) showPanel.value = false
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const { notificationApi } = await import('@/api/notification')
      const response = await notificationApi.getUnreadCount()
      notificationStore.setUnreadCount(response.data.data?.unread_count ?? 0)
    } catch { /* ignore */ }
    document.addEventListener('click', handleClickOutside)
  }
})

onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.notif-bell-wrap { position: relative; }
.bell-btn { padding: 8px; border-radius: 8px; color: var(--el-text-color-regular); }

.notif-panel {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 8px;
  width: 320px;
  max-width: 96vw;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-dark);
  border: 1px solid var(--el-border-color-lighter);
  z-index: 50;
  overflow: hidden;
}
@media (min-width: 640px) {
  .notif-panel { width: 384px; }
}

.notif-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.notif-panel-header h3 { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }

.notif-panel-body { max-height: 320px; overflow-y: auto; }
.notif-loading { display: flex; align-items: center; justify-content: center; padding: 32px 0; }
.notif-empty { display: flex; flex-direction: column; align-items: center; padding: 32px 0; color: var(--el-text-color-placeholder); font-size: 14px; }

.notif-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
}
.notif-item:hover { background: var(--el-fill-color-lighter); }
.notif-item.unread { background: var(--el-color-primary-light-9); }

.notif-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 12px;
}
.icon-system { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.icon-audit { background: var(--el-color-warning-light-9); color: var(--el-color-warning); }
.icon-interact { background: var(--el-color-danger-light-9); color: var(--el-color-danger); }
.icon-follow { background: rgba(124,58,237,0.1); color: #7c3aed; }
.icon-message { background: var(--el-color-success-light-9); color: var(--el-color-success); }
.icon-reply { background: var(--el-color-success-light-9); color: var(--el-color-success); }
.icon-mention { background: var(--el-color-warning-light-9); color: #f97316; }

.notif-content { flex: 1; min-width: 0; }
.notif-content p { font-size: 14px; color: var(--el-text-color-primary); line-height: 1.375; }
.notif-content span { font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 4px; }

.notif-dot {
  width: 8px;
  height: 8px;
  background: var(--el-color-primary);
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: 8px;
  margin-top: 6px;
}

.notif-panel-footer {
  border-top: 1px solid var(--el-border-color-lighter);
}
.notif-panel-footer a {
  display: block;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  color: var(--el-color-primary);
  text-decoration: none;
}

.panel-drop-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.panel-drop-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.panel-drop-enter-from { opacity: 0; transform: scale(0.95) translateY(-4px); }
.panel-drop-leave-to { opacity: 0; transform: scale(0.95) translateY(-4px); }
</style>
