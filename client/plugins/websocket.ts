/** WebSocket插件：私信实时通信 - 延迟连接，不阻塞首屏渲染 */
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
        // 根据消息类型分发处理，与后端 ChatWebSocketHandler 对齐
        switch (data.type) {
          case 'CHAT':
            // 新私信消息
            notificationStore.incrementUnread()
            // 触发浏览器系统通知
            showBrowserNotification(data.data)
            // 通过自定义事件通知聊天页面实时更新
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:chat', { detail: data.data }))
            }
            break
          case 'ONLINE':
            // 用户上线通知
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:online', { detail: data.data }))
            }
            break
          case 'OFFLINE':
            // 用户下线通知
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:offline', { detail: data.data }))
            }
            break
          case 'READ':
            // 消息已读通知
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:read', { detail: data.data }))
            }
            break
          case 'NOTIFICATION':
            // 系统通知（来自 RabbitMQ 推送）
            notificationStore.addNotification(data.data)
            showBrowserNotification(data.data)
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

  // 监听用户登录状态变化，延迟连接避免阻塞首屏
  if (import.meta.client) {
    watch(
      () => userStore.isLoggedIn,
      (loggedIn) => {
        if (loggedIn) {
          // 延迟到浏览器空闲时连接，不阻塞首屏渲染
          requestIdleCallback
            ? requestIdleCallback(() => connect())
            : setTimeout(() => connect(), 2000)
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

/** 浏览器系统通知：显示发送者信息和消息预览 */
function showBrowserNotification(data: Record<string, any>) {
  if (!import.meta.client) return
  if (!('Notification' in window)) return

  const senderId = data.senderId
  const content = data.content || data.title || '新消息'
  const senderName = data.senderNickname || data.title || '用户'

  if (Notification.permission === 'granted') {
    new Notification(`${senderName} 发来私信`, {
      body: content.length > 50 ? content.substring(0, 50) + '...' : content,
      icon: data.senderAvatar || '/favicon.svg',
      tag: `chat-${senderId}`,
    })
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission().then((permission) => {
      if (permission === 'granted') {
        new Notification(`${senderName} 发来私信`, {
          body: content.length > 50 ? content.substring(0, 50) + '...' : content,
          icon: data.senderAvatar || '/favicon.svg',
          tag: `chat-${senderId}`,
        })
      }
    })
  }
}
