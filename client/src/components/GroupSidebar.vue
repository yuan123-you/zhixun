<template>
  <!-- 群组列表侧边栏 -->
  <div class="group-sidebar">
    <div class="sidebar-header">
      <h2 class="sidebar-title">群组</h2>
      <button class="sidebar-create-btn" @click="$emit('create')" aria-label="创建群组">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
      </button>
    </div>

    <!-- 搜索 -->
    <div class="sidebar-search">
      <svg class="sidebar-search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        v-model="searchText"
        type="text"
        class="sidebar-search-input"
        placeholder="搜索群组..."
      />
    </div>

    <!-- 群组列表 -->
    <div class="sidebar-list">
      <div v-if="loading" class="sidebar-loading">
        <div v-for="i in 5" :key="i" class="sidebar-skeleton">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-text">
            <div class="skeleton-line w-24"></div>
            <div class="skeleton-line w-16"></div>
          </div>
        </div>
      </div>

      <div v-else-if="filteredGroups.length === 0" class="sidebar-empty">
        <p>暂无群组</p>
        <button class="sidebar-empty-btn" @click="$emit('create')">创建群组</button>
      </div>

      <div
        v-else
        v-for="group in filteredGroups"
        :key="group.id"
        class="sidebar-item"
        :class="{ active: group.id === activeGroupId }"
        @click="$emit('select', group.id)"
      >
        <div class="sidebar-item-avatar">
          <img v-if="group.avatar" :src="group.avatar" :alt="group.name" class="sidebar-avatar-img" />
          <div v-else class="sidebar-avatar-placeholder" :class="avatarColor(group.name)">
            {{ group.name.charAt(0) }}
          </div>
        </div>
        <div class="sidebar-item-info">
          <div class="sidebar-item-top">
            <span class="sidebar-item-name">{{ group.name }}</span>
            <span v-if="group.myRole === 2" class="sidebar-role-badge owner">群主</span>
            <span v-else-if="group.myRole === 1" class="sidebar-role-badge admin">管理</span>
          </div>
          <div class="sidebar-item-bottom">
            <span class="sidebar-item-meta">{{ group.memberCount }}人</span>
            <span v-if="group.description" class="sidebar-item-desc">{{ group.description }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：发现群组 -->
    <div class="sidebar-footer">
      <button class="sidebar-discover-btn" @click="$emit('discover')">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        发现群组
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { GroupInfo } from '@/api/group'
import { avatarColor } from '@/utils/color'

const props = defineProps<{
  groups: GroupInfo[]
  activeGroupId?: number | null
  loading?: boolean
}>()

defineEmits<{
  (e: 'select', id: number): void
  (e: 'create'): void
  (e: 'discover'): void
}>()

const searchText = ref('')

const filteredGroups = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return props.groups
  return props.groups.filter(g => g.name.toLowerCase().includes(kw))
})
</script>

<style scoped>
.group-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg, #f8fafc);
  border-right: 1px solid var(--zh-border, #e5e7eb);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 8px;
  flex-shrink: 0;
}
.sidebar-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--zh-text, #1e293b);
}
.sidebar-create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: pointer;
  transition: transform 0.15s, opacity 0.2s;
}
.sidebar-create-btn:hover { opacity: 0.9; transform: scale(1.05); }
.sidebar-create-btn:active { transform: scale(0.95); }

.sidebar-search {
  position: relative;
  padding: 6px 12px 10px;
  flex-shrink: 0;
}
.sidebar-search-icon {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(calc(-50% + 3px));
  width: 15px;
  height: 15px;
  color: var(--zh-text-tertiary, #94a3b8);
  pointer-events: none;
}
.sidebar-search-input {
  width: 100%;
  height: 34px;
  padding: 0 12px 0 34px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
  border-radius: 10px;
  background: var(--zh-bg-elevated, #fff);
  font-size: 13px;
  color: var(--zh-text, #1e293b);
  outline: none;
  transition: border-color 0.2s;
}
.sidebar-search-input:focus {
  border-color: var(--zh-primary, #6366f1);
}
.sidebar-search-input::placeholder {
  color: var(--zh-text-placeholder, #cbd5e1);
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 6px;
  -webkit-overflow-scrolling: touch;
}

.sidebar-loading { padding: 0 6px; }
.sidebar-skeleton {
  display: flex;
  gap: 10px;
  padding: 10px 8px;
}
.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--zh-border, #e5e7eb);
  flex-shrink: 0;
  animation: pulse 1.5s ease-in-out infinite;
}
.skeleton-text { flex: 1; display: flex; flex-direction: column; gap: 6px; padding-top: 4px; }
.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: var(--zh-border, #e5e7eb);
  animation: pulse 1.5s ease-in-out infinite;
}
.w-24 { width: 96px; }
.w-16 { width: 64px; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 13px;
  gap: 12px;
}
.sidebar-empty-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}
.sidebar-item:hover { background: var(--zh-bg-hover, #f1f5f9); }
.sidebar-item.active {
  background: rgba(99, 102, 241, 0.08);
  border-left: 3px solid var(--zh-primary, #6366f1);
  padding-left: 7px;
}

.sidebar-item-avatar { flex-shrink: 0; }
.sidebar-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
}
.sidebar-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
}

.sidebar-item-info { flex: 1; min-width: 0; }
.sidebar-item-top { display: flex; align-items: center; gap: 5px; }
.sidebar-item-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar-role-badge {
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 600;
  flex-shrink: 0;
}
.sidebar-role-badge.owner {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}
.sidebar-role-badge.admin {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.sidebar-item-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.sidebar-item-meta {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
}
.sidebar-item-desc {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer {
  padding: 8px 12px;
  border-top: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}
.sidebar-discover-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px;
  border: 1.5px dashed var(--zh-border, #e5e7eb);
  border-radius: 10px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.sidebar-discover-btn:hover {
  border-color: var(--zh-primary, #6366f1);
  color: var(--zh-primary, #6366f1);
  background: rgba(99, 102, 241, 0.04);
}

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  .group-sidebar {
    border-right: none;
  }
  .sidebar-header { padding: 12px 14px 6px; }
  .sidebar-item { padding: 8px; }
  .sidebar-avatar-img,
  .sidebar-avatar-placeholder { width: 36px; height: 36px; border-radius: 10px; }
}
</style>
