/** WebSocket插件：私信实时通信 - 延迟连接，不阻塞首屏渲染，带指数退避重连 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const userStore = useUserStore()
  const notificationStore = useNotificationStore()

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT_ATTEMPTS = 8
  const BASE_RECONNECT_DELAY = 2000

  // 计算指数退避延迟（上限30秒）
  const getReconnectDelay = () => {
    const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
    return Math.min(delay, 30000)
  }

  // 连接WebSocket
  const connect = () => {
    if (!import.meta.client) return
    if (!userStore.isLoggedIn) return
    if (!userStore.token) return
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

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
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        switch (data.type) {
          case 'CHAT':
            notificationStore.incrementUnread()
            showBrowserNotification(data.data)
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:chat', { detail: data.data }))
            }
            break
          case 'ONLINE':
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:online', { detail: data.data }))
            }
            break
          case 'OFFLINE':
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:offline', { detail: data.data }))
            }
            break
          case 'READ':
            if (import.meta.client) {
              window.dispatchEvent(new CustomEvent('ws:read', { detail: data.data }))
            }
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
      // 指数退避重连，达到最大次数后停止
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && userStore.isLoggedIn && userStore.token) {
        const delay = getReconnectDelay()
        reconnectAttempts++
        reconnectTimer = setTimeout(() => {
          if (userStore.isLoggedIn) {
            connect()
          }
        }, delay)
      }
    }

    ws.onerror = () => {
      // 仅关闭连接，由 onclose 统一处理重连逻辑
      ws?.close()
    }
  }

  // 断开WebSocket
  const disconnect = () => {
    reconnectAttempts = MAX_RECONNECT_ATTEMPTS // 阻止自动重连
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
    let initialCheckDone = false
    watch(
      () => ({ loggedIn: userStore.isLoggedIn, token: userStore.token }),
      ({ loggedIn, token }) => {
        if (loggedIn && token) {
          reconnectAttempts = 0
          // 首次连接延迟到浏览器空闲时，后续重连立即执行
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
