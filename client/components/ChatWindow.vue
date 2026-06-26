<template>
  <!-- 聊天窗口组件（支持滚动加载历史消息） -->
  <div class="flex flex-col h-full bg-white dark:bg-gray-900">
    <!-- 聊天头部 -->
    <div v-if="conversation" class="flex items-center px-4 py-3 border-b border-slate-200/60 dark:border-gray-700/60 flex-shrink-0">
      <button class="md:hidden p-2 mr-2 text-slate-600 dark:text-gray-400 hover:bg-slate-100 dark:hover:bg-gray-800 rounded-lg" @click="$emit('back')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="relative flex-shrink-0">
        <UserAvatar :src="conversation.user?.avatar" alt="头像" size="md" />
        <span
          v-if="isOnline"
          class="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-500 border-2 border-white dark:border-gray-900 rounded-full"
        ></span>
      </div>
      <div class="ml-3 min-w-0">
        <p class="font-medium text-sm text-slate-900 dark:text-white truncate">{{ conversation.user?.nickname }}</p>
        <p class="text-xs" :class="isOnline ? 'text-green-500' : 'text-slate-400 dark:text-gray-500'">{{ isOnline ? '在线' : '离线' }}</p>
      </div>
    </div>

    <!-- 消息列表（带滚动加载） -->
    <div ref="messageListRef" class="flex-1 overflow-y-auto px-3 py-2 space-y-2 custom-scrollbar">
      <!-- 顶部加载指示器 -->
      <div ref="topSentinelRef" class="flex justify-center py-2">
        <div v-if="loadingMore" class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        <span v-else-if="!hasMore" class="text-xs text-slate-300 dark:text-gray-600">—— 以上是全部消息 ——</span>
      </div>

      <!-- 消息列表 -->
      <div v-for="message in messages" :key="message.id" class="flex" :class="isMine(message) ? 'justify-end' : 'justify-start'">
        <!-- 对方消息 -->
        <div v-if="!isMine(message)" class="flex items-end gap-2 max-w-[70%]">
          <UserAvatar :src="message.sender?.avatar" alt="头像" size="sm" class="flex-shrink-0" />
          <div>
            <div class="bg-slate-100 dark:bg-gray-800 rounded-2xl rounded-bl-sm px-3 py-2">
              <p class="text-sm text-slate-900 dark:text-gray-100 whitespace-pre-wrap break-words">{{ message.content }}</p>
            </div>
            <span class="text-[10px] text-slate-400 dark:text-gray-500 mt-0.5 block">{{ formatTime(message.createdAt) }}</span>
          </div>
        </div>

        <!-- 我的消息 -->
        <div v-else class="max-w-[70%]">
          <div class="bg-primary text-white rounded-2xl rounded-tr-sm px-3 py-2">
            <p class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
          </div>
          <span class="text-[10px] text-slate-400 dark:text-gray-500 mt-0.5 block text-right">{{ formatTime(message.createdAt) }}</span>
        </div>
      </div>

      <!-- 消息为空 -->
      <div v-if="!loadingMore && messages.length === 0" class="flex flex-col items-center justify-center py-16">
        <p class="text-slate-400 dark:text-gray-500 text-sm">暂无消息，发送第一条吧</p>
      </div>

      <!-- 底部锚点（用于新消息自动滚到底） -->
      <div ref="bottomRef" class="h-0"></div>
    </div>

    <!-- 消息输入框 -->
    <div class="flex-shrink-0 p-3 border-t border-slate-200/60 dark:border-gray-700/60">
      <div class="flex items-center gap-2">
        <input
          v-model="inputContent"
          type="text"
          class="flex-1 h-10 px-4 bg-slate-100 dark:bg-gray-800 border-0 rounded-full text-sm text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/30 transition-shadow"
          placeholder="输入消息..."
          @keydown.enter="sendMessage"
        />
        <button
          class="flex-shrink-0 w-10 h-10 flex items-center justify-center bg-primary hover:bg-primary-600 disabled:bg-slate-300 dark:disabled:bg-gray-700 text-white rounded-full transition-colors"
          :disabled="!inputContent.trim()"
          @click="sendMessage"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 聊天窗口组件 —— 支持滚动加载历史消息、实时在线状态、相对时间显示 */
import type { Conversation, Message } from '~/types'
import { socialApi } from '~/api'

const props = withDefaults(defineProps<{
  conversation: Conversation | null
  messages: Message[]
  /** 是否正在加载更早的消息 */
  loadingMore?: boolean
  /** 是否还有更多历史消息（false 时显示"全部消息"提示） */
  hasMore?: boolean
}>(), {
  loadingMore: false,
  hasMore: true,
})

const emit = defineEmits<{
  send: [content: string]
  back: []
  /** 滚动到顶部时触发，用于加载更早的消息 */
  loadMore: []
}>()

const userStore = useUserStore()
const inputContent = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const topSentinelRef = ref<HTMLElement | null>(null)
const bottomRef = ref<HTMLElement | null>(null)
const isOnline = ref(false)
const isAtBottom = ref(true)

// 判断是否是我的消息
const isMine = (message: Message) => {
  return message.senderId === userStore.userInfo?.id
}

// 发送消息
const sendMessage = () => {
  if (!inputContent.value.trim()) return
  emit('send', inputContent.value.trim())
  inputContent.value = ''
  // 滚动到底部
  nextTick(() => {
    scrollToBottom()
  })
}

// ==================== 时间格式化 ====================
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')

  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now.getTime() - 86400000)
  const isYesterday = date.toDateString() === yesterday.toDateString()

  const timeStr = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  if (isToday) return timeStr
  if (isYesterday) return `昨天 ${timeStr}`

  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
}

// ==================== 在线状态 ====================
const fetchOnlineStatus = async () => {
  if (!props.conversation?.user?.id) return
  try {
    const { data } = await socialApi.getOnlineStatus(props.conversation.user.id)
    isOnline.value = data.data?.[props.conversation.user.id] ?? false
  } catch {
    isOnline.value = false
  }
}

watch(() => props.conversation?.user?.id, (newId) => {
  if (newId) fetchOnlineStatus()
}, { immediate: true })

// ==================== 滚动到底部 ====================
const scrollToBottom = () => {
  bottomRef.value?.scrollIntoView({ behavior: 'instant' })
}

// ==================== 新消息自动滚动 ====================
let prevMessageCount = 0
watch(
  () => props.messages.length,
  (newLen) => {
    // 如果是新增消息（不是加载历史），自动滚到底部
    if (newLen > prevMessageCount && !props.loadingMore) {
      nextTick(() => scrollToBottom())
    }
    prevMessageCount = newLen
  }
)

// ==================== IntersectionObserver：检测滚动到顶部 ====================
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (topSentinelRef.value) {
    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !props.loadingMore) {
          emit('loadMore')
        }
      },
      { threshold: 0.1 }
    )
    observer.observe(topSentinelRef.value)
  }
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})
</script>

<style scoped>
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
