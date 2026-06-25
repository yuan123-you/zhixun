<template>
  <!-- 发现页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">


    <PullToRefresh :on-refresh="() => refreshAll()" :error="usersError || rankError || tagsError">
      <!-- 热榜 -->
      <section class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white">{{ t('hotRank.title') }}</h2>
            <span v-if="rankUpdateTime" class="text-xs text-gray-400">{{ rankUpdateTime }}</span>
          </div>
          <NuxtLink to="/rank" class="text-sm text-primary hover:text-primary-600 transition-colors">查看全部</NuxtLink>
        </div>
        <div v-if="rankLoading" class="space-y-2">
          <div v-for="i in 5" :key="i" class="card p-3 flex items-center space-x-3 animate-pulse">
            <div class="w-7 h-7 rounded-full bg-gray-200 dark:bg-gray-700 shrink-0"></div>
            <div class="flex-1 h-4 bg-gray-200 dark:bg-gray-700 rounded"></div>
          </div>
        </div>
        <ErrorRetry v-else-if="rankError" :message="rankError" :on-retry="fetchHotRank" />
        <div v-else-if="hotRankItems.length > 0" class="space-y-2">
          <div
            v-for="(item, index) in hotRankItems"
            :key="item.id"
            class="card p-3 flex items-center gap-3 hover:shadow-md transition-shadow cursor-pointer"
            @click="navigateTo(`/articles/${item.id}`)"
          >
            <div class="w-7 h-7 rounded-full flex items-center justify-center shrink-0 font-bold text-xs" :class="getRankClass(index)">
              {{ index + 1 }}
            </div>
            <div class="flex-1 min-w-0 overflow-hidden">
              <h3 class="text-sm font-medium text-gray-900 dark:text-white truncate">{{ item.title }}</h3>
              <div class="flex items-center space-x-2 mt-0.5 text-xs text-gray-400">
                <span v-if="item.authorNickname" class="truncate max-w-[100px]">{{ item.authorNickname }}</span>
                <span v-if="item.authorNickname && item.score != null">·</span>
                <span v-if="item.score != null" class="shrink-0">{{ formatHeat(item.score) }} {{ t('article.heat') }}</span>
              </div>
            </div>
            <span class="text-xs text-accent font-bold shrink-0">{{ formatHeat(item.score ?? 0) }}</span>
          </div>
        </div>
        <EmptyState v-else :title="t('hotRank.empty')" />
      </section>

      <!-- 热门标签 -->
      <section class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white">{{ t('hotTags.title') }}</h2>
            <span v-if="tagsUpdateTime" class="text-xs text-gray-400">{{ tagsUpdateTime }}</span>
          </div>
          <NuxtLink to="/tags" class="text-sm text-primary hover:text-primary-600 transition-colors">{{ t('hotTags.viewAll') }}</NuxtLink>
        </div>
        <div v-if="tagsLoading" class="flex flex-wrap gap-2">
          <div v-for="i in 8" :key="i" class="px-3 py-1.5 text-sm rounded-full bg-gray-200 dark:bg-gray-700 animate-pulse w-16 h-7"></div>
        </div>
        <ErrorRetry v-else-if="tagsError" :message="tagsError" :on-retry="fetchHotTags" />
        <div v-else-if="hotTags.length > 0" class="flex flex-wrap gap-2">
          <NuxtLink
            v-for="tag in hotTags"
            :key="tag.id"
            to="/tags"
            class="px-3 py-1.5 text-sm rounded-full bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-primary/10 hover:text-primary dark:hover:text-primary transition-colors"
          >
            # {{ tag.name }}
          </NuxtLink>
        </div>
      </section>

      <!-- 推荐用户 -->
      <section class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white">{{ t('recommendUsers.title') }}</h2>
            <span v-if="usersUpdateTime" class="text-xs text-gray-400">{{ usersUpdateTime }}</span>
          </div>
          <button
            class="flex items-center gap-1 text-sm text-primary hover:text-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="usersLoading"
            @click="shuffleUsers"
          >
            <svg
              class="w-4 h-4 transition-transform"
              :class="{ 'animate-spin': usersLoading }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>{{ usersLoading ? t('common.refreshing') : t('common.refresh') }}</span>
          </button>
        </div>
        <div v-if="usersLoading" class="space-y-3">
          <div v-for="i in 3" :key="i" class="card p-4 flex items-center space-x-3 animate-pulse">
            <div class="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 shrink-0"></div>
            <div class="flex-1">
              <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24 mb-2"></div>
              <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-32"></div>
            </div>
          </div>
        </div>
        <ErrorRetry v-else-if="usersError" :message="usersError" :on-retry="fetchRecommendUsers" />
        <div v-else-if="recommendUsers.length > 0" class="space-y-3">
          <UserCard v-for="user in recommendUsers" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
        </div>
      </section>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/** 发现页：热榜、热门标签、推荐用户 - 联网实时加载确保数据真实性 */
import type { RankItem, User, Tag, ApiResponse } from '~/types'
import { storage } from '~/utils/storage'

const userStore = useUserStore()
const config = useRuntimeConfig()
const { t } = useI18n()

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
    const base = getApiBase()
    const res = await $fetch<ApiResponse<User[]>>(`${base}/users/recommend`, {
      params: { limit, offset, _t: Date.now() },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
      cache: 'no-store',
    })
    return res?.data || []
  },
  limit: 5,
  getItemId: (user) => user.id,
  shuffle: true,
  debounceMs: 300,
})

// 监听推荐用户数据变化后更新时间
watch(recommendUsers, () => {
  usersUpdateTime.value = formatTime()
})

// 全部刷新：使用 useRefresh 统一管理
const { refresh: refreshAll } = useRefresh({
  onRefresh: async () => {
    const base = getApiBase()

    const forceFetchRank = async () => {
      rankLoading.value = true
      rankError.value = ''
      try {
        const url = `${base}/rank/hot`
        const params = { period: 'daily' }
        const res = await forceRequest<ApiResponse<RankItem[]>>(
          () => $fetch<ApiResponse<RankItem[]>>(url, {
            params: { ...params, _t: Date.now() },
            cache: 'no-store',
          }),
          url,
          params,
        )
        hotRankItems.value = res?.data || []
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
        const url = `${base}/tags/hot`
        const params = { limit: 20 }
        const res = await forceRequest<ApiResponse<Tag[]>>(
          () => $fetch<ApiResponse<Tag[]>>(url, {
            params: { ...params, _t: Date.now() },
            cache: 'no-store',
          }),
          url,
          params,
        )
        hotTags.value = res?.data || []
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
  errorMessage: '刷新失败，请稍后重试',
})

// 自动刷新定时器
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
const AUTO_REFRESH_INTERVAL = 5 * 60 * 1000 // 5分钟自动刷新

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'bg-yellow-400 text-white'
  if (index === 1) return 'bg-gray-300 text-white'
  if (index === 2) return 'bg-orange-400 text-white'
  return 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
}

// 格式化热度值：>=1，最多1位小数
const formatHeat = (score: number) => {
  if (score == null || score < 1) return '1'
  const rounded = Math.round(score * 10) / 10
  if (rounded >= 10000) return `${(rounded / 10000).toFixed(1)}万`
  return rounded.toString()
}

// 格式化更新时间
const formatTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}更新`
}

// 构建API基础URL
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 获取热榜数据 - 使用缓存避免重复请求
const fetchHotRank = async () => {
  rankLoading.value = true
  rankError.value = ''
  try {
    const base = getApiBase()
    const url = `${base}/rank/hot`
    const params = { period: 'daily' }
    const res = await cachedRequest<ApiResponse<RankItem[]>>(
      () => $fetch<ApiResponse<RankItem[]>>(url, {
        params: { ...params, _t: Date.now() },
        headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
        cache: 'no-store',
      }),
      url,
      params,
    )
    hotRankItems.value = res?.data || []
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
    const base = getApiBase()
    const url = `${base}/tags/hot`
    const params = { limit: 20 }
    const res = await cachedRequest<ApiResponse<Tag[]>>(
      () => $fetch<ApiResponse<Tag[]>>(url, {
        params: { ...params, _t: Date.now() },
        headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
        cache: 'no-store',
      }),
      url,
      params,
    )
    hotTags.value = res?.data || []
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
    navigateTo('/login')
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

// SSR初始数据获取
const { data: rankData } = await useAsyncData('discover-rank', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<RankItem[]>>(`${base}/rank/hot`, {
      params: { period: 'daily' },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: tagData } = await useAsyncData('discover-tags', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/hot`, {
      params: { limit: 20 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: userData } = await useAsyncData('discover-users', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<User[]>>(`${base}/users/recommend`, {
      params: { limit: 5 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
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
  if (import.meta.client) {
    const base = getApiBase()
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
      persistData(cacheKey(`${base}/rank/hot`, { period: 'daily' }), hotRankItems.value)
    }
    if (hotTags.value.length > 0) {
      persistData(cacheKey(`${base}/tags/hot`, { limit: 20 }), hotTags.value)
    }
    if (recommendUsers.value.length > 0) {
      persistData(cacheKey(`${base}/users/recommend`, { limit: 5 }), recommendUsers.value)
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
})

// 页面可见性变化时：隐藏时停止定时器，可见时刷新并重启定时器
onMounted(() => {
  const visibilityHandler = () => {
    if (document.visibilityState === 'visible') {
      refreshAll()
      startAutoRefresh()
    } else {
      stopAutoRefresh()
    }
  }
  document.addEventListener('visibilitychange', visibilityHandler)
  onUnmounted(() => {
    document.removeEventListener('visibilitychange', visibilityHandler)
  })
})

onUnmounted(() => {
  stopAutoRefresh()
})

useHead({
  title: '发现 - 知讯',
})
</script>
