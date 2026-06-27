<template>
  <!-- 私信首页：会话列表 + 桌面端聊天面板 -->
  <div class="messages-page h-[calc(100vh-3rem)] md:h-[calc(100vh-4rem)] flex bg-white dark:bg-gray-900">
    <!-- ==================== 左侧会话列表 ==================== -->
    <div
      class="conversation-panel flex-shrink-0 w-full md:w-[380px] lg:w-[400px] border-r border-slate-200 dark:border-gray-700 flex flex-col"
      :class="{ 'hidden md:flex': activeConversation && isDesktop }"
    >
      <!-- 标题栏 -->
      <div class="flex items-center justify-between px-3 py-2.5 border-b border-slate-200/60 dark:border-gray-700/60">
        <h1 class="text-lg font-bold text-slate-900 dark:text-white">私信</h1>
        <span v-if="unreadTotal > 0" class="badge-primary text-xs px-2 py-0.5 rounded-full">
          {{ unreadTotal > 99 ? '99+' : unreadTotal }} 条未读
        </span>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto custom-scrollbar">
        <!-- 加载状态 -->
        <div v-if="loadingConversations" class="flex items-center justify-center py-20">
          <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="conversations.length === 0" class="flex flex-col items-center justify-center py-20 px-4">
          <svg class="w-20 h-20 text-slate-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p class="text-slate-500 dark:text-gray-400 text-sm">暂无私信会话</p>
          <p class="text-slate-400 dark:text-gray-500 text-xs mt-1">与互关好友开始聊天吧</p>
        </div>

        <!-- 会话项 -->
        <div v-else>
          <button
            v-for="conv in conversations"
            :key="conv.id || conv.user.id"
            class="conversation-item w-full flex items-center gap-3 px-4 py-3 hover:bg-slate-50 dark:hover:bg-gray-800 transition-colors text-left border-b border-slate-100 dark:border-gray-800/50 relative"
            :class="{ 'bg-primary-50/50 dark:bg-primary-900/20': activeConversation?.user?.id === conv.user.id }"
            @click="openConversation(conv)"
          >
            <!-- 头像 + 在线状态 -->
            <div class="relative flex-shrink-0">
              <UserAvatar :src="conv.user.avatar" :alt="conv.user.nickname" size="lg" />
              <span
                v-if="isUserOnline(conv.user.id)"
                class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-green-500 border-2 border-white dark:border-gray-900 rounded-full"
              ></span>
            </div>

            <!-- 会话信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <span class="font-medium text-sm text-slate-900 dark:text-white truncate">
                  {{ conv.user.nickname }}
                </span>
                <span class="text-xs text-slate-400 dark:text-gray-500 flex-shrink-0 ml-2">
                  {{ formatRelativeTime(conv.lastMessage?.createdAt || conv.updatedAt) }}
                </span>
              </div>
              <div class="flex items-center justify-between mt-0.5">
                <p class="text-xs text-slate-500 dark:text-gray-400 truncate max-w-[200px]">
                  {{ getLastMessagePreview(conv) }}
                </p>
                <span
                  v-if="conv.unreadCount > 0"
                  class="badge-primary text-[10px] min-w-[18px] h-[18px] flex items-center justify-center rounded-full px-1 flex-shrink-0 ml-2"
                >
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </div>
            </div>
          </button>
        </div>
      </div>

      <!-- 底部刷新 -->
      <div class="px-4 py-2 border-t border-slate-200/60 dark:border-gray-700/60">
        <button
          class="w-full text-xs text-slate-400 dark:text-gray-500 hover:text-primary dark:hover:text-primary-400 transition-colors py-1"
          @click="refreshConversations"
        >
          刷新会话列表
        </button>
      </div>
    </div>

    <!-- ==================== 右侧聊天面板（桌面端） ==================== -->
    <div
      v-if="isDesktop"
      class="flex-1 flex flex-col min-w-0"
    >
      <!-- 有选中会话时显示聊天窗口 -->
      <template v-if="activeConversation">
        <ChatWindow
          :conversation="activeConversation"
          :messages="currentMessages"
          :loading-more="loadingMessages && currentPage > 1"
          :has-more="hasMoreMessages"
          @send="onSendMessage"
          @load-more="onLoadMore"
        />
      </template>

      <!-- 未选择会话时的引导 -->
      <div v-else class="flex-1 flex flex-col items-center justify-center bg-slate-50/50 dark:bg-gray-800/30">
        <svg class="w-24 h-24 text-slate-200 dark:text-gray-700 mb-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
        </svg>
        <p class="text-slate-400 dark:text-gray-500 text-sm">选择左侧会话开始聊天</p>
        <p class="text-slate-300 dark:text-gray-600 text-xs mt-1">与您的互关好友畅快交流</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Conversation } from '~/types'
import { useMessageStore } from '~/stores/message'

definePageMeta({ middleware: 'auth' })

const messageStore = useMessageStore()
const { isDesktop } = useBreakpoints()

const {
  conversations,
  loadingConversations,
  currentMessages,
  loadingMessages,
  hasMoreMessages,
  currentPage,
  unreadTotal,
  currentChatUserId,
} = storeToRefs(messageStore)

const activeConversation = ref<Conversation | null>(null)

// ==================== 初始化 ====================
onMounted(async () => {
  await messageStore.fetchConversations()
  await messageStore.fetchUnreadCount()
})

// ==================== 打开会话 ====================
const openConversation = async (conv: Conversation) => {
  activeConversation.value = conv
  messageStore.currentChatUserId = conv.user.id
  await messageStore.fetchMessages(conv.user.id)
  await messageStore.markRead(conv.user.id)
}

// ==================== 发送消息 ====================
const onSendMessage = async (content: string) => {
  if (!activeConversation.value) return
  await messageStore.sendMessage(activeConversation.value.user.id, content)
}

// ==================== 加载更多历史消息 ====================
const onLoadMore = async () => {
  if (!activeConversation.value || !hasMoreMessages.value) return
  await messageStore.loadMoreMessages(activeConversation.value.user.id)
}

// ==================== 刷新会话列表 ====================
const refreshConversations = async () => {
  await messageStore.fetchConversations()
  await messageStore.fetchUnreadCount()
}

// ==================== 辅助方法 ====================
const isUserOnline = (userId: number) => messageStore.isUserOnline(userId)

const getLastMessagePreview = (conv: Conversation) => {
  if (!conv.lastMessage) return ''
  const content = conv.lastMessage.content || ''
  return content.length > 30 ? content.slice(0, 30) + '...' : content
}

/** 相对时间格式化 */
const formatRelativeTime = (timeStr: string) => {
  if (!timeStr) return ''
  const now = Date.now()
  const time = new Date(timeStr).getTime()
  const diff = now - time

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`

  const date = new Date(timeStr)
  const nowDate = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')

  if (date.getFullYear() === nowDate.getFullYear()) {
    if (date.getDate() === nowDate.getDate() - 1) {
      return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
</script>

<style scoped>
.messages-page {
  margin: -2.5rem 0 -4rem 0;
  height: 100vh;
}

@media (min-width: 768px) {
  .messages-page {
    margin: -2.5rem 0 0 0;
  }
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #4b5563;
}

.conversation-item {
  transition: background-color 0.15s ease;
}
.conversation-item:active {
  background-color: #f1f5f9;
}
.dark .conversation-item:active {
  background-color: #1f2937;
}
</style>
