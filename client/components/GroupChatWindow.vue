<template>
  <div class="flex flex-col h-full bg-white dark:bg-gray-800 rounded-2xl shadow-sm">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700">
      <div class="flex items-center gap-3">
        <button @click="$emit('close')" class="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg">
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </button>
        <div>
          <h3 class="font-semibold text-gray-900 dark:text-white">{{ group?.name }}</h3>
          <p class="text-xs text-gray-400">{{ group?.memberCount }} 名成员</p>
        </div>
      </div>
    </div>

    <!-- Messages -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-3 space-y-3" @scroll="onScroll">
      <div v-if="hasMore" class="text-center">
        <button @click="loadMore" :disabled="loadingMore" class="text-xs text-blue-600 hover:text-blue-700 py-1">
          {{ loadingMore ? '加载中...' : '加载更多消息' }}
        </button>
      </div>
      <div v-for="msg in messages" :key="msg.id"
        :class="msg.senderId === currentUserId ? 'flex justify-end' : 'flex justify-start'">
        <div v-if="msg.senderId !== currentUserId" class="flex items-start gap-2 max-w-[75%]">
          <img :src="msg.senderAvatar || '/avatar-placeholder.png'" class="w-8 h-8 rounded-full flex-shrink-0" />
          <div>
            <span class="text-xs text-gray-400 ml-1">{{ msg.senderName }}</span>
            <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-sm px-3 py-2 text-sm text-gray-900 dark:text-white">
              {{ msg.content }}
            </div>
          </div>
        </div>
        <div v-else class="max-w-[75%]">
          <div class="bg-blue-600 text-white rounded-2xl rounded-tr-sm px-3 py-2 text-sm">{{ msg.content }}</div>
        </div>
      </div>
      <div v-if="messages.length === 0" class="text-center py-10 text-gray-400 text-sm">暂无消息，发送第一条吧</div>
    </div>

    <!-- Input -->
    <div class="px-4 py-3 border-t border-gray-100 dark:border-gray-700 flex items-end gap-2">
      <textarea v-model="inputText" @keydown.enter.exact.prevent="sendMsg" placeholder="输入消息..."
        rows="1" class="flex-1 resize-none px-3 py-2 bg-gray-50 dark:bg-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 max-h-32"></textarea>
      <button @click="sendMsg" :disabled="!inputText.trim()"
        class="px-4 py-2 bg-blue-600 text-white rounded-xl text-sm hover:bg-blue-700 disabled:opacity-50 transition">发送</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { groupApi } from '~/api/group'
import type { GroupInfo, GroupMessage } from '~/api/group'

const props = defineProps<{ group: GroupInfo | null; currentUserId: number }>()
const emit = defineEmits(['close'])

const messages = ref<GroupMessage[]>([])
const inputText = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
let offset = 0

async function loadMessages(groupId: number) {
  const res = await groupApi.getMessages(groupId, offset, 50)
  const data = res.data.data
  if (offset === 0) messages.value = data
  else messages.value = [...data, ...messages.value]
  hasMore.value = data.length === 50
  if (offset === 0) { await nextTick(); scrollToBottom() }
}

async function loadMore() {
  loadingMore.value = true
  offset += 50
  await loadMessages(props.group!.id)
  loadingMore.value = false
}

async function sendMsg() {
  if (!inputText.value.trim() || !props.group) return
  await groupApi.sendMessage(props.group.id, inputText.value.trim())
  inputText.value = ''
  offset = 0
  await loadMessages(props.group.id)
}

function scrollToBottom() {
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

function onScroll() {
  if (msgContainer.value && msgContainer.value.scrollTop === 0 && hasMore.value) loadMore()
}

watch(() => props.group?.id, (id) => {
  if (id) { offset = 0; loadMessages(id) }
}, { immediate: true })
</script>
