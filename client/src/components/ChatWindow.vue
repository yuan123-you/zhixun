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
          <span class="sender-avatar" @click="message.senderId === 0 ? null : navigateToUser(message.sender?.id)">
            <UserAvatar v-if="message.senderId !== 0" :src="message.sender?.avatar" alt="头像" size="sm" />
            <img v-else :src="aiAvatarUrl" alt="AI" class="ai-avatar-img" />
          </span>
          <div class="message-bubble-wrap">
            <div v-if="message.type === 'ai_reply'" class="bubble bubble-other bubble-ai">
              <span class="ai-label">AI助手</span>
              <p class="ai-text">{{ message.content }}</p>
            </div>
            <div v-else-if="message.type === 'image'" class="bubble bubble-other bubble-image">
              <img :src="resolveMsgUrl(message.content)" alt="图片" class="msg-image" @click="previewImage(message.content)" />
            </div>
            <a v-else-if="message.type === 'file'" :href="getFileUrl(message.content)" target="_blank" rel="noopener" class="bubble bubble-other bubble-file">
              <div class="file-card-icon">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
              </div>
              <div class="file-card-info">
                <span class="file-name">{{ getFileName(message.content) }}</span>
                <span v-if="getFileSize(message.content)" class="file-size">{{ getFileSize(message.content) }}</span>
              </div>
              <div class="file-card-download">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
              </div>
            </a>
            <div v-else-if="message.type === 'voice'" class="bubble bubble-other voice-bubble">
              <VoiceMessage :url="getVoiceUrl(message.content)" :duration="getVoiceDuration(message.content)" :is-mine="false" />
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
            <a v-else-if="message.type === 'file'" :href="getFileUrl(message.content)" target="_blank" rel="noopener" class="bubble bubble-mine bubble-file">
              <div class="file-card-icon">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
              </div>
              <div class="file-card-info">
                <span class="file-name">{{ getFileName(message.content) }}</span>
                <span v-if="getFileSize(message.content)" class="file-size">{{ getFileSize(message.content) }}</span>
              </div>
              <div class="file-card-download">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
              </div>
            </a>
            <div v-else-if="message.type === 'voice'" class="bubble bubble-mine voice-bubble">
              <VoiceMessage :url="getVoiceUrl(message.content)" :duration="getVoiceDuration(message.content)" :is-mine="true" />
            </div>
            <div v-else class="bubble bubble-mine">{{ message.content }}</div>
            <span class="bubble-time mine-time">{{ formatTime(message.createdAt) }}</span>
          </div>
          <span class="my-avatar" @click="navigateToUser(userStore.userInfo?.id)">
            <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" size="sm" />
          </span>
        </template>
      </div>

      <!-- AI助手正在思考 -->
      <div v-if="aiThinking" class="message-row message-other">
        <span class="sender-avatar">
          <img :src="aiAvatarUrl" alt="AI" class="ai-avatar-img" />
        </span>
        <div class="message-bubble-wrap">
          <div class="bubble bubble-other bubble-ai">
            <span class="ai-label">AI助手</span>
            <div class="ai-typing-indicator">
              <span class="ai-dot"></span>
              <span class="ai-dot"></span>
              <span class="ai-dot"></span>
            </div>
          </div>
        </div>
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
      <!-- 工具栏 -->
      <div class="chat-toolbar">
        <EmojiPicker @select="onEmojiSelect" />
        <button class="chat-tool-btn" @click="triggerImageUpload" title="发送图片">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
        </button>
        <button class="chat-tool-btn" @click="triggerFileUpload" title="发送文件">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" /></svg>
        </button>
        <button class="chat-tool-btn" :class="{ active: voiceRecorder.isRecording.value }" @click="startVoiceRecord" title="语音消息">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" /></svg>
        </button>
        <button class="chat-tool-btn" :class="{ active: aiMode }" @click="toggleAIMode" title="AI助手">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="5" y="8" width="14" height="11" rx="3"/>
            <circle cx="9.5" cy="13" r="1.5" fill="currentColor" stroke="none"/>
            <circle cx="14.5" cy="13" r="1.5" fill="currentColor" stroke="none"/>
            <path d="M10 16.5h4"/>
            <path d="M12 3v3"/>
            <circle cx="12" cy="2.5" r="1.5" fill="currentColor" stroke="none"/>
            <path d="M3 12.5h2M19 12.5h2"/>
            <path d="M20 4l.5 1.5L22 6l-1.5.5L20 8l-.5-1.5L18 6l1.5-.5z" fill="currentColor" stroke="none" opacity="0.6"/>
            <path d="M3.5 18l.4 1L5 19.4l-1.1.4L3.5 21l-.4-1.2L2 19.4l1.1-.4z" fill="currentColor" stroke="none" opacity="0.5"/>
          </svg>
        </button>
      </div>
      <!-- 语音上传中提示 -->
      <Transition name="send-error-fade">
        <div v-if="voiceUploading" class="send-error-tip" style="background:#eff6ff;border-color:#bfdbfe;color:#2563eb;">
          <span>语音上传中...</span>
        </div>
      </Transition>
      <!-- 语音录制中 - 替换输入框 -->
      <div v-if="voiceRecorder.isRecording.value" class="voice-recording-bar">
        <span class="voice-rec-dot" />
        <span class="voice-rec-time">{{ voiceRecorder.formatTime(voiceRecorder.recordingTime.value) }}</span>
        <button class="voice-rec-stop" @click="finishVoiceRecord">发送</button>
        <button class="voice-rec-cancel" @click="cancelVoiceRecord">取消</button>
      </div>
      <div v-else class="chat-input-row">
        <div class="input-field-chat" :class="{ focused: inputFocused }">
          <input
            v-model="inputContent"
            type="text"
            class="input-control-chat"
            :placeholder="aiMode ? '向AI助手提问...' : '输入消息...'"
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

    <!-- 隐藏的file input -->
    <input ref="imageInputRef" type="file" accept="image/*" class="hidden" @change="onImageSelected" />
    <input ref="fileInputRef" type="file" class="hidden" @change="onFileSelected" />

    <!-- 上传进度 -->
    <Teleport to="body">
      <div v-if="fileUploading" class="chat-upload-overlay">
        <div class="chat-upload-card">
          <div class="chat-upload-spinner"></div>
          <p class="chat-upload-text">{{ uploadProgress > 0 ? `上传中 ${uploadProgress}%` : '上传中...' }}</p>
        </div>
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
import VoiceMessage from '@/components/VoiceMessage.vue'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
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
const fileInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)
const fileUploading = ref(false)
const uploadProgress = ref(0)
const previewImageUrl = ref('')
const aiMode = ref(false)
const aiThinking = ref(false)

/** 语音录制 */
const voiceRecorder = useVoiceRecorder()
const voiceUploading = ref(false)

/** 解析语音消息内容 - 兼容 JSON {url, duration} 和纯 URL 字符串，解析 MinIO 地址 */
const getVoiceUrl = (content: string): string => {
  try {
    const data = JSON.parse(content)
    const url = data.url || content
    return resolveUrl(url) || url
  } catch {
    return resolveUrl(content) || content
  }
}
const getVoiceDuration = (content: string): number => {
  try { const data = JSON.parse(content); return data.duration || 0 } catch { return 0 }
}

/** 开始语音录制 */
const startVoiceRecord = () => { voiceRecorder.startRecording() }

/** 结束录制并上传发送 */
const finishVoiceRecord = async () => {
  const duration = voiceRecorder.recordingTime.value
  voiceRecorder.stopRecording()
  // stopRecording 触发 onstop（同步），此时 audioBlob 已设置
  const finalBlob = voiceRecorder.audioBlob.value
  if (!finalBlob) return
  voiceUploading.value = true
  try {
    const voiceUrl = await fileApi.uploadSingleVoice(finalBlob)
    if (!voiceUrl) throw new Error('语音上传失败')
    const content = JSON.stringify({ url: voiceUrl, duration: duration || 0 })
    if (props.onSend) {
      await props.onSend(content, 'voice')
    }
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    sendError.value = err.message || '语音发送失败，请稍后重试'
  } finally {
    voiceUploading.value = false
    voiceRecorder.cancelRecording()
  }
}

/** 取消语音录制 */
const cancelVoiceRecord = () => {
  voiceRecorder.cancelRecording()
}

/** AI助手头像 - 机器人脸+渐变背景+星芒装饰 */
const aiAvatarUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJnIiB4MT0iMCIgeTE9IjAiIHgyPSIxIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjODE4Y2Y4Ii8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjNjM2NmYxIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjQwIiBoZWlnaHQ9IjQwIiBmaWxsPSJ1cmwoI2JnKSIgcng9IjIwIi8+PHJlY3QgeD0iMTEiIHk9IjE1IiB3aWR0aD0iMTgiIGhlaWdodD0iMTQiIHJ4PSI0IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjk1Ii8+PHJlY3QgeD0iMTUiIHk9IjE5IiB3aWR0aD0iMy41IiBoZWlnaHQ9IjMuNSIgcng9IjEuMiIgZmlsbD0iIzYzNjZmMSIvPjxyZWN0IHg9IjIxLjUiIHk9IjE5IiB3aWR0aD0iMy41IiBoZWlnaHQ9IjMuNSIgcng9IjEuMiIgZmlsbD0iIzYzNjZmMSIvPjxwYXRoIGQ9Ik0xNS41IDI2aDkiIHN0cm9rZT0iIzYzNjZmMSIgc3Ryb2tlLXdpZHRoPSIxLjgiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPjxyZWN0IHg9IjE5IiB5PSI4IiB3aWR0aD0iMiIgaGVpZ2h0PSI3IiByeD0iMSIgZmlsbD0id2hpdGUiLz48Y2lyY2xlIGN4PSIyMCIgY3k9IjciIHI9IjIuNSIgZmlsbD0iI2E1YjRmYyIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iNyIgcj0iMS4yIiBmaWxsPSJ3aGl0ZSIvPjxyZWN0IHg9IjcuNSIgeT0iMjAiIHdpZHRoPSIzLjUiIGhlaWdodD0iNCIgcng9IjEuNSIgZmlsbD0id2hpdGUiIGZpbGwtb3BhY2l0eT0iMC43NSIvPjxyZWN0IHg9IjI5IiB5PSIyMCIgd2lkdGg9IjMuNSIgaGVpZ2h0PSI0IiByeD0iMS41IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjc1Ii8+PHBhdGggZD0iTTMyIDEwbC44IDIuMkwzNSAxM2wtMi4yLjhMMzIgMTZsLS44LTIuMkwyOSAxM2wyLjItLjh6IiBmaWxsPSIjYzdkMmZlIiBmaWxsLW9wYWNpdHk9IjAuOCIvPjxwYXRoIGQ9Ik04IDMwbC42IDEuNUwxMCAzMmwtMS40LjVMOCAzNGwtLjYtMS41TDYgMzJsMS40LS41eiIgZmlsbD0iI2M3ZDJmZSIgZmlsbC1vcGFjaXR5PSIwLjYiLz48L3N2Zz4='

const navigateToUser = (userId?: number) => { if (userId) router.push(`/user/${userId}`) }
const isMine = (message: Message) => message.senderId === userStore.userInfo?.id

/** 解析消息中的资源 URL（用于图片消息） */
const resolveMsgUrl = (url: string) => resolveUrl(url) || url

/** 点击图片消息放大预览 */
const previewImage = (url: string) => { previewImageUrl.value = url }

/** 从文件消息JSON中提取URL */
const getFileUrl = (content: string) => {
  try {
    const url = JSON.parse(content).url || content
    return resolveUrl(url) || url
  } catch {
    return resolveUrl(content) || content
  }
}

/** 从文件消息JSON中提取文件名 */
const getFileName = (content: string) => {
  try { return JSON.parse(content).name || '文件' } catch { return '文件' }
}

/** 从文件消息JSON中提取文件大小（格式化） */
const getFileSize = (content: string) => {
  try {
    const size = JSON.parse(content).size
    if (!size) return ''
    return size > 1048576 ? (size / 1048576).toFixed(1) + ' MB' : (size / 1024).toFixed(1) + ' KB'
  } catch { return '' }
}

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

/** 切换AI助手模式 */
const toggleAIMode = () => {
  aiMode.value = !aiMode.value
}

/** 触发文件上传 */
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

/** 文件选择后上传并发送 */
const onFileSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  fileUploading.value = true
  uploadProgress.value = 0
  sendError.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.uploadFile(formData, (p) => { uploadProgress.value = p })
    const url = res.data.data
    const content = JSON.stringify({ url, name: file.name, size: file.size })
    if (props.onSend) {
      await props.onSend(content, 'file')
    }
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    sendError.value = err.message || '文件上传失败，请稍后重试'
    console.error('[ChatWindow] 文件上传失败:', err.message || err)
  } finally {
    fileUploading.value = false
    uploadProgress.value = 0
  }
}

/** 发送AI助手消息：先发送用户提问（带@AI助手前缀），再调用AI接口获取回复 */
const sendAIMsg = async (question: string) => {
  if (!question || !props.conversation?.user?.id) return
  sending.value = true
  sendError.value = ''
  inputContent.value = ''

  try {
    // 1. 先发送用户的提问消息（带 @AI助手 前缀，作为普通私信发送）
    const contentWithPrefix = '@AI助手 ' + question
    if (props.onSend) {
      await props.onSend(contentWithPrefix, 'text')
    }
    await nextTick()
    scrollToBottom()

    // 2. 显示AI思考指示器，调用AI接口获取回复
    aiThinking.value = true
    const res = await socialApi.sendAIMessage(props.conversation.user.id, question)
    aiThinking.value = false
    const raw: any = res.data?.data
    if (raw) {
      // 构建AI消息对象（与store中transformMessage逻辑一致）
      const aiMsg: Message = {
        id: Number(raw.id) || Date.now(),
        conversationId: Number(raw.conversationId) || 0,
        senderId: Number(raw.senderId) || 0,
        sender: {
          id: 0,
          uid: '0',
          username: raw.senderNickname || 'AI助手',
          nickname: raw.senderNickname || 'AI助手',
          avatar: raw.senderAvatar || aiAvatarUrl,
          bio: '', email: '', phone: '', gender: 0 as any,
          birthday: '', followCount: 0, followerCount: 0,
          articleCount: 0, likeCount: 0, isFollowing: false, createdAt: '',
        },
        content: raw.content || '',
        type: 'ai_reply',
        isRead: false,
        createdAt: raw.createdAt || new Date().toISOString(),
      }
      // 避免重复添加
      if (!messageStore.currentMessages.some(m => m.id === aiMsg.id)) {
        messageStore.currentMessages.push(aiMsg)
      }
    }
    aiMode.value = false
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    aiThinking.value = false
    sendError.value = err.message || 'AI回复失败，请稍后重试'
    console.error('[ChatWindow] AI消息失败:', err.message || err)
  } finally {
    sending.value = false
  }
}

const sendMessage = async () => {
  const rawContent = inputContent.value.trim()
  if (!rawContent || sending.value) return

  // AI模式或包含@AI助手提及：路由到AI助手
  const isAIMention = rawContent.includes('@AI助手')
  if (aiMode.value || isAIMention) {
    const question = rawContent.replace(/@AI助手\s*/g, '').trim()
    if (question) {
      await sendAIMsg(question)
    }
    return
  }

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
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--zh-bg-elevated);
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

/* 工具栏 */
.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 6px;
}
.chat-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  transition: all 0.15s;
  min-height: 0;
  min-width: 0;
}
.chat-tool-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}
.chat-tool-btn.active {
  background: var(--zh-primary-bg, rgba(99,102,241,0.1));
  color: var(--zh-primary, #6366f1);
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

/* AI消息气泡 */
.bubble-ai {
  border-left: 3px solid var(--zh-primary, #6366f1);
  background: rgba(99, 102, 241, 0.06) !important;
}
.ai-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--zh-primary, #6366f1);
  margin-bottom: 4px;
}
.ai-text {
  white-space: pre-wrap;
  line-height: 1.6;
  margin: 0;
}
.ai-avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

/* AI思考中的打字动画 */
.ai-typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}
.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  opacity: 0.4;
  animation: ai-bounce 1.4s ease-in-out infinite;
}
.ai-dot:nth-child(2) { animation-delay: 0.2s; }
.ai-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-bounce {
  0%, 80%, 100% { opacity: 0.4; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.1); }
}

/* 文件消息 */
.bubble-file {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  min-width: 200px;
  padding: 10px 12px !important;
  transition: opacity 0.15s;
}
.bubble-file:hover {
  opacity: 0.85;
}
.bubble-file .file-card-icon {
  flex-shrink: 0;
  color: var(--zh-primary, #6366f1);
}
.bubble-mine .file-card-icon {
  color: rgba(255, 255, 255, 0.9);
}
.bubble-file .file-card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.bubble-file .file-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
.bubble-file .file-size {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
}
.bubble-mine .file-size {
  color: rgba(255, 255, 255, 0.7);
}
.bubble-file .file-card-download {
  flex-shrink: 0;
  color: var(--zh-text-tertiary, #94a3b8);
}
.bubble-mine .file-card-download {
  color: rgba(255, 255, 255, 0.7);
}

/* 上传遮罩 */
.chat-upload-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-upload-card {
  background: var(--zh-bg-elevated, #fff);
  border-radius: 16px;
  padding: 24px 32px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.chat-upload-spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 12px;
  border: 3px solid var(--zh-border, #e5e7eb);
  border-top-color: var(--zh-primary, #6366f1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.chat-upload-text {
  font-size: 14px;
  color: var(--zh-text, #1e293b);
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

/* 语音录制栏 */
.voice-recording-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 24px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
}
.voice-rec-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  animation: voice-pulse 1s infinite;
  flex-shrink: 0;
}
@keyframes voice-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.voice-rec-time {
  font-size: 14px;
  font-weight: 500;
  color: var(--zh-text, #1e293b);
  font-variant-numeric: tabular-nums;
  min-width: 40px;
}
.voice-rec-stop {
  padding: 4px 16px;
  border: none;
  border-radius: 16px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
  margin-left: auto;
}
.voice-rec-stop:hover { opacity: 0.85; }
.voice-rec-cancel {
  padding: 4px 12px;
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 16px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.voice-rec-cancel:hover { background: var(--zh-bg-hover, #f1f5f9); }

/* 语音消息气泡 - 重置 padding 让 VoiceMessage 组件控制 */
.voice-bubble {
  padding: 4px 6px !important;
  min-width: 100px;
}
</style>
