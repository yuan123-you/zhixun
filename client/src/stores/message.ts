import { defineStore } from 'pinia'
import type { Conversation, Message, User } from '@/types'
import { socialApi } from '@/api'

/** 后端返回的会话原始数据（扁平结构） */
interface RawConversation {
  userId: number
  nickname: string
  avatar: string
  lastMessage: string
  unreadCount: number
  lastMessageTime: string
}

/** 后端返回的消息原始数据（兼容旧扁平结构与新嵌套 sender 字段） */
interface RawMessage {
  id?: number | null
  conversationId?: number | null
  type?: string | number | null
  senderId?: number | null
  senderNickname?: string | null
  senderAvatar?: string | null
  sender?: any
  receiverId?: number | null
  receiverNickname?: string | null
  receiverAvatar?: string | null
  content?: string | null
  isRead?: number | null
  createdAt?: string | null
}

/** 将后端扁平 ConversationVO 转换为前端嵌套 Conversation */
function transformConversation(raw: RawConversation): Conversation {
  const user: User = {
    id: raw.userId,
    uid: String(raw.userId),
    username: raw.nickname,
    nickname: raw.nickname,
    avatar: raw.avatar,
    bio: '',
    email: '',
    phone: '',
    gender: 0 as any,
    birthday: '',
    followCount: 0,
    followerCount: 0,
    articleCount: 0,
    likeCount: 0,
    isFollowing: false,
    createdAt: '',
  }
  const lastMessage: Message = {
    id: 0,
    conversationId: 0,
    senderId: raw.userId,
    sender: user,
    content: raw.lastMessage || '',
    type: 0,
    isRead: true,
    createdAt: raw.lastMessageTime || '',
  }
  return {
    id: raw.userId,
    user,
    lastMessage,
    unreadCount: raw.unreadCount || 0,
    updatedAt: raw.lastMessageTime || '',
  }
}

/** 将后端扁平 MessageVO 转换为前端嵌套 Message（兼容新嵌套 sender 字段） */
function transformMessage(raw: RawMessage): Message {
  const senderId = Number(raw.senderId) || 0
  // 优先从嵌套 sender 对象读取信息，回退到扁平字段
  const senderInfo: any = raw.sender || {}
  const senderNickname = senderInfo.nickname ?? raw.senderNickname ?? ''
  const senderAvatar = senderInfo.avatar ?? raw.senderAvatar ?? ''

  const sender: User = {
    id: senderId,
    uid: String(senderId),
    username: senderNickname,
    nickname: senderNickname,
    avatar: senderAvatar,
    bio: '',
    email: '',
    phone: '',
    gender: 0 as any,
    birthday: '',
    followCount: 0,
    followerCount: 0,
    articleCount: 0,
    likeCount: 0,
    isFollowing: false,
    createdAt: '',
  }
  return {
    id: Number(raw.id) || 0,
    conversationId: Number(raw.conversationId) || 0,
    senderId,
    sender,
    content: raw.content || '',
    type: raw.type != null ? String(raw.type) : 'text',
    isRead: raw.isRead === 1,
    createdAt: raw.createdAt || '',
  }
}

/** 私信状态管理 Store */
export const useMessageStore = defineStore('message', () => {
  // ==================== 本地持久化（IndexedDB） ====================
  const localDb = useChatLocalDb()
  const userStoreRef = useUserStore()
  /** 当前登录用户 ID，用于 IndexedDB 会话 key 计算 */
  const myUserId = computed(() => userStoreRef.userInfo?.id ?? 0)

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
      if (data.code === 0 || data.code === 200) {
        const rawList = (data.data.list || []) as unknown as RawConversation[]
        conversations.value = rawList.map(transformConversation)
        const ids = conversations.value.map(c => c.user.id)
        if (ids.length > 0) {
          fetchBatchOnlineStatus(ids)
        }
      }
    } catch (err: any) {
      console.error('[MessageStore] 获取会话列表失败:', err.message || err)
    } finally {
      loadingConversations.value = false
    }
  }

  // ==================== 消息记录 ====================
  const fetchMessages = async (userId: number, page = 1) => {
    if (page === 1) {
      currentMessages.value = []
      currentPage.value = 1
      hasMoreMessages.value = true

      // 先从 IndexedDB 加载本地消息，实现即时显示
      if (myUserId.value) {
        try {
          const localMsgs = await localDb.getMessages(myUserId.value, userId)
          if (localMsgs.length > 0) {
            currentMessages.value = localMsgs
          }
        } catch (e) {
          console.warn('[MessageStore] IndexedDB 本地加载失败:', e)
        }
      }
    }
    if (loadingMessages.value) return

    loadingMessages.value = true
    try {
      const { data } = await socialApi.getMessages(userId, { page, pageSize: 30 })
      if (data.code === 0 || data.code === 200) {
        const rawMsgs = (data.data.list || []) as unknown as RawMessage[]
        const msgs = rawMsgs.map(transformMessage)

        // 将服务端消息保存到 IndexedDB
        if (myUserId.value) {
          localDb.saveMessages(msgs, myUserId.value, userId).catch(e =>
            console.warn('[MessageStore] IndexedDB 保存失败:', e))
        }

        // 后端已按时间正序返回（最早在前），page=1是最新一页，page>1是更早的消息
        if (page === 1) {
          // 合并本地与服务端消息，服务端数据覆盖本地（更权威）
          const merged = localDb.mergeMessages(currentMessages.value, msgs)
          currentMessages.value = merged
        } else {
          // 加载更早的消息，插入到列表前面
          const merged = localDb.mergeMessages(currentMessages.value, msgs)
          currentMessages.value = merged.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
        }
        hasMoreMessages.value = rawMsgs.length >= 30
        currentPage.value = page
      }
    } catch (err: any) {
      console.error('[MessageStore] 获取消息失败:', err.message || err)
    } finally {
      loadingMessages.value = false
    }
  }

  const loadMoreMessages = async (userId: number) => {
    if (loadingMessages.value) return

    if (hasMoreMessages.value) {
      await fetchMessages(userId, currentPage.value + 1)
      return
    }

    // 服务端已无更多消息，检查 IndexedDB 是否有更早的本地消息
    if (!myUserId.value) return
    try {
      const allLocal = await localDb.getMessages(myUserId.value, userId)
      if (allLocal.length <= currentMessages.value.length) return

      // 找出本地有但当前列表中没有的消息（更早的历史消息）
      const currentIds = new Set(currentMessages.value.map(m => m.id))
      const olderLocal = allLocal.filter(m => !currentIds.has(m.id))
      if (olderLocal.length > 0) {
        currentMessages.value = [...olderLocal, ...currentMessages.value]
      }
    } catch (e) {
      console.warn('[MessageStore] IndexedDB 加载更多失败:', e)
    }
  }

  // ==================== 发送消息 ====================
  const sendMessage = async (userId: number, content: string, type: string = 'text') => {
    // 输入校验：避免发送空内容
    if (!content || !content.trim()) {
      const err = new Error('消息内容不能为空')
      console.warn('[MessageStore] 发送消息内容为空，跳过请求')
      throw err
    }

    let data: any
    try {
      const resp = await socialApi.sendMessage(userId, { content: content.trim(), type })
      data = resp.data
      console.debug('[MessageStore] 发送消息响应:', { userId, code: data?.code, hasData: !!data?.data })
    } catch (err: any) {
      // 拦截器已经 reject 出带 message 的 Error，直接透传便于上层显示
      console.error('[MessageStore] 发送消息请求失败:', err?.message || err)
      throw err instanceof Error ? err : new Error(err?.message || '消息发送失败，请稍后重试')
    }

    if ((data?.code === 0 || data?.code === 200) && data.data) {
      const raw = data.data as unknown as RawMessage
      const newMsg = transformMessage(raw)
      // 避免重复添加（如果 WebSocket 已经先一步把消息推过来）
      const exists = currentMessages.value.some(m => m.id && newMsg.id && m.id === newMsg.id)
      if (!exists) {
        currentMessages.value.push(newMsg)
      }
      // 同步保存到 IndexedDB
      if (myUserId.value) {
        localDb.saveMessages([newMsg], myUserId.value, userId).catch(e =>
          console.warn('[MessageStore] IndexedDB 保存发送消息失败:', e))
      }
      updateConversationLastMessage(userId, content.trim(), type)
      return newMsg
    }

    // 服务端返回 code !== 0/200 的情况（理论上会被 axios 拦截器拒绝，这里是兜底）
    const errMsg = data?.message && data.message !== 'success'
      ? data.message
      : '消息发送失败，请稍后重试'
    console.error('[MessageStore] 发送消息业务失败:', errMsg, 'userId:', userId, 'response:', data)
    throw new Error(errMsg)
  }

  // ==================== 标记已读 ====================
  const markRead = async (userId: number) => {
    try {
      await socialApi.markConversationRead(userId)
      const conv = conversations.value.find(c => c.user.id === userId)
      if (conv) conv.unreadCount = 0
    } catch (err: any) {
      console.error('[MessageStore] 标记已读失败:', err.message || err)
    }
  }

  // ==================== 更新会话最后消息 ====================
  const updateConversationLastMessage = (userId: number, content: string, type?: string) => {
    const conv = conversations.value.find(c => c.user.id === userId)
    if (conv) {
      let previewContent = content
      if (type === 'image') previewContent = '[图片]'
      else if (type === 'voice') previewContent = '[语音]'
      else if (type === 'file') previewContent = '[文件]'
      conv.lastMessage = {
        ...conv.lastMessage,
        content: previewContent,
        type: type || 'text',
        createdAt: new Date().toISOString(),
      }
      conv.updatedAt = new Date().toISOString()
      const idx = conversations.value.indexOf(conv)
      if (idx > 0) {
        conversations.value.splice(idx, 1)
        conversations.value.unshift(conv)
      }
    }
  }

  // ==================== 处理 WebSocket 实时消息 ====================
  const handleIncomingMessage = (wsData: any) => {
    if (!wsData) return
    const { senderId, content, messageId, senderNickname, senderAvatar, createdAt, messageType } = wsData

    const newMsg: Message = {
      id: messageId || Date.now(),
      conversationId: 0,
      senderId,
      sender: {
        id: senderId,
        uid: String(senderId),
        username: senderNickname || '用户',
        nickname: senderNickname || '用户',
        avatar: senderAvatar || '',
        bio: '',
        email: '',
        phone: '',
        gender: 0 as any,
        birthday: '',
        followCount: 0,
        followerCount: 0,
        articleCount: 0,
        likeCount: 0,
        isFollowing: false,
        createdAt: '',
      },
      content,
      type: messageType || 'text',
      isRead: false,
      createdAt: createdAt || new Date().toISOString(),
    }

    // 保存到 IndexedDB
    if (myUserId.value) {
      localDb.saveMessages([newMsg], myUserId.value, senderId).catch(e =>
        console.warn('[MessageStore] IndexedDB 保存接收消息失败:', e))
    }

    if (currentChatUserId.value === senderId) {
      currentMessages.value.push(newMsg)
      markRead(senderId)
    }

    const conv = conversations.value.find(c => c.user.id === senderId)
    if (conv) {
      conv.lastMessage = {
        ...newMsg,
        content: newMsg.type === 'image' ? '[图片]'
          : newMsg.type === 'file' ? '[文件]'
          : newMsg.type === 'voice' ? '[语音]'
          : newMsg.content,
      }
      if (currentChatUserId.value !== senderId) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
      }
      conv.updatedAt = newMsg.createdAt
      const idx = conversations.value.indexOf(conv)
      if (idx > 0) {
        conversations.value.splice(idx, 1)
        conversations.value.unshift(conv)
      }
    } else {
      fetchConversations()
    }

    fetchUnreadCount()
  }

  // ==================== 在线状态 ====================
  const fetchBatchOnlineStatus = async (ids: number[]) => {
    try {
      const { data } = await socialApi.getBatchOnlineStatus(ids)
      if ((data.code === 0 || data.code === 200) && data.data) {
        onlineStatus.value = { ...onlineStatus.value, ...data.data }
      }
    } catch (err: any) {
      console.error('[MessageStore] 获取在线状态失败:', err.message || err)
    }
  }

  const isUserOnline = (userId: number) => {
    return onlineStatus.value[userId] ?? false
  }

  // ==================== 未读总数 ====================
  const fetchUnreadCount = async () => {
    try {
      const { data } = await socialApi.getUnreadCount()
      if (data.code === 0 || data.code === 200) {
        unreadTotal.value = data.data.count ?? 0
      }
    } catch (err: any) {
      console.error('[MessageStore] 获取未读总数失败:', err.message || err)
    }
  }

  // ==================== WebSocket 事件监听 ====================
  let wsUnsubscribers: (() => void)[] = []

  const setupWsListeners = () => {
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
  setupWsListeners()

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
      // 退出登录时不再自动清理本地消息，改为手动清理模式
    }
  })

  // ==================== 手动清理会话 ====================
  const clearConversation = async (userId: number) => {
    if (!myUserId.value) return 0
    try {
      const count = await localDb.deleteByConv(myUserId.value, userId)
      // 如果正在查看该会话，清空当前显示
      if (currentChatUserId.value === userId) {
        currentMessages.value = []
        hasMoreMessages.value = false
      }
      return count
    } catch (e) {
      console.error('[MessageStore] 清理会话失败:', e)
      return 0
    }
  }

  return {
    conversations,
    unreadTotal,
    currentChatUserId,
    currentMessages,
    loadingConversations,
    loadingMessages,
    hasMoreMessages,
    currentPage,
    onlineStatus,
    myUserId,
    localDb,
    fetchConversations,
    fetchMessages,
    loadMoreMessages,
    sendMessage,
    markRead,
    handleIncomingMessage,
    fetchUnreadCount,
    isUserOnline,
    setupWsListeners,
    cleanupWsListeners,
    clearConversation,
  }
})