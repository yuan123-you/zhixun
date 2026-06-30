<template>
  <!-- 私信聊天页面（移动端全屏 / 桌面端备选入口） -->
  <div class="chat-page h-[calc(100dvh-50px)] md:h-[calc(100dvh-54px)] flex flex-col bg-[var(--zh-bg-elevated)] dark:bg-gray-900">
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
            {{ formatChatTime(msg.createdAt) }}
          </span>
        </div>

        <!-- 对方消息 -->
        <div v-if="msg.senderId !== myUserId" class="flex items-start gap-2 max-w-[75%]">
          <button class="flex-shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="msg.type === 'ai_reply' ? null : navigateToUser(msg.sender?.id)">
            <img v-if="msg.type === 'ai_reply'" :src="AI_AVATAR_URL" alt="AI" class="w-8 h-8 rounded-full object-cover" />
            <UserAvatar v-else :src="msg.sender?.avatar" :alt="msg.sender?.nickname" size="sm" />
          </button>
          <ChatBubble
            :content="msg.content"
            :message-type="msg.type"
            :is-mine="false"
            :time="formatChatTime(msg.createdAt)"
            @preview-image="previewImage"
          />
        </div>

        <!-- 我的消息 -->
        <div v-else class="flex items-start gap-2 justify-end">
          <ChatBubble
            :content="msg.content"
            :message-type="msg.type"
            :is-mine="true"
            :time="formatChatTime(msg.createdAt)"
            @preview-image="previewImage"
          />
          <button class="my-msg-avatar-btn" @click="navigateToUser(myUserId)">
            <UserAvatar :src="userStore.userInfo?.avatar" :alt="userStore.userInfo?.nickname" size="sm" />
          </button>
        </div>
      </template>

      <!-- AI回复占位（不显示思考动画） -->

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
      <VoiceRecordingBar
        v-if="voiceRecorder.isRecording"
        :recording-time="voiceRecorder.recordingTime"
        @finish="finishVoiceRecord"
        @cancel="cancelVoiceRecord"
      />
      <!-- 工具栏行 -->
      <ChatToolbar
        v-if="!voiceRecorder.isRecording"
        :ai-mode="aiMode"
        :is-recording="voiceRecorder.isRecording"
        @emoji="onEmojiSelect"
        @image="triggerImageUpload"
        @file="triggerFileUpload"
        @voice="startVoiceRecord"
        @ai="toggleAIMode"
      />
      <!-- 输入框行 -->
      <div v-if="!voiceRecorder.isRecording" class="msg-input-row">
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

    <!-- 隐藏的 file input -->
    <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
    <input ref="fileInputRef" type="file" style="display:none" @change="onFileSelected" />

    <!-- 图片预览弹层 -->
    <ImagePreviewOverlay
      :src="previewImageUrl ? resolveMsgUrl(previewImageUrl) : ''"
      @close="previewImageUrl = ''"
    />

    <!-- 文件上传进度遮罩 -->
    <UploadOverlay :uploading="fileUploading" :progress="0" />
  </div>
</template>

<script setup lang="ts">
import { useMessageStore } from '@/stores/message'
import { socialApi } from '@/api'
import { fileApi } from '@/api/file'
import { sanitizeText } from '@/utils/sanitize'
import type { Message } from '@/types'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'
import { useChatMedia } from '@/composables/chat/useChatMedia'
import { formatChatTime, getTimeDiff } from '@/composables/chat/useChatTimestamp'
import { AI_AVATAR_URL } from '@/composables/chat/useChatConstants'
import ChatBubble from '@/components/chat/ChatBubble.vue'
import ChatToolbar from '@/components/chat/ChatToolbar.vue'
import VoiceRecordingBar from '@/components/chat/VoiceRecordingBar.vue'
import ImagePreviewOverlay from '@/components/chat/ImagePreviewOverlay.vue'
import UploadOverlay from '@/components/chat/UploadOverlay.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { resolveUrl } = useResourceUrl()
const { resolveMsgUrl, getVoiceUrl, getVoiceDuration, getFileUrl, getFileName, getFileSize } = useChatMedia()

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
const fileInputRef = ref<HTMLInputElement | null>(null)
const fileUploading = ref(false)
const previewImageUrl = ref('')
const aiMode = ref(false)
const aiThinking = ref(false)

/** 语音录制 — reactive() 包裹使模板中 voiceRecorder.isRecording 自动解包为 boolean */
const voiceRecorder = reactive(useVoiceRecorder())
const voiceUploading = ref(false)

const startVoiceRecord = async () => {
  await voiceRecorder.startRecording()
  if (voiceRecorder.recordError) {
    showAlert(voiceRecorder.recordError)
  }
}

const finishVoiceRecord = async () => {
  const duration = voiceRecorder.recordingTime
  const finalBlob = await voiceRecorder.stopRecording()
  if (!finalBlob) { cancelVoiceRecord(); return }
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

/** 切换AI助手模式 */
const toggleAIMode = () => {
  aiMode.value = !aiMode.value
  inputRef.value?.focus()
}

const targetUserId = computed(() => Number(route.params.id))
const myUserId = computed(() => userStore.userInfo?.id)

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

/** 触发文件选择 */
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
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.uploadFile(formData)
    const url = res.data.data
    const content = JSON.stringify({ url, name: file.name, size: file.size })
    await messageStore.sendMessage(targetUserId.value, content, 'file')
    await nextTick()
    scrollToBottom()
  } catch (err: any) {
    showAlert(err.message || '文件上传失败，请稍后重试')
    console.error('[Messages] 发送文件失败:', err.message || err)
  } finally {
    fileUploading.value = false
  }
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

// 点击头像跳转用户主页（携带来源页面信息，确保返回按钮能回到私信详情页）
const navigateToUser = (userId?: number) => {
  if (userId) router.push({ path: `/user/${userId}`, state: { from: route.fullPath } })
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
            avatar: raw.senderAvatar || AI_AVATAR_URL,
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
  const from = (window.history.state as any)?.from
  if (from && typeof from === 'string') {
    router.replace(from)
  } else if (window.history.length > 1) {
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
  margin: 0;
  overflow: hidden;
}

@media (min-width: 768px) {
  .chat-page {
    margin: 0 auto 0 auto;
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

/* 输入框行 - 独立一行 */
.msg-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 我的消息头像按钮 - 覆盖全局 pointer:coarse 44px min-height，确保 32px 头像正确对齐 */
.my-msg-avatar-btn {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  flex-shrink: 0;
  min-height: 0;
  min-width: 0;
  line-height: 1;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: opacity 0.15s;
}
.my-msg-avatar-btn:hover { opacity: 0.8; }
</style>
