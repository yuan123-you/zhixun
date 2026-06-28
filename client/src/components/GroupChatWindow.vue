<template>
  <!-- 群组聊天窗口（统一交互风格） -->
  <div class="group-chat">
    <div class="group-chat-header">
      <button class="back-btn-gc" @click="$emit('close')" aria-label="返回">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div>
        <h3 class="group-chat-name">{{ group?.name }}</h3>
        <p class="group-chat-meta">
          <span v-if="group?.groupNumber" class="group-number">群号: {{ group.groupNumber }} · </span>
          {{ group?.memberCount }} 名成员
        </p>
      </div>
    </div>

    <div ref="msgContainer" class="group-chat-msgs" @scroll="onScroll">
      <!-- 加载更多历史消息指示器 -->
      <div v-if="loadingMore" class="flex justify-center py-3">
        <svg class="animate-spin w-4 h-4 text-[var(--zh-text-tertiary)]" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <span class="text-xs text-[var(--zh-text-tertiary)] ml-2">加载中...</span>
      </div>
      <div v-else-if="!hasMore && messages.length > 0" class="text-center py-3">
        <span class="text-xs text-[var(--zh-text-tertiary)]">—— 以上是全部消息 ——</span>
      </div>

      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.senderId === currentUserId ? 'mine' : 'other'">
        <template v-if="msg.senderId !== currentUserId">
          <img :src="msg.senderAvatar || '/avatar-placeholder.png'" class="msg-avatar" />
          <div class="msg-bubble-wrap">
            <span class="msg-author">{{ msg.senderName }}</span>
            <div class="msg-bubble msg-other-bubble">{{ msg.content }}</div>
          </div>
        </template>
        <div v-else class="msg-bubble msg-mine-bubble">{{ msg.content }}</div>
      </div>
      <div v-if="messages.length === 0" class="group-chat-empty">暂无消息，发送第一条吧</div>
    </div>

    <!-- 统一输入框样式 - 匹配登录页交互 -->
    <div class="group-chat-input">
      <div class="input-field-gc" :class="{ focused: inputFocused }">
        <input
          v-model="inputText"
          type="text"
          class="input-control-gc"
          placeholder="输入消息..."
          @focus="inputFocused = true"
          @blur="inputFocused = false"
          @keydown.enter.exact="sendMsg"
        />
      </div>
      <button class="send-btn-gc" :disabled="!inputText.trim()" @click="sendMsg" aria-label="发送">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage } from '@/api/group'
import { sanitizeText } from '@/utils/sanitize'

const props = defineProps<{ group: GroupInfo | null; currentUserId: number }>()
const emit = defineEmits(['close'])

const messages = ref<GroupMessage[]>([])
const inputText = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
const inputFocused = ref(false)
let offset = 0

async function loadMessages(groupId: number) {
  const res = await groupApi.getMessages(groupId, offset, 50)
  const data = res.data.data
  if (offset === 0) messages.value = data
  else messages.value = [...data, ...messages.value]
  hasMore.value = data.length === 50
  if (offset === 0) { await nextTick(); scrollToBottom() }
}

async function sendMsg() {
  const rawText = inputText.value.trim()
  if (!rawText || !props.group) return
  // 客户端输入净化：移除所有 HTML 标签，防止 XSS
  const sanitizedText = sanitizeText(rawText)
  await groupApi.sendMessage(props.group.id, sanitizedText)
  inputText.value = ''
  offset = 0
  await loadMessages(props.group.id)
}

function scrollToBottom() {
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

// 滚动到顶部时自动加载更多历史消息
function onScroll() {
  const el = msgContainer.value
  if (!el || !hasMore.value || loadingMore.value) return
  if (el.scrollTop < 50) {
    loadingMore.value = true
    const prevHeight = el.scrollHeight
    offset += 50
    loadMessages(props.group!.id).then(() => {
      nextTick(() => {
        if (el) el.scrollTop = el.scrollHeight - prevHeight
        loadingMore.value = false
      })
    })
  }
}

watch(() => props.group?.id, (id) => {
  if (id) { offset = 0; loadMessages(id) }
}, { immediate: true })
</script>

<style scoped>
.group-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg-elevated);
  border-radius: 16px;
  overflow: hidden;
}
.group-chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--zh-border);
  flex-shrink: 0;
}
.back-btn-gc {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}
.back-btn-gc:hover {
  background: var(--zh-bg-hover);
}
.group-chat-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--zh-text);
}
.group-chat-meta {
  font-size: 12px;
  color: var(--zh-text-tertiary);
}
.group-number { font-family: monospace; }
.group-chat-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.msg-row { display: flex; }
.msg-row.other { justify-content: flex-start; }
.msg-row.mine { justify-content: flex-end; }
.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-right: 8px;
}
.msg-author {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  margin-left: 4px;
}
.msg-bubble-wrap { max-width: 75%; }
.msg-bubble {
  padding: 8px 12px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
}
.msg-other-bubble {
  background: var(--zh-bg-hover);
  color: var(--zh-text);
  border-bottom-left-radius: 4px;
}
.msg-mine-bubble {
  background: var(--zh-primary-gradient);
  color: #fff;
  border-bottom-right-radius: 4px;
  max-width: 75%;
}
.group-chat-empty {
  text-align: center;
  padding: 40px 0;
  font-size: 14px;
  color: var(--zh-text-tertiary);
}

/* 统一输入框样式 - 匹配登录页交互 */
.group-chat-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--zh-border);
  background: var(--zh-bg-elevated);
  flex-shrink: 0;
}

.input-field-gc {
  display: flex;
  align-items: center;
  flex: 1;
  height: 42px;
  border: 1.5px solid var(--zh-border);
  border-radius: 24px;
  background: var(--zh-bg);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}
.input-field-gc.focused {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.input-control-gc {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 16px;
  font-size: 14px;
  color: var(--zh-text);
  font-family: inherit;
}
.input-control-gc::placeholder {
  color: var(--zh-text-placeholder);
  font-size: 13px;
}

.send-btn-gc {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  background: var(--zh-primary-gradient);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
  flex-shrink: 0;
}
.send-btn-gc:hover:not(:disabled) {
  opacity: 0.9;
  transform: scale(1.05);
}
.send-btn-gc:active:not(:disabled) {
  transform: scale(0.95);
}
.send-btn-gc:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
