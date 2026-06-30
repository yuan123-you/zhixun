<template>
  <!-- 群组成员面板 -->
  <div class="member-panel" :class="{ 'mobile-open': mobileOpen }">
    <!-- 移动端遮罩 -->
    <div v-if="mobileOpen" class="member-backdrop" @click="$emit('close')"></div>

    <div class="member-content">
      <!-- 头部 -->
      <div class="member-header">
        <div class="member-group-info">
          <img v-if="resolveUrl(group?.avatar)" :src="resolveUrl(group?.avatar) as string" class="member-group-avatar" :alt="group?.name" />
          <div v-else class="member-group-avatar-placeholder">{{ group?.name?.charAt(0) }}</div>
          <div class="member-group-text">
            <h4 class="member-group-name">{{ group?.name }}</h4>
            <p class="member-group-meta">
              <span v-if="group?.groupNumber" class="member-group-num">群号 {{ group.groupNumber }}</span>
              <span>{{ group?.memberCount }}人</span>
            </p>
          </div>
        </div>
        <button class="member-close-btn" @click="$emit('close')" aria-label="关闭">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 简介 -->
      <div v-if="group?.description" class="member-desc">
        <p>{{ group.description }}</p>
      </div>

      <!-- Tab 切换（仅群主/管理员可见申请Tab） -->
      <div v-if="canManageRequests" class="member-tabs">
        <button
          class="member-tab"
          :class="{ active: activeTab === 'members' }"
          @click="activeTab = 'members'"
        >
          成员
          <span class="member-tab-count">{{ allMembers.length }}</span>
        </button>
        <button
          class="member-tab"
          :class="{ active: activeTab === 'requests' }"
          @click="switchToRequests()"
        >
          入群申请
          <span v-if="pendingRequests.length > 0" class="member-tab-badge">{{ pendingRequests.length }}</span>
        </button>
      </div>

      <!-- 成员列表 -->
      <div v-if="activeTab === 'members'" class="member-list">
        <!-- 群主 -->
        <div v-if="owners.length > 0" class="member-section">
          <div class="member-section-title">群主 ({{ owners.length }})</div>
          <div v-for="m in owners" :key="m.id" class="member-item" @click="navigateToUser(m.userId)">
            <img :src="m.userAvatar || defaultAvatar" class="member-avatar member-avatar-clickable" :alt="m.userName" />
            <div class="member-info">
              <span class="member-name">{{ m.userName || m.nickname || '未知' }}</span>
              <span class="member-role-tag owner">群主</span>
            </div>
          </div>
        </div>

        <!-- 管理员 -->
        <div v-if="admins.length > 0" class="member-section">
          <div class="member-section-title">管理员 ({{ admins.length }})</div>
          <div v-for="m in admins" :key="m.id" class="member-item" @click="navigateToUser(m.userId)">
            <img :src="m.userAvatar || defaultAvatar" class="member-avatar member-avatar-clickable" :alt="m.userName" />
            <div class="member-info">
              <span class="member-name">{{ m.userName || m.nickname || '未知' }}</span>
              <span class="member-role-tag admin">管理员</span>
            </div>
            <button
              v-if="isOwner && m.userId !== currentUserId"
              class="member-kick-btn"
              :disabled="kickingSet.has(m.userId)"
              @click.stop="handleKick(m)"
              :title="'移出群组'"
            >
              <svg v-if="kickingSet.has(m.userId)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 成员 -->
        <div v-if="regularMembers.length > 0" class="member-section">
          <div class="member-section-title">成员 ({{ regularMembers.length }})</div>
          <div v-for="m in regularMembers" :key="m.id" class="member-item" @click="navigateToUser(m.userId)">
            <img :src="m.userAvatar || defaultAvatar" class="member-avatar member-avatar-clickable" :alt="m.userName" />
            <div class="member-info">
              <span class="member-name">{{ m.userName || m.nickname || '未知' }}</span>
            </div>
            <button
              v-if="isOwner && m.userId !== currentUserId"
              class="member-kick-btn"
              :disabled="kickingSet.has(m.userId)"
              @click.stop="handleKick(m)"
              :title="'移出群组'"
            >
              <svg v-if="kickingSet.has(m.userId)" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 加载/空 -->
        <div v-if="loading" class="member-loading">加载中...</div>
        <div v-else-if="owners.length === 0 && admins.length === 0 && regularMembers.length === 0" class="member-empty">
          暂无成员信息
        </div>
      </div>

      <!-- 入群申请列表 -->
      <div v-if="activeTab === 'requests'" class="member-list">
        <div v-if="requestsLoading" class="member-loading">加载中...</div>
        <div v-else-if="pendingRequests.length === 0" class="member-empty">
          <svg class="w-10 h-10 mx-auto mb-2 text-[var(--zh-text-tertiary)] opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          暂无待审批申请
        </div>
        <div v-else class="request-list">
          <div v-for="req in pendingRequests" :key="req.id" class="request-item">
            <div class="request-user">
              <img :src="req.userAvatar || defaultAvatar" class="request-avatar" :alt="req.userName" />
              <div class="request-info">
                <span class="request-name">{{ req.userName || '未知用户' }}</span>
                <span class="request-time">{{ formatTime(req.createdAt) }}</span>
              </div>
            </div>
            <p v-if="req.message" class="request-message">{{ req.message }}</p>
            <div class="request-actions">
              <button
                class="request-btn approve"
                :disabled="processingSet.has(req.id)"
                @click="handleApprove(req.id)"
              >
                <svg v-if="processingSet.has(req.id) && processingAction.get(req.id) === 'approve'" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                同意
              </button>
              <button
                class="request-btn reject"
                :disabled="processingSet.has(req.id)"
                @click="handleReject(req.id)"
              >
                <svg v-if="processingSet.has(req.id) && processingAction.get(req.id) === 'reject'" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
                拒绝
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="member-actions">
        <button class="member-action-btn leave" @click="$emit('leave')">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          退出群组
        </button>
        <button v-if="isOwner" class="member-action-btn dismiss" @click="$emit('dismiss')">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          解散群组
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMember, GroupJoinRequestInfo } from '@/api/group'
import { showToast } from '@/composables/useToast'
import { useResourceUrl } from '@/composables/useResourceUrl'

const { resolveUrl } = useResourceUrl()

const props = defineProps<{
  group: GroupInfo | null
  members: GroupMember[]
  loading?: boolean
  currentUserId?: number
  mobileOpen?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'leave'): void
  (e: 'dismiss'): void
  (e: 'member-changed'): void
}>()

const router = useRouter()
const route = useRoute()

/** 点击成员头像/条目跳转用户主页（携带来源页面信息，确保返回按钮能回到群组页） */
const navigateToUser = (userId: number) => {
  router.push({ path: `/user/${userId}`, state: { from: route.fullPath } })
}

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzNiIgaGVpZ2h0PSIzNiIgdmlld0JveD0iMCAwIDM2IDM2Ij48cmVjdCB3aWR0aD0iMzYiIGhlaWdodD0iMzYiIGZpbGw9IiNlMmU4ZjAiIHJ4PSIxOCIvPjx0ZXh0IHg9IjE4IiB5PSIyMyIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCIgZm9udC1zaXplPSIxNCI+PzwvdGV4dD48L3N2Zz4='

const activeTab = ref<'members' | 'requests'>('members')
const pendingRequests = ref<GroupJoinRequestInfo[]>([])
const requestsLoading = ref(false)
const processingSet = ref<Set<number>>(new Set())
const processingAction = ref<Map<number, string>>(new Map())
const kickingSet = ref<Set<number>>(new Set())

const owners = computed(() => props.members.filter(m => m.role === 2))
const admins = computed(() => props.members.filter(m => m.role === 1))
const regularMembers = computed(() => props.members.filter(m => m.role === 0))
const allMembers = computed(() => props.members)
const isOwner = computed(() => props.group?.ownerId === props.currentUserId)
const isAdmin = computed(() => {
  const me = props.members.find(m => m.userId === props.currentUserId)
  return me && me.role >= 1
})
const canManageRequests = computed(() => isOwner.value || isAdmin.value)

async function loadPendingRequests() {
  if (!props.group?.id || !canManageRequests.value) return
  requestsLoading.value = true
  try {
    const { data } = await groupApi.getPendingRequests(props.group.id)
    pendingRequests.value = data?.data || []
  } catch {
    pendingRequests.value = []
  } finally {
    requestsLoading.value = false
  }
}

function switchToRequests() {
  activeTab.value = 'requests'
  loadPendingRequests()
}

async function handleApprove(requestId: number) {
  if (processingSet.value.has(requestId)) return
  processingSet.value.add(requestId)
  processingAction.value.set(requestId, 'approve')
  try {
    await groupApi.approveJoinRequest(requestId)
    pendingRequests.value = pendingRequests.value.filter(r => r.id !== requestId)
    showToast('已同意入群申请', 'success', { position: 'top-center' })
    emit('member-changed')
  } catch (e: any) {
    const msg = e?.message || '操作失败'
    showToast(msg, 'error', { position: 'top-center' })
  } finally {
    processingSet.value.delete(requestId)
    processingAction.value.delete(requestId)
  }
}

async function handleReject(requestId: number) {
  if (processingSet.value.has(requestId)) return
  processingSet.value.add(requestId)
  processingAction.value.set(requestId, 'reject')
  try {
    await groupApi.rejectJoinRequest(requestId)
    pendingRequests.value = pendingRequests.value.filter(r => r.id !== requestId)
    showToast('已拒绝入群申请', 'success', { position: 'top-center' })
  } catch (e: any) {
    const msg = e?.message || '操作失败'
    showToast(msg, 'error', { position: 'top-center' })
  } finally {
    processingSet.value.delete(requestId)
    processingAction.value.delete(requestId)
  }
}

async function handleKick(member: GroupMember) {
  if (!props.group?.id || kickingSet.value.has(member.userId)) return
  const name = member.userName || member.nickname || '该成员'
  if (!confirm(`确定要将「${name}」移出群组吗？`)) return
  kickingSet.value.add(member.userId)
  try {
    await groupApi.kickMember(props.group.id, member.userId)
    showToast(`已将「${name}」移出群组`, 'success', { position: 'top-center' })
    emit('member-changed')
  } catch (e: any) {
    const msg = e?.message || '操作失败'
    showToast(msg, 'error', { position: 'top-center' })
  } finally {
    kickingSet.value.delete(member.userId)
  }
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// 当群组变化时重置状态
watch(() => props.group?.id, () => {
  activeTab.value = 'members'
  pendingRequests.value = []
})
</script>

<style scoped>
.member-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--zh-bg, #f8fafc);
  border-left: 1px solid var(--zh-border, #e5e7eb);
}

.member-backdrop {
  display: none;
}

.member-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 260px;
  overflow: hidden;
}

.member-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 14px 10px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
}
.member-group-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.member-group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}
.member-group-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.member-group-text { min-width: 0; }
.member-group-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.member-group-meta {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  display: flex;
  gap: 6px;
  margin-top: 1px;
}
.member-group-num { font-family: monospace; }

.member-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  flex-shrink: 0;
}
.member-close-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}

.member-desc {
  padding: 8px 14px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
}
.member-desc p {
  font-size: 12px;
  color: var(--zh-text-secondary, #64748b);
  line-height: 1.5;
}

.member-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
  -webkit-overflow-scrolling: touch;
}

.member-section { padding: 0 8px; margin-bottom: 8px; }
.member-section-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--zh-text-tertiary, #94a3b8);
  padding: 6px 8px 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background 0.15s;
  cursor: pointer;
}
.member-item:hover { background: var(--zh-bg-hover, #f1f5f9); }

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.member-avatar-clickable {
  transition: opacity 0.15s ease;
}
.member-avatar-clickable:hover {
  opacity: 0.8;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.member-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.member-role-tag {
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 600;
  flex-shrink: 0;
}
.member-role-tag.owner {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}
.member-role-tag.admin {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.member-kick-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: all 0.2s;
}
.member-item:hover .member-kick-btn {
  opacity: 1;
}
.member-kick-btn:hover:not(:disabled) {
  background: #fee2e2;
  color: #dc2626;
}
.member-kick-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* 移动端始终显示踢人按钮 */
@media (max-width: 768px) {
  .member-kick-btn {
    opacity: 0.7;
  }
}

.member-loading, .member-empty {
  text-align: center;
  padding: 24px 16px;
  font-size: 12px;
  color: var(--zh-text-tertiary, #94a3b8);
}

.member-actions {
  padding: 10px 12px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--zh-border, #e5e7eb);
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

/* 移动端底部留出tabbar空间 */
@media (max-width: 768px) {
  .member-actions {
    padding-bottom: 70px;
  }
}
.member-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px;
  border: none;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.member-action-btn.leave {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}
.member-action-btn.leave:hover {
  background: #fee2e2;
  color: #dc2626;
}
.member-action-btn.dismiss {
  background: transparent;
  color: #dc2626;
  border: 1px solid #fecaca;
}
.member-action-btn.dismiss:hover {
  background: #fee2e2;
}

/* ===== Tab 切换 ===== */
.member-tabs {
  display: flex;
  gap: 0;
  padding: 0 14px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}
.member-tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--zh-text-tertiary, #94a3b8);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}
.member-tab:hover {
  color: var(--zh-text-secondary, #64748b);
}
.member-tab.active {
  color: var(--zh-primary, #6366f1);
}
.member-tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  height: 2px;
  background: var(--zh-primary, #6366f1);
  border-radius: 1px;
}
.member-tab-count {
  font-size: 10px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-weight: 500;
}
.member-tab.active .member-tab-count {
  color: var(--zh-primary, #6366f1);
}
.member-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

/* ===== 入群申请列表 ===== */
.request-list {
  padding: 8px;
}
.request-item {
  padding: 10px;
  border-radius: 10px;
  background: var(--zh-bg-elevated, #fff);
  border: 1px solid var(--zh-border, #e5e7eb);
  margin-bottom: 8px;
}
.request-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.request-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.request-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.request-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.request-time {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
}
.request-message {
  font-size: 12px;
  color: var(--zh-text-secondary, #64748b);
  padding: 6px 8px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 6px;
  margin-bottom: 8px;
  line-height: 1.4;
  word-break: break-all;
}
.request-actions {
  display: flex;
  gap: 6px;
}
.request-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex: 1;
  padding: 6px 8px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.request-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.request-btn.approve {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}
.request-btn.approve:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.2);
}
.request-btn.reject {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
}
.request-btn.reject:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.15);
}

/* ===== 移动端：右侧滑入抽屉 ===== */
@media (max-width: 768px) {
  .member-panel {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 50;
    border-left: none;
    pointer-events: none;
  }
  .member-panel.mobile-open {
    pointer-events: auto;
  }
  .member-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 1;
  }
  .member-content {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    width: 280px;
    z-index: 2;
    background: var(--zh-bg, #f8fafc);
    transform: translateX(100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
  }
  .member-panel.mobile-open .member-content {
    transform: translateX(0);
  }
}
</style>
