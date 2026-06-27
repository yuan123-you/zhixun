<template>
  <!-- 消息与通知页面 - QQ风格 -->
  <div class="h-[calc(100vh-3rem)] flex flex-col">
    <!-- 顶部Tab栏 -->
    <div class="flex items-center border-b border-slate-200 bg-white shrink-0">
      <button
        v-for="tab in mainTabs"
        :key="tab.key"
        class="flex-1 py-2.5 text-sm font-medium text-center transition-colors relative"
        :class="activeMainTab === tab.key ? 'text-primary' : 'text-slate-500 hover:text-slate-700'"
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
        <div class="w-full md:w-80 border-r border-slate-200 flex flex-col shrink-0" :class="{ 'hidden md:flex': activeConversation }">
          <div class="p-3 border-b border-slate-200">
            <input v-model="conversationSearch" type="text" class="input text-sm bg-slate-50" placeholder="搜索会话..." />
          </div>
          <div class="flex-1 overflow-y-auto">
            <div v-if="loadingConv" class="flex items-center justify-center py-16">
              <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            </div>
            <ErrorRetry v-else-if="conversationsError" :message="conversationsError" :on-retry="loadConversations" />
            <template v-else>
              <button
                v-for="conv in filteredConversations"
                :key="conv.id"
                class="w-full flex items-center gap-3 px-3 py-3 hover:bg-slate-50 transition-colors text-left"
                :class="{ 'bg-primary-50/50': activeConversation?.id === conv.id }"
                @click="selectConversation(conv)"
              >
                <div class="relative shrink-0">
                  <UserAvatar :src="conv.user?.avatar" alt="" size="lg" />
                  <span v-if="conv.user?.isOnline" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-medium text-slate-900 truncate">{{ conv.user?.nickname }}</span>
                    <span class="text-2xs text-gray-400 shrink-0 ml-1">{{ formatConvTime(conv.updatedAt) }}</span>
                  </div>
                  <p class="text-xs text-slate-500 truncate mt-0.5">{{ conv.lastMessage?.content || '暂无消息' }}</p>
                </div>
                <span v-if="conv.unreadCount > 0" class="w-5 h-5 bg-danger text-white text-2xs rounded-full flex items-center justify-center shrink-0">
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </button>
              <EmptyState v-if="!loadingConv && conversations.length === 0" title="暂无会话" description="开始一段新的对话吧" />
            </template>
          </div>
        </div>

        <!-- 右侧聊天区域 -->
        <div class="flex-1 flex flex-col min-w-0" :class="{ 'hidden md:flex': !activeConversation }">
          <template v-if="activeConversation">
            <!-- 聊天头部 -->
            <div class="flex items-center gap-3 px-4 py-3 border-b border-slate-200 bg-white shrink-0">
              <button class="md:hidden p-1.5 text-slate-600 hover:bg-slate-50 rounded-lg" @click="activeConversation = null">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
              </button>
              <UserAvatar :src="activeConversation.user?.avatar" alt="" size="md" />
              <div class="min-w-0">
                <p class="font-medium text-slate-900 text-sm truncate">{{ activeConversation.user?.nickname }}</p>
                <p class="text-xs" :class="activeConversation.user?.isOnline ? 'text-green-500' : 'text-gray-400'">{{ activeConversation.user?.isOnline ? '在线' : '离线' }}</p>
              </div>
            </div>
            <!-- 消息列表 -->
            <div ref="msgListRef" class="flex-1 overflow-y-auto px-3 py-4 space-y-3 bg-slate-50">
              <div class="text-center text-xs text-slate-400 mb-3" v-if="messages.length > 0">以下为私信内容</div>
              <div v-for="msg in messages" :key="msg.id" class="flex" :class="isMyMsg(msg) ? 'justify-end' : 'justify-start'">
                <div v-if="!isMyMsg(msg)" class="flex items-end gap-2 max-w-[75%]">
                  <UserAvatar :src="msg.sender?.avatar" alt="" size="sm" class="shrink-0" />
                  <div>
                    <div class="bg-white rounded-2xl rounded-bl-sm px-3 py-2 shadow-sm">
                      <p class="text-sm text-slate-900 leading-relaxed">{{ msg.content }}</p>
                    </div>
                  </div>
                </div>
                <div v-else class="max-w-[75%]">
                  <div class="bg-primary text-white rounded-2xl rounded-br-sm px-3 py-2">
                    <p class="text-sm leading-relaxed">{{ msg.content }}</p>
                  </div>
                </div>
              </div>
            </div>
            <!-- 输入框 -->
            <div class="p-3 border-t border-slate-200 bg-white shrink-0">
              <div class="flex items-center gap-2">
                <EmojiPicker @select="(emoji: string) => inputContent += emoji" />
                <VoiceRecorderButton @send="(blob: Blob) => handleVoiceSend(blob)" />
                <input
                  v-model="inputContent"
                  type="text"
                  class="flex-1 input text-sm bg-slate-50"
                  placeholder="输入消息..."
                  @keydown.enter="sendMessage"
                />
                <button class="btn-primary text-sm px-4" :disabled="!inputContent.trim()" @click="sendMessage">发送</button>
              </div>
            </div>
          </template>
          <!-- 空状态 -->
          <div v-else class="flex-1 flex items-center justify-center bg-slate-50">
            <div class="text-center">
              <svg class="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-slate-500 text-sm">选择一个会话开始聊天</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 通知Tab ===== -->
    <template v-if="activeMainTab === 'notifications'">
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- 通知操作栏 -->
        <div class="flex items-center justify-between px-4 py-2 border-b border-slate-200 bg-white shrink-0">
          <div class="flex items-center gap-1 overflow-x-auto no-scrollbar">
            <button
              v-for="tab in notiTabs"
              :key="tab.value"
              class="shrink-0 px-3 py-1.5 text-xs rounded-full transition-colors"
              :class="activeNotiTab === tab.value ? 'bg-primary text-white' : 'text-slate-500 hover:text-slate-700 bg-slate-50'"
              @click="switchNotiTab(tab.value)"
            >{{ tab.label }}</button>
          </div>
          <div class="flex items-center gap-1 shrink-0 ml-2">
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-slate-500 hover:text-slate-700" @click="exitNotiBatch">取消</button>
            <button v-if="notiBatchMode" class="text-xs px-2 py-1 text-danger" :disabled="notiSelectedIds.length === 0" @click="batchDeleteNoti">删除({{ notiSelectedIds.length }})</button>
            <button v-if="!notiBatchMode && notifications.length > 0" class="text-xs px-2 py-1 text-slate-500 hover:text-slate-700" @click="enterNotiBatch">批量管理</button>
            <button v-if="!notiBatchMode && notiUnreadTotal > 0" class="text-xs px-2 py-1 text-primary hover:text-primary-600" @click="markAllNotiRead">全部已读</button>
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
              class="flex items-start gap-3 px-4 py-3 hover:bg-slate-50 transition-colors border-b border-slate-50"
              :class="{ 'bg-primary-50/30': !item.isRead }"
              @click="handleNotiClick(item)"
            >
              <input v-if="notiBatchMode" type="checkbox" :checked="notiSelectedIds.includes(item.id)" class="mt-0.5 w-4 h-4 rounded border-slate-300 text-primary" @click.stop="toggleNotiSelect(item.id)" />
              <!-- 图标 -->
              <div class="shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                :class="{
                  'bg-blue-50': item.type === 1, 'bg-amber-50': item.type === 2,
                  'bg-red-50': item.type === 3, 'bg-purple-50': item.type === 4,
                  'bg-emerald-50': item.type === 5,
                }"
              >
                <svg v-if="item.type === 1" class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 2" class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <svg v-else-if="item.type === 3" class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                <svg v-else-if="item.type === 4" class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>
                <svg v-else-if="item.type === 5" class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2">
                  <p class="text-sm leading-snug" :class="item.isRead ? 'text-slate-600' : 'text-slate-900 font-medium'">{{ item.title }}</p>
                  <span v-if="!item.isRead" class="shrink-0 w-2 h-2 bg-primary rounded-full mt-1.5" />
                </div>
                <p v-if="item.content" class="text-xs text-slate-400 mt-1 line-clamp-2">{{ item.content }}</p>
                <span class="text-2xs text-slate-400 mt-1">{{ formatNotiTime(item.createdAt) }}</span>
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
import type { Conversation, Message, Notification } from '~/types'
import { notificationApi } from '~/api/notification'
import { socialApi } from '~/api'

definePageMeta({ middleware: 'auth' })

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

// ===== 主Tab =====
const activeMainTab = ref<'messages' | 'notifications'>('messages')
const msgUnread = ref(0)

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

const filteredConversations = computed(() => {
  const kw = conversationSearch.value.trim().toLowerCase()
  return kw ? conversations.value.filter(c => c.user?.nickname?.toLowerCase().includes(kw)) : conversations.value
})

const loadConversations = async () => {
  loadingConv.value = true
  conversationsError.value = ''
  try {
    const { data } = await socialApi.getConversations()
    conversations.value = data.data.list || []
  } catch { conversationsError.value = '加载失败' } finally { loadingConv.value = false }
}

const loadMsgUnread = async () => {
  try { const { data } = await socialApi.getUnreadCount(); msgUnread.value = data.data.count || 0 } catch { msgUnread.value = 0 }
}

const selectConversation = async (conv: Conversation) => {
  activeConversation.value = conv
  const targetUserId = conv.user?.id
  if (!targetUserId) return
  try {
    const { data } = await socialApi.getMessages(targetUserId)
    messages.value = data.data.list || data.data.items || []
  } catch { messages.value = [] }
  if (conv.unreadCount > 0) {
    try { await socialApi.markConversationRead(targetUserId); conv.unreadCount = 0; loadMsgUnread() } catch {}
  }
}

const sendMessage = async () => {
  if (!inputContent.value.trim() || !activeConversation.value) return
  const targetUserId = activeConversation.value.user?.id
  if (!targetUserId) return
  const content = inputContent.value.trim()
  inputContent.value = ''
  try {
    const { data } = await socialApi.sendMessage(targetUserId, { content })
    messages.value.push(data.data)
    nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
  } catch {}
}

const handleVoiceSend = async (blob: Blob) => {
  if (!activeConversation.value) return
  const targetUserId = activeConversation.value.user?.id
  if (!targetUserId) return
  try {
    const formData = new FormData()
    formData.append('file', blob, 'voice.webm')
    const { post } = useApi()
    const uploadRes = await post<any>('/files/upload/voice', formData)
    const voiceUrl = uploadRes.data?.data
    if (voiceUrl) {
      const { data } = await socialApi.sendMessage(targetUserId, { content: voiceUrl, type: 'voice' })
      messages.value.push(data.data)
      nextTick(() => { if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight })
    }
  } catch {}
}

const isMyMsg = (msg: Message) => msg.senderId === userStore.userInfo?.id

const formatConvTime = (t: string) => {
  const d = new Date(t), n = new Date(), diff = n.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
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
  } catch { notiError.value = '加载失败' } finally { notiLoading.value = false }
}

const loadMoreNoti = async () => {
  notiLoadingMore.value = true; notiPage.value++
  try {
    const params: any = { page: notiPage.value, pageSize: 20 }
    if (activeNotiTab.value) params.type = activeNotiTab.value
    const { data } = await notificationApi.getNotifications(params)
    notifications.value.push(...(data.data?.list || [])); notiTotal.value = data.data?.total || 0
  } catch { notiPage.value-- } finally { notiLoadingMore.value = false }
}

const handleNotiClick = async (n: Notification) => {
  if (notiBatchMode.value) { toggleNotiSelect(n.id); return }
  if (!n.isRead) {
    try { await notificationApi.markAsRead(n.id); notificationStore.markAsRead(n.id); n.isRead = true } catch {}
  }
}

const markAllNotiRead = async () => {
  try { await notificationApi.markAllAsRead(); notificationStore.markAllAsRead(); notifications.value.forEach(n => n.isRead = true) } catch {}
}

const enterNotiBatch = () => { notiBatchMode.value = true; notiSelectedIds.value = [] }
const exitNotiBatch = () => { notiBatchMode.value = false; notiSelectedIds.value = [] }
const toggleNotiSelect = (id: number) => {
  const i = notiSelectedIds.value.indexOf(id)
  i > -1 ? notiSelectedIds.value.splice(i, 1) : notiSelectedIds.value.push(id)
}

const batchDeleteNoti = async () => {
  if (!notiSelectedIds.value.length) return
  try {
    await notificationApi.batchDeleteNotifications(notiSelectedIds.value)
    const set = new Set(notiSelectedIds.value)
    notifications.value = notifications.value.filter(n => !set.has(n.id))
    notiTotal.value -= set.size
    notiSelectedIds.value = []
  } catch {}
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
  loadConversations(); loadMsgUnread()
  fetchNotis()
  if (import.meta.client) {
    io = new IntersectionObserver(([e]) => { if (e?.isIntersecting && notiHasMore.value && !notiLoadingMore.value) loadMoreNoti() }, { rootMargin: '200px' })
    watch(notiSentinelRef, el => { io?.disconnect(); if (el) io?.observe(el) }, { immediate: true })
  }
})
onUnmounted(() => io?.disconnect())

useHead({ title: () => '消息' + ' - 知讯' })
</script>
