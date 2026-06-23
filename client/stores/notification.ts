import { defineStore } from 'pinia'
import type { Notification } from '~/types'

/** 通知状态管理 Store */
export const useNotificationStore = defineStore('notification', () => {
  // 未读通知数量
  const unreadCount = ref<number>(0)
  // 通知列表
  const notifications = ref<Notification[]>([])
  // WebSocket连接实例
  const wsInstance = ref<WebSocket | null>(null)
  // WebSocket连接状态
  const isConnected = ref<boolean>(false)

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

  // 连接WebSocket
  const connectWebSocket = () => {
    if (!import.meta.client) return

    const config = useRuntimeConfig()
    const userStore = useUserStore()
    if (!userStore.token) return

    const wsBase = config.public.wsBase as string
    let wsUrl: string
    if (/^wss?:\/\//.test(wsBase)) {
      wsUrl = `${wsBase}/notifications?token=${userStore.token}`
    } else {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      wsUrl = `${protocol}//${window.location.host}${wsBase}/notifications?token=${userStore.token}`
    }
    const ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      isConnected.value = true
    }

    ws.onmessage = (event) => {
      try {
        const notification: Notification = JSON.parse(event.data)
        addNotification(notification)
      } catch {
        // 忽略无效消息
      }
    }

    ws.onclose = () => {
      isConnected.value = false
      // 3秒后自动重连
      setTimeout(() => {
        if (userStore.isLoggedIn) {
          connectWebSocket()
        }
      }, 3000)
    }

    ws.onerror = () => {
      isConnected.value = false
    }

    wsInstance.value = ws
  }

  // 断开WebSocket
  const disconnectWebSocket = () => {
    if (wsInstance.value) {
      wsInstance.value.close()
      wsInstance.value = null
      isConnected.value = false
    }
  }

  return {
    unreadCount,
    notifications,
    isConnected,
    setUnreadCount,
    incrementUnread,
    decrementUnread,
    setNotifications,
    addNotification,
    markAsRead,
    markAllAsRead,
    connectWebSocket,
    disconnectWebSocket,
  }
})
