import { defineStore } from 'pinia'
import type { Notification } from '@/types'

/** 通知状态管理 Store */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref<number>(0)
  const notifications = ref<Notification[]>([])

  const setUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  const incrementUnread = () => {
    unreadCount.value++
  }

  const decrementUnread = () => {
    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  }

  const setNotifications = (list: Notification[]) => {
    notifications.value = list
  }

  const addNotification = (notification: Notification) => {
    notifications.value.unshift(notification)
    incrementUnread()
  }

  const markAsRead = (id: number) => {
    const notification = notifications.value.find((n) => n.id === id)
    if (notification && !notification.isRead) {
      notification.isRead = true
      decrementUnread()
    }
  }

  const markAllAsRead = () => {
    notifications.value.forEach((n) => {
      n.isRead = true
    })
    unreadCount.value = 0
  }

  const handleRealtimeUpdate = (data: any) => {
    if (data.type === 'new_notification') {
      addNotification(data.notification)
    } else if (data.type === 'mark_read') {
      markAsRead(data.notificationId)
    } else if (data.type === 'mark_all_read') {
      markAllAsRead()
    } else if (data.type === 'unread_count') {
      setUnreadCount(data.count)
    }
  }

  return {
    unreadCount,
    notifications,
    setUnreadCount,
    incrementUnread,
    decrementUnread,
    setNotifications,
    addNotification,
    markAsRead,
    markAllAsRead,
    handleRealtimeUpdate,
  }
})
