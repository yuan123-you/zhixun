/** WebSocket 插件：私信实时通信 - 延迟连接，带指数退避重连（纯 SPA 版） */
export function initWebSocketPlugin() {
  const userStore = useUserStore()
  const notificationStore = useNotificationStore()

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT_ATTEMPTS = 8
  const BASE_RECONNECT_DELAY = 2000

  const getReconnectDelay = () => {
    const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
    return Math.min(delay, 30000)
  }

  const connect = () => {
    if (!userStore.isLoggedIn) return
    if (!userStore.token) return
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

    const wsBase = import.meta.env.VITE_WS_BASE as string || ''
    let wsUrl: string
    // WebSocket 握手时浏览器会自动携带同域 Cookie（httpOnly Cookie 中的 accessToken）
    // 因此无需在 URL 中传递 token 参数
    if (/^wss?:\/\//.test(wsBase)) {
      wsUrl = `${wsBase}/chat`
    } else {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      wsUrl = `${protocol}//${window.location.host}${wsBase}/chat`
    }
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        switch (data.type) {
          case 'CHAT':
            notificationStore.incrementUnread()
            showBrowserNotification(data.data)
            window.dispatchEvent(new CustomEvent('ws:chat', { detail: data.data }))
            break
          case 'ONLINE':
            window.dispatchEvent(new CustomEvent('ws:online', { detail: data.data }))
            break
          case 'OFFLINE':
            window.dispatchEvent(new CustomEvent('ws:offline', { detail: data.data }))
            break
          case 'READ':
            window.dispatchEvent(new CustomEvent('ws:read', { detail: data.data }))
            break
          case 'NOTIFICATION':
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
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && userStore.isLoggedIn && userStore.token) {
        const delay = getReconnectDelay()
        reconnectAttempts++
        reconnectTimer = setTimeout(() => {
          if (userStore.isLoggedIn) connect()
        }, delay)
      }
    }

    ws.onerror = () => {
      ws?.close()
    }
  }

  const disconnect = () => {
    reconnectAttempts = MAX_RECONNECT_ATTEMPTS
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
  }

  const send = (data: Record<string, any>) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data))
    }
  }

  let initialCheckDone = false
  watch(
    () => ({ loggedIn: userStore.isLoggedIn, token: userStore.token }),
    ({ loggedIn, token }) => {
      if (loggedIn && token) {
        reconnectAttempts = 0
        if (!initialCheckDone) {
          initialCheckDone = true
          requestIdleCallback
            ? requestIdleCallback(() => connect())
            : setTimeout(() => connect(), 2000)
        } else {
          connect()
        }
      } else {
        disconnect()
      }
    },
    { immediate: true }
  )

  return { connect, disconnect, send }
}

/** 浏览器系统通知 */
function showBrowserNotification(data: Record<string, any>) {
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
