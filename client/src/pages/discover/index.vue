<template>
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5 animate-fade-in-up">

    <!-- ===== 英雄横幅 ===== -->
    <section class="discover-hero mb-2">
      <div class="discover-hero-bg"></div>
      <div class="discover-hero-glow"></div>
      <div class="discover-hero-content">
        <div class="discover-hero-icon-wrap">
          <svg class="discover-hero-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
            <path d="M11 8v6" />
            <path d="M8 11h6" />
          </svg>
        </div>
        <div>
          <h1 class="discover-hero-title">{{ '发现' }}</h1>
          <p class="discover-hero-subtitle">{{ '探索热门内容、标签和创作者，发现更多精彩' }}</p>
        </div>
      </div>
    </section>

    <!-- ===== 统计栏 ===== -->
    <div class="discover-stats mb-2">
      <div class="discover-stat-item">
        <span class="discover-stat-value">{{ hotRankItems.length }}</span>
        <span class="discover-stat-label">{{ '热榜文章' }}</span>
      </div>
      <div class="discover-stat-divider"></div>
      <div class="discover-stat-item">
        <span class="discover-stat-value">{{ hotTags.length }}</span>
        <span class="discover-stat-label">{{ '热门标签' }}</span>
      </div>
      <div class="discover-stat-divider"></div>
      <div class="discover-stat-item">
        <span class="discover-stat-value">{{ recommendUsers.length }}</span>
        <span class="discover-stat-label">{{ '推荐用户' }}</span>
      </div>
    </div>

    <PullToRefresh :on-refresh="() => refreshAll()" :error="usersError || rankError || tagsError">
      <!-- 热榜 -->
      <section class="discover-section mb-2">
        <div class="discover-section-header">
          <div class="flex items-center gap-2">
            <div class="section-icon" style="background: rgba(245, 158, 11, 0.12); color: #f59e0b;">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5C7 4 7 8 8 8c1 0 1-3 3-3a2.5 2.5 0 0 1 0 5H9" />
                <path d="M6 9v12" />
                <path d="M9 9v12" />
                <path d="M16 4l2 4-2 4" />
                <path d="M14 7h5" />
              </svg>
            </div>
            <h2 class="text-lg font-semibold text-[var(--zh-text)]">{{ '热榜' }}</h2>
            <span v-if="rankUpdateTime" class="text-xs text-[var(--zh-text-tertiary)]">{{ rankUpdateTime }}</span>
          </div>
          <RouterLink to="/rank" class="discover-view-all">
            {{ '查看全部' }}
            <svg class="discover-arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6" />
            </svg>
          </RouterLink>
        </div>

        <div v-if="rankLoading" class="space-y-1.5">
          <div v-for="i in 5" :key="i" class="discover-skeleton-card">
            <div class="discover-skeleton-rank"></div>
            <div class="flex-1">
              <div class="discover-skeleton-line w-3/4"></div>
              <div class="discover-skeleton-line w-1/3 mt-1.5"></div>
            </div>
          </div>
        </div>

        <ErrorRetry v-else-if="rankError" :message="rankError" :on-retry="fetchHotRank" />

        <div v-else-if="hotRankItems.length > 0" class="space-y-1">
          <div
            v-for="(item, index) in hotRankItems"
            :key="item.id"
            class="discover-rank-card stagger-item"
            :class="getRankCardClass(index)"
            @click="goToArticle(item.id)"
          >
            <div class="discover-rank-badge" :class="getRankBadgeClass(index)">
              <span v-if="index === 0" class="rank-medal">🥇</span>
              <span v-else-if="index === 1" class="rank-medal">🥈</span>
              <span v-else-if="index === 2" class="rank-medal">🥉</span>
              <span v-else class="rank-num">{{ index + 1 }}</span>
            </div>
            <div class="flex-1 min-w-0 overflow-hidden">
              <h3 class="text-sm font-medium text-[var(--zh-text)] truncate">{{ item.title }}</h3>
              <div class="flex items-center space-x-2 mt-0.5 text-xs text-[var(--zh-text-tertiary)]">
                <span v-if="item.authorNickname" class="truncate max-w-[100px]">{{ item.authorNickname }}</span>
                <span v-if="item.authorNickname && item.score != null">·</span>
                <span v-if="item.score != null" class="shrink-0 flex items-center gap-0.5">
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor" style="color: #f59e0b;">
                    <path d="M12 23c-4.5 0-8-3.5-8-8 0-3.5 2-6.5 5-8.5C8 9 9.5 11 12 11s4-2 3-4.5c3 2 5 5 5 8.5 0 4.5-3.5 8-8 8z"/>
                  </svg>
                  {{ formatHeat(item.score) }} {{ '热度' }}
                </span>
              </div>
            </div>
            <span class="discover-rank-heat" :class="index < 3 ? 'discover-rank-heat--top' : ''">
              <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor" style="color: #f59e0b;">
                <path d="M12 23c-4.5 0-8-3.5-8-8 0-3.5 2-6.5 5-8.5C8 9 9.5 11 12 11s4-2 3-4.5c3 2 5 5 5 8.5 0 4.5-3.5 8-8 8z"/>
              </svg>
              {{ formatHeat(item.score ?? 0) }}
            </span>
          </div>
        </div>

        <EmptyState v-else :title="'暂无热榜数据'" />
      </section>

      <!-- 分隔线 -->
      <div class="discover-divider"></div>

      <!-- 热门标签 -->
      <section class="discover-section mb-2">
        <div class="discover-section-header">
          <div class="flex items-center gap-2">
            <div class="section-icon" style="background: rgba(59, 130, 246, 0.12); color: #3b82f6;">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
                <line x1="7" y1="7" x2="7.01" y2="7" />
              </svg>
            </div>
            <h2 class="text-lg font-semibold text-[var(--zh-text)]">{{ '热门标签' }}</h2>
            <span v-if="tagsUpdateTime" class="text-xs text-[var(--zh-text-tertiary)]">{{ tagsUpdateTime }}</span>
          </div>
          <RouterLink to="/tags" class="discover-view-all">
            {{ '查看全部' }}
            <svg class="discover-arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6" />
            </svg>
          </RouterLink>
        </div>

        <div v-if="tagsLoading" class="flex flex-wrap gap-1.5">
          <div v-for="i in 8" :key="i" class="discover-skeleton-tag"></div>
        </div>

        <ErrorRetry v-else-if="tagsError" :message="tagsError" :on-retry="fetchHotTags" />

        <div v-else-if="hotTags.length > 0" class="flex flex-wrap gap-1.5">
          <RouterLink
            v-for="tag in hotTags"
            :key="tag.id"
            to="/tags"
            class="discover-tag-pill stagger-item"
            :style="getTagStyle(tag.name)"
          >
            <span class="discover-tag-hash">#</span>
            {{ tag.name }}
          </RouterLink>
        </div>
      </section>

      <!-- 分隔线 -->
      <div class="discover-divider"></div>

      <!-- 推荐用户 -->
      <section class="discover-section mb-1.5">
        <div class="discover-section-header">
          <div class="flex items-center gap-2">
            <div class="section-icon" style="background: rgba(34, 197, 94, 0.12); color: #22c55e;">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <h2 class="text-lg font-semibold text-[var(--zh-text)]">{{ '推荐关注' }}</h2>
            <span v-if="usersUpdateTime" class="text-xs text-[var(--zh-text-tertiary)]">{{ usersUpdateTime }}</span>
          </div>
          <button
            class="discover-view-all discover-shuffle-btn"
            :class="{ 'discover-shuffle-btn--loading': usersLoading, 'discover-shuffle-btn--success': usersRefreshSuccess }"
            :disabled="usersLoading"
            :aria-label="usersLoading ? '换一批中' : '换一批推荐用户'"
            @click="handleShuffleClick"
          >
            <svg
              v-if="!usersRefreshSuccess"
              class="discover-shuffle-icon"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <svg
              v-else
              class="discover-shuffle-icon discover-success-icon"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
            </svg>
            <span>{{ usersLoading ? '换一批中' : (usersRefreshSuccess ? '已换一批' : '换一批') }}</span>
          </button>
        </div>

        <div v-if="usersLoading" class="space-y-2">
          <div v-for="i in 3" :key="i" class="discover-skeleton-user">
            <div class="discover-skeleton-avatar"></div>
            <div class="flex-1">
              <div class="discover-skeleton-line w-24 mb-2"></div>
              <div class="discover-skeleton-line w-32"></div>
            </div>
          </div>
        </div>

        <ErrorRetry v-else-if="usersError" :message="usersError" :on-retry="fetchRecommendUsers" />

        <div v-else-if="recommendUsers.length > 0" class="discover-user-grid">
          <div v-for="user in recommendUsers" :key="user.id" class="discover-user-card stagger-item">
            <UserCard :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
        </div>
      </section>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/** 发现页：热榜、热门标签、推荐用户 - 联网实时加载确保数据真实性 */
import type { RankItem, User, Tag, ApiResponse } from '@/types'
import { storage } from '@/utils/storage'

// 跳转到作品详情页（使用 router 实例而非 navigateTo 自动导入，避免模板事件中未定义）
const router = useRouter()
const goToArticle = (id: number | string) => router.push(`/articles/${id}`)

const userStore = useUserStore()
const hotRankItems = ref<RankItem[]>([])
const hotTags = ref<Tag[]>([])

const rankLoading = ref(false)
const tagsLoading = ref(false)

const rankError = ref('')
const tagsError = ref('')

const rankUpdateTime = ref('')
const tagsUpdateTime = ref('')
const usersUpdateTime = ref('')

// 请求缓存
const { cachedRequest, forceRequest, invalidate } = useRequestCache({ ttl: 5 * 60 * 1000 })

// 推荐用户"换一批"：使用 useShuffleRefresh 实现去重和洗牌
const {
  items: recommendUsers,
  loading: usersLoading,
  error: usersError,
  refresh: shuffleUsers,
} = useShuffleRefresh<User>({
  fetchFn: async (offset, limit) => {
    const api = useApi()
    const res: any = await api.get<User[]>('/users/recommend', {
      params: { limit, offset, _t: Date.now() },
    })
    return res.data?.data || []
  },
  limit: 5,
  getItemId: (user) => user.id,
  shuffle: true,
  debounceMs: 300,
})

// "换一批"成功反馈状态：成功时显示对勾，2s后自动恢复
const usersRefreshSuccess = ref(false)
let successTimer: ReturnType<typeof setTimeout> | null = null

// 监听推荐用户数据变化后更新时间，并在成功刷新后展示反馈
watch(recommendUsers, (newVal, oldVal) => {
  usersUpdateTime.value = formatTime()
  // 仅当数据真正变化（非空且不同）时显示成功反馈
  if (newVal && newVal.length > 0 && (!oldVal || oldVal.length === 0 || newVal[0]?.id !== oldVal[0]?.id)) {
    usersRefreshSuccess.value = true
    if (successTimer) clearTimeout(successTimer)
    successTimer = setTimeout(() => {
      usersRefreshSuccess.value = false
    }, 1800)
  }
})

// 监听错误并显示提示
watch(usersError, (val) => {
  if (val) {
    showToast(val || '换一批失败，请稍后重试', 'error', { duration: 2200 })
  }
})

// 暴露给模板：当换一批失败时点击按钮可以重试
const handleShuffleClick = async () => {
  if (usersLoading.value) return
  // 重置成功状态以便新的反馈
  usersRefreshSuccess.value = false
  if (successTimer) {
    clearTimeout(successTimer)
    successTimer = null
  }
  try {
    await shuffleUsers()
  } catch {
    // 错误已在 useShuffleRefresh 内部处理，这里只兜底
  }
}

// 全部刷新：使用 useRefresh 统一管理
const { refresh: refreshAll } = useRefresh({
  onRefresh: async () => {
    const forceFetchRank = async () => {
      rankLoading.value = true
      rankError.value = ''
      try {
        const url = '/rank/hot'
        const params = { period: 'daily' }
        const api = useApi()
        const res = await forceRequest<ApiResponse<RankItem[]>>(
          () => api.get<RankItem[]>(url, {
            params: { ...params, _t: Date.now() },
          }),
          url,
          params,
        )
        hotRankItems.value = (res as any)?.data?.data || (res as any)?.data || []
        rankUpdateTime.value = formatTime()
      } catch {
        rankError.value = '热榜加载失败，请稍后重试'
        hotRankItems.value = []
      } finally {
        rankLoading.value = false
      }
    }

    const forceFetchTags = async () => {
      tagsLoading.value = true
      tagsError.value = ''
      try {
        const url = '/tags/hot'
        const params = { limit: 20 }
        const api = useApi()
        const res = await forceRequest<ApiResponse<Tag[]>>(
          () => api.get<Tag[]>(url, {
            params: { ...params, _t: Date.now() },
          }),
          url,
          params,
        )
        hotTags.value = (res as any)?.data?.data || (res as any)?.data || []
        tagsUpdateTime.value = formatTime()
      } catch {
        tagsError.value = '热门标签加载失败，请稍后重试'
        hotTags.value = []
      } finally {
        tagsLoading.value = false
      }
    }

    const forceFetchUsers = async () => {
      await shuffleUsers()
      usersUpdateTime.value = formatTime()
    }

    await Promise.allSettled([
      forceFetchRank(),
      forceFetchTags(),
      forceFetchUsers(),
    ])
  },
  debounceMs: 500,
  showError: true,
  errorMessage: '刷新失败，请检查网络后重试',
})

// 自动刷新定时器
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
// 修复：自动刷新周期从 5 分钟延长到 15 分钟。
// 之前的 5 分钟过于频繁，加上 visibilitychange 触发 refreshAll()，
// 用户每次切回 tab 都会看到数据自动重载，体感"页面莫名其妙地自动刷新"。
const AUTO_REFRESH_INTERVAL = 15 * 60 * 1000 // 15分钟自动刷新

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'bg-yellow-400 text-white'
  if (index === 1) return 'bg-gray-300 text-white'
  if (index === 2) return 'bg-orange-400 text-white'
  return 'bg-[var(--zh-bg-hover)] text-[var(--zh-text-secondary)]'
}

// 获取排名卡片样式
const getRankCardClass = (index: number) => {
  if (index === 0) return 'discover-rank-card--gold'
  if (index === 1) return 'discover-rank-card--silver'
  if (index === 2) return 'discover-rank-card--bronze'
  return ''
}

// 获取排名徽章样式
const getRankBadgeClass = (index: number) => {
  if (index === 0) return 'discover-rank-badge--gold'
  if (index === 1) return 'discover-rank-badge--silver'
  if (index === 2) return 'discover-rank-badge--bronze'
  return ''
}

// 格式化热度值：>=1，最多1位小数
const formatHeat = (score: number) => {
  if (score == null || score < 1) return '1'
  const rounded = Math.round(score * 10) / 10
  if (rounded >= 10000) return `${(rounded / 10000).toFixed(1)}万`
  return rounded.toString()
}

// 标签颜色调色板
const TAG_COLORS = [
  { r: 59, g: 130, b: 246 },   // 蓝
  { r: 239, g: 68, b: 68 },     // 红
  { r: 34, g: 197, b: 94 },     // 绿
  { r: 168, g: 85, b: 247 },    // 紫
  { r: 245, g: 158, b: 11 },    // 橙
  { r: 236, g: 72, b: 153 },    // 粉
  { r: 20, g: 184, b: 166 },    // 青
  { r: 99, g: 102, b: 241 },    // 靛
]

// 基于标签名生成颜色
const getTagStyle = (name: string) => {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const c = TAG_COLORS[Math.abs(hash) % TAG_COLORS.length]
  return {
    '--tag-color': `${c.r}, ${c.g}, ${c.b}`,
  }
}

// 格式化更新时间
const formatTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}更新`
}

// 获取热榜数据 - 使用缓存避免重复请求
const fetchHotRank = async () => {
  rankLoading.value = true
  rankError.value = ''
  try {
    const url = '/rank/hot'
    const params = { period: 'daily' }
    const api = useApi()
    const res = await cachedRequest<ApiResponse<RankItem[]>>(
      () => api.get<RankItem[]>(url, { ...params, _t: Date.now() }),
      url,
      params,
    )
    hotRankItems.value = (res as any)?.data?.data || (res as any)?.data || []
    rankUpdateTime.value = formatTime()
  } catch {
    rankError.value = '热榜加载失败，请稍后重试'
    hotRankItems.value = []
  } finally {
    rankLoading.value = false
  }
}

// 获取热门标签 - 使用缓存避免重复请求
const fetchHotTags = async () => {
  tagsLoading.value = true
  tagsError.value = ''
  try {
    const url = '/tags/hot'
    const params = { limit: 20 }
    const api = useApi()
    const res = await cachedRequest<ApiResponse<Tag[]>>(
      () => api.get<Tag[]>(url, { ...params, _t: Date.now() }),
      url,
      params,
    )
    hotTags.value = (res as any)?.data?.data || (res as any)?.data || []
    tagsUpdateTime.value = formatTime()
  } catch {
    tagsError.value = '热门标签加载失败，请稍后重试'
    hotTags.value = []
  } finally {
    tagsLoading.value = false
  }
}

// 获取推荐用户 - 委托给 useShuffleRefresh
const fetchRecommendUsers = () => shuffleUsers()

// 关注/取关
const toggleFollow = async (userId: number) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(userId)
    const user = recommendUsers.value.find(u => u.id === userId)
    if (user) {
      user.isFollowing = !user.isFollowing
      user.followerCount += user.isFollowing ? 1 : -1
    }
  } catch {
    // 忽略错误
  }
}

// 启动自动刷新定时器
const startAutoRefresh = () => {
  stopAutoRefresh()
  autoRefreshTimer = setInterval(() => {
    fetchHotRank()
    fetchHotTags()
  }, AUTO_REFRESH_INTERVAL)
}

// 停止自动刷新定时器
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

// 页面可见性变化时：仅控制定时器启停，不再触发 refreshAll()。
// 修复：之前切回 tab 时会立即调用 refreshAll()，导致用户每次切回都看到数据自动重新加载，
// 体感"页面莫名其妙地自动刷新"。改为仅在后台时停掉定时器，回到前台时让定时器按原节奏继续。
let visibilityHandler: (() => void) | null = null

// SSR初始数据获取
const { data: rankData } = useAsyncData('discover-rank', async () => {
  try {
    const api = useApi()
    const res = await api.get<RankItem[]>('/rank/hot', { period: 'daily' })
    return res.data?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: tagData } = useAsyncData('discover-tags', async () => {
  try {
    const api = useApi()
    const res = await api.get<Tag[]>('/tags/hot', { limit: 20 })
    return res.data?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: userData } = useAsyncData('discover-users', async () => {
  try {
    const api = useApi()
    const res = await api.get<User[]>('/users/recommend', { limit: 5 })
    return res.data?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

hotRankItems.value = rankData.value
hotTags.value = tagData.value
recommendUsers.value = userData.value

// 客户端挂载后：如果SSR数据为空则立即获取，并启动自动刷新
onMounted(() => {
  // 将 SSR 获取的数据持久化到 localStorage
  if (true) {
    const cacheKey = (url: string, params?: Record<string, any>) => {
      const sortedParams = params
        ? JSON.stringify(Object.entries(params).sort(([a], [b]) => a.localeCompare(b)))
        : ''
      return `${url}:${sortedParams}`
    }
    const persistData = (key: string, data: any) => {
      if (data && (Array.isArray(data) ? data.length > 0 : true)) {
        storage.set(`req_cache_${key}`, { data, cachedAt: Date.now(), ttl: 5 * 60 * 1000 }, 30 * 60 * 1000)
      }
    }
    if (hotRankItems.value.length > 0) {
      persistData(cacheKey('/rank/hot', { period: 'daily' }), hotRankItems.value)
    }
    if (hotTags.value.length > 0) {
      persistData(cacheKey('/tags/hot', { limit: 20 }), hotTags.value)
    }
    if (recommendUsers.value.length > 0) {
      persistData(cacheKey('/users/recommend', { limit: 5 }), recommendUsers.value)
    }
  }

  if (hotRankItems.value.length === 0) {
    fetchHotRank()
  }
  if (hotTags.value.length === 0) {
    fetchHotTags()
  }
  if (recommendUsers.value.length === 0) {
    shuffleUsers()
  }
  startAutoRefresh()

  // 合并自原第二个 onMounted：注册 visibilitychange 监听，仅控制定时器启停。
  // 修复：原先在切回 tab 时会立即调用 refreshAll()，体感为"页面自动刷新"。
  // 改为后台时停掉定时器，回前台时按原节奏继续，不再主动重载数据。
  visibilityHandler = () => {
    if (document.visibilityState === 'hidden') {
      stopAutoRefresh()
    } else if (document.visibilityState === 'visible') {
      startAutoRefresh()
    }
  }
  document.addEventListener('visibilitychange', visibilityHandler)
})

onUnmounted(() => {
  stopAutoRefresh()
  if (successTimer) {
    clearTimeout(successTimer)
    successTimer = null
  }
  if (visibilityHandler) {
    document.removeEventListener('visibilitychange', visibilityHandler)
    visibilityHandler = null
  }
})

useHead({
  title: '发现 - 知讯',
})
</script>

<style scoped>
/* ==========================================================================
   发现页专属样式 —— 知讯 (Zhixun)
   ========================================================================== */

/* ===== 英雄横幅 ===== */
.discover-hero {
  position: relative;
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  padding: 1.5rem 1.5rem;
}

.discover-hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 40%, #a78bfa 100%);
  opacity: 0.08;
  pointer-events: none;
}

.discover-hero-glow {
  position: absolute;
  top: -30%;
  right: -10%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.discover-hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.discover-hero-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--zh-radius-md);
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
}

.discover-hero-icon {
  width: 22px;
  height: 22px;
  color: #fff;
}

.discover-hero-title {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--zh-text);
  margin: 0;
  line-height: 1.2;
}

.discover-hero-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  color: var(--zh-text-secondary);
  line-height: 1.5;
}

/* ===== 统计栏 ===== */
.discover-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 0.75rem 1rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  box-shadow: var(--zh-shadow-sm);
}

.discover-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.1rem;
  flex: 1;
  min-width: 0;
}

.discover-stat-value {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--zh-primary);
  line-height: 1;
  animation: count-up 0.4s var(--zh-transition-spring) both;
}

.discover-stat-label {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
  line-height: 1;
}

.discover-stat-divider {
  width: 1px;
  height: 2rem;
  background: var(--zh-border);
  flex-shrink: 0;
}

/* ===== 区块 ===== */
.discover-section {
  /* container */
}

.discover-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.discover-section-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: var(--zh-primary);
  flex-shrink: 0;
}

/* ===== 查看全部链接 ===== */
.discover-view-all {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--zh-primary);
  border: none;
  background: none;
  cursor: pointer;
  transition: color var(--zh-transition-fast), gap var(--zh-transition-base);
  padding: 0;
  line-height: 1;
  text-decoration: none;
}

.discover-view-all:hover {
  color: var(--zh-primary-dark);
  gap: 0.35rem;
}

.discover-view-all:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.discover-arrow-icon {
  width: 0.875rem;
  height: 0.875rem;
  transition: transform var(--zh-transition-base);
  flex-shrink: 0;
}

.discover-view-all:hover .discover-arrow-icon {
  transform: translateX(2px);
}

/* ===== 分隔线 ===== */
.discover-divider {
  height: 1px;
  margin: 0.5rem 0 1.25rem;
  background: linear-gradient(
    to right,
    transparent 0%,
    var(--zh-border) 20%,
    var(--zh-border) 80%,
    transparent 100%
  );
}

/* ===== 热榜卡片 ===== */
.discover-rank-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.65rem 0.75rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color var(--zh-transition-base);
}

.discover-rank-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--zh-shadow-md), 0 6px 20px rgba(79, 70, 229, 0.06);
  border-color: var(--zh-border-focus);
}

/* 前三名卡片特殊样式 */
.discover-rank-card--gold {
  border-left: 3px solid #f59e0b;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(245, 158, 11, 0.04) 100%);
}

.discover-rank-card--silver {
  border-left: 3px solid #94a3b8;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(148, 163, 184, 0.04) 100%);
}

.discover-rank-card--bronze {
  border-left: 3px solid #d97706;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(217, 119, 6, 0.04) 100%);
}

/* 排名徽章 */
.discover-rank-badge {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}

.discover-rank-card:hover .discover-rank-badge {
  transform: scale(1.15);
}

/* 前三名渐变徽章 */
.discover-rank-badge--gold {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%);
  color: #fff;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.4), 0 0 0 1px rgba(245, 158, 11, 0.15);
}

.discover-rank-badge--silver {
  background: linear-gradient(135deg, #e2e8f0 0%, #94a3b8 50%, #64748b 100%);
  color: #fff;
  box-shadow: 0 2px 10px rgba(148, 163, 184, 0.4), 0 0 0 1px rgba(148, 163, 184, 0.15);
}

.discover-rank-badge--bronze {
  background: linear-gradient(135deg, #fbbf24 0%, #d97706 50%, #b45309 100%);
  color: #fff;
  box-shadow: 0 2px 10px rgba(217, 119, 6, 0.4), 0 0 0 1px rgba(217, 119, 6, 0.15);
}

/* 热度值 */
.discover-rank-heat {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
  font-weight: 500;
  flex-shrink: 0;
}

.discover-rank-heat--top {
  color: var(--zh-primary);
  font-weight: 700;
}

/* ===== 标签胶囊 ===== */
.discover-tag-pill {
  --tag-color: 79, 70, 229;
  display: inline-flex;
  align-items: center;
  gap: 0.1rem;
  padding: 0.35rem 0.85rem;
  font-size: 0.8125rem;
  font-weight: 500;
  line-height: 1.4;
  border-radius: var(--zh-radius-full);
  background: rgba(var(--tag-color), 0.08);
  color: rgb(var(--tag-color));
  text-decoration: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(var(--tag-color), 0.12);
}

.discover-tag-pill:hover {
  background: rgba(var(--tag-color), 0.15);
  border-color: rgba(var(--tag-color), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(var(--tag-color), 0.15);
}

.discover-tag-hash {
  opacity: 0.6;
  font-weight: 600;
}

/* ===== 骨架屏 ===== */
.discover-skeleton-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.65rem 0.75rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  overflow: hidden;
}

.discover-skeleton-rank {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  flex-shrink: 0;
}

.discover-skeleton-line {
  height: 0.75rem;
  border-radius: var(--zh-radius-xs);
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}

.discover-skeleton-tag {
  width: 4rem;
  height: 1.75rem;
  border-radius: var(--zh-radius-full);
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}

.discover-skeleton-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  overflow: hidden;
}

.discover-skeleton-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 推荐用户网格 ===== */
.discover-user-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.625rem;
}

.discover-user-card {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: var(--zh-radius-md);
}

.discover-user-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.06);
}

/* ===== 换一批按钮 ===== */
.discover-shuffle-btn {
  gap: 0.3rem;
  transition: color var(--zh-transition-fast), gap var(--zh-transition-base);
}

.discover-shuffle-icon {
  width: 1rem;
  height: 1rem;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.discover-shuffle-btn--loading .discover-shuffle-icon {
  animation: shuffle-spin 0.8s linear infinite;
}

.discover-shuffle-btn--success {
  color: #10b981;
}

.discover-shuffle-btn--success .discover-success-icon {
  animation: success-pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.discover-shuffle-btn:not(:disabled):active .discover-shuffle-icon {
  transform: rotate(180deg);
}

@keyframes shuffle-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes success-pop {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}

/* ===== 响应式 ===== */
@media (max-width: 767.98px) {
  .discover-hero {
    padding: 1rem 1.25rem;
  }

  .discover-hero-icon-wrap {
    width: 36px;
    height: 36px;
    border-radius: var(--zh-radius-sm);
  }

  .discover-hero-icon {
    width: 18px;
    height: 18px;
  }

  .discover-hero-title {
    font-size: 1.25rem;
  }

  .discover-hero-subtitle {
    font-size: 0.8125rem;
  }

  .discover-stats {
    padding: 0.5rem 0.75rem;
  }

  .discover-stat-value {
    font-size: 1rem;
  }

  .discover-stat-label {
    font-size: 0.6875rem;
  }

  .discover-rank-card {
    padding: 0.5rem 0.625rem;
  }

  .discover-tag-pill {
    padding: 0.3rem 0.7rem;
    font-size: 0.75rem;
  }

  .discover-user-grid {
    grid-template-columns: 1fr;
  }
}

/* ==========================================================================
   发现页增强样式
   ========================================================================== */

/* 区块间距 */
.discover-section {
  margin-bottom: 0.75rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--zh-text);
}

.update-badge {
  font-size: 0.65rem;
  color: var(--zh-text-tertiary);
  background: var(--zh-bg-hover);
  padding: 1px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 0.8rem;
  color: var(--zh-primary);
  font-weight: 500;
  transition: all 0.2s ease;
  text-decoration: none;
}

.section-link:hover {
  color: var(--zh-primary-dark);
  gap: 4px;
}

.refresh-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--zh-primary-bg);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 热榜条目 */
.rank-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}

.rank-item:hover {
  border-color: var(--zh-border-focus);
  box-shadow: var(--zh-shadow-sm);
  transform: translateX(2px);
}

.rank-badge {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 0.75rem;
}

.rank-medal {
  font-size: 1rem;
  line-height: 1;
}

.rank-num {
  font-size: 0.75rem;
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--zh-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: var(--zh-text-tertiary);
  flex-wrap: wrap;
}

.rank-author {
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-sep {
  flex-shrink: 0;
}

.rank-heat {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #f59e0b;
  flex-shrink: 0;
}

.rank-score {
  text-align: right;
  flex-shrink: 0;
}

.score-value {
  display: block;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--zh-warning);
}

.score-label {
  display: block;
  font-size: 0.6rem;
  color: var(--zh-text-tertiary);
}

/* 标签芯片 */
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  padding: 0.4rem 0.85rem;
  font-size: 0.8rem;
  font-weight: 500;
  border-radius: 9999px;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.tag-chip:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
  border-color: rgba(var(--zh-primary-rgb), 0.2);
  transform: translateY(-1px);
}

.tag-hash {
  color: var(--zh-primary);
  font-weight: 600;
  opacity: 0.6;
}

/* 骨架卡片 */
.skeleton-card-sm {
  padding: 0.75rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-md);
}
</style>