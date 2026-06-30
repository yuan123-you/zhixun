<template>
  <!-- 群组聊天记录搜索面板 -->
  <div class="search-panel">
    <div class="search-header">
      <div class="search-input-wrap">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索聊天记录..."
          @keydown.enter="doSearch"
        />
        <button v-if="keyword" class="search-clear" @click="keyword = ''; doSearch()">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
      </div>
      <button class="search-close-btn" @click="$emit('close')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
      </button>
    </div>

    <!-- 筛选条件 -->
    <div class="search-filters">
      <div class="filter-chips">
        <button
          class="filter-chip"
          :class="{ active: filterType !== '' }"
          @click="showTypeDropdown = !showTypeDropdown"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
          {{ filterType ? typeLabel(filterType) : '全部类型' }}
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" /></svg>
        </button>
        <button
          class="filter-chip"
          :class="{ active: filterMember !== '' }"
          @click="showMemberDropdown = !showMemberDropdown"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
          {{ filterMember ? getMemberName(filterMember) : '全部成员' }}
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" /></svg>
        </button>
        <button
          class="filter-chip"
          :class="{ active: filterStartDate !== '' || filterEndDate !== '' }"
          @click="showDateFilter = !showDateFilter"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          日期
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" /></svg>
        </button>
      </div>
    </div>

    <!-- 类型下拉 -->
    <div v-if="showTypeDropdown" class="filter-dropdown" @click.stop>
      <button class="dropdown-item" :class="{ active: filterType === '' }" @click="filterType = ''; showTypeDropdown = false">全部类型</button>
      <button class="dropdown-item" :class="{ active: filterType === 'text' }" @click="filterType = 'text'; showTypeDropdown = false">文本消息</button>
      <button class="dropdown-item" :class="{ active: filterType === 'image' }" @click="filterType = 'image'; showTypeDropdown = false">图片</button>
      <button class="dropdown-item" :class="{ active: filterType === 'file' }" @click="filterType = 'file'; showTypeDropdown = false">文件</button>
    </div>

    <!-- 成员下拉 -->
    <div v-if="showMemberDropdown" class="filter-dropdown filter-dropdown-members" @click.stop>
      <button class="dropdown-item" :class="{ active: filterMember === '' }" @click="filterMember = ''; showMemberDropdown = false">全部成员</button>
      <button
        v-for="m in members"
        :key="m.userId"
        class="dropdown-item"
        :class="{ active: filterMember === String(m.userId) }"
        @click="filterMember = String(m.userId); showMemberDropdown = false"
      >
        <img :src="m.userAvatar || defaultAvatar" class="dropdown-avatar" />
        {{ m.nickname || m.userName }}
      </button>
    </div>

    <!-- 日期筛选展开 -->
    <div v-if="showDateFilter" class="date-filter-bar">
      <input v-model="filterStartDate" type="date" class="search-date" placeholder="开始日期" />
      <span class="search-date-sep">至</span>
      <input v-model="filterEndDate" type="date" class="search-date" placeholder="结束日期" />
      <button class="date-clear-btn" v-if="filterStartDate || filterEndDate" @click="filterStartDate = ''; filterEndDate = ''">清除</button>
    </div>

    <!-- 活跃筛选标签 -->
    <div v-if="activeFilterTags.length > 0" class="active-filters">
      <span v-for="tag in activeFilterTags" :key="tag.key" class="active-filter-tag" @click="tag.clear()">
        {{ tag.label }}
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
      </span>
      <button class="clear-all-btn" @click="clearAllFilters">清除全部</button>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" @click="showTypeDropdown = false; showMemberDropdown = false">
      <div v-if="loading" class="search-loading">
        <div class="search-spinner"></div>
        <span>搜索中...</span>
      </div>
      <div v-else-if="searched && results.length === 0" class="search-empty">
        <svg class="w-10 h-10 opacity-30 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p>未找到匹配的聊天记录</p>
      </div>
      <div v-else-if="results.length > 0" class="search-list">
        <div
          v-for="msg in results"
          :key="msg.id"
          class="search-result-item"
          @click="$emit('locateMessage', msg.id)"
        >
          <img :src="msg.senderAvatar || defaultAvatar" class="search-result-avatar" />
          <div class="search-result-body">
            <div class="search-result-header">
              <span class="search-result-name">{{ msg.senderName || '未知用户' }}</span>
              <span v-if="msg.messageType && msg.messageType !== 'text'" class="search-result-type-badge">{{ typeLabel(msg.messageType) }}</span>
              <span class="search-result-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
            <p class="search-result-content">
              <template v-if="msg.messageType === 'image'">[图片]</template>
              <template v-else-if="msg.messageType === 'file'">[文件]</template>
              <template v-else-if="msg.messageType === 'ai_reply'">[AI回复] </template>
              <template v-else>{{ truncateContent(msg.content) }}</template>
            </p>
          </div>
        </div>
        <div v-if="hasMore" class="search-load-more">
          <button @click="loadMore" :disabled="loading">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
      <div v-else class="search-hint">
        <svg class="w-10 h-10 opacity-30 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <p>输入关键词搜索聊天记录</p>
        <p class="search-hint-sub">支持按类型、成员和日期筛选</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { groupApi } from '@/api/group'
import type { GroupMessage, GroupMember } from '@/api/group'

const props = defineProps<{
  groupId: number
  members: GroupMember[]
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'locateMessage', msgId: number): void
}>()

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNlMmU4ZjAiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCIgZm9udC1zaXplPSIxNiI+PzwvdGV4dD48L3N2Zz4='

const keyword = ref('')
const filterType = ref('')
const filterMember = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')
const results = ref<GroupMessage[]>([])
const loading = ref(false)
const searched = ref(false)
const hasMore = ref(false)
const showTypeDropdown = ref(false)
const showMemberDropdown = ref(false)
const showDateFilter = ref(false)
let searchOffset = 0
const PAGE_SIZE = 30

const activeFilterTags = computed(() => {
  const tags: { key: string; label: string; clear: () => void }[] = []
  if (filterType.value) {
    tags.push({ key: 'type', label: typeLabel(filterType.value), clear: () => { filterType.value = '' } })
  }
  if (filterMember.value) {
    tags.push({ key: 'member', label: getMemberName(filterMember.value), clear: () => { filterMember.value = '' } })
  }
  if (filterStartDate.value) {
    tags.push({ key: 'start', label: `从 ${filterStartDate.value}`, clear: () => { filterStartDate.value = '' } })
  }
  if (filterEndDate.value) {
    tags.push({ key: 'end', label: `至 ${filterEndDate.value}`, clear: () => { filterEndDate.value = '' } })
  }
  return tags
})

function clearAllFilters() {
  filterType.value = ''
  filterMember.value = ''
  filterStartDate.value = ''
  filterEndDate.value = ''
}

function getMemberName(userId: string): string {
  const m = props.members.find(m => String(m.userId) === userId)
  return m ? (m.nickname || m.userName) : '未知成员'
}

async function doSearch() {
  // 至少需要一个搜索条件（关键词、类型筛选、成员筛选或日期筛选）
  const hasKeyword = keyword.value.trim().length > 0
  const hasTypeFilter = filterType.value !== ''
  const hasMemberFilter = filterMember.value !== ''
  const hasDateFilter = filterStartDate.value !== '' || filterEndDate.value !== ''

  if (!hasKeyword && !hasTypeFilter && !hasMemberFilter && !hasDateFilter) {
    results.value = []
    searched.value = false
    return
  }
  loading.value = true
  searched.value = true
  searchOffset = 0
  try {
    const params: Record<string, any> = {
      offset: 0,
      limit: PAGE_SIZE,
    }
    if (hasKeyword) params.keyword = keyword.value.trim()
    if (hasTypeFilter) params.messageType = filterType.value
    if (filterStartDate.value) params.startDate = filterStartDate.value
    if (filterEndDate.value) params.endDate = filterEndDate.value
    if (hasMemberFilter) params.senderId = Number(filterMember.value)

    const res = await groupApi.searchMessages(props.groupId, params)
    results.value = res.data.data || []
    hasMore.value = results.value.length === PAGE_SIZE
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value) return
  loading.value = true
  searchOffset += PAGE_SIZE
  try {
    const params: Record<string, any> = {
      offset: searchOffset,
      limit: PAGE_SIZE,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (filterType.value) params.messageType = filterType.value
    if (filterStartDate.value) params.startDate = filterStartDate.value
    if (filterEndDate.value) params.endDate = filterEndDate.value
    if (filterMember.value) params.senderId = Number(filterMember.value)

    const res = await groupApi.searchMessages(props.groupId, params)
    const newResults = res.data.data || []
    results.value = [...results.value, ...newResults]
    hasMore.value = newResults.length === PAGE_SIZE
  } catch { /* silent */ } finally {
    loading.value = false
  }
}

// 筛选条件变化时自动搜索
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch([filterType, filterMember, filterStartDate, filterEndDate], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(doSearch, 300)
})

function truncateContent(content: string): string {
  if (!content) return ''
  return content.length > 80 ? content.substring(0, 80) + '...' : content
}

function typeLabel(type: string): string {
  const labels: Record<string, string> = {
    text: '文本', image: '图片', file: '文件', ai_reply: 'AI回复', system: '系统', voice: '语音'
  }
  return labels[type] || type
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

// 点击外部关闭下拉
function onGlobalClick() {
  showTypeDropdown.value = false
  showMemberDropdown.value = false
}
onMounted(() => document.addEventListener('click', onGlobalClick))
onUnmounted(() => document.removeEventListener('click', onGlobalClick))
</script>

<style scoped>
.search-panel {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: var(--zh-bg-elevated, #fff);
  z-index: 5;
  display: flex; flex-direction: column;
  overflow: hidden;
}
.search-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}
.search-input-wrap {
  flex: 1; display: flex; align-items: center; gap: 8px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 20px; padding: 7px 14px;
  border: 1.5px solid transparent;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-input-wrap:focus-within {
  border-color: var(--zh-primary, #6366f1);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  background: var(--zh-bg-elevated, #fff);
}
.search-icon {
  width: 16px; height: 16px;
  color: var(--zh-text-tertiary, #94a3b8);
  flex-shrink: 0;
}
.search-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 14px; color: var(--zh-text, #1e293b);
}
.search-input::placeholder { color: var(--zh-text-placeholder, #cbd5e1); }
.search-clear {
  border: none; background: transparent; color: var(--zh-text-tertiary);
  cursor: pointer; padding: 2px; min-height: 0; min-width: 0;
  display: flex; align-items: center;
}
.search-close-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: none; border-radius: 10px;
  background: transparent;
  color: var(--zh-text-tertiary);
  cursor: pointer; transition: background 0.15s;
  flex-shrink: 0; min-height: 0; min-width: 0;
}
.search-close-btn:hover { background: var(--zh-bg-hover, #f1f5f9); }

/* 筛选条件 */
.search-filters {
  padding: 8px 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}
.filter-chips {
  display: flex; gap: 6px; flex-wrap: wrap;
}
.filter-chip {
  display: flex; align-items: center; gap: 4px;
  padding: 5px 10px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 18px;
  background: var(--zh-bg, #f8fafc);
  color: var(--zh-text-secondary, #64748b);
  font-size: 12px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  white-space: nowrap; min-height: 0;
}
.filter-chip:hover {
  border-color: var(--zh-primary, #6366f1);
  color: var(--zh-primary, #6366f1);
}
.filter-chip.active {
  background: rgba(99, 102, 241, 0.08);
  border-color: var(--zh-primary, #6366f1);
  color: var(--zh-primary, #6366f1);
}

/* 下拉选择 */
.filter-dropdown {
  position: absolute; left: 14px; z-index: 10;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 4px; min-width: 140px;
}
.filter-dropdown-members {
  max-height: 240px; overflow-y: auto;
}
.dropdown-item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 8px 12px;
  border: none; border-radius: 8px;
  background: transparent;
  color: var(--zh-text, #1e293b);
  font-size: 12px; cursor: pointer;
  transition: background 0.15s;
  text-align: left; min-height: 0;
}
.dropdown-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.dropdown-item.active {
  color: var(--zh-primary, #6366f1);
  background: rgba(99, 102, 241, 0.08);
  font-weight: 600;
}
.dropdown-avatar {
  width: 20px; height: 20px;
  border-radius: 50%; object-fit: cover;
}

/* 日期筛选 */
.date-filter-bar {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--zh-border, #f1f5f9);
  flex-shrink: 0;
  background: var(--zh-bg, #f8fafc);
}
.search-date {
  font-size: 12px; padding: 5px 8px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 8px; background: var(--zh-bg-elevated, #fff);
  color: var(--zh-text, #1e293b);
  outline: none;
  transition: border-color 0.2s;
}
.search-date:focus {
  border-color: var(--zh-primary, #6366f1);
}
.search-date-sep {
  font-size: 11px; color: var(--zh-text-tertiary);
}
.date-clear-btn {
  font-size: 11px; color: #ef4444;
  background: none; border: none;
  cursor: pointer; padding: 4px 8px;
  border-radius: 6px; min-height: 0;
}
.date-clear-btn:hover {
  background: #fef2f2;
}

/* 活跃筛选标签 */
.active-filters {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  border-bottom: 1px solid var(--zh-border, #f1f5f9);
  flex-shrink: 0;
  flex-wrap: wrap;
}
.active-filter-tag {
  display: flex; align-items: center; gap: 3px;
  padding: 3px 8px;
  background: rgba(99, 102, 241, 0.08);
  color: var(--zh-primary, #6366f1);
  border-radius: 12px;
  font-size: 11px; font-weight: 500;
  cursor: pointer; transition: background 0.15s;
}
.active-filter-tag:hover {
  background: rgba(99, 102, 241, 0.16);
}
.clear-all-btn {
  font-size: 11px; color: var(--zh-text-tertiary, #94a3b8);
  background: none; border: none; cursor: pointer;
  padding: 3px 6px; border-radius: 6px; min-height: 0;
}
.clear-all-btn:hover { color: #ef4444; }

/* 搜索结果 */
.search-results {
  flex: 1; overflow-y: auto; padding: 4px 0;
}
.search-loading, .search-empty, .search-hint {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 16px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 13px;
}
.search-hint-sub {
  font-size: 11px; color: var(--zh-text-tertiary);
  opacity: 0.6; margin-top: 4px;
}
.search-spinner {
  width: 24px; height: 24px; margin-bottom: 8px;
  border: 2.5px solid var(--zh-border); border-top-color: var(--zh-primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.search-result-item {
  display: flex; gap: 10px;
  padding: 10px 14px; cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--zh-border, #f1f5f9);
}
.search-result-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.search-result-avatar {
  width: 28px; height: 28px;
  border-radius: 50%; object-fit: cover;
  flex-shrink: 0; margin-top: 2px;
}
.search-result-body { flex: 1; min-width: 0; }
.search-result-header {
  display: flex; align-items: center; gap: 6px; margin-bottom: 3px;
}
.search-result-name {
  font-size: 12px; font-weight: 600; color: var(--zh-text, #1e293b);
}
.search-result-type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.08);
  color: var(--zh-primary, #6366f1);
  font-weight: 500;
}
.search-result-time {
  font-size: 10px; color: var(--zh-text-tertiary, #94a3b8); margin-left: auto;
  flex-shrink: 0;
}
.search-result-content {
  font-size: 13px; color: var(--zh-text-secondary, #64748b);
  line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; margin: 0;
}
.search-load-more {
  text-align: center; padding: 12px;
}
.search-load-more button {
  font-size: 12px; color: var(--zh-primary, #6366f1);
  background: transparent; border: none; cursor: pointer;
  padding: 6px 16px; border-radius: 8px;
  transition: background 0.15s;
  min-height: 0; min-width: 0;
  font-weight: 500;
}
.search-load-more button:hover { background: var(--zh-bg-hover, #f1f5f9); }
.search-load-more button:disabled { color: var(--zh-text-tertiary); cursor: default; }

@media (max-width: 768px) {
  .search-filters { padding: 6px 10px; }
  .search-header { padding: 8px 10px; }
  .search-result-avatar { width: 24px; height: 24px; }
}
</style>
