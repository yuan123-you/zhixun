<template>
  <!-- 私信首页：会话列表 + 桌面端聊天面板 -->
  <div class="messages-page h-[calc(100dvh-50px)] md:h-[calc(100dvh-54px)] flex bg-[var(--zh-bg-elevated)] dark:bg-gray-900">
    <!-- ==================== 左侧会话列表 ==================== -->
    <div
      class="conversation-panel flex-shrink-0 w-full md:w-[380px] lg:w-[400px] flex flex-col relative"
      :class="{ 'hidden md:flex': activeConversation && isDesktop }"
    >
      <!-- 渐变分隔线 -->
      <div class="panel-divider"></div>

      <!-- 标题栏 - 玻璃态效果 -->
      <div class="header-glass relative z-10">
        <div class="flex items-center justify-between px-4 py-3">
          <div class="flex items-center gap-2.5">
            <h1 class="text-lg font-bold text-[var(--zh-text)] dark:text-white tracking-tight">私信</h1>
            <span
              v-if="unreadTotal > 0"
              class="unread-badge animate-badge-pop text-xs px-2 py-0.5 rounded-full font-semibold"
              :class="{ 'unread-pulse': unreadTotal > 0 }"
            >
              {{ unreadTotal > 99 ? '99+' : unreadTotal }} 条未读
            </span>
          </div>
          <button
            class="search-btn w-8 h-8 flex items-center justify-center rounded-full hover:bg-[var(--zh-bg-hover)] dark:hover:bg-gray-800/60 transition-colors"
            title="搜索会话"
          >
            <svg class="w-4 h-4 text-[var(--zh-text-secondary)] dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>

        <!-- 搜索/过滤输入框 -->
        <div class="px-4 pb-3">
          <div class="search-input-wrapper">
            <svg class="search-input-icon w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              placeholder="搜索会话..."
              class="search-input"
              readonly
            />
          </div>
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto custom-scrollbar">
        <!-- 加载状态 -->
        <div v-if="loadingConversations" class="flex items-center justify-center py-20">
          <div class="w-6 h-6 border-2 border-[var(--zh-primary)] border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="conversations.length === 0" class="flex flex-col items-center justify-center py-20 px-4">
          <div class="empty-illustration">
            <svg class="w-32 h-32" viewBox="0 0 128 128" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 背景装饰圆 -->
              <circle cx="64" cy="58" r="50" class="empty-circle-outer" />
              <circle cx="64" cy="58" r="36" class="empty-circle-inner" />
              <!-- 聊天气泡 -->
              <rect x="32" y="38" width="40" height="28" rx="12" class="empty-bubble-left" />
              <rect x="56" y="62" width="40" height="28" rx="12" class="empty-bubble-right" />
              <!-- 三个点 -->
              <circle cx="42" cy="52" r="2.5" class="empty-dot" />
              <circle cx="50" cy="52" r="2.5" class="empty-dot-delayed" />
              <circle cx="58" cy="52" r="2.5" class="empty-dot-more" />
              <!-- 装饰小圆 -->
              <circle cx="22" cy="48" r="8" class="empty-deco" />
            </svg>
          </div>
          <p class="text-[var(--zh-text-secondary)] dark:text-gray-400 text-sm font-medium mt-3">暂无私信会话</p>
          <p class="text-[var(--zh-text-tertiary)] dark:text-gray-500 text-xs mt-1.5">与互关好友开始聊天吧</p>
        </div>

        <!-- 会话项 -->
        <div v-else class="conversation-list">
          <button
            v-for="(conv, index) in sortedConversations"
            :key="conv.id || conv.user.id"
            class="conversation-item w-full flex items-center gap-3 px-4 py-3 text-left relative"
            :class="{
              'conversation-active': activeConversation?.user?.id === conv.user.id,
              'conversation-has-unread': conv.unreadCount > 0,
            }"
            :style="{ '--stagger-index': index }"
            @click="openConversation(conv)"
          >
            <!-- 左侧选中指示条 -->
            <div class="accent-bar"></div>

            <!-- 在线状态 -->
            <div class="relative flex-shrink-0">
              <UserAvatar :src="conv.user?.avatar" :alt="conv.user?.nickname" size="lg" />
              <!-- 未读蓝色圆点 -->
              <span
                v-if="conv.unreadCount > 0"
                class="unread-dot-indicator"
              ></span>
              <span
                v-if="isUserOnline(conv.user.id)"
                class="online-dot"
              ></span>
            </div>

            <!-- 会话信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <span class="font-medium text-sm text-[var(--zh-text)] dark:text-gray-100 truncate">
                  {{ conv.user?.nickname }}
                </span>
                <span class="text-[10px] text-[var(--zh-text-tertiary)] dark:text-gray-500 flex-shrink-0 ml-2">
                  {{ formatRelativeTime(conv.lastMessage?.createdAt || conv.updatedAt) }}
                </span>
              </div>
              <div class="flex items-center mt-0.5">
                <p class="flex-1 min-w-0 text-xs text-[var(--zh-text-secondary)] dark:text-gray-400 truncate preview-text">
                  {{ getLastMessagePreview(conv) }}
                </p>
                <span
                  v-if="conv.unreadCount > 0"
                  class="unread-count animate-badge-pop text-[10px] min-w-[18px] h-[18px] flex items-center justify-center rounded-full px-1 flex-shrink-0 ml-2 font-semibold"
                  :class="{ 'unread-pulse': conv.unreadCount > 0 }"
                >
                  {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                </span>
              </div>
            </div>
          </button>
        </div>
      </div>

      <!-- 底部刷新 -->
      <div class="refresh-footer">
        <button
          class="refresh-btn"
          @click="refreshConversations"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          刷新会话列表
        </button>
      </div>
    </div>

    <!-- ==================== 右侧聊天面板（桌面端） ==================== -->
    <div
      v-if="isDesktop"
      class="flex-1 flex flex-col min-w-0"
    >
      <!-- 有选中会话时显示聊天窗口 -->
      <template v-if="activeConversation">
        <ChatWindow
          :conversation="activeConversation"
          :messages="currentMessages"
          :loading-more="loadingMessages && currentPage > 1"
          :has-more="hasMoreMessages"
          :on-send="onSendMessage"
          @load-more="onLoadMore"
        />
      </template>

      <!-- 未选择会话时的引导 -->
      <div v-else class="chat-empty-guide">
        <div class="chat-empty-icon">
          <svg viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- 外层圆环 -->
            <circle cx="48" cy="48" r="44" class="guide-circle-outer" />
            <circle cx="48" cy="48" r="36" class="guide-circle-inner" />
            <!-- 聊天气泡 -->
            <path d="M28 34h40a3 3 0 013 3v22a3 3 0 01-3 3h-6l-6 8-6-8H28a3 3 0 01-3-3V37a3 3 0 013-3z" class="guide-bubble" />
            <!-- 消息横线 -->
            <rect x="36" y="42" width="24" height="2.5" rx="1.25" class="guide-line guide-line-1" />
            <rect x="36" y="48" width="16" height="2.5" rx="1.25" class="guide-line guide-line-2" />
            <!-- 小三角装饰 -->
            <path d="M62 58l-4 4-4-4" class="guide-arrow" />
            <!-- 外部装饰点 -->
            <circle cx="18" cy="40" r="3" class="guide-orb guide-orb-1" />
            <circle cx="78" cy="32" r="2.5" class="guide-orb guide-orb-2" />
            <circle cx="72" cy="68" r="3.5" class="guide-orb guide-orb-3" />
          </svg>
        </div>
        <p class="chat-empty-title">选择左侧会话开始聊天</p>
        <p class="chat-empty-subtitle">与您的互关好友畅快交流</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Conversation } from '@/types'
import { useMessageStore } from '@/stores/message'

const { setTitle } = usePageHeaderTitle()
setTitle('私信')

const messageStore = useMessageStore()
const { isDesktop } = useBreakpoints()

const {
  conversations,
  loadingConversations,
  currentMessages,
  loadingMessages,
  hasMoreMessages,
  currentPage,
  unreadTotal,
  currentChatUserId,
} = storeToRefs(messageStore)

const activeConversation = ref<Conversation | null>(null)

// ==================== 按时间降序排列会话 ====================
const sortedConversations = computed(() => {
  return [...conversations.value].sort((a, b) => {
    const timeA = new Date(a.lastMessage?.createdAt || a.updatedAt || 0).getTime()
    const timeB = new Date(b.lastMessage?.createdAt || b.updatedAt || 0).getTime()
    return timeB - timeA // 最新的排最前
  })
})

// ==================== 初始化 ====================
onMounted(async () => {
  await messageStore.fetchConversations()
  await messageStore.fetchUnreadCount()
})

// ==================== 打开会话 ====================
const router = useRouter()
const openConversation = async (conv: Conversation) => {
  // 移动端：导航到全屏聊天页
  if (!isDesktop.value) {
    router.push(`/messages/${conv.user.id}`)
    return
  }
  // 桌面端：内联聊天面板
  activeConversation.value = conv
  messageStore.currentChatUserId = conv.user.id
  await messageStore.fetchMessages(conv.user.id)
  await messageStore.markRead(conv.user.id)
}

// ==================== 发送消息 ====================
const onSendMessage = async (content: string, type?: string) => {
  if (!activeConversation.value) return
  return messageStore.sendMessage(activeConversation.value.user.id, content, type)
}

// ==================== 加载更多历史消息 ====================
const onLoadMore = async () => {
  if (!activeConversation.value || !hasMoreMessages.value) return
  await messageStore.loadMoreMessages(activeConversation.value.user.id)
}

// ==================== 刷新会话列表 ====================
const refreshConversations = async () => {
  await messageStore.fetchConversations()
  await messageStore.fetchUnreadCount()
}

// ==================== 辅助方法 ====================
const isUserOnline = (userId: number) => messageStore.isUserOnline(userId)

const getLastMessagePreview = (conv: Conversation) => {
  if (!conv.lastMessage) return ''
  if (conv.lastMessage.type === 'image') return '[图片]'
  if (conv.lastMessage.type === 'voice') return '[语音]'
  if (conv.lastMessage.type === 'file') return '[文件]'
  if (conv.lastMessage.type === 'ai_reply') return '[AI回复]'
  const content = conv.lastMessage.content || ''
  return content.length > 30 ? content.slice(0, 30) + '...' : content
}

/** 相对时间格式化 */
const formatRelativeTime = (timeStr: string) => {
  if (!timeStr) return ''
  const now = Date.now()
  const time = new Date(timeStr).getTime()
  if (isNaN(time)) return ''
  const diff = now - time

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`

  const date = new Date(timeStr)
  const nowDate = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')

  if (date.getFullYear() === nowDate.getFullYear()) {
    if (date.getDate() === nowDate.getDate() - 1) {
      return `昨天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.messages-page {
  margin: 0;
}

@media (min-width: 768px) {
  .messages-page {
    margin: 0;
  }
}

/* ==================== 渐变分隔线 ==================== */
.panel-divider {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent 0%,
    var(--zh-border) 20%,
    var(--zh-border) 80%,
    transparent 100%
  );
  z-index: 5;
}

.dark .panel-divider {
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgb(55 65 81 / 0.5) 20%,
    rgb(55 65 81 / 0.5) 80%,
    transparent 100%
  );
}

/* ==================== 玻璃态标题栏 ==================== */
.header-glass {
  background: linear-gradient(
    135deg,
    rgba(79, 70, 229, 0.06) 0%,
    rgba(79, 70, 229, 0.02) 50%,
    rgba(255, 255, 255, 0.04) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(79, 70, 229, 0.08);
}

.dark .header-glass {
  background: linear-gradient(
    135deg,
    rgba(79, 70, 229, 0.12) 0%,
    rgba(79, 70, 229, 0.04) 50%,
    rgba(30, 41, 59, 0.3) 100%
  );
  border-bottom: 1px solid rgba(79, 70, 229, 0.12);
}

/* 搜索按钮 */
.search-btn {
  opacity: 0.7;
  transition: opacity 0.2s, background-color 0.2s;
}
.search-btn:hover {
  opacity: 1;
}

/* 搜索输入框 */
.search-input-wrapper {
  position: relative;
}

.search-input-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--zh-text-tertiary);
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 34px;
  padding: 0 12px 0 32px;
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  background: var(--zh-bg);
  font-size: 13px;
  color: var(--zh-text);
  outline: none;
  cursor: default;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-input::placeholder {
  color: var(--zh-text-tertiary);
}

.search-input:focus {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.dark .search-input {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(71, 85, 105, 0.3);
  color: #e2e8f0;
}

/* 未读总数徽章 */
.unread-badge {
  background: linear-gradient(135deg, var(--zh-primary), #6366f1);
  color: #fff;
  letter-spacing: 0.01em;
}

/* ==================== 自定义滚动条 ==================== */
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

/* ==================== 空状态插图 ==================== */
.empty-illustration {
  animation: float-slow 3s ease-in-out infinite;
}

.empty-circle-outer {
  fill: none;
  stroke: var(--zh-border);
  stroke-width: 1.5;
  opacity: 0.4;
}

.empty-circle-inner {
  fill: var(--zh-bg-hover);
  stroke: var(--zh-border-light);
  stroke-width: 1;
  opacity: 0.6;
}

.empty-bubble-left {
  fill: var(--zh-bg-hover);
  stroke: var(--zh-border);
  stroke-width: 1.2;
  opacity: 0.7;
  animation: bubble-drift 3s ease-in-out infinite;
}

.empty-bubble-right {
  fill: rgba(79, 70, 229, 0.08);
  stroke: rgba(79, 70, 229, 0.15);
  stroke-width: 1.2;
  opacity: 0.7;
  animation: bubble-drift 3s 0.5s ease-in-out infinite;
}

.empty-deco {
  fill: rgba(79, 70, 229, 0.06);
  animation: deco-float 4s ease-in-out infinite;
}

.dark .empty-circle-outer {
  stroke: rgb(71 85 105 / 0.35);
}

.dark .empty-circle-inner {
  fill: rgb(30 41 59 / 0.35);
  stroke: rgb(71 85 105 / 0.15);
}

.dark .empty-bubble-left {
  fill: rgb(51 65 85 / 0.25);
  stroke: rgb(71 85 105 / 0.25);
}

.dark .empty-bubble-right {
  fill: rgba(79, 70, 229, 0.1);
  stroke: rgba(79, 70, 229, 0.18);
}

.dark .empty-deco {
  fill: rgba(79, 70, 229, 0.08);
}

.empty-dot,
.empty-dot-delayed,
.empty-dot-more {
  fill: var(--zh-text-tertiary);
  opacity: 0.4;
}

.empty-dot {
  animation: dot-pulse 1.4s ease-in-out infinite;
}

.empty-dot-delayed {
  animation: dot-pulse 1.4s 0.2s ease-in-out infinite;
}

.empty-dot-more {
  animation: dot-pulse 1.4s 0.4s ease-in-out infinite;
}

.dark .empty-dot,
.dark .empty-dot-delayed,
.dark .empty-dot-more {
  fill: rgb(148 163 184 / 0.35);
}

/* ==================== 会话列表 ==================== */
.conversation-list {
  padding: 4px 0;
}

/* 会话项 */
.conversation-item {
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
  border-bottom: 1px solid var(--zh-border-light);
  opacity: 0;
  animation: stagger-in 0.4s ease-out forwards;
  animation-delay: calc(var(--stagger-index, 0) * 0.05s + 0.1s);
  cursor: pointer;
}

.dark .conversation-item {
  border-bottom-color: rgba(51, 65, 85, 0.3);
}

.conversation-item:last-child {
  border-bottom: none;
}

/* 左侧选中指示条 - 渐变 */
.accent-bar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 60%;
  background: linear-gradient(180deg, var(--zh-primary), #8b5cf6);
  border-radius: 0 3px 3px 0;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.conversation-item:hover .accent-bar {
  transform: translateY(-50%) scaleY(0.6);
  background: linear-gradient(180deg, var(--zh-primary), #6366f1);
}

.conversation-active .accent-bar {
  transform: translateY(-50%) scaleY(1);
  background: linear-gradient(180deg, var(--zh-primary), #8b5cf6);
}

/* 悬停态 */
.conversation-item:hover {
  background-color: var(--zh-bg-hover);
}
.dark .conversation-item:hover {
  background-color: rgba(51, 65, 85, 0.25);
}

/* 选中态 */
.conversation-active {
  background-color: rgba(79, 70, 229, 0.06) !important;
}
.dark .conversation-active {
  background-color: rgba(79, 70, 229, 0.12) !important;
}

/* 有未读消息的会话 */
.conversation-has-unread {
  background-color: rgba(79, 70, 229, 0.02);
}
.dark .conversation-has-unread {
  background-color: rgba(79, 70, 229, 0.04);
}

/* 未读蓝色圆点指示器 */
.unread-dot-indicator {
  position: absolute;
  top: -2px;
  left: -2px;
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.5);
  animation: unread-dot-pulse 2s ease-in-out infinite;
  z-index: 2;
}

.dark .unread-dot-indicator {
  border-color: rgb(17, 24, 39);
}

/* 在线状态点（带增强脉冲动画） */
.online-dot {
  position: absolute;
  bottom: -0.5px;
  right: -0.5px;
  width: 13px;
  height: 13px;
  background: #22c55e;
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.6);
  animation: online-pulse 2s ease-in-out infinite;
  z-index: 2;
}

.dark .online-dot {
  border-color: rgb(17, 24, 39);
}

/* 消息预览截断 */
.preview-text {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 未读计数徽章 - 渐变红色 */
.unread-count {
  background: linear-gradient(135deg, #ef4444, #f43f5e);
  color: #fff;
  box-shadow: 0 1px 6px rgba(239, 68, 68, 0.4);
}

/* 未读徽章脉冲动画 */
.unread-pulse {
  animation: badge-pulse 2s ease-in-out infinite;
}

/* ==================== 底部刷新 ==================== */
.refresh-footer {
  padding: 10px 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(to top, rgba(248, 250, 252, 0.8), transparent);
}

.dark .refresh-footer {
  border-top-color: rgba(55, 65, 81, 0.3);
  background: linear-gradient(to top, rgba(17, 24, 39, 0.5), transparent);
}

.refresh-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 0;
  font-size: 12px;
  color: var(--zh-text-tertiary);
  border-radius: var(--zh-radius-md);
  transition: color 0.2s, background-color 0.2s;
  cursor: pointer;
}

.refresh-btn:hover {
  color: var(--zh-primary);
  background-color: rgba(79, 70, 229, 0.06);
}

.dark .refresh-btn {
  color: rgb(148 163 184 / 0.7);
}
.dark .refresh-btn:hover {
  color: #818cf8;
  background-color: rgba(79, 70, 229, 0.1);
}

/* ==================== 桌面端聊天占位 ==================== */
.chat-placeholder {
  background: linear-gradient(
    135deg,
    rgba(248, 250, 252, 0.6) 0%,
    rgba(248, 250, 252, 0.3) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
}

.dark .chat-placeholder {
  background: linear-gradient(
    135deg,
    rgba(30, 41, 59, 0.3) 0%,
    rgba(30, 41, 59, 0.15) 50%,
    rgba(17, 24, 39, 0.1) 100%
  );
}

/* 引导空状态 */
.chat-empty-guide {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.03) 0%, rgba(139, 92, 246, 0.02) 50%, rgba(255, 255, 255, 0) 100%);
}

.dark .chat-empty-guide {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.06) 0%, rgba(139, 92, 246, 0.04) 50%, rgba(17, 24, 39, 0) 100%);
}

.chat-empty-icon {
  width: 6rem;
  height: 6rem;
  color: var(--zh-text-placeholder);
  margin-bottom: 1.5rem;
  animation: float-slow 4s ease-in-out infinite;
}

/* 引导图标内元素 */
.guide-circle-outer {
  stroke: currentColor;
  stroke-width: 1.5;
  opacity: 0.15;
  animation: guide-pulse-ring 3s ease-in-out infinite;
}

.guide-circle-inner {
  stroke: currentColor;
  stroke-width: 1;
  opacity: 0.08;
}

.guide-bubble {
  stroke: currentColor;
  stroke-width: 1.5;
  fill: none;
  opacity: 0.45;
  animation: guide-bubble-float 3s ease-in-out infinite;
}

.guide-line {
  fill: currentColor;
}

.guide-line-1 {
  opacity: 0.25;
  animation: guide-line-shimmer 2.5s ease-in-out infinite;
}

.guide-line-2 {
  opacity: 0.18;
  animation: guide-line-shimmer 2.5s 0.3s ease-in-out infinite;
}

.guide-arrow {
  stroke: currentColor;
  stroke-width: 1.2;
  fill: none;
  opacity: 0.2;
  animation: guide-arrow-bounce 2s ease-in-out infinite;
}

.guide-orb {
  fill: rgba(79, 70, 229, 0.15);
  animation: guide-orb-float 3s ease-in-out infinite;
}

.guide-orb-1 {
  animation-delay: 0s;
}

.guide-orb-2 {
  animation-delay: 0.8s;
}

.guide-orb-3 {
  animation-delay: 1.6s;
}

.dark .guide-orb {
  fill: rgba(79, 70, 229, 0.2);
}

/* ==================== 关键帧动画 ==================== */

/* 错位入场动画 */
@keyframes stagger-in {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 徽章脉冲 */
@keyframes badge-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.08);
  }
}

/* 在线状态脉冲 */
@keyframes online-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
}

/* 缓慢浮动 */
@keyframes float-slow {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

/* 点脉冲 */
@keyframes dot-pulse {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 0.7;
    transform: scale(1.2);
  }
}

/* 气泡淡入淡出 */
@keyframes bubble-fade {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

/* 打字点动画 */
@keyframes typing-dot {
  0%, 60%, 100% {
    opacity: 0;
    transform: translateY(0);
  }
  30% {
    opacity: 0.7;
    transform: translateY(-2px);
  }
}

/* 横线脉冲 */
@keyframes line-pulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.6;
  }
}

/* 装饰圆浮动 */
@keyframes deco-float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(4px, -4px);
  }
}

/* 点击态 */
.conversation-item:active {
  background-color: var(--zh-bg-hover);
  transform: scale(0.995);
}
.dark .conversation-item:active {
  background-color: rgba(51, 65, 85, 0.4);
}

/* 未读蓝色圆点脉冲 */
@keyframes unread-dot-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.5);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(59, 130, 246, 0);
  }
}

/* 引导面板动画 */
@keyframes guide-pulse-ring {
  0%, 100% {
    opacity: 0.12;
    transform: scale(1);
  }
  50% {
    opacity: 0.22;
    transform: scale(1.02);
  }
}

@keyframes guide-bubble-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes guide-line-shimmer {
  0%, 100% {
    opacity: 0.18;
  }
  50% {
    opacity: 0.35;
  }
}

@keyframes guide-arrow-bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(3px);
  }
}

@keyframes guide-orb-float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.15;
  }
  50% {
    transform: translate(2px, -4px) scale(1.2);
    opacity: 0.3;
  }
}

@keyframes bubble-drift {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

/* ==================== 消息页增强样式 ==================== */

/* 会话面板 */
.conversation-panel {
  flex-shrink: 0;
  width: 100%;
  border-right: 1px solid var(--zh-border);
  display: flex;
  flex-direction: column;
}
@media (min-width: 768px) {
  .conversation-panel {
    width: 380px;
  }
}
@media (min-width: 1024px) {
  .conversation-panel {
    width: 400px;
  }
}

/* 会话项 */
.conversation-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  border-bottom: 1px solid var(--zh-border-light);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  position: relative;
}
.conversation-item:hover {
  background: var(--zh-bg-hover);
}
.conversation-item.active {
  background: rgba(var(--zh-primary-rgb), 0.05);
}
.conversation-item:active {
  background: var(--zh-bg-active);
}

/* 在线状态指示器 */
.online-dot {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 0.75rem;
  height: 0.75rem;
  background: #10b981;
  border: 2px solid var(--zh-bg-elevated);
  border-radius: 50%;
}
.dark .online-dot {
  border-color: #1e293b;
}

/* 未读徽章 */
.unread-badge {
  min-width: 1.15rem;
  height: 1.15rem;
  padding: 0 0.3rem;
  border-radius: 9999px;
  background: var(--zh-primary);
  color: #fff;
  font-size: 0.6rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 引导空状态 */
.chat-empty-guide {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(var(--zh-primary-rgb), 0.02);
}
.chat-empty-icon {
  width: 6rem;
  height: 6rem;
  color: var(--zh-text-placeholder);
  margin-bottom: 1.5rem;
  opacity: 0.5;
}
.chat-empty-title {
  font-size: 0.9rem;
  color: var(--zh-text-tertiary);
  margin-bottom: 0.25rem;
}
.chat-empty-subtitle {
  font-size: 0.75rem;
  color: var(--zh-text-placeholder);
}

/* 自定义滚动条 */
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

/* 标题栏 */
.conversation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 1rem;
  border-bottom: 1px solid rgba(var(--zh-primary-rgb), 0.06);
}
.conversation-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--zh-text);
}
.total-unread-badge {
  font-size: 0.7rem;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  background: var(--zh-primary);
  color: #fff;
  font-weight: 600;
}

/* 刷新按钮 */
.refresh-btn-bottom {
  width: 100%;
  padding: 0.5rem;
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
  background: none;
  border: none;
  border-top: 1px solid rgba(var(--zh-primary-rgb), 0.06);
  cursor: pointer;
  transition: color 0.2s ease;
}
.refresh-btn-bottom:hover {
  color: var(--zh-primary);
}

/* ==================== 新增动画关键帧 ==================== */

/* 呼吸动画 - 在线状态指示器 */
.animate-breathe {
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

/* 弹跳动画 - 未读消息徽章 */
.animate-badge-pop {
  animation: badge-pop 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes badge-pop {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  60% {
    transform: scale(1.15);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>