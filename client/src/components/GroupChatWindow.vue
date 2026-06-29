<template>
  <!-- QQ风格群组聊天窗口 -->
  <div class="qq-chat">
    <!-- 头部 -->
    <div class="qq-chat-header">
      <button class="qq-back-btn" @click="$emit('close')" aria-label="返回">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="qq-header-info">
        <h3 class="qq-chat-title">{{ group?.name }}</h3>
        <p class="qq-chat-subtitle">
          <span v-if="group?.groupNumber" class="qq-group-num">{{ group.groupNumber }}</span>
          <span class="qq-member-count">{{ group?.memberCount }}人</span>
        </p>
      </div>
      <button class="qq-members-btn" @click="$emit('toggleMembers')" aria-label="成员列表">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      </button>
    </div>

    <!-- 消息列表 -->
    <div ref="msgContainer" class="qq-chat-msgs" @scroll="onScroll">
      <!-- 加载更多 -->
      <div v-if="loadingMore" class="qq-load-more">
        <svg class="animate-spin w-4 h-4 text-[var(--zh-text-tertiary)]" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <span class="text-xs text-[var(--zh-text-tertiary)] ml-1.5">加载中...</span>
      </div>
      <div v-else-if="!hasMore && messages.length > 0" class="qq-no-more">
        <span>—— 以上是全部消息 ——</span>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <!-- 时间分隔线 -->
        <div v-if="shouldShowTimeSeparator(idx)" class="qq-time-sep">
          <span>{{ formatTime(msg.createdAt) }}</span>
        </div>

        <!-- 系统消息 -->
        <div v-if="msg.messageType === 'system'" class="qq-system-msg">
          <span>{{ msg.content }}</span>
        </div>

        <!-- 普通消息 -->
        <div v-else class="qq-msg-row" :class="{ mine: msg.senderId === currentUserId }">
          <!-- 他人消息 -->
          <template v-if="msg.senderId !== currentUserId">
            <img :src="msg.senderAvatar || defaultAvatar" class="qq-avatar" :alt="msg.senderName" />
            <div class="qq-msg-wrap">
              <span class="qq-msg-name">{{ msg.senderName || '未知用户' }}</span>
              <div class="qq-bubble qq-bubble-other">
                <span v-if="msg.messageType === 'image'" class="qq-img-msg">
                  <img :src="msg.content" alt="图片" class="qq-msg-image" />
                </span>
                <span v-else>{{ msg.content }}</span>
              </div>
              <span class="qq-msg-time">{{ formatMsgTime(msg.createdAt) }}</span>
            </div>
          </template>
          <!-- 自己消息 -->
          <template v-else>
            <div class="qq-msg-wrap qq-msg-wrap-mine">
              <span class="qq-msg-name qq-msg-name-mine">{{ currentUserName || '我' }}</span>
              <div class="qq-bubble qq-bubble-mine">
                <span v-if="msg.messageType === 'image'" class="qq-img-msg">
                  <img :src="msg.content" alt="图片" class="qq-msg-image" />
                </span>
                <span v-else>{{ msg.content }}</span>
              </div>
              <span class="qq-msg-time qq-msg-time-mine">{{ formatMsgTime(msg.createdAt) }}</span>
            </div>
            <img :src="currentUserAvatar || defaultAvatar" class="qq-avatar" alt="我" />
          </template>
        </div>
      </template>

      <div v-if="messages.length === 0 && !loadingMore" class="qq-empty">
        <svg class="w-12 h-12 text-[var(--zh-text-tertiary)] opacity-40 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <p>暂无消息，发送第一条吧</p>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="qq-chat-input">
      <div class="qq-input-wrap" :class="{ focused: inputFocused }">
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="qq-textarea"
          placeholder="输入消息..."
          rows="1"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
          @keydown.enter.exact.prevent="sendMsg"
          @input="autoResize"
        ></textarea>
      </div>
      <button
        class="qq-send-btn"
        :disabled="!inputText.trim()"
        @click="sendMsg"
        aria-label="发送"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage } from '@/api/group'
import { sanitizeText } from '@/utils/sanitize'

const props = defineProps<{
  group: GroupInfo | null
  currentUserId: number
  currentUserAvatar?: string
  currentUserName?: string
  initialMessages?: GroupMessage[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'toggleMembers'): void
  (e: 'messageSent', msg: GroupMessage): void
  (e: 'messagesLoaded', msgs: GroupMessage[]): void
}>()

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNlMmU4ZjAiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCIgZm9udC1zaXplPSIxNiI+PzwvdGV4dD48L3N2Zz4='

const messages = ref<GroupMessage[]>([])
const inputText = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
const inputFocused = ref(false)
let offset = 0

// 外部传入消息（WebSocket推送）
function addExternalMessage(msg: GroupMessage) {
  // 去重
  if (messages.value.some(m => m.id === msg.id)) return
  messages.value.push(msg)
  nextTick(scrollToBottom)
}

defineExpose({ addExternalMessage })

async function loadMessages(gid: number) {
  try {
    const res = await groupApi.getMessages(gid, offset, 50)
    const data = res.data.data
    if (offset === 0) {
      messages.value = data.reverse()
      emit('messagesLoaded', messages.value)
    } else {
      messages.value = [...data.reverse(), ...messages.value]
    }
    hasMore.value = data.length === 50
    if (offset === 0) {
      await nextTick()
      scrollToBottom()
    }
  } catch {
    // 静默失败
  }
}

async function sendMsg() {
  const rawText = inputText.value.trim()
  if (!rawText || !props.group) return
  const sanitizedText = sanitizeText(rawText)
  try {
    const res = await groupApi.sendMessage(props.group.id, sanitizedText)
    const msg = res.data.data
    if (msg && !messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      emit('messageSent', msg)
    }
    inputText.value = ''
    if (inputRef.value) inputRef.value.style.height = 'auto'
    await nextTick()
    scrollToBottom()
  } catch {
    // 静默
  }
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

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

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

// 时间分隔：超过5分钟显示分隔线
function shouldShowTimeSeparator(idx: number): boolean {
  if (idx === 0) return true
  const prev = new Date(messages.value[idx - 1].createdAt).getTime()
  const curr = new Date(messages.value[idx].createdAt).getTime()
  return (curr - prev) > 5 * 60 * 1000
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = d.toDateString() === yesterday.toDateString()

  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')

  if (isToday) return `${hh}:${mm}`
  if (isYesterday) return `昨天 ${hh}:${mm}`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${hh}:${mm}`
}

function formatMsgTime(dateStr: string): string {
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 监听group变化重新加载
watch(() => props.group?.id, (id) => {
  if (id) {
    offset = 0
    messages.value = []
    hasMore.value = true
    loadMessages(id)
  }
}, { immediate: true })
</script>

<style scoped>
.qq-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg-elevated, #fff);
  overflow: hidden;
}

/* ===== 头部 ===== */
.qq-chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
  background: var(--zh-bg-elevated, #fff);
  z-index: 2;
}
.qq-back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}
.qq-back-btn:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-header-info { flex: 1; min-width: 0; }
.qq-chat-title {
  font-weight: 700;
  font-size: 15px;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.qq-chat-subtitle {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  display: flex;
  gap: 6px;
  margin-top: 1px;
}
.qq-group-num { font-family: 'JetBrains Mono', monospace; }
.qq-members-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.qq-members-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}

/* ===== 消息列表 ===== */
.qq-chat-msgs {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

.qq-load-more, .qq-no-more {
  text-align: center;
  padding: 10px 0;
  font-size: 12px;
  color: var(--zh-text-tertiary, #94a3b8);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 时间分隔线 */
.qq-time-sep {
  text-align: center;
  padding: 12px 0 6px;
}
.qq-time-sep span {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg, #f8fafc);
  padding: 2px 10px;
  border-radius: 10px;
}

/* 系统消息 */
.qq-system-msg {
  text-align: center;
  padding: 6px 0;
}
.qq-system-msg span {
  font-size: 12px;
  color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg-hover, #f1f5f9);
  padding: 3px 12px;
  border-radius: 12px;
}

/* 消息行 */
.qq-msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
}
.qq-msg-row.mine {
  justify-content: flex-end;
}

/* 头像 */
.qq-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  margin-top: 2px;
}

/* 消息包装 */
.qq-msg-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.qq-msg-wrap-mine {
  align-items: flex-end;
}

.qq-msg-name {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  padding-left: 2px;
}
.qq-msg-name-mine {
  text-align: right;
  padding-right: 2px;
  padding-left: 0;
}

/* 气泡 */
.qq-bubble {
  padding: 9px 13px;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
}
.qq-bubble-other {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text, #1e293b);
  border-radius: 4px 18px 18px 18px;
}
.qq-bubble-mine {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border-radius: 18px 4px 18px 18px;
}

/* 消息时间 */
.qq-msg-time {
  font-size: 10px;
  color: var(--zh-text-tertiary, #94a3b8);
  padding-left: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.qq-msg-time-mine {
  text-align: right;
  padding-right: 2px;
  padding-left: 0;
}
.qq-msg-row:hover .qq-msg-time {
  opacity: 1;
}

/* 图片消息 */
.qq-msg-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
}

/* 空状态 */
.qq-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 14px;
}

/* ===== 输入区域 ===== */
.qq-chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 14px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--zh-border, #e5e7eb);
  background: var(--zh-bg-elevated, #fff);
  flex-shrink: 0;
}
.qq-input-wrap {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 20px;
  background: var(--zh-bg, #f8fafc);
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.qq-input-wrap.focused {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}
.qq-textarea {
  width: 100%;
  min-height: 40px;
  max-height: 120px;
  border: none;
  outline: none;
  background: transparent;
  padding: 9px 16px;
  font-size: 14px;
  color: var(--zh-text, #1e293b);
  font-family: inherit;
  resize: none;
  line-height: 1.5;
}
.qq-textarea::placeholder {
  color: var(--zh-text-placeholder, #cbd5e1);
  font-size: 13px;
}
.qq-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
  flex-shrink: 0;
}
.qq-send-btn:hover:not(:disabled) { opacity: 0.9; transform: scale(1.05); }
.qq-send-btn:active:not(:disabled) { transform: scale(0.95); }
.qq-send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .qq-chat-header { padding: 8px 12px; }
  .qq-chat-msgs { padding: 6px 10px; }
  .qq-chat-input { padding: 8px 10px; padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px)); }
  .qq-avatar { width: 32px; height: 32px; }
  .qq-bubble { font-size: 13.5px; padding: 8px 11px; }
  .qq-msg-wrap { max-width: 78%; }
  .qq-msg-time { opacity: 1; }
  .qq-msg-image { max-width: 160px; max-height: 160px; }
}
</style>
