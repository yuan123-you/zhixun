<template>
  <!-- 我的关注列表 - 优化版 -->
  <div class="following-page animate-fade-in-up">
    <!-- 页面头部 - 渐变背景 -->
    <div class="page-header">
      <div class="header-content">
        <div class="flex items-center gap-3">
          <div class="header-icon">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div>
            <h1 class="header-title">我的关注</h1>
            <p class="header-subtitle">你关注的创作者 · 共 {{ totalCount }} 人</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="page-content">
      <!-- 错误状态 -->
      <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchFollowing" />

      <!-- 骨架屏 -->
      <div v-if="loading && !users.length" class="skeleton-list">
        <div v-for="i in 5" :key="i" class="skeleton-card">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-info">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line skeleton-line--text"></div>
          </div>
          <div class="skeleton-btn"></div>
        </div>
      </div>

      <!-- 关注列表 -->
      <div v-else class="users-list">
        <div
          v-for="(user, index) in users"
          :key="user.id"
          class="user-card stagger-item"
          :style="{ animationDelay: `${index * 50}ms` }"
          @click="navigateTo(`/user/${user.id}`)"
        >
          <!-- 左侧渐变装饰条 -->
          <div class="card-accent"></div>

          <div class="card-body">
            <div class="user-main">
              <div class="avatar-wrapper">
                <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
                <div class="online-dot" v-if="false"></div>
              </div>
              <div class="user-info">
                <p class="user-name">{{ user.nickname }}</p>
                <p class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</p>
              </div>
            </div>
            <button
              class="follow-btn follow-btn--following"
              @click.stop="handleToggleFollow(user)"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
              </svg>
              <span>已关注</span>
            </button>
          </div>
        </div>

        <EmptyState v-if="!loading && users.length === 0" title="暂无关注" description="去发现更多有趣的人吧">
          <template #action>
            <RouterLink to="/discover" class="btn-primary text-sm inline-flex items-center gap-1.5">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              去发现
            </RouterLink>
          </template>
        </EmptyState>
      </div>

      <!-- 无限滚动 -->
      <div ref="sentinelRef" class="h-px"></div>
      <div v-if="loading && users.length > 0" class="load-more">
        <span class="loading-spinner"></span>
        <span>加载中...</span>
      </div>
      <div v-if="!hasMore && users.length > 0" class="load-more load-more--end">
        <span class="end-line"></span>
        <span class="end-text">没有更多了</span>
        <span class="end-line"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 关注列表 - 现代化设计 */
import type { User } from '@/types'
import type { FollowUser } from '@/api/social'
import { navigateTo } from '@/composables/navigateTo'
import { showToast } from '@/composables/useToast'

const { setTitle } = usePageHeaderTitle()
setTitle('我的关注')

const userStore = useUserStore()

const users = ref<FollowUser[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const totalCount = ref(0)
const pageSize = 20

const fetchFollowing = async () => {
  const userId = userStore.userInfo?.id
  if (!userId) {
    error.value = '请先登录'
    return
  }
  if (loading.value) return
  loading.value = true
  error.value = null
  try {
    const { data } = await useApi().get<any>(`/users/${userStore.userInfo!.id}/following`, {
      page: page.value,
      pageSize,
    })
    const list = data.data?.list || data.data || []
    const total = data.data?.total || list.length
    if (page.value === 1) {
      users.value = list
    } else {
      users.value.push(...list)
    }
    totalCount.value = total
    hasMore.value = users.value.length < total
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '加载失败，请稍后重试'
    error.value = msg
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  page.value++
  await fetchFollowing()
  if (error.value) page.value--
}

useScrollSentinel({ onLoadMore: loadMore, hasMore, loading })

const handleToggleFollow = async (targetUser: FollowUser) => {
  try {
    await useApi().post(`/users/${targetUser.id}/follow`)
    users.value = users.value.filter(u => u.id !== targetUser.id)
    totalCount.value = Math.max(0, totalCount.value - 1)
    userStore.updateProfile({ followCount: Math.max(0, (userStore.userInfo?.followCount || 0) - 1) })
    showToast('已取消关注')
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '操作失败，请重试'
    showToast(msg, 'error')
  }
}

onMounted(() => {
  fetchFollowing()
})

useHead({
  title: () => '我的关注 - 知讯',
})
</script>

<style scoped>
.following-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px 24px;
}

/* 页面头部 */
.page-header {
  position: relative;
  margin: 0 -16px 20px;
  padding: 24px 16px;
  background: linear-gradient(135deg, #eef2ff 0%, #f0fdfa 50%, #faf5ff 100%);
  border-bottom: 1px solid var(--zh-border);
}
.dark .page-header {
  background: linear-gradient(135deg, #1e1b4b 0%, #042f2e 50%, #1a1033 100%);
}

.header-content {
  max-width: 800px;
  margin: 0 auto;
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--zh-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--zh-text);
  letter-spacing: -0.02em;
}

.header-subtitle {
  font-size: 13px;
  color: var(--zh-text-secondary);
  margin-top: 2px;
}

.page-content {
  padding: 0;
}

/* 骨架屏 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--zh-bg-elevated);
  border-radius: var(--zh-radius-lg);
  border: 1px solid var(--zh-border-light);
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--zh-bg-hover);
  flex-shrink: 0;
}

.skeleton-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 50%, var(--zh-bg-hover) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

.skeleton-line--title { width: 100px; }
.skeleton-line--text { width: 200px; height: 12px; }

.skeleton-btn {
  width: 72px;
  height: 32px;
  border-radius: 20px;
  background: var(--zh-bg-hover);
  flex-shrink: 0;
}

/* 用户列表 */
.users-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 用户卡片 */
.user-card {
  position: relative;
  background: var(--zh-bg-elevated);
  border-radius: var(--zh-radius-lg);
  border: 1px solid var(--zh-border);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--zh-transition-base);
  box-shadow: var(--zh-shadow-xs);
}

.user-card:hover {
  border-color: var(--zh-primary-light);
  box-shadow: var(--zh-shadow-card);
  transform: translateY(-1px);
}

.user-card:active {
  transform: scale(0.985);
  transition: transform 0.1s ease;
}

.card-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--zh-primary) 0%, var(--zh-primary-light) 100%);
  opacity: 0;
  transition: opacity var(--zh-transition-base);
}

.user-card:hover .card-accent {
  opacity: 1;
}

.card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 12px;
}

.user-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #10b981;
  border: 2px solid var(--zh-bg-elevated);
  border-radius: 50%;
}

.user-info {
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--zh-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.01em;
}

.user-bio {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

/* 关注按钮 */
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;
  border: none;
  cursor: pointer;
  transition: all var(--zh-transition-fast);
  flex-shrink: 0;
  white-space: nowrap;
}

.follow-btn--following {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  border: 1px solid var(--zh-border);
}

.follow-btn--following:hover {
  background: #fee2e2;
  color: #ef4444;
  border-color: #fecaca;
}

.follow-btn--following:hover svg {
  display: none;
}

.follow-btn--following:hover span::after {
  content: '取消关注';
}

.follow-btn--following span {
  display: inline;
}

.follow-btn--following:hover span {
  display: none;
}

.follow-btn:active {
  transform: scale(0.95);
}

/* 加载更多 */
.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  font-size: 13px;
  color: var(--zh-text-tertiary);
}

.load-more--end {
  gap: 12px;
}

.end-line {
  width: 40px;
  height: 1px;
  background: var(--zh-border);
}

.end-text {
  flex-shrink: 0;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 767.98px) {
  .following-page {
    padding: 0 10px 16px;
  }
  .page-header {
    margin: 0 -10px 16px;
    padding: 18px 10px;
  }
  .header-title {
    font-size: 18px;
  }
  .card-body {
    padding: 12px;
  }
  .user-name {
    font-size: 14px;
  }
}
</style>