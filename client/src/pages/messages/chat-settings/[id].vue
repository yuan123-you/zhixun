<template>
  <!-- 聊天设置页面（QQ 风格） -->
  <div class="settings-page h-[calc(100dvh-3.75rem)] md:h-[calc(100dvh-4rem)] flex flex-col bg-[var(--zh-bg-elevated)] dark:bg-gray-900">
    <!-- 头部 -->
    <div class="flex items-center gap-3 px-4 py-3 border-b border-[var(--zh-border)]/60 dark:border-gray-700/60 flex-shrink-0">
      <button
        class="p-2 -ml-2 text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] hover:bg-slate-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
        @click="goBack"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <h1 class="font-semibold text-base text-[var(--zh-text)] dark:text-gray-100">聊天设置</h1>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 overflow-y-auto custom-scrollbar">
      <!-- ==================== 用户信息卡 ==================== -->
      <div class="px-4 py-5">
        <div class="user-card">
          <button class="flex items-center gap-4 w-full text-left" @click="navigateToUser">
            <div class="relative flex-shrink-0">
              <UserAvatar :src="targetUser?.avatar" :alt="targetUser?.nickname" size="lg" />
              <span
                v-if="isOnline"
                class="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 border-2 border-white dark:border-gray-900 rounded-full"
              ></span>
            </div>
            <div class="min-w-0 flex-1">
              <p class="font-semibold text-[15px] text-[var(--zh-text)] dark:text-gray-100 truncate">
                {{ remark || targetUser?.nickname || '用户' }}
              </p>
              <p v-if="remark" class="text-xs text-[var(--zh-text-tertiary)] dark:text-gray-500 mt-0.5 truncate">
                {{ targetUser?.nickname }}
              </p>
              <div class="flex items-center gap-2 mt-1">
                <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 dark:bg-gray-800 text-[var(--zh-text-secondary)] dark:text-gray-400">
                  UID: {{ targetUserId }}
                </span>
                <span class="text-xs" :class="isOnline ? 'text-green-500' : 'text-[var(--zh-text-tertiary)] dark:text-gray-500'">
                  {{ isOnline ? '在线' : '离线' }}
                </span>
              </div>
            </div>
            <svg class="w-4 h-4 text-[var(--zh-text-tertiary)] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>

      <!-- ==================== 聊天统计 ==================== -->
      <div class="px-4 pb-3">
        <div class="stats-card">
          <div class="stats-item">
            <span class="stats-value">{{ stats.totalCount }}</span>
            <span class="stats-label">消息总数</span>
          </div>
          <div class="stats-divider"></div>
          <div class="stats-item">
            <span class="stats-value">{{ stats.imageCount }}</span>
            <span class="stats-label">图片</span>
          </div>
          <div class="stats-divider"></div>
          <div class="stats-item">
            <span class="stats-value">{{ stats.textCount }}</span>
            <span class="stats-label">文本</span>
          </div>
          <div v-if="stats.earliestTime" class="stats-divider"></div>
          <div v-if="stats.earliestTime" class="stats-item">
            <span class="stats-value stats-value-date">{{ formatDateShort(stats.earliestTime) }}</span>
            <span class="stats-label">最早消息</span>
          </div>
        </div>
      </div>

      <!-- ==================== 功能菜单 ==================== -->
      <div class="px-4 pb-3">
        <div class="menu-section">
          <!-- 查找聊天记录 -->
          <button class="menu-item" @click="toggleSearchHistory">
            <div class="menu-icon-wrap menu-icon-blue">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <span class="menu-text">查找聊天记录</span>
            <svg class="menu-arrow" :class="{ 'menu-arrow-open': showSearchHistory }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>

          <!-- 搜索面板 -->
          <Transition name="slide-down">
            <div v-if="showSearchHistory" class="search-panel">
              <!-- 类型筛选 Tab -->
              <div class="filter-tabs">
                <button
                  v-for="tab in filterTabs"
                  :key="tab.value"
                  class="filter-tab"
                  :class="{ active: activeFilterTab === tab.value }"
                  @click="activeFilterTab = tab.value; applyFilters()"
                >
                  {{ tab.label }}
                </button>
              </div>

              <!-- 日期范围筛选 -->
              <div class="date-filter-row">
                <label class="date-label">从</label>
                <input type="date" class="date-input" v-model="filterStartDate" @change="applyFilters" />
                <label class="date-label">至</label>
                <input type="date" class="date-input" v-model="filterEndDate" @change="applyFilters" />
                <button v-if="filterStartDate || filterEndDate" class="date-clear" @click="filterStartDate = ''; filterEndDate = ''; applyFilters()">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <!-- 关键词搜索 -->
              <div class="keyword-row">
                <svg class="keyword-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  v-model="filterKeyword"
                  type="text"
                  class="keyword-input"
                  placeholder="搜索消息内容..."
                  @input="debouncedApplyFilters"
                />
              </div>

              <!-- 搜索结果 -->
              <div class="search-results">
                <div v-if="searching" class="search-loading">
                  <div class="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
                </div>
                <div v-else-if="filteredMessages.length === 0" class="search-empty">
                  <p>未找到匹配的消息</p>
                </div>
                <div v-else class="search-list">
                  <span class="search-count">找到 {{ filteredMessages.length }} 条消息</span>
                  <div
                    v-for="msg in filteredMessages"
                    :key="msg.id"
                    class="search-result-item"
                    @click="goToMessage(msg)"
                  >
                    <div class="result-header">
                      <span class="result-sender">{{ msg.senderId === myUserId ? '我' : (targetUser?.nickname || '对方') }}</span>
                      <span class="result-time">{{ formatMessageTime(msg.createdAt) }}</span>
                    </div>
                    <div v-if="msg.type === 'image'" class="result-image-row">
                      <img :src="resolveMsgUrl(msg.content)" class="result-thumb" alt="图片" />
                      <span class="result-image-tag">图片</span>
                    </div>
                    <p v-else class="result-content">{{ msg.content }}</p>
                  </div>
                </div>
              </div>
            </div>
          </Transition>

          <!-- 添加备注 -->
          <div class="menu-item-divider"></div>
          <div class="menu-item remark-item" @click.stop>
            <div class="menu-icon-wrap menu-icon-orange">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </div>
            <div class="remark-content">
              <span class="menu-text">备注</span>
              <div v-if="!editingRemark" class="remark-display" @click="startEditRemark">
                <span v-if="remark" class="remark-text">{{ remark }}</span>
                <span v-else class="remark-placeholder">点击添加备注</span>
                <svg class="w-3.5 h-3.5 text-[var(--zh-text-tertiary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </div>
              <div v-else class="remark-edit">
                <input
                  ref="remarkInputRef"
                  v-model="remarkDraft"
                  type="text"
                  class="remark-input"
                  placeholder="输入备注名..."
                  maxlength="20"
                  @keydown.enter="saveRemark"
                  @keydown.escape="cancelEditRemark"
                />
                <div class="remark-actions">
                  <button class="remark-btn remark-btn-cancel" @click="cancelEditRemark">取消</button>
                  <button class="remark-btn remark-btn-save" @click="saveRemark">保存</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 消息免打扰 -->
          <div class="menu-item-divider"></div>
          <div class="menu-item">
            <div class="menu-icon-wrap menu-icon-gray">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
              </svg>
            </div>
            <span class="menu-text">消息免打扰</span>
            <button class="toggle-switch" :class="{ active: muteNotification }" @click="muteNotification = !muteNotification; saveChatSettings()">
              <span class="toggle-thumb"></span>
            </button>
          </div>

          <!-- 置顶会话 -->
          <div class="menu-item-divider"></div>
          <div class="menu-item">
            <div class="menu-icon-wrap menu-icon-green">
              <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
            </div>
            <span class="menu-text">置顶会话</span>
            <button class="toggle-switch" :class="{ active: pinConversation }" @click="pinConversation = !pinConversation; saveChatSettings()">
              <span class="toggle-thumb"></span>
            </button>
          </div>
        </div>
      </div>

      <!-- ==================== 危险操作区 ==================== -->
      <div class="px-4 pb-6 space-y-3">
        <button class="danger-btn" @click="confirmClearCache">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          清除本地缓存
        </button>
        <button class="danger-btn danger-btn-strong" @click="confirmDeleteAll">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          删除全部聊天记录
        </button>
      </div>
    </div>

    <!-- ==================== 确认对话框 ==================== -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showConfirmDialog" class="modal-overlay" @click.self="showConfirmDialog = false">
          <div class="modal-box">
            <div class="modal-icon" :class="confirmType === 'delete' ? 'modal-icon-red' : 'modal-icon-blue'">
              <svg v-if="confirmType === 'delete'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </div>
            <h3 class="modal-title">{{ confirmTitle }}</h3>
            <p class="modal-desc">{{ confirmDesc }}</p>
            <div class="modal-actions">
              <button class="modal-btn modal-btn-cancel" @click="showConfirmDialog = false">取消</button>
              <button class="modal-btn" :class="confirmType === 'delete' ? 'modal-btn-danger' : 'modal-btn-primary'" @click="executeConfirm">
                {{ confirmBtnText }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Toast 提示 -->
    <Teleport to="body">
      <Transition name="toast-fade">
        <div v-if="toastMsg" class="toast-bar">{{ toastMsg }}</div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { Message } from '@/types'
import { useMessageStore } from '@/stores/message'
import { storage, STORAGE_KEYS } from '@/utils/storage'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { resolveUrl } = useResourceUrl()

const { setTitle } = usePageHeaderTitle()
setTitle('聊天设置')

const { conversations, myUserId } = storeToRefs(messageStore)
const localDb = messageStore.localDb

const targetUserId = computed(() => Number(route.params.id))

const resolveMsgUrl = (url: string) => resolveUrl(url) || url

// ==================== 用户信息 ====================
const targetUser = computed(() => {
  const conv = conversations.value.find(c => c.user.id === targetUserId.value)
  return conv?.user || null
})

const isOnline = computed(() => {
  if (!targetUserId.value) return false
  return messageStore.isUserOnline(targetUserId.value)
})

const navigateToUser = () => {
  if (targetUserId.value) router.push(`/user/${targetUserId.value}`)
}

// ==================== 聊天统计 ====================
interface ConvStats {
  totalCount: number
  imageCount: number
  textCount: number
  earliestTime: string | null
  latestTime: string | null
}

const stats = ref<ConvStats>({
  totalCount: 0,
  imageCount: 0,
  textCount: 0,
  earliestTime: null,
  latestTime: null,
})

const loadStats = async () => {
  if (!myUserId.value) return
  try {
    stats.value = await localDb.getStats(myUserId.value, targetUserId.value)
  } catch (e) {
    console.warn('[ChatSettings] 加载统计失败:', e)
  }
}

// ==================== 备注 ====================
const remark = ref('')
const editingRemark = ref(false)
const remarkDraft = ref('')
const remarkInputRef = ref<HTMLInputElement | null>(null)

const loadRemark = () => {
  if (!myUserId.value) return
  const key = `${STORAGE_KEYS.CHAT_REMARK_PREFIX}${myUserId.value}_${targetUserId.value}`
  remark.value = storage.get<string>(key) || ''
}

const startEditRemark = () => {
  remarkDraft.value = remark.value
  editingRemark.value = true
  nextTick(() => {
    remarkInputRef.value?.focus()
    remarkInputRef.value?.select()
  })
}

const cancelEditRemark = () => {
  editingRemark.value = false
  remarkDraft.value = ''
}

const saveRemark = () => {
  if (!myUserId.value) return
  const key = `${STORAGE_KEYS.CHAT_REMARK_PREFIX}${myUserId.value}_${targetUserId.value}`
  const val = remarkDraft.value.trim()
  if (val) {
    storage.set(key, val)
    remark.value = val
    showToast('备注已保存')
  } else {
    storage.remove(key)
    remark.value = ''
    showToast('备注已清除')
  }
  editingRemark.value = false
}

// ==================== 会话设置（免打扰/置顶） ====================
const muteNotification = ref(false)
const pinConversation = ref(false)

interface ChatSettingsData {
  mute?: boolean
  pin?: boolean
}

const loadChatSettings = () => {
  if (!myUserId.value) return
  const key = `${STORAGE_KEYS.CHAT_SETTINGS_PREFIX}${myUserId.value}_${targetUserId.value}`
  const data = storage.get<ChatSettingsData>(key)
  if (data) {
    muteNotification.value = data.mute ?? false
    pinConversation.value = data.pin ?? false
  }
}

const saveChatSettings = () => {
  if (!myUserId.value) return
  const key = `${STORAGE_KEYS.CHAT_SETTINGS_PREFIX}${myUserId.value}_${targetUserId.value}`
  storage.set<ChatSettingsData>(key, {
    mute: muteNotification.value,
    pin: pinConversation.value,
  })
}

// ==================== 查找聊天记录 ====================
const showSearchHistory = ref(false)
const searching = ref(false)
const filteredMessages = ref<Message[]>([])
const filterStartDate = ref('')
const filterEndDate = ref('')
const filterKeyword = ref('')
const activeFilterTab = ref('all')

const filterTabs = [
  { label: '全部', value: 'all' },
  { label: '图片', value: 'image' },
  { label: '文本', value: 'text' },
]

const toggleSearchHistory = () => {
  showSearchHistory.value = !showSearchHistory.value
  if (showSearchHistory.value && filteredMessages.value.length === 0) {
    applyFilters()
  }
}

let filterTimer: ReturnType<typeof setTimeout> | null = null
const debouncedApplyFilters = () => {
  if (filterTimer) clearTimeout(filterTimer)
  filterTimer = setTimeout(() => applyFilters(), 300)
}

const applyFilters = async () => {
  if (!myUserId.value) return
  searching.value = true
  try {
    const filters: any = {}
    if (filterStartDate.value) filters.startDate = filterStartDate.value
    if (filterEndDate.value) filters.endDate = filterEndDate.value
    if (activeFilterTab.value !== 'all') filters.type = activeFilterTab.value
    if (filterKeyword.value.trim()) filters.keyword = filterKeyword.value.trim()
    filteredMessages.value = await localDb.getFilteredMessages(myUserId.value, targetUserId.value, filters)
  } catch (e) {
    console.warn('[ChatSettings] 筛选失败:', e)
    filteredMessages.value = []
  } finally {
    searching.value = false
  }
}

const goToMessage = (msg: Message) => {
  router.push(`/messages/${targetUserId.value}`)
}

// ==================== 危险操作确认 ====================
const showConfirmDialog = ref(false)
const confirmType = ref<'clear' | 'delete'>('clear')
const confirmTitle = ref('')
const confirmDesc = ref('')
const confirmBtnText = ref('')
let confirmAction: (() => Promise<void>) | null = null

const confirmClearCache = () => {
  confirmType.value = 'clear'
  confirmTitle.value = '清除本地缓存'
  confirmDesc.value = '将清除该会话在本地的缓存数据。服务端消息不受影响，下次打开时会重新加载。'
  confirmBtnText.value = '清除缓存'
  confirmAction = async () => {
    await messageStore.clearConversation(targetUserId.value)
    await loadStats()
    filteredMessages.value = []
    showToast('本地缓存已清除')
  }
  showConfirmDialog.value = true
}

const confirmDeleteAll = () => {
  confirmType.value = 'delete'
  confirmTitle.value = '删除全部聊天记录'
  confirmDesc.value = `确定要删除与「${targetUser.value?.nickname || '该用户'}」的所有本地聊天记录吗？此操作不可恢复。`
  confirmBtnText.value = '确认删除'
  confirmAction = async () => {
    const count = await messageStore.clearConversation(targetUserId.value)
    await loadStats()
    filteredMessages.value = []
    showConfirmDialog.value = false
    showToast(`已删除 ${count} 条消息`)
  }
  showConfirmDialog.value = true
}

const executeConfirm = async () => {
  showConfirmDialog.value = false
  if (confirmAction) {
    await confirmAction()
    confirmAction = null
  }
}

// ==================== Toast ====================
const toastMsg = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const showToast = (msg: string) => {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2000)
}

// ==================== 辅助 ====================
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(`/messages/${targetUserId.value}`)
  }
}

const formatDateShort = (timeStr: string) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

const formatMessageTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (conversations.value.length === 0) {
    await messageStore.fetchConversations()
  }
  loadRemark()
  loadChatSettings()
  await loadStats()
})
</script>

<style scoped>
.settings-page {
  margin: -3rem 0 0 0;
}

@media (min-width: 768px) {
  .settings-page {
    margin: -4rem auto 0 auto;
    max-width: 640px;
    border-left: 1px solid #e2e8f0;
    border-right: 1px solid #e2e8f0;
  }
  .dark .settings-page {
    border-color: #374151;
  }
}

.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }
.dark .custom-scrollbar::-webkit-scrollbar-thumb { background: #4b5563; }

/* ==================== 用户卡片 ==================== */
.user-card {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04), rgba(139, 92, 246, 0.02));
  border: 1px solid var(--zh-border);
  border-radius: 16px;
  padding: 16px;
  transition: box-shadow 0.2s;
}
.user-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.dark .user-card {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(139, 92, 246, 0.04));
  border-color: rgba(55, 65, 81, 0.6);
}
.dark .user-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

/* ==================== 统计卡片 ==================== */
.stats-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: var(--zh-bg);
  border: 1px solid var(--zh-border);
  border-radius: 14px;
  padding: 14px 8px;
}
.stats-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.stats-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--zh-primary);
  line-height: 1;
}
.stats-value-date {
  font-size: 12px;
  font-weight: 600;
}
.stats-label {
  font-size: 11px;
  color: var(--zh-text-tertiary);
}
.stats-divider {
  width: 1px;
  height: 28px;
  background: var(--zh-border);
  flex-shrink: 0;
}
.dark .stats-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(55, 65, 81, 0.5);
}
.dark .stats-divider {
  background: rgba(55, 65, 81, 0.5);
}

/* ==================== 功能菜单 ==================== */
.menu-section {
  border: 1px solid var(--zh-border);
  border-radius: 14px;
  overflow: hidden;
  background: var(--zh-bg);
}
.dark .menu-section {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(55, 65, 81, 0.5);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  color: var(--zh-text);
  font-size: 14px;
}
.menu-item:hover {
  background: var(--zh-bg-hover);
}
.dark .menu-item:hover {
  background: rgba(51, 65, 85, 0.25);
}

.menu-item-divider {
  height: 1px;
  margin: 0 16px;
  background: var(--zh-border);
}
.dark .menu-item-divider {
  background: rgba(55, 65, 81, 0.4);
}

.menu-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-icon-blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}
.menu-icon-orange {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}
.menu-icon-gray {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}
.menu-icon-green {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.menu-text {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.menu-arrow {
  width: 16px;
  height: 16px;
  color: var(--zh-text-tertiary);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}
.menu-arrow-open {
  transform: rotate(90deg);
}

/* ==================== 搜索面板 ==================== */
.search-panel {
  padding: 0 16px 16px;
}

.filter-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.filter-tab {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--zh-border);
  background: var(--zh-bg);
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.filter-tab.active {
  background: var(--zh-primary);
  color: #fff;
  border-color: var(--zh-primary);
}
.dark .filter-tab {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(55, 65, 81, 0.5);
  color: #94a3b8;
}
.dark .filter-tab.active {
  background: var(--zh-primary);
  color: #fff;
  border-color: var(--zh-primary);
}

.date-filter-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.date-label {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  flex-shrink: 0;
}
.date-input {
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--zh-border);
  border-radius: 8px;
  background: var(--zh-bg);
  font-size: 12px;
  color: var(--zh-text);
  outline: none;
  flex: 1;
  min-width: 0;
}
.date-input:focus {
  border-color: var(--zh-primary);
}
.dark .date-input {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(55, 65, 81, 0.5);
  color: #e2e8f0;
}
.date-clear {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: var(--zh-bg-hover);
  color: var(--zh-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.keyword-row {
  position: relative;
  margin-bottom: 10px;
}
.keyword-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  color: var(--zh-text-tertiary);
  pointer-events: none;
}
.keyword-input {
  width: 100%;
  height: 34px;
  padding: 0 12px 0 32px;
  border: 1px solid var(--zh-border);
  border-radius: 10px;
  background: var(--zh-bg);
  font-size: 13px;
  color: var(--zh-text);
  outline: none;
}
.keyword-input:focus {
  border-color: var(--zh-primary);
}
.dark .keyword-input {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(55, 65, 81, 0.5);
  color: #e2e8f0;
}

.search-results {
  max-height: 360px;
  overflow-y: auto;
}
.search-loading,
.search-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  color: var(--zh-text-tertiary);
  font-size: 13px;
}
.search-count {
  display: block;
  font-size: 11px;
  color: var(--zh-text-tertiary);
  padding: 4px 0 8px;
}
.search-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.search-result-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--zh-bg);
  border: 1px solid var(--zh-border-light);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.search-result-item:hover {
  background: var(--zh-bg-hover);
  border-color: var(--zh-primary);
}
.dark .search-result-item {
  background: rgba(30, 41, 59, 0.4);
  border-color: rgba(55, 65, 81, 0.4);
}
.dark .search-result-item:hover {
  background: rgba(51, 65, 85, 0.3);
  border-color: var(--zh-primary);
}
.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.result-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--zh-text);
}
.result-time {
  font-size: 10px;
  color: var(--zh-text-tertiary);
}
.result-content {
  font-size: 13px;
  color: var(--zh-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-image-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}
.result-image-tag {
  font-size: 11px;
  color: var(--zh-text-tertiary);
  background: var(--zh-bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
}

/* ==================== 备注 ==================== */
.remark-item {
  cursor: default;
}
.remark-content {
  flex: 1;
  min-width: 0;
}
.remark-display {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  margin-top: 2px;
}
.remark-text {
  font-size: 13px;
  color: var(--zh-primary);
  font-weight: 500;
}
.remark-placeholder {
  font-size: 13px;
  color: var(--zh-text-tertiary);
}
.remark-edit {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.remark-input {
  flex: 1;
  height: 30px;
  padding: 0 10px;
  border: 1.5px solid var(--zh-primary);
  border-radius: 8px;
  background: var(--zh-bg);
  font-size: 13px;
  color: var(--zh-text);
  outline: none;
}
.dark .remark-input {
  background: rgba(30, 41, 59, 0.6);
  color: #e2e8f0;
}
.remark-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.remark-btn {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}
.remark-btn:hover { opacity: 0.85; }
.remark-btn-cancel {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}
.remark-btn-save {
  background: var(--zh-primary);
  color: #fff;
}

/* ==================== 开关 ==================== */
.toggle-switch {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  background: #d1d5db;
  border: none;
  cursor: pointer;
  position: relative;
  transition: background 0.3s;
  flex-shrink: 0;
  padding: 0;
}
.toggle-switch.active {
  background: var(--zh-primary);
}
.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toggle-switch.active .toggle-thumb {
  transform: translateX(20px);
}
.dark .toggle-switch {
  background: #4b5563;
}
.dark .toggle-switch.active {
  background: var(--zh-primary);
}

/* ==================== 危险按钮 ==================== */
.danger-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.05);
  color: #ef4444;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.danger-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
}
.danger-btn-strong {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.3);
  font-weight: 600;
}
.danger-btn-strong:hover {
  background: rgba(239, 68, 68, 0.15);
}
.dark .danger-btn {
  background: rgba(220, 38, 38, 0.1);
  border-color: rgba(220, 38, 38, 0.25);
  color: #fca5a5;
}
.dark .danger-btn:hover {
  background: rgba(220, 38, 38, 0.18);
}
.dark .danger-btn-strong {
  background: rgba(220, 38, 38, 0.15);
  border-color: rgba(220, 38, 38, 0.35);
}

/* ==================== 确认对话框 ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
}
.modal-box {
  width: 320px;
  background: var(--zh-bg-elevated);
  border-radius: 20px;
  padding: 28px 24px 20px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.dark .modal-box {
  background: #1e293b;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
.modal-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.modal-icon-red {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}
.modal-icon-blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}
.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--zh-text);
  margin-bottom: 8px;
}
.modal-desc {
  font-size: 13px;
  color: var(--zh-text-secondary);
  line-height: 1.5;
  margin-bottom: 24px;
}
.modal-actions {
  display: flex;
  gap: 10px;
}
.modal-btn {
  flex: 1;
  padding: 10px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}
.modal-btn:hover { opacity: 0.85; }
.modal-btn-cancel {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}
.modal-btn-danger {
  background: #ef4444;
  color: #fff;
}
.modal-btn-primary {
  background: var(--zh-primary);
  color: #fff;
}

/* ==================== Toast ==================== */
.toast-bar {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  padding: 10px 24px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 13px;
  border-radius: 24px;
  backdrop-filter: blur(8px);
  white-space: nowrap;
}

/* ==================== 过渡动画 ==================== */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  max-height: 0;
}
.slide-down-enter-to,
.slide-down-leave-from {
  opacity: 1;
  max-height: 600px;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}
</style>
