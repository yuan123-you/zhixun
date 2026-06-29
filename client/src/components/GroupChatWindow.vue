<template>
  <!-- QQ风格群组聊天窗口 v2 -->
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
      <!-- 右上角更多菜单 -->
      <div class="qq-header-actions">
        <button class="qq-header-btn" @click="showMenu = !showMenu" aria-label="更多">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/>
          </svg>
        </button>
        <button class="qq-header-btn" @click="$emit('toggleMembers')" aria-label="成员列表">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        </button>
        <!-- 下拉菜单 -->
        <Teleport to="body">
          <div v-if="showMenu" class="qq-menu-overlay" @click="showMenu = false">
            <div class="qq-menu" @click.stop>
              <button class="qq-menu-item" @click="showSearch = true; showMenu = false">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                <span>查找聊天记录</span>
              </button>
              <button class="qq-menu-item qq-menu-item-danger" @click="handleLeaveGroup">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
                <span>退出群组</span>
              </button>
            </div>
          </div>
        </Teleport>
      </div>
    </div>

    <!-- 搜索面板 -->
    <GroupSearchPanel
      v-if="showSearch"
      :group-id="group!.id"
      :members="members"
      @close="showSearch = false"
      @locate-message="handleLocateMessage"
    />

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
        <div v-else class="qq-msg-row" :class="{ mine: msg.senderId === currentUserId }" :data-msg-id="msg.id">
          <!-- 他人消息（左） -->
          <template v-if="msg.senderId !== currentUserId">
            <img :src="msg.senderId === 0 ? aiAvatar : (msg.senderAvatar || defaultAvatar)" class="qq-avatar" :alt="msg.senderName" />
            <div class="qq-msg-wrap">
              <span class="qq-msg-name">{{ msg.senderId === 0 ? 'AI助手' : (msg.senderName || '未知用户') }}</span>
              <div
                class="qq-bubble qq-bubble-other"
                :class="{
                  'qq-bubble-ai': msg.messageType === 'ai_reply',
                  'qq-bubble-mentioned': isMentioned(msg)
                }"
              >
                <!-- 文本消息 -->
                <p v-if="msg.messageType === 'text'" class="qq-msg-text" v-html="renderMentions(msg.content, msg.mentionedUserIds)"></p>
                <!-- 图片消息 -->
                <div v-else-if="msg.messageType === 'image'" class="qq-img-msg" @click="previewImage(msg.content)">
                  <img :src="msg.content" alt="图片" class="qq-msg-image" loading="lazy" />
                </div>
                <!-- 文件消息 -->
                <div v-else-if="msg.messageType === 'file'" class="qq-file-msg">
                  <FileCard :content="msg.content" />
                </div>
                <!-- AI回复消息 -->
                <p v-else-if="msg.messageType === 'ai_reply'" class="qq-msg-text qq-ai-text" v-html="renderMentions(msg.content, msg.mentionedUserIds)"></p>
                <!-- 其他 -->
                <p v-else class="qq-msg-text">{{ msg.content }}</p>
              </div>
              <span class="qq-msg-time">{{ formatMsgTime(msg.createdAt) }}</span>
            </div>
          </template>
          <!-- 自己消息（右） -->
          <template v-else>
            <div class="qq-msg-wrap qq-msg-wrap-mine">
              <span class="qq-msg-name qq-msg-name-mine">{{ currentUserName || '我' }}</span>
              <div class="qq-bubble qq-bubble-mine" :class="{ 'qq-bubble-mentioned': isMentioned(msg) }">
                <p v-if="msg.messageType === 'text'" class="qq-msg-text" v-html="renderMentions(msg.content, msg.mentionedUserIds)"></p>
                <div v-else-if="msg.messageType === 'image'" class="qq-img-msg" @click="previewImage(msg.content)">
                  <img :src="msg.content" alt="图片" class="qq-msg-image" loading="lazy" />
                </div>
                <div v-else-if="msg.messageType === 'file'" class="qq-file-msg">
                  <FileCard :content="msg.content" />
                </div>
                <p v-else class="qq-msg-text">{{ msg.content }}</p>
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

    <!-- 图片预览 -->
    <Teleport to="body">
      <div v-if="previewSrc" class="qq-preview-overlay" @click="previewSrc = ''">
        <img :src="previewSrc" class="qq-preview-img" />
      </div>
    </Teleport>

    <!-- 输入区域 -->
    <div class="qq-chat-input">
      <!-- 工具栏 -->
      <div class="qq-toolbar">
        <EmojiPicker @select="(emoji: string) => insertEmoji(emoji)" />
        <button class="qq-tool-btn" @click="triggerImageUpload" title="发送图片">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
        </button>
        <button class="qq-tool-btn" @click="triggerFileUpload" title="发送文件">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" /></svg>
        </button>
        <button class="qq-tool-btn" :class="{ active: aiMode }" @click="toggleAIMode" title="AI助手">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
        </button>
      </div>
      <!-- 输入框 + 发送 -->
      <div class="qq-input-row">
        <div class="qq-input-wrap" :class="{ focused: inputFocused }">
          <!-- @提及下拉框 -->
          <div v-if="showMentionList" class="qq-mention-dropdown">
            <div
              v-for="(member, mIdx) in filteredMembers"
              :key="member.userId"
              class="qq-mention-item"
              :class="{ active: mentionIndex === mIdx }"
              @mousedown.prevent="selectMention(member)"
            >
              <img :src="member.userAvatar || defaultAvatar" class="qq-mention-avatar" />
              <span class="qq-mention-name">{{ member.nickname || member.userName }}</span>
            </div>
            <div v-if="filteredMembers.length === 0" class="qq-mention-empty">无匹配成员</div>
          </div>
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="qq-textarea"
            :placeholder="aiMode ? '向AI助手提问...' : '输入消息...'"
            rows="1"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            @keydown.enter.exact.prevent="sendMsg"
            @keydown.tab.prevent="handleMentionTab"
            @input="onInputChange"
          ></textarea>
        </div>
        <button
          class="qq-send-btn"
          :disabled="!canSend"
          @click="sendMsg"
          aria-label="发送"
        >
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 隐藏的file input -->
    <input ref="imageInput" type="file" accept="image/*" class="hidden" @change="handleImageSelect" />
    <input ref="fileInput" type="file" class="hidden" @change="handleFileSelect" />

    <!-- 上传进度 -->
    <Teleport to="body">
      <div v-if="uploading" class="qq-upload-overlay">
        <div class="qq-upload-card">
          <div class="qq-upload-spinner"></div>
          <p class="qq-upload-text">{{ uploadProgress > 0 ? `上传中 ${uploadProgress}%` : '上传中...' }}</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed, onMounted } from 'vue'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage, GroupMember } from '@/api/group'
import { fileApi } from '@/api/file'
import { sanitizeText } from '@/utils/sanitize'
import { showToast } from '@/composables/useToast'
import EmojiPicker from '@/components/EmojiPicker.vue'
import GroupSearchPanel from '@/components/GroupSearchPanel.vue'

const props = defineProps<{
  group: GroupInfo | null
  currentUserId: number
  currentUserAvatar?: string
  currentUserName?: string
  initialMessages?: GroupMessage[]
  members?: GroupMember[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'toggleMembers'): void
  (e: 'messageSent', msg: GroupMessage): void
  (e: 'messagesLoaded', msgs: GroupMessage[]): void
  (e: 'leave'): void
}>()

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNlMmU4ZjAiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCIgZm9udC1zaXplPSIxNiI+PzwvdGV4dD48L3N2Zz4='
const aiAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiM2MzY2ZjEiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iI2ZmZiIgZm9udC1zaXplPSIxNiIgZm9udC13ZWlnaHQ9ImJvbGQiPkFJPC90ZXh0Pjwvc3ZnPg=='

const messages = ref<GroupMessage[]>([])
const inputText = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
const inputFocused = ref(false)
const showMenu = ref(false)
const showSearch = ref(false)
const previewSrc = ref('')
const aiMode = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
let offset = 0

// @提及相关
const showMentionList = ref(false)
const mentionIndex = ref(0)
const mentionQuery = ref('')
const mentionedIds = ref<number[]>([])
let mentionStartPos = 0

const canSend = computed(() => inputText.value.trim().length > 0)

const filteredMembers = computed(() => {
  if (!props.members) return []
  const q = mentionQuery.value.toLowerCase()
  return props.members.filter(m => {
    if (m.userId === props.currentUserId) return false
    const name = (m.nickname || m.userName || '').toLowerCase()
    return !q || name.includes(q)
  }).slice(0, 6)
})

// 外部传入消息（WebSocket推送）
function addExternalMessage(msg: GroupMessage) {
  if (messages.value.some(m => m.id === msg.id)) return
  messages.value.push(msg)
  nextTick(scrollToBottom)
}

// 滚动到指定消息
function scrollToMessage(msgId: number) {
  const el = msgContainer.value
  if (!el) return
  const target = el.querySelector(`[data-msg-id="${msgId}"]`) as HTMLElement
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target.classList.add('qq-highlight-flash')
    setTimeout(() => target.classList.remove('qq-highlight-flash'), 2000)
  }
}

defineExpose({ addExternalMessage, scrollToMessage })

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
  } catch { /* silent */ }
}

async function sendMsg() {
  const rawText = inputText.value.trim()
  if (!rawText || !props.group) return

  // 检测是否为@AI助手模式
  const isAIBotMention = rawText.includes('@AI助手')
  if (aiMode.value || isAIBotMention) {
    await sendAIMessage(rawText.replace(/@AI助手\s*/g, '').trim())
    return
  }

  const sanitizedText = sanitizeText(rawText)
  const ids = mentionedIds.value.length > 0 ? [...mentionedIds.value] : undefined
  try {
    const res = await groupApi.sendMessage(props.group.id, sanitizedText, 'text', ids)
    const msg = res.data.data
    if (msg && !messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      emit('messageSent', msg)
    }
    resetInput()
  } catch { /* silent */ }
}

async function sendAIMessage(question: string) {
  if (!question || !props.group) return
  // 先显示用户提问
  try {
    const res = await groupApi.sendAIMessage(props.group.id, question)
    const aiMsg = res.data.data
    if (aiMsg && !messages.value.some(m => m.id === aiMsg.id)) {
      messages.value.push(aiMsg)
    }
    resetInput()
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    showToast('AI回复失败，请稍后重试', 'error', { position: 'top-center' })
  }
}

function resetInput() {
  inputText.value = ''
  mentionedIds.value = []
  aiMode.value = false
  if (inputRef.value) inputRef.value.style.height = 'auto'
  await_nextTick_scroll()
}

async function await_nextTick_scroll() {
  await nextTick()
  scrollToBottom()
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

// ========== @提及 ==========

function onInputChange() {
  autoResize()
  detectMention()
}

function detectMention() {
  const text = inputText.value
  const cursorPos = inputRef.value?.selectionStart || text.length
  const before = text.substring(0, cursorPos)
  const match = before.match(/@([^\s@]*)$/)
  if (match) {
    showMentionList.value = true
    mentionQuery.value = match[1]
    mentionStartPos = cursorPos - match[0].length
    mentionIndex.value = 0
  } else {
    showMentionList.value = false
    mentionQuery.value = ''
  }
}

function selectMention(member: GroupMember) {
  const name = member.nickname || member.userName || '未知'
  const text = inputText.value
  const before = text.substring(0, mentionStartPos)
  const after = text.substring(inputRef.value?.selectionStart || text.length)
  inputText.value = before + '@' + name + ' ' + after
  mentionedIds.value.push(member.userId)
  showMentionList.value = false
  nextTick(() => {
    if (inputRef.value) {
      const pos = before.length + name.length + 2
      inputRef.value.selectionStart = pos
      inputRef.value.selectionEnd = pos
      inputRef.value.focus()
    }
  })
}

function handleMentionTab() {
  if (showMentionList.value && filteredMembers.value.length > 0) {
    selectMention(filteredMembers.value[mentionIndex.value])
  }
}

function isMentioned(msg: GroupMessage): boolean {
  return msg.mentionedUserIds?.includes(props.currentUserId) || false
}

function renderMentions(content: string, userIds?: number[]): string {
  if (!content) return ''
  // 高亮 @用户名 文本
  return content.replace(/@(\S+?)(?=\s|$)/g, '<span class="qq-mention-tag">@$1</span>')
}

// ========== 工具栏 ==========

function insertEmoji(emoji: string) {
  inputText.value += emoji
  nextTick(() => inputRef.value?.focus())
}

function toggleAIMode() {
  aiMode.value = !aiMode.value
  nextTick(() => inputRef.value?.focus())
}

function triggerImageUpload() {
  imageInput.value?.click()
}

function triggerFileUpload() {
  fileInput.value?.click()
}

async function handleImageSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || !props.group) return
  // 重置input
  (e.target as HTMLInputElement).value = ''
  uploading.value = true
  uploadProgress.value = 0
  try {
    const url = await fileApi.uploadSingleImage(file, (p) => { uploadProgress.value = p })
    await groupApi.sendMessage(props.group.id, url, 'image')
    // 消息会通过WebSocket推送回来
  } catch {
    showToast('图片上传失败', 'error', { position: 'top-center' })
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

async function handleFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || !props.group) return
  (e.target as HTMLInputElement).value = ''
  uploading.value = true
  uploadProgress.value = 0
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.uploadFile(formData, (p) => { uploadProgress.value = p })
    const url = res.data.data
    const content = JSON.stringify({ url, name: file.name, size: file.size })
    await groupApi.sendMessage(props.group.id, content, 'file')
  } catch {
    showToast('文件上传失败', 'error', { position: 'top-center' })
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

function previewImage(src: string) {
  previewSrc.value = src
}

function handleLeaveGroup() {
  showMenu.value = false
  if (confirm(`确定要退出群组「${props.group?.name}」吗？`)) {
    emit('leave')
  }
}

function handleLocateMessage(msgId: number) {
  showSearch.value = false
  scrollToMessage(msgId)
}

// ========== 时间格式化 ==========

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

<!-- FileCard 子组件 (内联) -->
<script lang="ts">
import { defineComponent, h } from 'vue'

const FileCard = defineComponent({
  name: 'FileCard',
  props: { content: { type: String, required: true } },
  setup(props) {
    return () => {
      try {
        const data = JSON.parse(props.content)
        const size = data.size ? (data.size > 1048576 ? (data.size / 1048576).toFixed(1) + ' MB' : (data.size / 1024).toFixed(1) + ' KB') : ''
        return h('a', {
          href: data.url,
          target: '_blank',
          rel: 'noopener',
          class: 'qq-file-card'
        }, [
          h('div', { class: 'qq-file-icon' }, [
            h('svg', { class: 'w-8 h-8', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
              h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '1.5', d: 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z' })
            ])
          ]),
          h('div', { class: 'qq-file-info' }, [
            h('p', { class: 'qq-file-name' }, data.name || '未知文件'),
            h('p', { class: 'qq-file-size' }, size)
          ]),
          h('div', { class: 'qq-file-download' }, [
            h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
              h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4' })
            ])
          ])
        ])
      } catch {
        return h('a', { href: props.content, target: '_blank', class: 'qq-file-card' }, [
          h('div', { class: 'qq-file-info' }, [h('p', { class: 'qq-file-name' }, '文件')])
        ])
      }
    }
  }
})
export default { components: { FileCard } }
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
  width: 36px; height: 36px;
  border: none; border-radius: 10px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  cursor: pointer; transition: background 0.2s;
  flex-shrink: 0;
}
.qq-back-btn:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-header-info { flex: 1; min-width: 0; }
.qq-chat-title {
  font-weight: 700; font-size: 15px;
  color: var(--zh-text, #1e293b);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.qq-chat-subtitle {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  display: flex; gap: 6px; margin-top: 1px;
}
.qq-group-num { font-family: 'JetBrains Mono', monospace; }
.qq-header-actions { display: flex; gap: 4px; flex-shrink: 0; }
.qq-header-btn {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px;
  border: none; border-radius: 10px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer; transition: all 0.2s;
  flex-shrink: 0;
}
.qq-header-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}

/* ===== 下拉菜单 ===== */
.qq-menu-overlay {
  position: fixed; inset: 0; z-index: 9998;
}
.qq-menu {
  position: absolute; top: 52px; right: 14px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 6px; min-width: 160px;
  z-index: 9999;
}
.qq-menu-item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 10px 12px;
  border: none; border-radius: 8px;
  background: transparent;
  color: var(--zh-text, #1e293b);
  font-size: 13px; cursor: pointer;
  transition: background 0.15s;
}
.qq-menu-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.qq-menu-item-danger { color: #ef4444; }
.qq-menu-item-danger:hover { background: #fef2f2; }

/* ===== 消息列表 ===== */
.qq-chat-msgs {
  flex: 1; overflow-y: auto; overflow-x: hidden;
  padding: 8px 14px;
  display: flex; flex-direction: column; gap: 4px;
  -webkit-overflow-scrolling: touch;
}
.qq-load-more, .qq-no-more {
  text-align: center; padding: 10px 0;
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
  display: flex; align-items: center; justify-content: center;
}

/* 时间分隔线 */
.qq-time-sep { text-align: center; padding: 12px 0 6px; }
.qq-time-sep span {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg, #f8fafc);
  padding: 2px 10px; border-radius: 10px;
}

/* 系统消息 */
.qq-system-msg { text-align: center; padding: 6px 0; }
.qq-system-msg span {
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
  background: var(--zh-bg-hover, #f1f5f9);
  padding: 3px 12px; border-radius: 12px;
}

/* 消息行 - 与私信保持一致 */
.qq-msg-row {
  display: flex; align-items: flex-start; gap: 6px; padding: 4px 0;
}
.qq-msg-row.mine { justify-content: flex-end; }

/* 头像 */
.qq-avatar {
  width: 32px; height: 32px;
  border-radius: 50%; flex-shrink: 0;
  object-fit: cover; margin-top: 2px;
}

/* 消息包装 */
.qq-msg-wrap {
  max-width: 75%; display: flex; flex-direction: column; gap: 2px;
}
.qq-msg-wrap-mine { align-items: flex-end; }

.qq-msg-name {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  padding-left: 2px;
}
.qq-msg-name-mine {
  text-align: right; padding-right: 2px; padding-left: 0;
}

/* 气泡 - 与私信样式统一 */
.qq-bubble {
  padding: 8px 12px;
  font-size: 14px; line-height: 1.55;
  word-break: break-word;
}
.qq-bubble-other {
  background: var(--zh-bg-elevated, #fff);
  color: var(--zh-text, #1e293b);
  border-radius: 12px 12px 12px 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.qq-bubble-mine {
  background: var(--zh-primary, #6366f1);
  color: #fff;
  border-radius: 12px 12px 4px 12px;
}
.qq-bubble-ai {
  border-left: 3px solid var(--zh-primary, #6366f1);
  background: rgba(99, 102, 241, 0.06);
}
.qq-bubble-mentioned {
  border-left: 3px solid var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.15);
}

/* 消息时间 - 始终显示 */
.qq-msg-time {
  font-size: 10px; color: var(--zh-text-tertiary, #94a3b8);
  padding-left: 2px;
}
.qq-msg-time-mine {
  text-align: right; padding-right: 2px; padding-left: 0;
}

/* @提及标签 */
:deep(.qq-mention-tag) {
  color: var(--zh-primary, #6366f1);
  font-weight: 500;
}

/* AI文本样式 */
.qq-ai-text {
  white-space: pre-wrap;
}

/* 图片消息 */
.qq-img-msg { cursor: pointer; }
.qq-msg-image {
  max-width: 200px; max-height: 200px;
  border-radius: 8px; object-fit: cover;
}

/* 文件消息 */
:deep(.qq-file-card) {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 8px;
  text-decoration: none; color: inherit;
  min-width: 200px;
  transition: background 0.15s;
}
:deep(.qq-file-card:hover) { background: var(--zh-bg, #e2e8f0); }
:deep(.qq-file-icon) { color: var(--zh-primary, #6366f1); flex-shrink: 0; }
:deep(.qq-file-info) { flex: 1; min-width: 0; }
:deep(.qq-file-name) {
  font-size: 13px; font-weight: 500;
  color: var(--zh-text, #1e293b);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin: 0;
}
:deep(.qq-file-size) {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8); margin: 2px 0 0;
}
:deep(.qq-file-download) {
  color: var(--zh-text-tertiary, #94a3b8); flex-shrink: 0;
}

/* 空状态 */
.qq-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  flex: 1; color: var(--zh-text-tertiary, #94a3b8); font-size: 14px;
}

/* ===== 输入区域 ===== */
.qq-chat-input {
  padding: 6px 14px 10px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--zh-border, #e5e7eb);
  background: var(--zh-bg-elevated, #fff);
  flex-shrink: 0;
}
.qq-toolbar {
  display: flex; align-items: center; gap: 2px;
  padding-bottom: 6px;
}
.qq-tool-btn {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px;
  border: none; border-radius: 6px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer; transition: all 0.15s;
  min-height: 0; min-width: 0;
}
.qq-tool-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}
.qq-tool-btn.active {
  background: var(--zh-primary-bg, rgba(99,102,241,0.1));
  color: var(--zh-primary, #6366f1);
}

.qq-input-row {
  display: flex; align-items: flex-end; gap: 8px;
}
.qq-input-wrap {
  flex: 1; min-height: 38px; max-height: 120px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 20px;
  background: var(--zh-bg, #f8fafc);
  overflow: visible; position: relative;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.qq-input-wrap.focused {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}
.qq-textarea {
  width: 100%; min-height: 38px; max-height: 120px;
  border: none; outline: none; background: transparent;
  padding: 8px 16px;
  font-size: 14px; color: var(--zh-text, #1e293b);
  font-family: inherit; resize: none; line-height: 1.5;
}
.qq-textarea::placeholder {
  color: var(--zh-text-placeholder, #cbd5e1); font-size: 13px;
}
.qq-send-btn {
  display: flex; align-items: center; justify-content: center;
  width: 38px; height: 38px;
  border: none; border-radius: 50%;
  background: var(--zh-primary, #6366f1);
  color: #fff; cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
  flex-shrink: 0; min-height: 0; min-width: 0;
}
.qq-send-btn:hover:not(:disabled) { opacity: 0.9; transform: scale(1.05); }
.qq-send-btn:active:not(:disabled) { transform: scale(0.95); }
.qq-send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ===== @提及下拉框 ===== */
.qq-mention-dropdown {
  position: absolute; bottom: 100%; left: 8px; right: 8px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 10px;
  box-shadow: 0 -4px 16px rgba(0,0,0,0.1);
  max-height: 200px; overflow-y: auto;
  z-index: 10; margin-bottom: 4px;
}
.qq-mention-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; cursor: pointer;
  transition: background 0.1s;
}
.qq-mention-item:hover, .qq-mention-item.active {
  background: var(--zh-bg-hover, #f1f5f9);
}
.qq-mention-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  object-fit: cover;
}
.qq-mention-name {
  font-size: 13px; color: var(--zh-text, #1e293b);
}
.qq-mention-empty {
  padding: 12px; text-align: center;
  font-size: 12px; color: var(--zh-text-tertiary, #94a3b8);
}

/* ===== 图片预览 ===== */
.qq-preview-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,0.85);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.qq-preview-img {
  max-width: 90vw; max-height: 90vh;
  border-radius: 8px; object-fit: contain;
}

/* ===== 上传遮罩 ===== */
.qq-upload-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
}
.qq-upload-card {
  background: var(--zh-bg-elevated, #fff);
  border-radius: 16px; padding: 24px 32px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.qq-upload-spinner {
  width: 32px; height: 32px; margin: 0 auto 12px;
  border: 3px solid var(--zh-border, #e5e7eb);
  border-top-color: var(--zh-primary, #6366f1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.qq-upload-text {
  font-size: 14px; color: var(--zh-text, #1e293b);
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 高亮闪烁 ===== */
:deep(.qq-highlight-flash) {
  animation: highlightPulse 2s ease-out;
}
@keyframes highlightPulse {
  0% { background: rgba(99, 102, 241, 0.2); }
  100% { background: transparent; }
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .qq-chat-header { padding: 8px 12px; }
  .qq-chat-msgs { padding: 6px 10px; }
  .qq-chat-input { padding: 6px 10px 8px; padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px)); }
  .qq-avatar { width: 28px; height: 28px; }
  .qq-bubble { font-size: 13.5px; padding: 7px 10px; }
  .qq-msg-wrap { max-width: 80%; }
  .qq-msg-image { max-width: 160px; max-height: 160px; }
  .qq-menu { top: 48px; right: 10px; }
}
</style>
