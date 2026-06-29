<template>
  <!-- Ta的粉丝列表 -->
  <div class="followers-page animate-fade-in-up">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="flex items-center gap-3">
          <div class="header-icon">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          </div>
          <div>
            <h1 class="header-title">{{ isOwn ? '我的粉丝' : `${userName || 'Ta'}的粉丝` }}</h1>
            <p class="header-subtitle">{{ isOwn ? '关注你的人' : `关注${userName || 'Ta'}的人` }} · 共 {{ totalCount }} 人</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="page-content">
      <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchList" />

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

      <!-- 粉丝列表 -->
      <div v-else class="users-list">
        <div
          v-for="(user, index) in users"
          :key="user.id"
          class="user-card stagger-item"
          :style="{ animationDelay: `${index * 50}ms` }"
          @click="navigateTo(`/user/${user.id}`)"
        >
          <div class="card-accent" :class="{ 'card-accent--mutual': user.isMutualFollow }"></div>

          <div class="card-body">
            <div class="user-main">
              <div class="avatar-wrapper">
                <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
              </div>
              <div class="user-info">
                <div class="flex items-center gap-1.5">
                  <p class="user-name">{{ user.nickname }}</p>
                  <span v-if="user.isMutualFollow" class="mutual-badge">
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
                    </svg>
                    互关
                  </span>
                </div>
                <p class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</p>
              </div>
            </div>
            <button
              v-if="!isOwn && user.id !== userStore.userInfo?.id"
              class="follow-btn"
              :class="user.isFollowing ? 'follow-btn--following' : 'follow-btn--primary'"
              :disabled="loadingIds[user.id]"
              @click.stop="toggleFollowUser(user)"
            >
              <template v-if="user.isFollowing">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
                </svg>
                <span>已关注</span>
              </template>
              <template v-else>
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                <span>关注</span>
              </template>
            </button>
          </div>
        </div>

        <EmptyState v-if="!loading && users.length === 0" :title="isOwn ? '暂无粉丝' : '暂无粉丝'" :description="isOwn ? '发布优质内容可以吸引更多粉丝' : 'Ta还没有粉丝'">
          <template v-if="isOwn" #action>
            <RouterLink to="/editor" class="btn-primary text-sm inline-flex items-center gap-1.5">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              去创作
            </RouterLink>
          </template>
        </EmptyState>
      </div>

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
import { navigateTo } from '@/composables/navigateTo'
const route = useRoute(); const userStore = useUserStore()
const targetId = computed(() => Number(route.params.id))
const isOwn = computed(() => userStore.userInfo?.id === targetId.value)
const userName = ref('')
const { setTitle } = usePageHeaderTitle()
watchEffect(() => { setTitle(isOwn.value ? '我的粉丝' : `${userName.value || 'Ta'}的粉丝`) })

interface FollowItem { id: number; nickname: string; avatar: string; bio?: string; isFollowing?: boolean; isMutualFollow?: boolean }
const users = ref<FollowItem[]>([]); const loading = ref(false); const hasMore = ref(true)
const error = ref<string | null>(null); const page = ref(1); const totalCount = ref(0); const loadingIds = ref<Record<number, boolean>>({})

const fetchList = async () => {
  if (loading.value) return
  loading.value = true; error.value = null
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.getFollowers(targetId.value, { page: page.value, pageSize: 20 })
    const list = data.data?.list || []
    if (page.value === 1) users.value = list; else users.value.push(...list)
    totalCount.value = data.data?.total || list.length
    hasMore.value = users.value.length < totalCount.value
    // 首次加载时并行获取目标用户资料（用于显示正确的用户名）
    if (page.value === 1 && !userName.value) {
      import('@/api').then(({ userApi }) =>
        userApi.getProfile(targetId.value).then((res: any) => {
          if (res.data?.data?.nickname) userName.value = res.data.data.nickname
        }).catch(() => {})
      )
    }
  } catch { error.value = '加载失败，请稍后重试' }
  finally { loading.value = false }
}

const loadMore = async () => { if (loading.value || !hasMore.value) return; page.value++; await fetchList(); if (error.value) page.value-- }
useScrollSentinel({ onLoadMore: loadMore, hasMore, loading })

const toggleFollowUser = async (user: FollowItem) => {
  loadingIds.value[user.id] = true
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.toggleFollow(user.id)
    user.isFollowing = data.data.followed
    user.isMutualFollow = data.data.followed && user.isMutualFollow
  } finally { loadingIds.value[user.id] = false }
}

onMounted(() => fetchList())

// 核心修复：动态路由 targetId 变化时（切换用户）重置并重新加载
watch(() => targetId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  users.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  totalCount.value = 0
  userName.value = ''  // 重置用户名，让 fetchList 重新获取目标用户资料
  await fetchList()
})

useHead({ title: () => `${isOwn.value ? '我' : userName.value || 'Ta'}的粉丝 - 知讯` })
</script>

<style scoped>
.followers-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px 24px;
}
.page-header {
  position: relative;
  margin: 0 -16px 20px;
  padding: 24px 16px;
  background: linear-gradient(135deg, #fef2f2 0%, #f0fdfa 50%, #eef2ff 100%);
  border-bottom: 1px solid var(--zh-border);
}
.dark .page-header {
  background: linear-gradient(135deg, #1a1033 0%, #042f2e 50%, #1e1b4b 100%);
}
.header-content { max-width: 800px; margin: 0 auto; }
.header-icon {
  width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, #f43f5e, #ec4899);
  color: white; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);
}
.header-title { font-size: 20px; font-weight: 700; color: var(--zh-text); letter-spacing: -0.02em; }
.header-subtitle { font-size: 13px; color: var(--zh-text-secondary); margin-top: 2px; }
.page-content { padding: 0; }
.skeleton-list { display: flex; flex-direction: column; gap: 10px; }
.skeleton-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: var(--zh-bg-elevated); border-radius: var(--zh-radius-lg); border: 1px solid var(--zh-border-light); }
.skeleton-avatar { width: 48px; height: 48px; border-radius: 50%; background: var(--zh-bg-hover); flex-shrink: 0; }
.skeleton-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.skeleton-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 50%, var(--zh-bg-hover) 75%); background-size: 200% 100%; animation: shimmer 1.8s ease-in-out infinite; }
.skeleton-line--title { width: 100px; }
.skeleton-line--text { width: 200px; height: 12px; }
.skeleton-btn { width: 72px; height: 32px; border-radius: 20px; background: var(--zh-bg-hover); flex-shrink: 0; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.users-list { display: flex; flex-direction: column; gap: 10px; }
.user-card { position: relative; background: var(--zh-bg-elevated); border-radius: var(--zh-radius-lg); border: 1px solid var(--zh-border); overflow: hidden; cursor: pointer; transition: all var(--zh-transition-base); box-shadow: var(--zh-shadow-xs); }
.user-card:hover { border-color: var(--zh-primary-light); box-shadow: var(--zh-shadow-card); transform: translateY(-1px); }
.user-card:active { transform: scale(0.985); transition: transform 0.1s ease; }
.card-accent { position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: linear-gradient(180deg, #f43f5e 0%, #ec4899 100%); opacity: 0; transition: opacity var(--zh-transition-base); }
.card-accent--mutual { background: linear-gradient(180deg, #10b981 0%, #34d399 100%); }
.user-card:hover .card-accent { opacity: 1; }
.card-body { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; gap: 12px; }
.user-main { display: flex; align-items: center; gap: 12px; min-width: 0; flex: 1; }
.avatar-wrapper { position: relative; flex-shrink: 0; }
.user-info { min-width: 0; }
.user-name { font-size: 15px; font-weight: 600; color: var(--zh-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; letter-spacing: -0.01em; }
.user-bio { font-size: 12px; color: var(--zh-text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.mutual-badge { display: inline-flex; align-items: center; gap: 2px; padding: 1px 6px; font-size: 10px; font-weight: 600; color: #059669; background: #ecfdf5; border-radius: 4px; white-space: nowrap; }
.dark .mutual-badge { background: #064e3b; color: #34d399; }
.follow-btn { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; font-size: 12px; font-weight: 500; border-radius: 20px; border: none; cursor: pointer; transition: all var(--zh-transition-fast); flex-shrink: 0; white-space: nowrap; }
.follow-btn--primary { background: var(--zh-primary); color: white; box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25); }
.follow-btn--primary:hover { background: var(--zh-primary-dark); box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35); }
.follow-btn--following { background: var(--zh-bg-hover); color: var(--zh-text-secondary); border: 1px solid var(--zh-border); }
.follow-btn--following:hover { background: #fee2e2; color: #ef4444; border-color: #fecaca; }
.follow-btn--following:hover svg { display: none; }
.follow-btn--following:hover span::after { content: '取消关注'; }
.follow-btn--following span { display: inline; }
.follow-btn--following:hover span { display: none; }
.follow-btn:active { transform: scale(0.95); }
.stagger-item { opacity: 0; animation: stagger-in 0.45s cubic-bezier(0.4, 0, 0.2, 1) forwards; }
@keyframes stagger-in { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
.load-more { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 20px 0; font-size: 13px; color: var(--zh-text-tertiary); }
.load-more--end { gap: 12px; }
.end-line { width: 40px; height: 1px; background: var(--zh-border); }
.end-text { flex-shrink: 0; }
.loading-spinner { width: 16px; height: 16px; border: 2px solid var(--zh-border); border-top-color: var(--zh-primary); border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 767.98px) {
  .followers-page { padding: 0 10px 16px; }
  .page-header { margin: 0 -10px 16px; padding: 18px 10px; }
  .header-title { font-size: 18px; }
  .card-body { padding: 12px; }
  .user-name { font-size: 14px; }
}
</style>
