<template>
  <!-- 私信聊天页面（移动端全屏 / 桌面端备选入口） -->
  <div class="chat-page h-[calc(100dvh-3.75rem-3.125rem)] md:h-[calc(100dvh-4rem-3.125rem)] flex flex-col bg-[var(--zh-bg-elevated)] dark:bg-gray-900" style="padding-bottom:env(safe-area-inset-bottom,0px)">
    <!-- 聊天头部 -->
    <div class="flex items-center gap-3 px-4 py-3 border-b border-[var(--zh-border)]/60 dark:border-gray-700/60 bg-[var(--zh-bg-elevated)] dark:bg-gray-900 flex-shrink-0 sticky top-0 z-10">
      <!-- 返回按钮 -->
      <button
        class="p-2 -ml-2 text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] hover:bg-slate-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
        @click="goBack"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <!-- 用户信息 -->
      <div class="flex items-center gap-3 flex-1 min-w-0">
        <button class="relative flex-shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(targetUserId)">
          <UserAvatar :src="targetUser?.avatar" :alt="targetUser?.nickname" size="md" />
          <span
            v-if="isOnline"
            class="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-500 border-2 border-white dark:border-gray-900 rounded-full"
          ></span>
        </button>
        <div class="min-w-0">
          <p class="font-medium text-sm text-[var(--zh-text)] dark:text-[var(--zh-text)] truncate">
            {{ targetUser?.nickname || '加载中...' }}
          </p>
          <p class="text-xs" :class="isOnline ? 'text-green-500' : 'text-[var(--zh-text-tertiary)] dark:text-[var(--zh-text-secondary)]'">
            {{ isOnline ? '在线' : '离线（消息将在对方上线后送达）' }}
          </p>
        </div>
      </div>

      <!-- 更多菜单按钮 -->
      <button
        class="p-2 -mr-2 text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] hover:bg-slate-100 dark:hover:bg-gray-800 rounded-lg transition-colors flex-shrink-0"
        title="聊天设置"
        @click="router.push(`/messages/chat-settings/${targetUserId}`)"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
          <circle cx="12" cy="5" r="1.5" />
          <circle cx="12" cy="12" r="1.5" />
          <circle cx="12" cy="19" r="1.5" />
        </svg>
      </button>
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
        <span class="text-xs text-slate-300 dark:text-[var(--zh-text-secondary)]">—— 以上是全部消息 ——</span>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingMessages && currentPage === 1" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loadingMessages && currentMessages.length === 0" class="flex flex-col items-center justify-center py-20">
        <svg class="w-16 h-16 text-slate-200 dark:text-[var(--zh-text-secondary)] mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <p class="text-[var(--zh-text-tertiary)] dark:text-[var(--zh-text-secondary)] text-sm">发送第一条消息吧</p>
      </div>

      <!-- 消息气泡 -->
      <template v-for="(msg, idx) in currentMessages" :key="msg.id || idx">
        <!-- 时间分隔线（与前一条消息间隔超过5分钟则显示） -->
        <div
          v-if="idx === 0 || getTimeDiff(currentMessages[idx - 1]?.createdAt, msg.createdAt) > 5"
          class="flex justify-center"
        >
          <span class="text-[11px] text-[var(--zh-text-tertiary)] dark:text-[var(--zh-text-secondary)] bg-slate-100 dark:bg-gray-800 px-3 py-0.5 rounded-full">
            {{ formatMessageTime(msg.createdAt) }}
          </span>
        </div>

        <!-- 对方消息 -->
        <div v-if="msg.senderId !== myUserId" class="flex items-start gap-2 max-w-[75%]">
          <button class="flex-shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="msg.type === 'ai_reply' ? null : navigateToUser(msg.sender?.id)">
            <img v-if="msg.type === 'ai_reply'" :src="aiAvatarUrl" alt="AI" class="w-8 h-8 rounded-full object-cover" />
            <UserAvatar v-else :src="msg.sender?.avatar" :alt="msg.sender?.nickname" size="sm" />
          </button>
          <div>
            <!-- AI回复消息 -->
            <div v-if="msg.type === 'ai_reply'" class="bg-slate-100 dark:bg-gray-800 rounded-2xl rounded-tl-sm px-3 py-2 border-l-3 border-indigo-500">
              <span class="block text-[11px] font-semibold text-indigo-500 mb-0.5">AI助手</span>
              <p class="text-sm text-[var(--zh-text)] dark:text-gray-100 whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
            <div v-else-if="msg.type === 'image'" class="msg-image-wrap">
              <img :src="resolveMsgUrl(msg.content)" alt="图片" class="msg-image" @click="previewImage(msg.content)" />
            </div>
            <div v-else-if="msg.type === 'voice'" class="voice-bubble-other">
              <VoiceMessage :url="getVoiceUrl(msg.content)" :duration="getVoiceDuration(msg.content)" :is-mine="false" />
            </div>
            <div v-else class="bg-slate-100 dark:bg-gray-800 rounded-2xl rounded-tl-sm px-3 py-2">
              <p class="text-sm text-[var(--zh-text)] dark:text-gray-100 whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
            <span class="text-[10px] text-[var(--zh-text-tertiary)] dark:text-[var(--zh-text-secondary)] mt-0.5 block">
              {{ formatMessageTime(msg.createdAt) }}
            </span>
          </div>
        </div>

        <!-- 我的消息 -->
        <div v-else class="flex items-start gap-2 justify-end">
          <div class="max-w-[75%]">
            <div v-if="msg.type === 'image'" class="msg-image-wrap msg-image-wrap-mine">
              <img :src="resolveMsgUrl(msg.content)" alt="图片" class="msg-image" @click="previewImage(msg.content)" />
            </div>
            <div v-else-if="msg.type === 'voice'" class="voice-bubble-mine">
              <VoiceMessage :url="getVoiceUrl(msg.content)" :duration="getVoiceDuration(msg.content)" :is-mine="true" />
            </div>
            <div v-else class="bg-primary text-white rounded-2xl rounded-tr-sm px-3 py-2">
              <p class="text-sm whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
            <span class="text-[10px] text-[var(--zh-text-tertiary)] dark:text-[var(--zh-text-secondary)] mt-0.5 block text-right">
              {{ formatMessageTime(msg.createdAt) }}
            </span>
          </div>
          <button class="flex-shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(myUserId)">
            <UserAvatar :src="userStore.userInfo?.avatar" :alt="userStore.userInfo?.nickname" size="sm" />
          </button>
        </div>
      </template>

      <!-- AI助手正在思考 -->
      <div v-if="aiThinking" class="flex items-start gap-2 max-w-[75%]">
        <button class="flex-shrink-0 rounded-full hover:opacity-80 transition-opacity">
          <img :src="aiAvatarUrl" alt="AI" class="w-8 h-8 rounded-full object-cover" />
        </button>
        <div>
          <div class="bg-slate-100 dark:bg-gray-800 rounded-2xl rounded-tl-sm px-3 py-2">
            <span class="block text-[11px] font-semibold text-indigo-500 mb-1">AI助手</span>
            <div class="flex items-center gap-1 py-1">
              <span class="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style="animation-delay:0s"></span>
              <span class="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style="animation-delay:0.2s"></span>
              <span class="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-bounce" style="animation-delay:0.4s"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部哨兵：用于自动滚到底 -->
      <div ref="bottomSentinelRef" class="h-0"></div>
    </div>

    <!-- 消息输入区 -->
    <div class="flex-shrink-0 border-t border-[var(--zh-border)]/60 dark:border-gray-700/60 bg-[var(--zh-bg-elevated)] dark:bg-gray-900 px-3 py-2">
      <!-- 图片上传中提示 -->
      <div v-if="imageUploading" class="flex items-center gap-2 px-2 py-1 mb-1 text-xs text-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-blue-400 rounded-lg">
        <div class="w-3 h-3 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        图片上传中...
      </div>
      <!-- 语音上传中提示 -->
      <div v-if="voiceUploading" class="flex items-center gap-2 px-2 py-1 mb-1 text-xs text-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-blue-400 rounded-lg">
        <div class="w-3 h-3 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        语音上传中...
      </div>
      <!-- 语音录制中 -->
      <div v-if="voiceRecorder.isRecording.value" class="voice-recording-bar">
        <span class="voice-rec-dot" />
        <span class="voice-rec-time">{{ voiceRecorder.formatTime(voiceRecorder.recordingTime.value) }}</span>
        <button class="voice-rec-stop" @click="finishVoiceRecord">发送</button>
        <button class="voice-rec-cancel" @click="cancelVoiceRecord">取消</button>
      </div>
      <div v-else class="flex items-center gap-2">
        <!-- 表情按钮 -->
        <EmojiPicker @select="onEmojiSelect" />
        <!-- 图片按钮 -->
        <button class="input-action-btn" title="发送图片" @click="triggerImageUpload" :disabled="imageUploading">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
        </button>
        <!-- 语音按钮 -->
        <button class="input-action-btn" :class="{ 'ai-active': voiceRecorder.isRecording.value }" title="语音消息" @click="startVoiceRecord">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
          </svg>
        </button>
        <!-- AI助手按钮 -->
        <button class="input-action-btn" :class="{ 'ai-active': aiMode }" title="AI助手" @click="toggleAIMode">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
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
        <div class="input-field-msg" :class="{ focused: inputFocused }">
          <input
            ref="inputRef"
            v-model="inputContent"
            type="text"
            class="input-control-msg"
            :placeholder="aiMode ? '向AI助手提问...' : '输入消息...'"
            :disabled="sending"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            @keydown.enter="onSend"
          />
        </div>
        <button
          class="send-btn-msg"
          :disabled="!inputContent.trim() || sending"
          @click="onSend"
          aria-label="发送"
        >
          <svg v-if="!sending" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
          <div v-else class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
        </button>
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
import { useMessageStore } from '@/stores/message'
import { socialApi } from '@/api'
import { fileApi } from '@/api/file'
import { sanitizeText } from '@/utils/sanitize'
import type { Message } from '@/types'
import VoiceMessage from '@/components/VoiceMessage.vue'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { resolveUrl } = useResourceUrl()

const {
  currentMessages,
  loadingMessages,
  hasMoreMessages,
  currentPage,
  conversations,
} = storeToRefs(messageStore)

const inputContent = ref('')
const sending = ref(false)
const inputFocused = ref(false)
const messageContainerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const bottomSentinelRef = ref<HTMLElement | null>(null)
const isOnline = ref(false)
const isInitialLoad = ref(true)
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)
const previewImageUrl = ref('')
const aiMode = ref(false)
const aiThinking = ref(false)

/** 语音录制 */
const voiceRecorder = useVoiceRecorder()
const voiceUploading = ref(false)

/** 解析语音消息内容 */
const getVoiceUrl = (content: string): string => {
  try { const data = JSON.parse(content); return data.url || content } catch { return content }
}
const getVoiceDuration = (content: string): number => {
  try { const data = JSON.parse(content); return data.duration || 0 } catch { return 0 }
}

const startVoiceRecord = () => { voiceRecorder.startRecording() }

const finishVoiceRecord = async () => {
  const duration = voiceRecorder.recordingTime.value
  voiceRecorder.stopRecording()
  const finalBlob = voiceRecorder.audioBlob.value
  if (!finalBlob) return
  voiceUploading.value = true
  try {
    const voiceUrl = await fileApi.uploadSingleVoice(finalBlob)
    if (!voiceUrl) throw new Error('语音上传失败')
    const content = JSON.stringify({ url: voiceUrl, duration: duration || 0 })
    await messageStore.sendMessage(targetUserId.value, content, 'voice')
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    showAlert(err.message || '语音发送失败，请稍后重试')
  } finally {
    voiceUploading.value = false
    voiceRecorder.cancelRecording()
  }
}

const cancelVoiceRecord = () => { voiceRecorder.cancelRecording() }

/** AI助手头像 - 机器人脸+渐变背景+星芒装饰 */
const aiAvatarUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJnIiB4MT0iMCIgeTE9IjAiIHgyPSIxIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjODE4Y2Y4Ii8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjNjM2NmYxIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjQwIiBoZWlnaHQ9IjQwIiBmaWxsPSJ1cmwoI2JnKSIgcng9IjIwIi8+PHJlY3QgeD0iMTEiIHk9IjE1IiB3aWR0aD0iMTgiIGhlaWdodD0iMTQiIHJ4PSI0IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjk1Ii8+PHJlY3QgeD0iMTUiIHk9IjE5IiB3aWR0aD0iMy41IiBoZWlnaHQ9IjMuNSIgcng9IjEuMiIgZmlsbD0iIzYzNjZmMSIvPjxyZWN0IHg9IjIxLjUiIHk9IjE5IiB3aWR0aD0iMy41IiBoZWlnaHQ9IjMuNSIgcng9IjEuMiIgZmlsbD0iIzYzNjZmMSIvPjxwYXRoIGQ9Ik0xNS41IDI2aDkiIHN0cm9rZT0iIzYzNjZmMSIgc3Ryb2tlLXdpZHRoPSIxLjgiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPjxyZWN0IHg9IjE5IiB5PSI4IiB3aWR0aD0iMiIgaGVpZ2h0PSI3IiByeD0iMSIgZmlsbD0id2hpdGUiLz48Y2lyY2xlIGN4PSIyMCIgY3k9IjciIHI9IjIuNSIgZmlsbD0iI2E1YjRmYyIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iNyIgcj0iMS4yIiBmaWxsPSJ3aGl0ZSIvPjxyZWN0IHg9IjcuNSIgeT0iMjAiIHdpZHRoPSIzLjUiIGhlaWdodD0iNCIgcng9IjEuNSIgZmlsbD0id2hpdGUiIGZpbGwtb3BhY2l0eT0iMC43NSIvPjxyZWN0IHg9IjI5IiB5PSIyMCIgd2lkdGg9IjMuNSIgaGVpZ2h0PSI0IiByeD0iMS41IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjc1Ii8+PHBhdGggZD0iTTMyIDEwbC44IDIuMkwzNSAxM2wtMi4yLjhMMzIgMTZsLS44LTIuMkwyOSAxM2wyLjItLjh6IiBmaWxsPSIjYzdkMmZlIiBmaWxsLW9wYWNpdHk9IjAuOCIvPjxwYXRoIGQ9Ik04IDMwbC42IDEuNUwxMCAzMmwtMS40LjVMOCAzNGwtLjYtMS41TDYgMzJsMS40LS41eiIgZmlsbD0iI2M3ZDJmZSIgZmlsbC1vcGFjaXR5PSIwLjYiLz48L3N2Zz4='

/** 切换AI助手模式 */
const toggleAIMode = () => {
  aiMode.value = !aiMode.value
  inputRef.value?.focus()
}

const targetUserId = computed(() => Number(route.params.id))
const myUserId = computed(() => userStore.userInfo?.id)

/** 解析消息中的资源 URL */
const resolveMsgUrl = (url: string) => resolveUrl(url) || url

/** 点击图片消息预览 */
const previewImage = (url: string) => { previewImageUrl.value = url }

/** 表情选择回调 */
const onEmojiSelect = (emoji: string) => {
  inputContent.value += emoji
  inputRef.value?.focus()
}

/** 触发图片文件选择 */
const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

/** 图片选择后上传并发送 */
const onImageSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  if (!file.type.startsWith('image/')) {
    showAlert('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    showAlert('图片大小不能超过10MB')
    return
  }

  imageUploading.value = true
  try {
    const imageUrl = await fileApi.uploadSingleImage(file)
    if (!imageUrl) {
      throw new Error('图片上传失败')
    }
    await messageStore.sendMessage(targetUserId.value, imageUrl, 'image')
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    showAlert(err.message || '图片发送失败，请稍后重试')
    console.error('[Messages] 发送图片失败:', err.message || err)
  } finally {
    imageUploading.value = false
  }
}

// 点击头像跳转用户主页
const navigateToUser = (userId?: number) => {
  if (userId) router.push(`/user/${userId}`)
}

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
const initChat = async () => {
  if (conversations.value.length === 0) {
    await messageStore.fetchConversations()
  }
  messageStore.currentChatUserId = targetUserId.value
  await messageStore.fetchMessages(targetUserId.value)
  await messageStore.markRead(targetUserId.value)
  await nextTick()
  scrollToBottom()
  fetchOnlineStatus()
  isInitialLoad.value = true
  setTimeout(() => {
    inputRef.value?.focus()
    isInitialLoad.value = false
  }, 300)
}

onMounted(() => initChat())

// 核心修复：动态路由 targetUserId 变化时（切换聊天对象）重新加载
watch(() => targetUserId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  inputContent.value = ''
  await initChat()
})

onUnmounted(() => {
  messageStore.currentChatUserId = null
})

// ==================== 发送消息 ====================
const onSend = async () => {
  const rawContent = inputContent.value.trim()
  if (!rawContent || sending.value) return

  // 检测是否为AI助手模式
  const isAIMention = rawContent.includes('@AI助手')
  if (aiMode.value || isAIMention) {
    const question = rawContent.replace(/@AI助手\s*/g, '').trim()
    if (!question) return

    sending.value = true
    inputContent.value = ''

    try {
      // 1. 先发送用户消息（带 @AI助手 前缀）
      const contentWithPrefix = '@AI助手 ' + question
      await messageStore.sendMessage(targetUserId.value, sanitizeText(contentWithPrefix), 'text')
      await nextTick()
      scrollToBottom()

      // 2. 显示AI思考指示器，调用AI接口
      aiThinking.value = true
      const res = await socialApi.sendAIMessage(targetUserId.value, question)
      aiThinking.value = false
      const raw: any = res.data?.data
      if (raw) {
        const aiMsg: Message = {
          id: Number(raw.id) || Date.now(),
          conversationId: Number(raw.conversationId) || 0,
          senderId: 0,
          sender: {
            id: 0, uid: '0',
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
        if (!messageStore.currentMessages.some(m => m.id === aiMsg.id)) {
          messageStore.currentMessages.push(aiMsg)
        }
      }
      aiMode.value = false
      await nextTick()
      scrollToBottom()
    } catch (err: any) {
      aiThinking.value = false
      inputContent.value = rawContent
      showAlert(err.message || 'AI回复失败，请稍后重试')
      console.error('[Messages] AI消息失败:', err.message || err)
    } finally {
      sending.value = false
      inputRef.value?.focus()
    }
    return
  }

  // 客户端输入净化：移除所有 HTML 标签，防止 XSS
  const content = sanitizeText(rawContent)

  sending.value = true
  inputContent.value = ''

  try {
    await messageStore.sendMessage(targetUserId.value, content, 'text')
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    inputContent.value = rawContent // 恢复原始输入内容
    showAlert(err.message || '消息发送失败，请稍后重试')
    console.error('[Messages] 发送消息失败:', err.message || err)
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
    if ((data.code === 0 || data.code === 200) && data.data) {
      const status = data.data[targetUserId.value]
      isOnline.value = status ?? false
    }
  } catch {
    isOnline.value = false
  }
}

/** 消息时间格式化 */
const formatMessageTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
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
  const prevMs = new Date(prevTime).getTime()
  const currMs = new Date(currTime).getTime()
  if (isNaN(prevMs) || isNaN(currMs)) return 0
  return Math.abs(currMs - prevMs) / 60000
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
  margin: -3rem 0 0 0;
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

/* 统一输入框样式 - 匹配登录页交互 */
.input-field-msg {
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
.input-field-msg.focused {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.input-control-msg {
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
.input-control-msg::placeholder {
  color: var(--zh-text-placeholder);
  font-size: 13px;
}
.input-control-msg:disabled {
  opacity: 0.5;
}

.send-btn-msg {
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
.send-btn-msg:hover:not(:disabled) {
  opacity: 0.9;
  transform: scale(1.05);
}
.send-btn-msg:active:not(:disabled) {
  transform: scale(0.95);
}
.send-btn-msg:disabled {
  opacity: 0.4;
  cursor: not-allowed;
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
.input-action-btn.ai-active {
  background: var(--zh-primary-bg, rgba(99, 102, 241, 0.1));
  color: var(--zh-primary, #6366f1);
}

/* 图片消息 */
.msg-image-wrap {
  border-radius: 12px;
  overflow: hidden;
}
.msg-image-wrap-mine {
  text-align: right;
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

/* 语音消息气泡 */
.voice-bubble-other {
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 18px 18px 18px 4px;
  padding: 4px 6px;
  min-width: 100px;
}
.voice-bubble-mine {
  background: var(--zh-primary, #6366f1);
  border-radius: 18px 18px 4px 18px;
  padding: 4px 6px;
  min-width: 100px;
}
</style>
