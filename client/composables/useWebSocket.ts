import type { Ref } from 'vue'

/** WebSocket消息类型 */
export interface WebSocketMessage {
  type: string
  data: any
  timestamp: number
}

/** WebSocket选项 */
export interface UseWebSocketOptions {
  /** 自动连接 */
  autoConnect?: boolean
  /** 自动重连 */
  autoReconnect?: boolean
  /** 重连延迟（毫秒） */
  reconnectDelay?: number
  /** 最大重连次数 */
  maxReconnectAttempts?: number
  /** 心跳间隔（毫秒） */
  heartbeatInterval?: number
}

/** 默认选项 */
const DEFAULT_OPTIONS: UseWebSocketOptions = {
  autoConnect: true,
  autoReconnect: true,
  reconnectDelay: 3000,
  maxReconnectAttempts: 5,
  heartbeatInterval: 30000,
}

/**
 * WebSocket组合式函数
 * 用于实时数据同步
 */
export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // WebSocket实例
  let ws: WebSocket | null = null

  // 连接状态
  const isConnected: Ref<boolean> = ref(false)

  // 连接错误
  const error: Ref<Error | null> = ref(null)

  // 重连次数
  let reconnectAttempts = 0

  // 重连定时器
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // 心跳定时器
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  // 消息监听器
  const messageListeners = new Map<string, Set<(data: any) => void>>()

  // 连接URL
  const getWebSocketUrl = (): string => {
    const config = useRuntimeConfig()
    const userStore = useUserStore()

    // 构建WebSocket URL（SSR安全：仅在客户端访问 window）
    const baseUrl = config.public.wsBase || (import.meta.client ? window.location.origin.replace('http', 'ws') : '')
    const token = userStore.token
    return `${baseUrl}/ws?token=${token}`
  }

  // 连接WebSocket
  const connect = () => {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    try {
      const url = getWebSocketUrl()
      ws = new WebSocket(url)

      ws.onopen = () => {
        console.log('[WebSocket] 连接成功')
        isConnected.value = true
        error.value = null
        reconnectAttempts = 0

        // 启动心跳
        startHeartbeat()
      }

      ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          handleMessage(message)
        } catch (err) {
          console.error('[WebSocket] 消息解析失败:', err)
        }
      }

      ws.onerror = (err) => {
        console.error('[WebSocket] 连接错误:', err)
        error.value = new Error('WebSocket连接错误')
      }

      ws.onclose = () => {
        console.log('[WebSocket] 连接关闭')
        isConnected.value = false
        stopHeartbeat()

        // 自动重连
        if (opts.autoReconnect && reconnectAttempts < (opts.maxReconnectAttempts || 5)) {
          reconnectAttempts++
          console.log(`[WebSocket] ${opts.reconnectDelay}ms 后进行第 ${reconnectAttempts} 次重连...`)
          reconnectTimer = setTimeout(() => {
            connect()
          }, opts.reconnectDelay)
        }
      }
    } catch (err) {
      console.error('[WebSocket] 创建连接失败:', err)
      error.value = err as Error
    }
  }

  // 断开连接
  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (ws) {
      ws.close()
      ws = null
    }

    isConnected.value = false
    stopHeartbeat()
  }

  // 发送消息
  const send = (type: string, data: any) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      console.warn('[WebSocket] 连接未建立，无法发送消息')
      return false
    }

    try {
      const message: WebSocketMessage = {
        type,
        data,
        timestamp: Date.now(),
      }
      ws.send(JSON.stringify(message))
      return true
    } catch (err) {
      console.error('[WebSocket] 发送消息失败:', err)
      return false
    }
  }

  // 处理接收到的消息
  const handleMessage = (message: WebSocketMessage) => {
    const { type, data } = message

    // 触发对应类型的监听器
    const listeners = messageListeners.get(type)
    if (listeners) {
      listeners.forEach((listener) => {
        try {
          listener(data)
        } catch (err) {
          console.error(`[WebSocket] 消息监听器执行失败 (${type}):`, err)
        }
      })
    }

    // 触发全局监听器
    const globalListeners = messageListeners.get('*')
    if (globalListeners) {
      globalListeners.forEach((listener) => {
        try {
          listener(message)
        } catch (err) {
          console.error('[WebSocket] 全局消息监听器执行失败:', err)
        }
      })
    }
  }

  // 订阅消息
  const subscribe = (type: string, callback: (data: any) => void) => {
    if (!messageListeners.has(type)) {
      messageListeners.set(type, new Set())
    }
    messageListeners.get(type)!.add(callback)

    // 返回取消订阅函数
    return () => {
      const listeners = messageListeners.get(type)
      if (listeners) {
        listeners.delete(callback)
        if (listeners.size === 0) {
          messageListeners.delete(type)
        }
      }
    }
  }

  // 启动心跳
  const startHeartbeat = () => {
    if (!opts.heartbeatInterval) return

    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      send('ping', { timestamp: Date.now() })
    }, opts.heartbeatInterval)
  }

  // 停止心跳
  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  // 自动连接
  if (opts.autoConnect && import.meta.client) {
    onMounted(() => {
      const userStore = useUserStore()
      if (userStore.isLoggedIn) {
        connect()
      }
    })

    // 监听登录状态变化
    watch(() => useUserStore().isLoggedIn, (isLoggedIn) => {
      if (isLoggedIn) {
        connect()
      } else {
        disconnect()
      }
    })
  }

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected: readonly(isConnected),
    error: readonly(error),
    connect,
    disconnect,
    send,
    subscribe,
  }
}
