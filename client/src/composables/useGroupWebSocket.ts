import { ref } from 'vue'
import type { GroupMessage } from '@/api/group'

interface WsMessage {
  type: 'CHAT' | 'SYSTEM' | 'ERROR'
  data: any
}

/**
 * 群组聊天 WebSocket composable
 * 连接 /ws/group-chat?token=xxx&groupId=xxx
 * 自动重连（指数退避，最多5次）
 */
export function useGroupWebSocket(groupId: number, onMessage: (msg: GroupMessage) => void, onSystem?: (content: string) => void) {
  const connected = ref(false)
  const error = ref<string | null>(null)

  let ws: WebSocket | null = null
  let reconnectAttempt = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let disposed = false

  function getToken(): string {
    if (typeof window === 'undefined') return ''
    try {
      const raw = localStorage.getItem('zhixun_accessToken')
      return raw ? JSON.parse(raw).value || '' : ''
    } catch { return '' }
  }

  function connect() {
    if (disposed || !groupId) return
    const token = getToken()
    if (!token) {
      error.value = '未登录'
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}/ws/group-chat?token=${encodeURIComponent(token)}&groupId=${groupId}`

    try {
      ws = new WebSocket(url)
    } catch (e) {
      error.value = '连接失败'
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      connected.value = true
      error.value = null
      reconnectAttempt = 0
    }

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data)
        if (msg.type === 'CHAT' && msg.data) {
          onMessage(msg.data as GroupMessage)
        } else if (msg.type === 'SYSTEM' && msg.data?.content && onSystem) {
          onSystem(msg.data.content)
        } else if (msg.type === 'ERROR') {
          error.value = msg.data?.message || '未知错误'
        }
      } catch {
        // 忽略无法解析的消息
      }
    }

    ws.onclose = () => {
      connected.value = false
      if (!disposed) scheduleReconnect()
    }

    ws.onerror = () => {
      connected.value = false
    }
  }

  function scheduleReconnect() {
    if (disposed || reconnectAttempt >= 5) return
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), 30000)
    reconnectAttempt++
    reconnectTimer = setTimeout(connect, delay)
  }

  function send(type: string, data: any) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type, data }))
    }
  }

  function disconnect() {
    disposed = true
    if (reconnectTimer) clearTimeout(reconnectTimer)
    if (ws) {
      ws.onclose = null
      ws.close()
      ws = null
    }
    connected.value = false
  }

  // 初始连接
  connect()

  return { connected, error, send, disconnect }
}
