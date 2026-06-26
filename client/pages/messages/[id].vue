<template>
  <!-- 私信聊天页面（移动端全屏 / 桌面端备选入口） -->
  <div class="chat-page h-[calc(100vh-3rem)] md:h-[calc(100vh-4rem)] flex flex-col bg-white dark:bg-gray-900">
    <!-- 聊天头部 -->
    <div class="flex items-center gap-3 px-4 py-3 border-b border-slate-200/60 dark:border-gray-700/60 bg-white dark:bg-gray-900 flex-shrink-0">
      <!-- 返回按钮 -->
      <button
        class="p-2 -ml-2 text-slate-600 dark:text-gray-400 hover:bg-slate-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
        @click="goBack"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <!-- 用户信息 -->
      <div class="flex items-center gap-3 flex-1 min-w-0">
        <div class="relative flex-shrink-0">
          <UserAvatar :src="targetUser?.avatar" :alt="targetUser?.nickname" size="md" />
          <span
            v-if="isOnline"
            class="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-500 border-2 border-white dark:border-gray-900 rounded-full"
          ></span>
        </div>
        <div class="min-w-0">
          <p class="font-medium text-sm text-slate-900 dark:text-white truncate">
            {{ targetUser?.nickname || '加载中...' }}
          </p>
          <p class="text-xs" :class="isOnline ? 'text-green-500' : 'text-slate-400 dark:text-gray-500'">
            {{ isOnline ? '在线' : statusText }}
          </p>
        </div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div
      ref="messageContainerRef"
      class="flex-1 overflow-y-auto px-3 py-2 space-y-3 custom-scrollbar"
    >
      <!-- 加载更早消息的指示器 -->
      <div v-if="loadingMessages && currentPage > 1" class="flex justify-center py-3">
        <div class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- 没有更多消息 -->
      <div v-else-if="!hasMoreMessages && currentMessages.length > 0" class="flex justify-center py-3">
        <span class="text-xs text-slate-300 dark:text-gray-600">—— 以上是全部消息 ——</span>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingMessages && currentPage === 1" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loadingMessages && currentMessages.length === 0" class="flex flex-col items-center justify-center py-20">
        <svg class="w-16 h-16 text-slate-200 dark:text-gray-700 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <p class="text-slate-400 dark:text-gray-500 text-sm">发送第一条消息吧</p>
      </div>

      <!-- 消息气泡 -->
      <template v-for="(msg, idx) in currentMessages" :key="msg.id || idx">
        <!-- 时间分隔线（与前一条消息间隔超过5分钟则显示） -->
        <div
          v-if="idx === 0 || getTimeDiff(currentMessages[idx - 1]?.createdAt, msg.createdAt) > 5"
          class="flex justify-center"
        >
          <span class="text-[11px] text-slate-400 dark:text-gray-500 bg-slate-100 dark:bg-gray-800 px-3 py-0.5 rounded-full">
            {{ formatMessageTime(msg.createdAt) }}
          </span>
        </div>

        <!-- 对方消息 -->
        <div v-if="msg.senderId !== myUserId" class="flex items-start gap-2 max-w-[75%]">
          <UserAvatar :src="msg.sender?.avatar" :alt="msg.sender?.nickname" size="sm" class="flex-shrink-0 mt-0.5" />
          <div>
            <div class="bg-slate-100 dark:bg-gray-800 rounded-2xl rounded-tl-sm px-3 py-2">
              <p class="text-sm text-slate-900 dark:text-gray-100 whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
            <span class="text-[10px] text-slate-400 dark:text-gray-500 mt-0.5 block">
              {{ formatMessageTime(msg.createdAt) }}
            </span>
          </div>
        </div>

        <!-- 我的消息 -->
        <div v-else class="flex justify-end">
          <div class="max-w-[75%]">
            <div class="bg-primary text-white rounded-2xl rounded-tr-sm px-3 py-2">
              <p class="text-sm whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
            <span class="text-[10px] text-slate-400 dark:text-gray-500 mt-0.5 block text-right">
              {{ formatMessageTime(msg.createdAt) }}
            </span>
          </div>
        </div>
      </template>

      <!-- 底部哨兵：用于自动滚到底 -->
      <div ref="bottomSentinelRef" class="h-0"></div>
    </div>

    <!-- 消息输入区 -->
    <div class="flex-shrink-0 border-t border-slate-200/60 dark:border-gray-700/60 bg-white dark:bg-gray-900 px-3 py-2">
      <div class="flex items-center gap-2">
        <input
          ref="inputRef"
          v-model="inputContent"
          type="text"
          class="flex-1 h-10 px-4 bg-slate-100 dark:bg-gray-800 border-0 rounded-full text-sm text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/30 transition-shadow"
          placeholder="输入消息..."
          :disabled="sending"
          @keydown.enter="onSend"
        />
        <button
          class="flex-shrink-0 w-10 h-10 flex items-center justify-center bg-primary hover:bg-primary-600 disabled:bg-slate-300 dark:disabled:bg-gray-700 text-white rounded-full transition-colors"
          :disabled="!inputContent.trim() || sending"
          @click="onSend"
        >
          <svg v-if="!sending" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
          <div v-else class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMessageStore } from '~/stores/message'

definePageMeta({ middleware: 'auth' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const {
  currentMessages,
  loadingMessages,
  hasMoreMessages,
  currentPage,
  conversations,
} = storeToRefs(messageStore)

const inputContent = ref('')
const sending = ref(false)
const messageContainerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const bottomSentinelRef = ref<HTMLElement | null>(null)
const isOnline = ref(false)
const statusText = ref('离线')
const isInitialLoad = ref(true)

const targetUserId = computed(() => Number(route.params.id))
const myUserId = computed(() => userStore.userInfo?.id)

// 从会话列表中找到目标用户信息
const targetUser = computed(() => {
  const conv = conversations.value.find(c => c.user.id === targetUserId.value)
  return conv?.user || null
})

// 如果不在会话列表中，构造基本用户信息
const fallbackUser = computed(() => {
  if (targetUser.value) return targetUser.value
  return {
    id: targetUserId.value,
    nickname: `用户 ${targetUserId.value}`,
    avatar: '',
  }
})

// ==================== 生命周期 ====================
onMounted(async () => {
  // 加载会话列表（获取用户信息）
  if (conversations.value.length === 0) {
    await messageStore.fetchConversations()
  }

  // 加载消息
  messageStore.currentChatUserId = targetUserId.value
  await messageStore.fetchMessages(targetUserId.value)
  await messageStore.markRead(targetUserId.value)

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 获取在线状态
  fetchOnlineStatus()

  // 聚焦输入框
  setTimeout(() => {
    inputRef.value?.focus()
    isInitialLoad.value = false
  }, 300)
})

onUnmounted(() => {
  messageStore.currentChatUserId = null
})

// ==================== 发送消息 ====================
const onSend = async () => {
  const content = inputContent.value.trim()
  if (!content || sending.value) return

  sending.value = true
  inputContent.value = ''

  try {
    await messageStore.sendMessage(targetUserId.value, content)
    await nextTick()
    scrollToBottom()
  } catch {
    inputContent.value = content // 恢复内容
  } finally {
    sending.value = false
    inputRef.value?.focus()
  }
}

// ==================== 滚动加载历史消息 ====================
let scrollLoadLock = false

const handleScroll = () => {
  const el = messageContainerRef.value
  if (!el || scrollLoadLock) return

  // 滚动到顶部 80px 以内时触发加载更多
  if (el.scrollTop < 80 && hasMoreMessages.value && !loadingMessages.value) {
    scrollLoadLock = true
    const prevHeight = el.scrollHeight
    messageStore.loadMoreMessages(targetUserId.value).then(() => {
      nextTick(() => {
        if (el) {
          el.scrollTop = el.scrollHeight - prevHeight
        }
        scrollLoadLock = false
      })
    })
  }
}

// ==================== WebSocket 实时消息监听 ====================
// 新消息到达时自动滚动到底部
watch(
  () => currentMessages.value.length,
  () => {
    if (!isInitialLoad.value) {
      nextTick(() => scrollToBottom())
    }
  }
)

// ==================== 辅助方法 ====================
const scrollToBottom = () => {
  bottomSentinelRef.value?.scrollIntoView({ behavior: 'instant' })
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/messages')
  }
}

const fetchOnlineStatus = async () => {
  try {
    const { socialApi } = await import('~/api')
    const { data } = await socialApi.getOnlineStatus(targetUserId.value)
    if (data.code === 0 && data.data) {
      const status = data.data[targetUserId.value]
      isOnline.value = status ?? false
      statusText.value = status ? '在线' : '离线'
    }
  } catch {
    isOnline.value = false
    statusText.value = '离线'
  }
}

/** 消息时间格式化 */
const formatMessageTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')

  const isToday = date.toDateString() === now.toDateString()
  const isYesterday = new Date(now.getTime() - 86400000).toDateString() === date.toDateString()

  if (isToday) return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (isYesterday) return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`

  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 两条消息之间的时间差（分钟） */
const getTimeDiff = (prevTime: string, currTime: string) => {
  if (!prevTime || !currTime) return 0
  return Math.abs(new Date(currTime).getTime() - new Date(prevTime).getTime()) / 60000
}

// 挂载滚动监听
onMounted(() => {
  nextTick(() => {
    messageContainerRef.value?.addEventListener('scroll', handleScroll, { passive: true })
  })
})

onUnmounted(() => {
  messageContainerRef.value?.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.chat-page {
  margin: -3rem 0 -4rem 0;
  height: 100vh;
}

@media (min-width: 768px) {
  .chat-page {
    margin: -4rem auto 0 auto;
    max-width: 800px;
    border-left: 1px solid #e2e8f0;
    border-right: 1px solid #e2e8f0;
  }
  .dark .chat-page {
    border-color: #374151;
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
</style>
