<template>
  <!-- 私信页面 -->
  <div class="max-w-5xl mx-auto px-4 py-6">
    <div class="card overflow-hidden" style="height: calc(100vh - 8rem)">
      <div class="flex h-full">
        <!-- 左侧会话列表 -->
        <div class="w-full md:w-80 border-r border-gray-200 dark:border-gray-700 flex flex-col" :class="{ 'hidden md:flex': activeConversation }">
          <!-- 搜索会话 -->
          <div class="p-4 border-b border-gray-200 dark:border-gray-700">
            <input type="text" class="input text-sm" placeholder="搜索会话..." />
          </div>

          <!-- 会话列表 -->
          <div class="flex-1 overflow-y-auto">
            <button
              v-for="conv in conversations"
              :key="conv.id"
              class="w-full flex items-center space-x-3 p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              :class="{ 'bg-gray-50 dark:bg-gray-700/50': activeConversation?.id === conv.id }"
              @click="selectConversation(conv)"
            >
              <img :src="conv.user?.avatar || '/default-avatar.png'" class="w-12 h-12 rounded-full object-cover shrink-0" alt="头像" />
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-gray-900 dark:text-white">{{ conv.user?.nickname }}</span>
                  <span class="text-2xs text-gray-400">{{ formatTime(conv.updatedAt) }}</span>
                </div>
                <p class="text-xs text-gray-500 dark:text-gray-400 line-clamp-1 mt-0.5">{{ conv.lastMessage?.content }}</p>
              </div>
              <span v-if="conv.unreadCount > 0" class="w-5 h-5 bg-danger text-white text-2xs rounded-full flex items-center justify-center shrink-0">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </span>
            </button>

            <!-- 空状态 -->
            <EmptyState v-if="conversations.length === 0" title="暂无会话" description="开始一段新的对话吧" />
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
              <svg class="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-gray-500 dark:text-gray-400">选择一个会话开始聊天</p>
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

const conversations = ref<Conversation[]>([])
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const loading = ref(false)
const unreadTotal = ref(0)

// 加载会话列表
const loadConversations = async () => {
  loading.value = true
  try {
    const { data } = await socialApi.getConversations()
    conversations.value = data.data.list || []
  } catch {
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
  // 加载会话消息
  try {
    const { data } = await socialApi.getMessages(conv.id)
    messages.value = data.data.list || data.data.items || []
  } catch {
    messages.value = []
  }
  // 标记该会话已读
  if (conv.unreadCount > 0) {
    try {
      await socialApi.markConversationRead(conv.id)
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
  try {
    const { data } = await socialApi.sendMessage(activeConversation.value.id, { content })
    messages.value.push(data.data)
    // 更新会话列表中最后一条消息
    const conv = conversations.value.find((c) => c.id === activeConversation.value!.id)
    if (conv) {
      conv.lastMessage = data.data
      conv.updatedAt = data.data.createdAt
    }
  } catch {
    // 发送失败处理
  }
}

// WebSocket集成：监听新消息，更新会话列表
if (import.meta.client) {
  const { $ws } = useNuxtApp()
  watch(
    () => conversations.value,
    () => {
      // 会话列表变化时更新未读总数
      const total = conversations.value.reduce((sum, c) => sum + c.unreadCount, 0)
      unreadTotal.value = total
    },
    { deep: true }
  )
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
  title: '私信 - 知讯',
})
</script>
