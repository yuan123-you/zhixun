import { defineStore } from 'pinia'
import type { Conversation, Message } from '~/types'
import { socialApi } from '~/api'

/** 私信状态管理 Store */
export const useMessageStore = defineStore('message', () => {
  // ==================== 状态 ====================
  const conversations = ref<Conversation[]>([])
  const unreadTotal = ref(0)
  const currentChatUserId = ref<number | null>(null)
  const currentMessages = ref<Message[]>([])
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const hasMoreMessages = ref(true)
  const currentPage = ref(1)
  const onlineStatus = ref<Record<number, boolean>>({})

  // ==================== 会话列表 ====================
  const fetchConversations = async () => {
    loadingConversations.value = true
    try {
      const { data } = await socialApi.getConversations({ page: 1, pageSize: 100 })
      if (data.code === 0) {
        conversations.value = data.data.list || []
        // 批量获取在线状态
        const ids = conversations.value.map(c => c.user.id)
        if (ids.length > 0) {
          fetchBatchOnlineStatus(ids)
        }
      }
    } catch { /* 静默失败 */ }
    finally { loadingConversations.value = false }
  }

  // ==================== 消息记录 ====================
  const fetchMessages = async (userId: number, page = 1) => {
    if (page === 1) {
      currentMessages.value = []
      currentPage.value = 1
      hasMoreMessages.value = true
    }
    if (loadingMessages.value) return

    loadingMessages.value = true
    try {
      const { data } = await socialApi.getMessages(userId, { page, pageSize: 30 })
      if (data.code === 0) {
        const msgs = data.data.list || []
        if (page === 1) {
          currentMessages.value = [...msgs].reverse()
        } else {
          currentMessages.value = [...msgs.reverse(), ...currentMessages.value]
        }
        hasMoreMessages.value = msgs.length >= 30
        currentPage.value = page
      }
    } finally { loadingMessages.value = false }
  }

  /** 加载更早的消息（滚动到顶部触发） */
  const loadMoreMessages = async (userId: number) => {
    if (!hasMoreMessages.value || loadingMessages.value) return
    await fetchMessages(userId, currentPage.value + 1)
  }

  // ==================== 发送消息 ====================
  const sendMessage = async (userId: number, content: string) => {
    const { data } = await socialApi.sendMessage(userId, { content, type: 0 })
    if (data.code === 0) {
      const newMsg: Message = data.data
      currentMessages.value.push(newMsg)
      updateConversation(userId, newMsg, 0)
      return newMsg
    }
    return null
  }

  // ==================== 标记已读 ====================
  const markRead = async (userId: number) => {
    try {
      await socialApi.markConversationRead(userId)
      const conv = conversations.value.find(c => c.user.id === userId)
      if (conv) conv.unreadCount = 0
    } catch { /* 静默失败 */ }
  }

  // ==================== 更新会话 ====================
  const updateConversation = (userId: number, message: Message, unreadInc = 0) => {
    const conv = conversations.value.find(c => c.user.id === userId)
    if (conv) {
      conv.lastMessage = message
      conv.updatedAt = message.createdAt
      if (unreadInc > 0) {
        conv.unreadCount = (conv.unreadCount || 0) + unreadInc
      }
      // 置顶
      const idx = conversations.value.indexOf(conv)
      if (idx > 0) {
        conversations.value.splice(idx, 1)
        conversations.value.unshift(conv)
      }
      // 如果不在当前聊天中则递增未读数
      if (currentChatUserId.value !== userId) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
      }
    }
  }

  // ==================== 处理 WebSocket 实时消息 ====================
  const handleIncomingMessage = (wsData: any) => {
    if (!wsData) return
    const { senderId, content, messageId, senderNickname, senderAvatar, createdAt } = wsData

    // 构建前端 Message 对象
    const newMsg: Message = {
      id: messageId || Date.now(),
      conversationId: 0,
      senderId,
      sender: {
        id: senderId,
        nickname: senderNickname || '用户',
        avatar: senderAvatar || '',
      } as any,
      content,
      type: 0,
      isRead: false,
      createdAt: createdAt || new Date().toISOString(),
    }

    // 如果正在和该用户聊天，直接追加消息
    if (currentChatUserId.value === senderId) {
      currentMessages.value.push(newMsg)
      markRead(senderId)
    }

    // 更新会话列表
    const conv = conversations.value.find(c => c.user.id === senderId)
    if (conv) {
      conv.lastMessage = newMsg
      if (currentChatUserId.value !== senderId) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
      }
      conv.updatedAt = newMsg.createdAt
      // 置顶
      const idx = conversations.value.indexOf(conv)
      if (idx > 0) {
        conversations.value.splice(idx, 1)
        conversations.value.unshift(conv)
      }
    } else {
      // 新会话，刷新列表
      fetchConversations()
    }

    // 更新总未读数
    fetchUnreadCount()
  }

  // ==================== 在线状态 ====================
  const fetchBatchOnlineStatus = async (ids: number[]) => {
    try {
      const { data } = await socialApi.getBatchOnlineStatus(ids)
      if (data.code === 0 && data.data) {
        onlineStatus.value = { ...onlineStatus.value, ...data.data }
      }
    } catch { /* 静默失败 */ }
  }

  const isUserOnline = (userId: number) => {
    return onlineStatus.value[userId] ?? false
  }

  // ==================== 未读总数 ====================
  const fetchUnreadCount = async () => {
    try {
      const { data } = await socialApi.getUnreadCount()
      if (data.code === 0) {
        unreadTotal.value = data.data.count ?? 0
      }
    } catch { /* 静默失败 */ }
  }

  // ==================== WebSocket 事件监听 ====================
  let wsUnsubscribers: (() => void)[] = []

  const setupWsListeners = () => {
    if (!import.meta.client) return
    cleanupWsListeners()

    const onChat = (e: Event) => {
      handleIncomingMessage((e as CustomEvent).detail)
    }
    const onOnline = (e: Event) => {
      const detail = (e as CustomEvent).detail
      if (detail?.userId) {
        onlineStatus.value = { ...onlineStatus.value, [detail.userId]: true }
      }
    }
    const onOffline = (e: Event) => {
      const detail = (e as CustomEvent).detail
      if (detail?.userId) {
        onlineStatus.value = { ...onlineStatus.value, [detail.userId]: false }
      }
    }

    window.addEventListener('ws:chat', onChat)
    window.addEventListener('ws:online', onOnline)
    window.addEventListener('ws:offline', onOffline)

    wsUnsubscribers = [
      () => window.removeEventListener('ws:chat', onChat),
      () => window.removeEventListener('ws:online', onOnline),
      () => window.removeEventListener('ws:offline', onOffline),
    ]
  }

  const cleanupWsListeners = () => {
    wsUnsubscribers.forEach(fn => fn())
    wsUnsubscribers = []
  }

  // 客户端初始化时注册 WebSocket 监听
  if (import.meta.client) {
    setupWsListeners()
  }

  // ==================== 退出登录时清理 ====================
  const userStore = useUserStore()
  watch(() => userStore.isLoggedIn, (loggedIn) => {
    if (loggedIn) {
      setupWsListeners()
      fetchConversations()
      fetchUnreadCount()
    } else {
      cleanupWsListeners()
      conversations.value = []
      currentMessages.value = []
      currentChatUserId.value = null
      unreadTotal.value = 0
    }
  })

  return {
    // 状态
    conversations,
    unreadTotal,
    currentChatUserId,
    currentMessages,
    loadingConversations,
    loadingMessages,
    hasMoreMessages,
    onlineStatus,
    // 方法
    fetchConversations,
    fetchMessages,
    loadMoreMessages,
    sendMessage,
    markRead,
    updateConversation,
    handleIncomingMessage,
    fetchUnreadCount,
    isUserOnline,
    setupWsListeners,
    cleanupWsListeners,
  }
})
