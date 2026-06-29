<template>
  <!-- 消息与通知页面 - QQ风格 -->
  <div class="h-[calc(100dvh-3.75rem)] flex flex-col">
    <!-- 顶部Tab栏 -->
    <div class="flex items-center border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
      <button v-if="activeMainTab === 'messages' && activeConversation" class="md:hidden p-1.5 text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] rounded-lg mr-1 shrink-0" @click="activeConversation = null">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
      </button>
      <button
        v-for="tab in mainTabs"
        :key="tab.key"
        class="flex-1 py-2.5 text-sm font-medium text-center transition-colors relative"
        :class="activeMainTab === tab.key ? 'text-primary' : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]'"
        @click="switchMainTab(tab.key)"
      >
        <span>{{ tab.label }}</span>
        <span v-if="tab.unread > 0" class="ml-1 inline-flex items-center justify-center min-w-[1.125rem] h-4.5 bg-danger text-white text-2xs rounded-full px-1.5">{{ tab.unread > 99 ? '99+' : tab.unread }}</span>
        <div v-if="activeMainTab === tab.key" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-primary rounded-full" />
      </button>
    </div>

    <!-- ===== 私信Tab ===== -->
    <template v-if="activeMainTab === 'messages'">
      <div class="flex-1 flex overflow-hidden">
        <!-- 左侧会话列表 -->
        <div class="w-full md:w-80 border-r border-[var(--zh-border)] flex flex-col shrink-0" :class="{ 'hidden md:flex': activeConversation }">
          <div class="p-3 border-b border-[var(--zh-border)]">
            <input v-model="conversationSearch" type="text" class="input text-sm bg-[var(--zh-bg-hover)]" placeholder="搜索会话..." />
          </div>
          <div class="flex-1 overflow-y-auto">
            <div v-if="loadingConv" class="flex items-center justify-center py-16">
              <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            </div>
            <ErrorRetry v-else-if="conversationsError" :message="conversationsError" :on-retry="loadConversations" />
            <template v-else>
              <div
                v-for="conv in filteredConversations"
                :key="conv.id"
                role="button"
                tabindex="0"
                class="w-full flex items-center gap-3 px-3 py-3 hover:bg-[var(--zh-bg-hover)] transition-colors text-left cursor-pointer"
                :class="{ 'bg-primary-50/50': activeConversation?.id === conv.id }"
                @click="selectConversation(conv)"
                @keydown.enter="selectConversation(conv)"
                @keydown.space.prevent="selectConversation(conv)"
              >
                <div
                  role="button"
                  tabindex="0"
                  class="relative shrink-0 rounded-full hover:opacity-80 transition-opacity"
                  @click.stop="navigateToUser(conv.user?.id)"
                  @keydown.enter.stop="navigateToUser(conv.user?.id)"
                >
                  <UserAvatar :src="conv.user?.avatar" alt="" size="lg" />
                  <span v-if="conv.user?.isOnline" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-[var(--zh-text)] truncate">{{ conv.user?.nickname }}</span>
                    <span class="text-2xs text-[var(--zh-text-tertiary)] shrink-0 ml-1">{{ formatConvTime(conv.updatedAt) }}</span>
                  </div>
                  <p class="text-xs text-[var(--zh-text-secondary)] truncate mt-0.5">{{ getConvLastMessage(conv) }}</p>
                </div>
                <span v-if="conv.unreadCount > 0" class="w-5 h-5 bg-danger text-white text-2xs rounded-full flex items-center justify-center shrink-0">
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </div>
              <EmptyState v-if="!loadingConv && conversations.length === 0" title="暂无会话" description="开始一段新的对话吧" />
            </template>
          </div>
        </div>

        <!-- 右侧聊天区域 -->
        <div class="flex-1 flex flex-col min-w-0" :class="{ 'hidden md:flex': !activeConversation }">
          <template v-if="activeConversation">
            <!-- 聊天头部 -->
            <div class="flex items-center gap-2 px-3 py-2 border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
              <button class="shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(activeConversation.user?.id)">
                <UserAvatar :src="activeConversation.user?.avatar" alt="" size="sm" />
              </button>
              <div class="min-w-0">
                <p class="font-medium text-[var(--zh-text)] text-sm leading-tight truncate">{{ activeConversation.user?.nickname }}</p>
                <p class="text-2xs leading-tight" :class="activeConversation.user?.isOnline ? 'text-green-500' : 'text-[var(--zh-text-tertiary)]'">{{ activeConversation.user?.isOnline ? '在线' : '离线' }}</p>
              </div>
            </div>
            <!-- 消息列表 -->
            <div ref="msgListRef" class="flex-1 overflow-y-auto px-3 py-3 space-y-2 bg-[var(--zh-bg-hover)]">
              <div class="text-center text-2xs text-[var(--zh-text-tertiary)] mb-2" v-if="messages.length > 0">以下为私信内容</div>
              <div v-for="msg in messages" :key="msg.id" class="flex" :class="isMyMsg(msg) ? 'justify-end' : 'justify-start'">
                <!-- 对方消息（左） -->
                <div v-if="!isMyMsg(msg)" class="flex items-start gap-1.5 max-w-[80%]">
                  <button class="shrink-0 rounded-full hover:opacity-80 transition-opacity" @click="navigateToUser(msg.sender?.id || msg.senderId)">
                    <UserAvatar :src="msg.sender?.avatar" alt="" size="sm" />
                  </button>
                  <div class="min-w-0 flex-1">
                    <div class="inline-block max-w-full bg-[var(--zh-bg-elevated)] rounded-xl rounded-bl-sm px-2.5 py-1.5 shadow-sm">
                      <p class="text-sm text-[var(--zh-text)] leading-snug whitespace-pre-wrap break-all">{{ msg.content }}</p>
                    </div>
                    <p class="text-2xs text-[var(--zh-text-tertiary)] mt-0.5 ml-1 leading-none">{{ formatMsgTime(msg.createdAt) }}</p>
                  </div>
                </div>
                <!-- 我的消息（右） -->
                <div v-else class="max-w-[80%]">
                  <div class="inline-block max-w-full bg-primary text-white rounded-xl rounded-br-sm px-2.5 py-1.5">
                    <p class="text-sm leading-snug whitespace-pre-wrap break-all">{{ msg.content }}</p>
                  </div>
                  <p class="text-2xs text-[var(--zh-text-tertiary)] mt-0.5 mr-1 text-right leading-none">{{ formatMsgTime(msg.createdAt) }}</p>
                </div>
              </div>
              <div v-if="messages.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
                <svg class="w-12 h-12 text-slate-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                <p class="text-xs text-[var(--zh-text-tertiary)]">还没有消息，发送第一条吧</p>
              </div>
            </div>
            <!-- 输入框 -->
            <div class="px-2 py-2 border-t border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0 safe-bottom">
              <div class="flex items-center gap-1.5">
                <EmojiPicker @select="(emoji: string) => inputContent += emoji" />
                <VoiceRecorderButton @send="(blob: Blob) => handleVoiceSend(blob)" />
                <input
                  v-model="inputContent"
                  type="text"
                  class="flex-1 input text-sm py-1.5 bg-[var(--zh-bg-hover)]"
                  placeholder="输入消息..."
                  @keydown.enter="sendMessage"
                />
                <button class="btn-primary text-sm px-3 py-1.5" :disabled="!inputContent.trim() || sendingMessage" @click="sendMessage">
                  {{ sendingMessage ? '发送中...' : '发送' }}
                </button>
              </div>
            </div>
          </template>
          <!-- 空状态 -->
          <div v-else class="flex-1 flex items-center justify-center bg-[var(--zh-bg-hover)]">
            <div class="text-center">
              <svg class="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-[var(--zh-text-secondary)] text-sm">选择一个会话开始聊天</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 通知Tab ===== -->
    <template v-if="activeMainTab === 'notifications'">
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- 通知操作栏 -->
        <div class="flex items-center justify-between px-4 py-2 border-b border-[var(--zh-border)] bg-[var(--zh-bg-elevated)] shrink-0">
          <div class="flex items-center gap-1 overflow-x-auto no-scrollbar">
            <button
              v-for="tab in notiTabs"
              :key="tab.value"
              class="shrink-0 px-3 py-1.5 text-xs rounded-full transition-colors"
              :class="activeNotiTab === tab.value ? 'bg-primary text-white' : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)] bg-[var(--zh-bg-hover)]'"
              @click="switchNotiTab(tab.value)"
            >{{ tab.label }}</button>
          </div>
          <div class="flex items-center gap-1 shrink-0 ml-2">
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]" @click="exitNotiBatch">取消</button>
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-danger" :disabled="notiSelectedIds.length === 0 || notiDeleting" @click="batchDeleteNoti">
              {{ notiDeleting ? '删除中...' : `删除(${notiSelectedIds.length})` }}
            </button>
            <button v-if="!notiBatchMode && notifications.length > 0" class="text-xs px-2 py-1 text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]" @click="enterNotiBatch">批量管理</button>
            <button v-if="!notiBatchMode && notiUnreadTotal > 0" class="text-xs px-2 py-1 text-primary hover:text-primary-600" :disabled="notiMarkingRead" @click="markAllNotiRead">
              {{ notiMarkingRead ? '标记中...' : '全部已读' }}
            </button>
          </div>
        </div>
        <!-- 通知列表 -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="notiLoading && notifications.length === 0" class="flex justify-center py-16">
            <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
          <ErrorRetry v-else-if="notiError" :message="notiError" :on-retry="fetchNotis" />
          <EmptyState v-else-if="notifications.length === 0" title="暂无通知" />
          <div v-else>
            <div
              v-for="item in notifications"
              :key="item.id"
              class="flex items-start gap-3 px-4 py-3 hover:bg-[var(--zh-bg-hover)] transition-colors border-b border-slate-50"
              :class="{ 'bg-primary-50/30': !item.isRead }"
              @click="handleNotiClick(item)"
            >
              <input v-if="notiBatchMode" type="checkbox" :checked="notiSelectedIds.includes(item.id)" class="mt-0.5 w-4 h-4 rounded border-[var(--zh-border)] text-primary" @click.stop="toggleNotiSelect(item.id)" />
              <!-- 图标 -->
              <div class="shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                :class="{
                  'bg-[var(--zh-primary-bg)]': item.type === 1, 'bg-amber-50': item.type === 2,
                  'bg-red-50': item.type === 3, 'bg-purple-50': item.type === 4,
                  'bg-emerald-50': item.type === 5,
                }"
              >
                <svg v-if="item.type === 1" class="w-5 h-5 text-[var(--zh-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 2" class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 3" class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                <svg v-else-if="item.type === 4" class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>
                <svg v-else-if="item.type === 5" class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2">
                  <p class="text-sm leading-snug" :class="item.isRead ? 'text-[var(--zh-text-secondary)]' : 'text-[var(--zh-text)] font-medium'">{{ item.title }}</p>
                  <span v-if="!item.isRead" class="shrink-0 w-2 h-2 bg-primary rounded-full mt-1.5" />
                </div>
                <p v-if="item.content" class="text-xs text-[var(--zh-text-tertiary)] mt-1 line-clamp-2">{{ item.content }}</p>
                <span class="text-2xs text-[var(--zh-text-tertiary)] mt-1">{{ formatNotiTime(item.createdAt) }}</span>
              </div>
            </div>
            <div v-if="notiHasMore" ref="notiSentinelRef" class="flex justify-center py-3">
              <div v-if="notiLoadingMore" class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 消息中心 - QQ风格：私信 + 通知 */
import type { Conversation, Message, Notification } from '@/types'
import { notificationApi } from '@/api/notification'
import { socialApi } from '@/api'
import { showToast } from '@/composables/useToast'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })
const router = useRouter()

// ===== 主Tab =====
const activeMainTab = ref<'messages' | 'notifications'>('messages')
// 未读私信数直接从会话列表计算，避免接口异常导致角标错误
const msgUnread = computed(() => conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0))

const mainTabs = computed(() => [
  { key: 'messages' as const, label: '私信', unread: msgUnread.value },
  { key: 'notifications' as const, label: '通知', unread: notiUnreadTotal.value },
])

const switchMainTab = (key: string) => {
  activeMainTab.value = key as 'messages' | 'notifications'
}

// ===== 私信模块 =====
const conversations = ref<Conversation[]>([])
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const conversationSearch = ref('')
const loadingConv = ref(false)
const conversationsError = ref('')
const inputContent = ref('')
const msgListRef = ref<HTMLElement | null>(null)
const sendingMessage = ref(false)

// 点击头像跳转用户主页
const navigateToUser = (userId?: number) => {
  if (userId) router.push(`/user/${userId}`)
}

const filteredConversations = computed(() => {
  const kw = conversationSearch.value.trim().toLowerCase()
  return kw ? conversations.value.filter(c => c.user?.nickname?.toLowerCase().includes(kw)) : conversations.value
})

const loadConversations = async () => {
  loadingConv.value = true
  conversationsError.value = ''
  try {
    const { data } = await socialApi.getConversations({ page: 1, pageSize: 100 })
    // 兼容后端返回的扁平结构（userId/nickname/avatar）与旧版本的嵌套结构（user.id/user.nickname/...）
    conversations.value = (data.data.list || []).map((c: any) => {
      if (c.user) return c
      return {
        ...c,
        user: {
          id: c.userId,
          nickname: c.nickname,
          avatar: c.avatar,
          isOnline: !!c.isOnline,
        },
      }
    })
  } catch {
    conversationsError.value = '私信列表加载失败，请检查网络连接后重试'
  } finally {
    loadingConv.value = false
  }
}

const selectConversation = async (conv: Conversation) => {
  activeConversation.value = conv
  // 兼容扁平结构与嵌套结构
  const targetUserId = (conv as any).user?.id ?? (conv as any).userId
  if (!targetUserId) return
  try {
    const { data } = await socialApi.getMessages(targetUserId, { page: 1, pageSize: 30 })
    const me = userStore.userInfo
    const rawList = (data.data && data.data.list) || []
    messages.value = rawList.map((m: any) => transformMsg(m, me))
  } catch {
    showToast('消息记录加载失败，请重试', 'error')
    messages.value = []
  }
  if (conv.unreadCount > 0) {
    try {
      await socialApi.markConversationRead(targetUserId)
      conv.unreadCount = 0
    } catch {
      // 标记已读失败不影响浏览消息，静默处理
    }
  }
}

const sendMessage = async () => {
  if (!inputContent.value.trim() || !activeConversation.value || sendingMessage.value) return
  // 兼容扁平结构与嵌套结构
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) return
  const content = inputContent.value.trim()
  const me = userStore.userInfo
  // 乐观更新：先在前端列表中插入一条"发送中"的消息，让用户立刻看到反馈
  const tempId = -Date.now()
  const tempMsg: Message = {
    id: tempId,
    conversationId: 0,
    senderId: me?.id || 0,
    sender: me || { id: 0, uid: '0', username: '', nickname: '', avatar: '', bio: '', email: '', phone: '', gender: 0 as any, birthday: '', followCount: 0, followerCount: 0, articleCount: 0, likeCount: 0, isFollowing: false, createdAt: '' },
    content,
    type: 0 as any,
    isRead: true,
    createdAt: new Date().toISOString(),
  }
  messages.value.push(tempMsg)
  inputContent.value = ''
  sendingMessage.value = true
  nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
  try {
    const { data } = await socialApi.sendMessage(targetUserId, { content })
    if (data && data.data) {
      // 用后端返回的正式消息替换乐观消息
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx >= 0) {
        const real = transformMsg(data.data, me)
        messages.value.splice(idx, 1, real)
      }
      // 同步更新会话列表中的最后消息预览
      const conv = conversations.value.find(c => c.user?.id === targetUserId)
      if (conv) (conv as any).lastMessage = content
    }
    nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
  } catch (e: any) {
    // 发送失败：移除乐观消息，恢复输入框内容
    const idx = messages.value.findIndex(m => m.id === tempId)
    if (idx >= 0) messages.value.splice(idx, 1)
    inputContent.value = content
    const errMsg = e?.response?.data?.message || e?.message || '消息发送失败'
    showToast(`${errMsg}，请检查网络后重试`, 'error')
  } finally {
    sendingMessage.value = false
  }
}

const handleVoiceSend = async (blob: Blob) => {
  if (!activeConversation.value || sendingMessage.value) return
  // 兼容扁平结构与嵌套结构
  const targetUserId = (activeConversation.value as any).user?.id ?? (activeConversation.value as any).userId
  if (!targetUserId) return

  // 语音文件校验
  if (!blob || blob.size === 0) {
    showToast('语音录制失败，请重新录制', 'error')
    return
  }
  if (blob.size > 10 * 1024 * 1024) {
    showToast('语音文件大小超过 10MB 限制，请缩短录制时间', 'error')
    return
  }

  sendingMessage.value = true
  const me = userStore.userInfo
  try {
    const formData = new FormData()
    formData.append('file', blob, 'voice.webm')
    const { upload } = useApi()
    const uploadRes = await upload<any>('/files/upload/voice', formData)
    const voiceUrl = uploadRes.data?.data
    if (voiceUrl) {
      const { data } = await socialApi.sendMessage(targetUserId, { content: voiceUrl, type: 1 })
      if (data && data.data) {
        const idx = messages.value.length
        const real = transformMsg(data.data, me)
        messages.value.push(real)
        nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
        // 同步更新会话列表预览
        const conv = conversations.value.find(c => c.user?.id === targetUserId)
        if (conv) (conv as any).lastMessage = '[语音]'
      } else {
        showToast('语音消息发送失败', 'error')
      }
    } else {
      showToast('语音上传失败：服务器未返回地址', 'error')
    }
  } catch (e: any) {
    if (e?.code === 'ERR_CANCELED' || e?.name === 'CanceledError') return
    const errMsg = e?.response?.data?.message || e?.message || '语音消息发送失败'
    showToast(`${errMsg}，请检查网络后重试`, 'error')
  } finally {
    sendingMessage.value = false
  }
}

const isMyMsg = (msg: Message) => msg.senderId === userStore.userInfo?.id

const formatConvTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const n = new Date(), diff = n.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

/**
 * 会话列表中的最后消息预览
 * 后端 ConversationVO.lastMessage 为 String（已解密的内容文本），
 * 但老数据可能是 Message 对象；两者都做兼容。
 */
const getConvLastMessage = (conv: any): string => {
  if (!conv) return '暂无消息'
  const lm = conv.lastMessage
  if (lm == null) return '暂无消息'
  if (typeof lm === 'string') return lm || '暂无消息'
  if (typeof lm === 'object') {
    const type = lm.type
    if (type === 1) return '[图片]'
    if (type === 2) return '[系统消息]'
    return lm.content || '暂无消息'
  }
  return '暂无消息'
}

/**
 * 单条消息的发送时间（紧凑显示）
 */
const formatMsgTime = (s: string) => {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return ''
  const n = new Date()
  const pad = (x: number) => x.toString().padStart(2, '0')
  const sameDay = d.toDateString() === n.toDateString()
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 将后端 MessageVO 转换为前端 Message 类型
 * 兼容：嵌套 sender 字段为空、扁平 senderId/senderNickname/senderAvatar
 */
const transformMsg = (raw: any, me: any): Message => {
  const senderId = Number(raw?.senderId) || 0
  const senderInfo: any = raw?.sender || {}
  const senderNickname = senderInfo.nickname ?? raw?.senderNickname ?? me?.nickname ?? '用户'
  const senderAvatar = senderInfo.avatar ?? raw?.senderAvatar ?? me?.avatar ?? ''
  const sender = senderId === me?.id && me
    ? me
    : {
        id: senderId,
        uid: String(senderId),
        username: senderNickname,
        nickname: senderNickname,
        avatar: senderAvatar,
        bio: '',
        email: '',
        phone: '',
        gender: 0 as any,
        birthday: '',
        followCount: 0,
        followerCount: 0,
        articleCount: 0,
        likeCount: 0,
        isFollowing: false,
        createdAt: '',
      }
  return {
    id: Number(raw?.id) || 0,
    conversationId: Number(raw?.conversationId) || 0,
    senderId,
    sender,
    content: raw?.content || '',
    type: Number(raw?.type) || 0,
    isRead: raw?.isRead === 1 || raw?.isRead === true,
    createdAt: raw?.createdAt || new Date().toISOString(),
  }
}

// ===== 通知模块 =====
const notiTabs = [
  { label: '全部', value: 0 }, { label: '系统', value: 1 }, { label: '审核', value: 2 },
  { label: '互动', value: 3 }, { label: '关注', value: 4 }, { label: '私信', value: 5 },
]
const activeNotiTab = ref(0)
const notifications = ref<Notification[]>([])
const notiLoading = ref(false)
const notiLoadingMore = ref(false)
const notiError = ref('')
const notiPage = ref(1)
const notiTotal = ref(0)
const notiBatchMode = ref(false)
const notiSelectedIds = ref<number[]>([])
const notiSentinelRef = ref<HTMLElement | null>(null)
const notiUnreadTotal = computed(() => notificationStore.unreadCount)
const notiHasMore = computed(() => notifications.value.length < notiTotal.value)
const notiMarkingRead = ref(false)
const notiDeleting = ref(false)

const switchNotiTab = (type: number) => {
  activeNotiTab.value = type; notiPage.value = 1; notifications.value = []; notiSelectedIds.value = []; fetchNotis()
}

const fetchNotis = async () => {
  notiLoading.value = true; notiError.value = ''
  try {
    const params: any = { page: notiPage.value, pageSize: 20 }
    if (activeNotiTab.value) params.type = activeNotiTab.value
    const { data } = await notificationApi.getNotifications(params)
    const d = data.data; notifications.value = d?.list || []; notiTotal.value = d?.total || 0
  } catch {
    notiError.value = '通知列表加载失败，请检查网络连接后重试'
  } finally {
    notiLoading.value = false
  }
}

const loadMoreNoti = async () => {
  notiLoadingMore.value = true; notiPage.value++
  try {
    const params: any = { page: notiPage.value, pageSize: 20 }
    if (activeNotiTab.value) params.type = activeNotiTab.value
    const { data } = await notificationApi.getNotifications(params)
    notifications.value.push(...(data.data?.list || [])); notiTotal.value = data.data?.total || 0
  } catch {
    notiPage.value--
    showToast('加载更多通知失败，请下滑重试', 'error')
  } finally {
    notiLoadingMore.value = false
  }
}

const handleNotiClick = async (n: Notification) => {
  if (notiBatchMode.value) { toggleNotiSelect(n.id); return }
  if (!n.isRead) {
    try {
      await notificationApi.markAsRead(n.id)
      notificationStore.markAsRead(n.id)
      n.isRead = true
    } catch {
      showToast('标记已读失败，请重试', 'error')
    }
  }
}

const markAllNotiRead = async () => {
  if (notiMarkingRead.value) return
  notiMarkingRead.value = true
  try {
    await notificationApi.markAllAsRead()
    notificationStore.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    showToast('已全部标记为已读')
  } catch {
    showToast('全部标记已读失败，请重试', 'error')
  } finally {
    notiMarkingRead.value = false
  }
}

const enterNotiBatch = () => { notiBatchMode.value = true; notiSelectedIds.value = [] }
const exitNotiBatch = () => { notiBatchMode.value = false; notiSelectedIds.value = [] }
const toggleNotiSelect = (id: number) => {
  const i = notiSelectedIds.value.indexOf(id)
  i > -1 ? notiSelectedIds.value.splice(i, 1) : notiSelectedIds.value.push(id)
}

const batchDeleteNoti = async () => {
  if (!notiSelectedIds.value.length || notiDeleting.value) return
  notiDeleting.value = true
  try {
    await notificationApi.batchDeleteNotifications(notiSelectedIds.value)
    const set = new Set(notiSelectedIds.value)
    notifications.value = notifications.value.filter(n => !set.has(n.id))
    notiTotal.value -= set.size
    notiSelectedIds.value = []
    showToast(`已删除 ${set.size} 条通知`)
  } catch {
    showToast('批量删除失败，请重试', 'error')
  } finally {
    notiDeleting.value = false
  }
}

const formatNotiTime = (s: string) => {
  if (!s) return ''
  const d = new Date(s), n = new Date(), m = Math.floor((n.getTime() - d.getTime()) / 60000)
  if (m < 1) return '刚刚'; if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60); if (h < 24) return `${h}小时前`
  const days = Math.floor(h / 24); if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 无限滚动
let io: IntersectionObserver | null = null
onMounted(() => {
  loadConversations()
  fetchNotis()
  if (true) {
    io = new IntersectionObserver(([e]) => { if (e?.isIntersecting && notiHasMore.value && !notiLoadingMore.value) loadMoreNoti() }, { rootMargin: '200px' })
    watch(notiSentinelRef, el => { io?.disconnect(); if (el) io?.observe(el) }, { immediate: true })
  }
})
onUnmounted(() => io?.disconnect())

useHead({ title: () => '消息' + ' - 知讯' })
</script>
