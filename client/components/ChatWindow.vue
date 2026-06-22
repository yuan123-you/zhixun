<template>
  <!-- 聊天窗口组件 -->
  <div class="flex flex-col h-full bg-white dark:bg-gray-800">
    <!-- 聊天头部 -->
    <div v-if="conversation" class="flex items-center p-4 border-b border-gray-200 dark:border-gray-700">
      <button class="md:hidden p-2 mr-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="$emit('back')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <img :src="conversation.user?.avatar || '/default-avatar.png'" class="w-10 h-10 rounded-full object-cover" alt="头像" />
      <div class="ml-3">
        <p class="font-medium text-gray-900 dark:text-white">{{ conversation.user?.nickname }}</p>
        <p class="text-xs text-green-500">在线</p>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="messageListRef" class="flex-1 overflow-y-auto p-4 space-y-4">
      <div v-for="message in messages" :key="message.id" class="flex" :class="isMine(message) ? 'justify-end' : 'justify-start'">
        <!-- 对方消息 -->
        <div v-if="!isMine(message)" class="flex items-end space-x-2 max-w-[70%]">
          <img :src="message.sender?.avatar || '/default-avatar.png'" class="w-8 h-8 rounded-full object-cover shrink-0" alt="头像" />
          <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-bl-sm px-4 py-2">
            <p class="text-sm text-gray-900 dark:text-white">{{ message.content }}</p>
            <span class="text-2xs text-gray-400 mt-1 block">{{ formatTime(message.createdAt) }}</span>
          </div>
        </div>

        <!-- 我的消息 -->
        <div v-else class="flex items-end space-x-2 max-w-[70%]">
          <div class="bg-primary text-white rounded-2xl rounded-br-sm px-4 py-2">
            <p class="text-sm">{{ message.content }}</p>
            <span class="text-2xs text-primary-200 mt-1 block">{{ formatTime(message.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息输入框 -->
    <div class="p-4 border-t border-gray-200 dark:border-gray-700">
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

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 监听消息变化，自动滚动到底部
watch(() => props.messages.length, () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
})
</script>
