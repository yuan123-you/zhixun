<template>
  <!-- 私信页面 -->
  <div class="max-w-5xl mx-auto px-4 py-6">
    <div class="card overflow-hidden" style="height: calc(100vh - 8rem)">
      <div class="flex h-full">
        <!-- 左侧会话列表 -->
        <div class="w-full md:w-80 border-r border-slate-200 flex flex-col" :class="{ 'hidden md:flex': activeConversation }">
          <!-- 搜索会话 -->
          <div class="p-2 border-b border-slate-200">
            <input v-model="conversationSearch" type="text" class="input text-sm" placeholder="搜索会话..." />
          </div>

          <!-- 会话列表 -->
          <div class="flex-1 overflow-y-auto">
            <!-- 加载状态 -->
            <div v-if="loading" class="flex items-center justify-center py-16">
              <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            </div>

            <!-- 错误重试 -->
            <ErrorRetry v-else-if="conversationsError" :message="conversationsError" :on-retry="loadConversations" />

            <template v-else>
              <button
                v-for="conv in filteredConversations"
                :key="conv.id"
                class="w-full flex items-center space-x-3 p-4 hover:bg-slate-50 transition-colors"
                :class="{ 'bg-slate-50': activeConversation?.id === conv.id }"
                @click="selectConversation(conv)"
              >
                <UserAvatar :src="conv.user?.avatar" alt="头像" size="lg" />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-slate-900">{{ conv.user?.nickname }}</span>
                    <span class="text-2xs text-gray-400">{{ formatTime(conv.updatedAt) }}</span>
                  </div>
                  <p class="text-xs text-slate-500 line-clamp-1 mt-0.5">{{ conv.lastMessage?.content }}</p>
                </div>
                <span v-if="conv.unreadCount > 0" class="w-5 h-5 bg-danger text-white text-2xs rounded-full flex items-center justify-center shrink-0">
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </button>

              <!-- 空状态 -->
              <EmptyState v-if="conversations.length === 0" title="暂无会话" description="开始一段新的对话吧" />
            </template>
          </div>
        </div>

        <!-- 右侧聊天窗口 -->
        <div class="flex-1 flex flex-col" :class="{ 'hidden md:flex': !activeConversation }">
          <ChatWindow
            v-if="activeConversation"
            :conversation="activeConversation"
            :messages="messages"
            @send="sendMessage"
            @back="activeConversation = null"
          />

          <!-- 未选择会话时的提示 -->
          <div v-else class="flex-1 flex items-center justify-center">
            <div class="text-center">
              <svg class="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-slate-500">{{ '选择一个会话开始聊天' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 私信页面：左侧会话列表 + 右侧聊天窗口 */
import type { Conversation, Message } from '~/types'
import { socialApi } from '~/api'

definePageMeta({
  middleware: 'auth',
})

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

const conversations = ref<Conversation[]>([])
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const loading = ref(false)
const conversationsError = ref('')
const unreadTotal = ref(0)
const conversationSearch = ref('')

// 搜索过滤会话列表
const filteredConversations = computed(() => {
  const keyword = conversationSearch.value.trim().toLowerCase()
  if (!keyword) return conversations.value
  return conversations.value.filter(conv =>
    conv.user?.nickname?.toLowerCase().includes(keyword)
  )
})

// 加载会话列表
const loadConversations = async () => {
  loading.value = true
  conversationsError.value = ''
  try {
    const response = await cachedRequest(
      () => socialApi.getConversations(),
      '/conversations'
    )
    conversations.value = response.data.data.list || []
  } catch {
    conversationsError.value = '加载会话列表失败，请稍后重试'
    conversations.value = []
  } finally {
    loading.value = false
  }
}

// 加载未读消息总数
const loadUnreadCount = async () => {
  try {
    const { data } = await socialApi.getUnreadCount()
    unreadTotal.value = data.data.count || 0
  } catch {
    unreadTotal.value = 0
  }
}

// 选择会话
const selectConversation = async (conv: Conversation) => {
  activeConversation.value = conv
  // 加载会话消息（后端使用对方用户ID，不是会话ID）
  const targetUserId = conv.user?.id
  if (!targetUserId) return
  try {
    const { data } = await socialApi.getMessages(targetUserId)
    messages.value = data.data.list || data.data.items || []
  } catch {
    messages.value = []
  }
  // 标记该会话已读
  if (conv.unreadCount > 0) {
    try {
      await socialApi.markConversationRead(targetUserId)
      conv.unreadCount = 0
      loadUnreadCount()
    } catch {
      // 标记已读失败
    }
  }
}

// 发送消息
const sendMessage = async (content: string) => {
  if (!activeConversation.value) return
  const targetUserId = activeConversation.value.user?.id
  if (!targetUserId) return
  try {
    const { data } = await socialApi.sendMessage(targetUserId, { content })
    messages.value.push(data.data)
    // 更新会话列表中最后一条消息
    const conv = conversations.value.find((c) => c.user?.id === targetUserId)
    if (conv) {
      conv.lastMessage = data.data
      conv.updatedAt = data.data.createdAt
    }
    // 重新加载未读消息总数
    loadUnreadCount()
  } catch {
    // 发送失败处理
  }
}

// WebSocket集成：监听新消息，更新会话列表和聊天窗口
if (import.meta.client) {
  // 监听会话列表变化，更新未读总数
  watch(
    () => conversations.value,
    () => {
      const total = conversations.value.reduce((sum, c) => sum + c.unreadCount, 0)
      unreadTotal.value = total
    },
    { deep: true }
  )

  // 监听 WebSocket 聊天消息，实时更新聊天界面
  const handleWsChat = ((event: CustomEvent) => {
    const chatData = event.detail
    if (!chatData) return
    // 如果当前正在与发送者聊天，将消息添加到聊天窗口
    if (activeConversation.value && activeConversation.value.user?.id === chatData.senderId) {
      messages.value.push({
        id: Date.now(),
        conversationId: activeConversation.value.id,
        senderId: chatData.senderId,
        sender: { id: chatData.senderId, nickname: chatData.senderNickname, avatar: chatData.senderAvatar } as any,
        content: chatData.content,
        type: 0,
        isRead: false,
        createdAt: chatData.createdAt || new Date().toISOString(),
      })
    }
    // 刷新会话列表
    loadConversations()
  }) as EventListener

  // 监听在线/离线状态变化
  const handleWsOnline = () => { loadConversations() }
  const handleWsOffline = () => { loadConversations() }

  onMounted(() => {
    window.addEventListener('ws:chat', handleWsChat)
    window.addEventListener('ws:online', handleWsOnline)
    window.addEventListener('ws:offline', handleWsOffline)
  })
  onUnmounted(() => {
    window.removeEventListener('ws:chat', handleWsChat)
    window.removeEventListener('ws:online', handleWsOnline)
    window.removeEventListener('ws:offline', handleWsOffline)
  })
}

// 页面加载时获取数据
onMounted(async () => {
  await loadConversations()
  await loadUnreadCount()
})

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / 86400000)

  if (days === 0) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 页面元信息
useHead({
  title: () => '私信' + ' - 知讯',
})
</script>
