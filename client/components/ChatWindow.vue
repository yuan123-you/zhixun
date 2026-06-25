<template>
  <!-- 聊天窗口组件 -->
  <div class="flex flex-col h-full bg-white">
    <!-- 聊天头部 -->
    <div v-if="conversation" class="flex items-center p-4 border-b border-slate-200/60">
      <button class="md:hidden p-2 mr-2 text-slate-600 hover:bg-slate-50 rounded-lg" @click="$emit('back')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <UserAvatar :src="conversation.user?.avatar" alt="头像" size="md" />
      <div class="ml-3">
        <p class="font-medium text-slate-900">{{ conversation.user?.nickname }}</p>
        <p class="text-xs" :class="isOnline ? 'text-green-500' : 'text-gray-400'">{{ isOnline ? '在线' : '离线' }}</p>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="messageListRef" class="flex-1 overflow-y-auto p-2 space-y-2">
      <div v-for="message in messages" :key="message.id" class="flex" :class="isMine(message) ? 'justify-end' : 'justify-start'">
        <!-- 对方消息 -->
        <div v-if="!isMine(message)" class="flex items-end space-x-2 max-w-[70%]">
          <UserAvatar :src="message.sender?.avatar" alt="头像" size="sm" />
          <div class="bg-slate-100 rounded-2xl rounded-bl-sm px-3 py-1.5">
            <p class="text-sm text-slate-900">{{ message.content }}</p>
            <span class="text-[10px] text-gray-400 mt-1 block">{{ formatTime(message.createdAt) }}</span>
          </div>
        </div>

        <!-- 我的消息 -->
        <div v-else class="flex items-end space-x-2 max-w-[70%]">
          <div class="bg-primary text-white rounded-2xl rounded-br-sm px-4 py-2">
            <p class="text-sm">{{ message.content }}</p>
            <span class="text-[10px] text-primary-200 mt-1 block">{{ formatTime(message.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息输入框 -->
    <div class="p-2 border-t border-slate-200/60">
      <div class="flex items-center space-x-2">
        <input
          v-model="inputContent"
          type="text"
          class="input flex-1"
          placeholder="输入消息..."
          @keydown.enter="sendMessage"
        />
        <button class="btn-primary" :disabled="!inputContent.trim()" @click="sendMessage">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 聊天窗口组件 */
import type { Conversation, Message } from '~/types'
import { socialApi } from '~/api'

const props = defineProps<{
  conversation: Conversation | null
  messages: Message[]
}>()

const emit = defineEmits<{
  send: [content: string]
  back: []
}>()

const userStore = useUserStore()
const inputContent = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const isOnline = ref(false)

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
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 格式化时间：YYYY-MM-DD HH:MM:SS
const formatTime = (time: string) => {
  const date = new Date(time)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// 查询对方用户在线状态
const fetchOnlineStatus = async () => {
  if (!props.conversation?.user?.id) return
  try {
    const { data } = await socialApi.getOnlineStatus(props.conversation.user.id)
    isOnline.value = data.data?.[props.conversation.user.id] ?? false
  } catch {
    isOnline.value = false
  }
}

// 监听会话变化，刷新在线状态
watch(() => props.conversation?.user?.id, (newId) => {
  if (newId) {
    fetchOnlineStatus()
  }
}, { immediate: true })

// 监听消息变化，自动滚动到底部
watch(() => props.messages.length, () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
})
</script>
