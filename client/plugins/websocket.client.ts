import { useWebSocket } from '~/composables/useWebSocket'

/**
 * WebSocket插件
 * 在客户端自动连接WebSocket，并监听实时数据更新
 */
export default defineNuxtPlugin(() => {
  // 仅客户端执行
  if (!import.meta.client) return

  // 创建WebSocket实例
  const ws = useWebSocket({
    autoConnect: true,
    autoReconnect: true,
    reconnectDelay: 3000,
    maxReconnectAttempts: 5,
    heartbeatInterval: 30000,
  })

  // 监听通知更新
  ws.subscribe('notification', (data) => {
    const notificationStore = useNotificationStore()
    notificationStore.handleRealtimeUpdate(data)
  })

  // 监听用户数据更新
  ws.subscribe('user', (data) => {
    const userStore = useUserStore()
    userStore.updateProfile(data)
  })

  // 监听文章数据更新
  ws.subscribe('article', (data) => {
    // 可以在这里触发页面数据刷新
    const event = new CustomEvent('article-update', { detail: data })
    window.dispatchEvent(event)
  })

  // 监听Token过期
  ws.subscribe('token-expired', () => {
    const userStore = useUserStore()
    userStore.logout()
    navigateTo('/login')
  })

  // 提供全局访问
  return {
    provide: {
      ws,
    },
  }
})
