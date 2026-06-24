import { defineStore } from 'pinia'
import type { Notification } from '~/types'

/** 通知状态管理 Store */
export const useNotificationStore = defineStore('notification', () => {
  // 未读通知数量
  const unreadCount = ref<number>(0)
  // 通知列表
  const notifications = ref<Notification[]>([])

  // 设置未读数量
  const setUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  // 增加未读数量
  const incrementUnread = () => {
    unreadCount.value++
  }

  // 减少未读数量
  const decrementUnread = () => {
    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  }

  // 设置通知列表
  const setNotifications = (list: Notification[]) => {
    notifications.value = list
  }

  // 添加新通知
  const addNotification = (notification: Notification) => {
    notifications.value.unshift(notification)
    incrementUnread()
  }

  // 标记通知为已读
  const markAsRead = (id: number) => {
    const notification = notifications.value.find((n) => n.id === id)
    if (notification && !notification.isRead) {
      notification.isRead = true
      decrementUnread()
    }
  }

  // 全部标记为已读
  const markAllAsRead = () => {
    notifications.value.forEach((n) => {
      n.isRead = true
    })
    unreadCount.value = 0
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
  }
})
