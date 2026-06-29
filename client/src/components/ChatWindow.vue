<template>
  <!-- 聊天窗口 - 现代气泡 + 流畅动画 -->
  <div class="chat-window">
    <!-- 头部 -->
    <div v-if="conversation" class="chat-header">
      <el-button class="back-btn" text :icon="ArrowLeft" @click="$emit('back')" />
      <el-badge is-dot :hidden="!isOnline" type="success" class="avatar-dot">
        <span class="avatar-wrap" @click="navigateToUser(conversation.user?.id)">
          <UserAvatar :src="conversation.user?.avatar" alt="头像" size="md" />
        </span>
      </el-badge>
      <div class="chat-user-info">
        <p class="chat-username">{{ conversation.user?.nickname }}</p>
        <p class="chat-status" :class="{ online: isOnline }">
          {{ isOnline ? '在线' : '离线' }}
        </p>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="messageListRef" class="chat-messages">
      <div ref="topSentinelRef" class="load-sentinel">
        <el-icon v-if="loadingMore" class="is-loading" :size="18"><Loading /></el-icon>
        <span v-else-if="!hasMore" class="all-loaded">— 以上是全部消息 —</span>
      </div>

      <div
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="isMine(message) ? 'message-mine' : 'message-other'"
      >
        <template v-if="!isMine(message)">
          <span class="sender-avatar" @click="navigateToUser(message.sender?.id)">
            <UserAvatar :src="message.sender?.avatar" alt="头像" size="sm" />
          </span>
          <div class="message-bubble-wrap">
            <div class="bubble bubble-other">{{ message.content }}</div>
            <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
          </div>
        </template>
        <template v-else>
          <div class="message-bubble-wrap">
            <div class="bubble bubble-mine">{{ message.content }}</div>
            <span class="bubble-time mine-time">{{ formatTime(message.createdAt) }}</span>
          </div>
          <span class="my-avatar" @click="navigateToUser(userStore.userInfo?.id)">
            <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" size="sm" />
          </span>
        </template>
      </div>

      <div v-if="!loadingMore && messages.length === 0" class="empty-messages">
        <p>暂无消息，发送第一条吧</p>
      </div>
      <div ref="bottomRef" style="height: 0;" />
    </div>

    <!-- 输入框 - 统一交互风格 -->
    <div class="chat-input-bar">
      <!-- 发送错误提示 -->
      <Transition name="send-error-fade">
        <div v-if="sendError" class="send-error-tip">
          <span>{{ sendError }}</span>
          <button class="send-error-close" @click="sendError = ''">&times;</button>
        </div>
      </Transition>
      <div class="chat-input-row">
        <div class="input-field-chat" :class="{ focused: inputFocused }">
          <input
            v-model="inputContent"
            type="text"
            class="input-control-chat"
            placeholder="输入消息..."
            :disabled="sending"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            @keydown.enter="sendMessage"
          />
          <button
            class="input-send-btn"
            :disabled="!inputContent.trim() || sending"
            @click="sendMessage"
            aria-label="发送消息"
          >
            <svg v-if="!sending" class="send-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
            <div v-else class="send-spinner"></div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Conversation, Message } from '@/types'
import { socialApi } from '@/api'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { sanitizeText } from '@/utils/sanitize'

const router = useRouter()
const userStore = useUserStore()

const props = withDefaults(defineProps<{
  conversation: Conversation | null
  messages: Message[]
  loadingMore?: boolean
  hasMore?: boolean
  onSend?: (content: string) => Promise<any>
}>(), { loadingMore: false, hasMore: true, onSend: undefined })

const emit = defineEmits<{ back: []; loadMore: [] }>()

const inputContent = ref('')
const sending = ref(false)
const sendError = ref('')
const inputFocused = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const topSentinelRef = ref<HTMLElement | null>(null)
const bottomRef = ref<HTMLElement | null>(null)
const isOnline = ref(false)

const navigateToUser = (userId?: number) => { if (userId) router.push(`/user/${userId}`) }
const isMine = (message: Message) => message.senderId === userStore.userInfo?.id

const sendMessage = async () => {
  const rawContent = inputContent.value.trim()
  if (!rawContent || sending.value) return

  // 客户端输入净化：移除所有 HTML 标签，防止 XSS
  const content = sanitizeText(rawContent)

  sending.value = true
  sendError.value = ''
  inputContent.value = ''

  try {
    if (props.onSend) {
      await props.onSend(content)
    } else {
      console.warn('[ChatWindow] 未提供 onSend 回调，消息无法发送')
      sendError.value = '消息发送功能暂不可用，请刷新页面后重试'
      inputContent.value = rawContent
      return
    }
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    sendError.value = err.message || '消息发送失败，请稍后重试'
    inputContent.value = rawContent // 恢复原始输入内容
    console.error('[ChatWindow] 发送消息失败:', err.message || err)
  } finally {
    sending.value = false
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now.getTime() - 86400000)
  const isYesterday = date.toDateString() === yesterday.toDateString()
  const timeStr = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (isToday) return timeStr
  if (isYesterday) return `昨天 ${timeStr}`
  if (date.getFullYear() === now.getFullYear()) return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
}

const fetchOnlineStatus = async () => {
  if (!props.conversation?.user?.id) return
  try {
    const { data } = await socialApi.getOnlineStatus(props.conversation.user.id)
    isOnline.value = data.data?.[props.conversation.user.id] ?? false
  } catch { isOnline.value = false }
}
watch(() => props.conversation?.user?.id, (newId) => { if (newId) fetchOnlineStatus() }, { immediate: true })

const scrollToBottom = () => bottomRef.value?.scrollIntoView({ behavior: 'instant' })
let prevMessageCount = 0
watch(() => props.messages.length, (newLen) => {
  if (newLen > prevMessageCount && !props.loadingMore) nextTick(() => scrollToBottom())
  prevMessageCount = newLen
})

let observer: IntersectionObserver | null = null
onMounted(() => {
  if (topSentinelRef.value) {
    observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && !props.loadingMore) emit('loadMore')
    }, { threshold: 0.1 })
    observer.observe(topSentinelRef.value)
  }
})
onUnmounted(() => observer?.disconnect())
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg-elevated);
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--zh-border);
  flex-shrink: 0;
}
.back-btn { display: none; }
@media (max-width: 767.98px) { .back-btn { display: inline-flex; margin-right: 8px; } }

.avatar-wrap {
  display: inline-block;
  cursor: pointer;
  border-radius: 50%;
  transition: opacity var(--zh-transition-fast);
}
.avatar-wrap:hover { opacity: 0.75; }

.chat-user-info { margin-left: 12px; min-width: 0; }
.chat-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--zh-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-status { font-size: 12px; color: var(--zh-text-tertiary); }
.chat-status.online { color: var(--zh-success); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.load-sentinel {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
.all-loaded { font-size: 11px; color: var(--zh-text-placeholder); }

.message-row { display: flex; }
.message-mine { justify-content: flex-end; }
.message-other { justify-content: flex-start; }

.message-bubble-wrap { max-width: 72%; }

.bubble {
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-other {
  background: var(--zh-bg-hover);
  color: var(--zh-text);
  border-bottom-left-radius: 6px;
}
.bubble-mine {
  background: var(--zh-primary-gradient);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.bubble-time {
  display: block;
  font-size: 10px;
  color: var(--zh-text-placeholder);
  margin-top: 4px;
}
.mine-time { text-align: right; }

.sender-avatar {
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  margin-right: 8px;
  align-self: flex-start;
}

.my-avatar {
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: 8px;
  align-self: flex-start;
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--zh-text-tertiary);
  font-size: 14px;
}

/* 输入区域 */
.chat-input-bar {
  flex-shrink: 0;
  padding: 10px 14px;
  border-top: 1px solid var(--zh-border);
  background: var(--zh-bg-elevated);
}

/* 发送错误提示 */
.send-error-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  margin-bottom: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 12px;
  color: #dc2626;
}
.dark .send-error-tip {
  background: rgba(220, 38, 38, 0.15);
  border-color: rgba(220, 38, 38, 0.3);
  color: #fca5a5;
}
.send-error-close {
  background: none;
  border: none;
  font-size: 16px;
  color: inherit;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.send-error-fade-enter-active,
.send-error-fade-leave-active {
  transition: all 0.25s ease;
}
.send-error-fade-enter-from,
.send-error-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.chat-input-row {
  display: flex;
  align-items: center;
}

/* 统一输入框样式 - 匹配登录页交互 */
.input-field-chat {
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
.input-field-chat.focused {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.input-control-chat {
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
.input-control-chat::placeholder {
  color: var(--zh-text-placeholder);
  font-size: 13px;
}
.input-control-chat:disabled {
  opacity: 0.5;
}

.input-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-right: 4px;
  border: none;
  border-radius: 50%;
  background: var(--zh-primary-gradient);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
  flex-shrink: 0;
}
.input-send-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: scale(1.05);
}
.input-send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
.input-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-icon {
  width: 18px;
  height: 18px;
}

.send-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
