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
      <!-- 更多菜单按钮 -->
      <button
        class="more-menu-btn"
        title="聊天设置"
        @click="router.push(`/messages/chat-settings/${conversation.user?.id}`)"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <circle cx="12" cy="5" r="1.5" />
          <circle cx="12" cy="12" r="1.5" />
          <circle cx="12" cy="19" r="1.5" />
        </svg>
      </button>
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
            <div v-if="message.type === 'image'" class="bubble bubble-other bubble-image">
              <img :src="resolveMsgUrl(message.content)" alt="图片" class="msg-image" @click="previewImage(message.content)" />
            </div>
            <div v-else class="bubble bubble-other">{{ message.content }}</div>
            <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
          </div>
        </template>
        <template v-else>
          <div class="message-bubble-wrap">
            <div v-if="message.type === 'image'" class="bubble bubble-mine bubble-image">
              <img :src="resolveMsgUrl(message.content)" alt="图片" class="msg-image" @click="previewImage(message.content)" />
            </div>
            <div v-else class="bubble bubble-mine">{{ message.content }}</div>
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
      <!-- 图片上传进度提示 -->
      <Transition name="send-error-fade">
        <div v-if="imageUploading" class="send-error-tip" style="background:#eff6ff;border-color:#bfdbfe;color:#2563eb;">
          <span>图片上传中...</span>
        </div>
      </Transition>
      <div class="chat-input-row">
        <!-- 表情按钮 -->
        <EmojiPicker @select="onEmojiSelect" />
        <!-- 图片上传按钮 -->
        <button class="input-action-btn" title="发送图片" @click="triggerImageUpload" :disabled="imageUploading">
          <svg class="action-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
        </button>
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

    <!-- 图片预览弹层 -->
    <Teleport to="body">
      <div v-if="previewImageUrl" class="image-preview-overlay" @click="previewImageUrl = ''">
        <img :src="resolveMsgUrl(previewImageUrl)" class="image-preview-img" alt="预览" @click.stop />
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { Conversation, Message } from '@/types'
import { socialApi } from '@/api'
import { fileApi } from '@/api/file'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { sanitizeText } from '@/utils/sanitize'

const router = useRouter()
const userStore = useUserStore()
const { resolveUrl } = useResourceUrl()

const props = withDefaults(defineProps<{
  conversation: Conversation | null
  messages: Message[]
  loadingMore?: boolean
  hasMore?: boolean
  onSend?: (content: string, type?: string) => Promise<any>
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
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)
const previewImageUrl = ref('')

const navigateToUser = (userId?: number) => { if (userId) router.push(`/user/${userId}`) }
const isMine = (message: Message) => message.senderId === userStore.userInfo?.id

/** 解析消息中的资源 URL（用于图片消息） */
const resolveMsgUrl = (url: string) => resolveUrl(url) || url

/** 点击图片消息放大预览 */
const previewImage = (url: string) => { previewImageUrl.value = url }

/** 表情选择回调 */
const onEmojiSelect = (emoji: string) => {
  inputContent.value += emoji
}

/** 触发图片文件选择 */
const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

/** 图片文件选择后上传并发送 */
const onImageSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = '' // 重置以允许重复选择同一文件

  // 校验文件类型和大小
  if (!file.type.startsWith('image/')) {
    sendError.value = '请选择图片文件'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    sendError.value = '图片大小不能超过10MB'
    return
  }

  imageUploading.value = true
  sendError.value = ''

  try {
    // 上传图片到服务器
    const imageUrl = await fileApi.uploadSingleImage(file)
    if (!imageUrl) {
      throw new Error('图片上传失败，返回地址为空')
    }

    // 发送图片消息
    if (props.onSend) {
      await props.onSend(imageUrl, 'image')
    } else {
      sendError.value = '消息发送功能暂不可用，请刷新页面后重试'
      return
    }
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    sendError.value = err.message || '图片发送失败，请稍后重试'
    console.error('[ChatWindow] 发送图片失败:', err.message || err)
  } finally {
    imageUploading.value = false
  }
}

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
      await props.onSend(content, 'text')
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

/* 更多菜单按钮 */
.more-menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-left: auto;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  flex-shrink: 0;
}
.more-menu-btn:hover {
  background: var(--zh-bg-hover);
  color: var(--zh-primary);
}

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
  gap: 4px;
}

/* 操作按钮（表情/图片） */
.input-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  flex-shrink: 0;
  position: relative;
  min-height: 0;
  min-width: 0;
}
.input-action-btn:hover:not(:disabled) {
  background: var(--zh-bg-hover);
  color: var(--zh-primary);
}
.input-action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.action-icon {
  width: 20px;
  height: 20px;
}

/* 图片消息 */
.bubble-image {
  padding: 4px !important;
  background: transparent !important;
}
.msg-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 12px;
  object-fit: cover;
  cursor: pointer;
  display: block;
  transition: transform 0.15s ease;
}
.msg-image:hover {
  transform: scale(1.02);
}

/* 图片预览弹层 */
.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.75);
  cursor: pointer;
}
.image-preview-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
  cursor: default;
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
