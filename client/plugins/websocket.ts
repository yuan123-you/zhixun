/** WebSocket插件：私信实时通信 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const notificationStore = useNotificationStore()

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // 连接WebSocket
  const connect = () => {
    if (!import.meta.client) return
    if (!userStore.isLoggedIn) return
    if (ws && ws.readyState === WebSocket.OPEN) return

    const wsBase = config.public.wsBase as string
    let wsUrl: string
    if (/^wss?:\/\//.test(wsBase)) {
      // 绝对地址（本地开发直连后端）
      wsUrl = `${wsBase}/chat?token=${userStore.token}`
    } else {
      // 相对地址，根据当前页面协议和主机推导（生产环境经 Nginx 代理）
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      wsUrl = `${protocol}//${window.location.host}${wsBase}/chat?token=${userStore.token}`
    }
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      // WebSocket连接成功
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        // 根据消息类型分发处理
        switch (data.type) {
          case 'message':
            // 新私信消息
            notificationStore.incrementUnread()
            break
          case 'notification':
            // 系统通知
            notificationStore.addNotification(data.payload)
            break
        }
      } catch {
        // 忽略无效消息
      }
    }

    ws.onclose = () => {
      ws = null
      // 5秒后自动重连
      reconnectTimer = setTimeout(() => {
        if (userStore.isLoggedIn) {
          connect()
        }
      }, 5000)
    }

    ws.onerror = () => {
      ws?.close()
    }
  }

  // 断开WebSocket
  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
  }

  // 发送消息
  const send = (data: Record<string, any>) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data))
    }
  }

  // 监听用户登录状态变化
  if (import.meta.client) {
    watch(
      () => userStore.isLoggedIn,
      (loggedIn) => {
        if (loggedIn) {
          connect()
        } else {
          disconnect()
        }
      },
      { immediate: true }
    )
  }

  // 提供全局 $ws 对象
  return {
    provide: {
      ws: {
        connect,
        disconnect,
        send,
      },
    },
  }
})
