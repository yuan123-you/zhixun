<template>
  <!-- 群组聊天记录搜索面板 -->
  <div class="search-panel">
    <div class="search-header">
      <div class="search-input-wrap">
        <svg class="w-4 h-4 text-[var(--zh-text-tertiary)] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索聊天记录..."
          @keydown.enter="doSearch"
        />
        <button v-if="keyword" class="search-clear" @click="keyword = ''; results = []">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
      </div>
      <button class="search-close-btn" @click="$emit('close')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
      </button>
    </div>

    <!-- 筛选条件 -->
    <div class="search-filters">
      <select v-model="filterType" class="search-select">
        <option value="">全部类型</option>
        <option value="text">文本消息</option>
        <option value="image">图片</option>
        <option value="file">文件</option>
      </select>
      <select v-model="filterMember" class="search-select">
        <option value="">全部成员</option>
        <option v-for="m in members" :key="m.userId" :value="m.userId">
          {{ m.nickname || m.userName }}
        </option>
      </select>
      <div class="search-date-wrap">
        <input v-model="filterStartDate" type="date" class="search-date" />
        <span class="search-date-sep">至</span>
        <input v-model="filterEndDate" type="date" class="search-date" />
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results">
      <div v-if="loading" class="search-loading">
        <div class="search-spinner"></div>
        <span>搜索中...</span>
      </div>
      <div v-else-if="searched && results.length === 0" class="search-empty">
        <p>未找到匹配的聊天记录</p>
      </div>
      <div v-else-if="results.length > 0" class="search-list">
        <div
          v-for="msg in results"
          :key="msg.id"
          class="search-result-item"
          @click="$emit('locateMessage', msg.id)"
        >
          <div class="search-result-header">
            <img :src="msg.senderAvatar || defaultAvatar" class="search-result-avatar" />
            <span class="search-result-name">{{ msg.senderName || '未知用户' }}</span>
            <span class="search-result-time">{{ formatTime(msg.createdAt) }}</span>
          </div>
          <p class="search-result-content">
            <template v-if="msg.messageType === 'image'">[图片]</template>
            <template v-else-if="msg.messageType === 'file'">[文件]</template>
            <template v-else-if="msg.messageType === 'ai_reply'">[AI回复] </template>
            <template v-else>{{ truncateContent(msg.content) }}</template>
          </p>
          <span v-if="msg.messageType" class="search-result-type">{{ typeLabel(msg.messageType) }}</span>
        </div>
        <div v-if="hasMore" class="search-load-more">
          <button @click="loadMore" :disabled="loading">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
      <div v-else class="search-hint">
        <svg class="w-8 h-8 text-[var(--zh-text-tertiary)] opacity-40 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <p>输入关键词搜索聊天记录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
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
let searchOffset = 0
const PAGE_SIZE = 30

async function doSearch() {
  if (!keyword.value.trim() && !filterType.value && !filterMember.value && !filterStartDate.value && !filterEndDate.value) {
    results.value = []
    searched.value = false
    return
  }
  loading.value = true
  searched.value = true
  searchOffset = 0
  try {
    const res = await groupApi.searchMessages(props.groupId, {
      keyword: keyword.value.trim() || undefined,
      messageType: filterType.value || undefined,
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined,
      senderId: filterMember.value ? Number(filterMember.value) : undefined,
      offset: 0,
      limit: PAGE_SIZE,
    })
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
    const res = await groupApi.searchMessages(props.groupId, {
      keyword: keyword.value.trim() || undefined,
      messageType: filterType.value || undefined,
      startDate: filterStartDate.value || undefined,
      endDate: filterEndDate.value || undefined,
      senderId: filterMember.value ? Number(filterMember.value) : undefined,
      offset: searchOffset,
      limit: PAGE_SIZE,
    })
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
    text: '文本', image: '图片', file: '文件', ai_reply: 'AI回复', system: '系统'
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
  border-radius: 20px; padding: 6px 14px;
}
.search-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 14px; color: var(--zh-text, #1e293b);
}
.search-input::placeholder { color: var(--zh-text-placeholder, #cbd5e1); }
.search-clear {
  border: none; background: transparent; color: var(--zh-text-tertiary);
  cursor: pointer; padding: 2px; min-height: 0; min-width: 0;
}
.search-close-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: none; border-radius: 8px;
  background: transparent;
  color: var(--zh-text-tertiary);
  cursor: pointer; transition: background 0.15s;
  flex-shrink: 0; min-height: 0; min-width: 0;
}
.search-close-btn:hover { background: var(--zh-bg-hover, #f1f5f9); }

/* 筛选条件 */
.search-filters {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}
.search-select {
  font-size: 12px; padding: 4px 8px;
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 6px; background: var(--zh-bg, #f8fafc);
  color: var(--zh-text, #1e293b);
  outline: none;
}
.search-date-wrap {
  display: flex; align-items: center; gap: 4px;
}
.search-date {
  font-size: 11px; padding: 4px 6px;
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 6px; background: var(--zh-bg, #f8fafc);
  color: var(--zh-text, #1e293b);
  outline: none;
}
.search-date-sep {
  font-size: 11px; color: var(--zh-text-tertiary);
}

/* 搜索结果 */
.search-results {
  flex: 1; overflow-y: auto; padding: 8px 0;
}
.search-loading, .search-empty, .search-hint {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 16px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 13px;
}
.search-spinner {
  width: 24px; height: 24px; margin-bottom: 8px;
  border: 2.5px solid var(--zh-border); border-top-color: var(--zh-primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.search-result-item {
  padding: 10px 14px; cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--zh-border, #f1f5f9);
  position: relative;
}
.search-result-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.search-result-header {
  display: flex; align-items: center; gap: 6px; margin-bottom: 4px;
}
.search-result-avatar {
  width: 20px; height: 20px; border-radius: 50%; object-fit: cover;
}
.search-result-name {
  font-size: 12px; font-weight: 500; color: var(--zh-text, #1e293b);
}
.search-result-time {
  font-size: 10px; color: var(--zh-text-tertiary, #94a3b8); margin-left: auto;
}
.search-result-content {
  font-size: 13px; color: var(--zh-text-secondary, #64748b);
  line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; margin: 0;
}
.search-result-type {
  position: absolute; top: 10px; right: 14px;
  font-size: 10px; color: var(--zh-text-tertiary);
  background: var(--zh-bg-hover, #f1f5f9);
  padding: 1px 6px; border-radius: 4px;
}
.search-load-more {
  text-align: center; padding: 12px;
}
.search-load-more button {
  font-size: 12px; color: var(--zh-primary, #6366f1);
  background: transparent; border: none; cursor: pointer;
  padding: 4px 12px; border-radius: 6px;
  transition: background 0.15s;
  min-height: 0; min-width: 0;
}
.search-load-more button:hover { background: var(--zh-bg-hover, #f1f5f9); }
.search-load-more button:disabled { color: var(--zh-text-tertiary); cursor: default; }

@media (max-width: 768px) {
  .search-filters { padding: 6px 10px; gap: 6px; }
  .search-header { padding: 8px 10px; }
}
</style>
