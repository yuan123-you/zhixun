<template>
  <!-- 首页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-4 2xl:px-8 py-6">
    <PullToRefresh :on-refresh="() => handleRefresh()" :error="error || undefined">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- 轮播图 -->
        <LazyBannerCarousel v-if="bannerList.length > 0" :banners="bannerList" />
        <!-- 轮播图骨架屏 -->
        <div v-else class="animate-pulse rounded-xl overflow-hidden">
          <div class="w-full bg-gray-200 dark:bg-gray-700" style="padding-bottom: 40%"></div>
        </div>

        <!-- 公告栏 -->
        <div v-if="announcementList.length > 0" class="mt-4">
          <LazyAnnouncementBar :announcements="announcementList" />
        </div>

        <!-- Tab切换 - 平板端增大触摸区域 -->
        <div class="flex items-center border-b border-gray-200 dark:border-gray-700 mb-6 overflow-x-auto no-scrollbar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-4 py-3 text-sm font-medium border-b-2 transition-colors whitespace-nowrap no-tap-highlight touch-target"
            :class="activeTab === tab.key
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>

          <!-- 推荐Tab的"换一批"刷新按钮 -->
          <button
            v-if="activeTab === 'recommend'"
            class="ml-auto flex items-center gap-1 text-sm text-primary hover:text-primary-dark transition-colors px-3 py-2 rounded-full hover:bg-primary/5 no-tap-highlight"
            :disabled="refreshing"
            @click="handleRefresh"
          >
            <svg
              class="w-4 h-4 transition-transform"
              :class="{ 'animate-spin': refreshing }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>{{ refreshing ? t('common.refreshing') : t('common.refresh') }}</span>
          </button>
        </div>

        <!-- 文章列表 - 平板端双列 -->
        <ArticleList
          :articles="articles"
          :loading="loading"
          :has-more="hasMore"
          :error="error"
          @load-more="loadMore"
          @retry="handleRetry"
        />
      </div>

      <!-- 右侧边栏 -->
      <div class="hidden lg:block w-80 shrink-0 space-y-6">
        <LazyHotRank :items="hotRankItems" />
      </div>
    </div>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/** 首页：推荐/热门/最新/关注四个Tab，文章卡片列表 */

import type { Article, PageResult, ApiResponse, RankItem } from '~/types'
import type { BannerItem, AnnouncementItem } from '~/api/banner'
import { storage } from '~/utils/storage'

const userStore = useUserStore()
const config = useRuntimeConfig()

const { t } = useI18n()

// Tab配置
const tabs = [
  { key: 'recommend', label: computed(() => t('nav.recommend')) },
  { key: 'hot', label: computed(() => t('nav.hot')) },
  { key: 'latest', label: computed(() => t('nav.latest')) },
  { key: 'following', label: computed(() => t('nav.follow')) },
]

const activeTab = ref('recommend')
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const bannerList = ref<BannerItem[]>([])
const announcementList = ref<AnnouncementItem[]>([])
const hotRankItems = ref<RankItem[]>([])

// 请求缓存
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

// 推荐刷新机制
const refreshKey = ref('')

// 构建API基础URL：SSR时使用内部地址，客户端时走Nginx代理
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 通用分页数据获取
const fetchPage = async (url: string, params: Record<string, any> = {}) => {
  const base = getApiBase()
  const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}${url}`, {
    params: { page: params.page || 1, pageSize: params.pageSize || 20, ...params },
    headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
  })
  return res.data
}

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  articles.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  // 切换到推荐Tab时重置refreshKey
  if (key === 'recommend') {
    refreshKey.value = ''
  }
  // 切换Tab时失效对应的feed缓存
  const { invalidateByPrefix } = useRequestCache()
  const feedPrefixMap: Record<string, string> = {
    recommend: '/feed/recommend',
    hot: '/feed/hot',
    latest: '/feed/latest',
    following: '/feed/following',
  }
  const prefix = feedPrefixMap[key]
  if (prefix) {
    invalidateByPrefix(prefix)
  }
  fetchArticles()
}

// 推荐刷新：换一批 - 使用 useRefresh 统一管理加载状态和错误处理
const { loading: refreshing, refresh: handleRefresh } = useRefresh({
  onRefresh: async () => {
    error.value = null
    page.value = 1
    hasMore.value = true

    const base = getApiBase()
    const params: Record<string, any> = {
      page: 1,
      pageSize: 20,
      refresh: 1,
    }
    if (refreshKey.value) {
      params.refresh_key = refreshKey.value
    }

    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/recommend`, {
      params,
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    const data = res?.data
    const items = data?.list || []

    articles.value = items
    refreshKey.value = (data as any)?.refresh_key || ''
    hasMore.value = items.length >= 20
  },
  debounceMs: 300,
  showError: true,
  errorMessage: undefined, // 使用默认错误消息
  onError: () => {
    hasMore.value = false
  },
})

// 获取文章列表
const fetchArticles = async () => {
  if (loading.value) return

  // 关注tab需要登录
  if (activeTab.value === 'following' && !userStore.isLoggedIn) {
    articles.value = []
    hasMore.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    const pageSize = 20
    let url: string
    let extraParams: Record<string, any> = {}
    switch (activeTab.value) {
      case 'latest':
        url = '/feed/latest'
        break
      case 'following':
        url = '/feed/following'
        break
      case 'hot':
        url = '/feed/hot'
        break
      default:
        url = '/feed/recommend'
        // 推荐Tab翻页时携带refresh_key
        if (refreshKey.value) {
          extraParams.refresh_key = refreshKey.value
        }
    }

    const data = await fetchPage(url, { page: page.value, pageSize, ...extraParams })
    const items = data?.list || []
    if (page.value === 1) {
      articles.value = items
    } else {
      articles.value.push(...items)
    }
    // 首次加载推荐时保存refresh_key
    if (activeTab.value === 'recommend' && page.value === 1) {
      refreshKey.value = (data as any)?.refresh_key || ''
    }
    hasMore.value = items.length >= pageSize
  } catch {
    error.value = t('common.loadFailed')
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  const prevPage = page.value
  page.value++
  try {
    await fetchArticles()
  } catch {
    // 加载失败，回退页码
    page.value = prevPage
  }
}

// 重试上次失败的请求
const handleRetry = () => {
  error.value = null
  fetchArticles()
}

// 客户端刷新banner和公告数据（使用请求缓存）
const refreshBannerAndAnnouncements = async () => {
  if (!import.meta.client) return
  const base = getApiBase()
  const headers = import.meta.server ? { 'X-SSR-Request': 'true' } : {}

  try {
    const [banners, announcements] = await Promise.all([
      cachedRequest(
        () => $fetch<ApiResponse<BannerItem[]>>(`${base}/banners`, { headers }),
        `${base}/banners`,
        undefined,
        { ttl: 10 * 60 * 1000 }
      ),
      cachedRequest(
        () => $fetch<ApiResponse<AnnouncementItem[]>>(`${base}/announcements`, { headers }),
        `${base}/announcements`,
        undefined,
        { ttl: 10 * 60 * 1000 }
      ),
    ])
    bannerList.value = banners?.data || bannerList.value
    announcementList.value = announcements?.data || announcementList.value
  } catch {
    // 静默失败，保留现有数据
  }
}

// SSR数据获取 - 使用单个 useAsyncData + Promise.all 并行请求，避免串行等待
// 热门排行榜已移至客户端懒加载，减少 SSR 请求数
const { data: homeData } = await useAsyncData('home-init', async () => {
  const base = getApiBase()
  const headers = import.meta.server ? { 'X-SSR-Request': 'true' } : {}

  const [feedRes, bannerRes, announcementRes] = await Promise.all([
    $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/recommend`, {
      params: { page: 1, pageSize: 20 },
      headers,
    }).catch(() => null),
    $fetch<ApiResponse<BannerItem[]>>(`${base}/banners`, { headers }).catch(() => null),
    $fetch<ApiResponse<AnnouncementItem[]>>(`${base}/announcements`, { headers }).catch(() => null),
  ])

  return {
    feed: feedRes?.data?.list || [],
    refreshKey: (feedRes?.data as any)?.refresh_key || '',
    banners: bannerRes?.data || [],
    announcements: announcementRes?.data || [],
  }
}, {
  default: () => ({ feed: [] as Article[], refreshKey: '', banners: [] as BannerItem[], announcements: [] as AnnouncementItem[] }),
  // 客户端缓存：60秒内使用缓存，避免导航回首页时重复请求
  getCachedData(key, nuxtApp) {
    const cached = nuxtApp.payload.data[key] || nuxtApp.static.data[key]
    if (!cached) return undefined
    const cacheTimestamp = (nuxtApp.payload as any)._homeInitTimestamp || 0
    const now = Date.now()
    if (now - cacheTimestamp > 60 * 1000) return undefined
    return cached
  },
})

// 记录缓存时间戳，供 getCachedData 判断过期
if (import.meta.client) {
  const nuxtApp = useNuxtApp()
  if (!(nuxtApp.payload as any)._homeInitTimestamp) {
    ;(nuxtApp.payload as any)._homeInitTimestamp = Date.now()
  }
}

articles.value = homeData.value.feed
refreshKey.value = homeData.value.refreshKey
bannerList.value = homeData.value.banners
announcementList.value = homeData.value.announcements

// 热门排行榜：客户端懒加载，减少 SSR 请求压力
const { data: lazyHotRank } = useLazyData<RankItem[]>({
  fetchFn: async () => {
    const base = getApiBase()
    const headers = import.meta.server ? { 'X-SSR-Request': 'true' } : {}
    const res = await $fetch<ApiResponse<RankItem[]>>(`${base}/rank/hot`, {
      params: { period: 'all', limit: 10 },
      headers,
    }).catch(() => null)
    return res?.data || []
  },
  immediate: true,
  defaultData: [],
})
watch(lazyHotRank, (val) => {
  if (val && val.length > 0) {
    hotRankItems.value = val
  }
})

// 客户端挂载后：将 SSR 数据持久化到 localStorage，供后续页面导航使用
onMounted(() => {
  if (import.meta.client && bannerList.value.length > 0) {
    const base = getApiBase()
    // 生成与 useRequestCache 一致的缓存 key
    const cacheKey = (url: string, params?: Record<string, any>) => {
      const sortedParams = params
        ? JSON.stringify(Object.entries(params).sort(([a], [b]) => a.localeCompare(b)))
        : ''
      return `${url}:${sortedParams}`
    }
    // 直接写入 localStorage 持久化，供下次页面加载时使用
    const persistData = (key: string, data: any) => {
      storage.set(`req_cache_${key}`, { data, cachedAt: Date.now(), ttl: 10 * 60 * 1000 }, 30 * 60 * 1000)
    }
    persistData(cacheKey(`${base}/banners`), bannerList.value)
    persistData(cacheKey(`${base}/announcements`), announcementList.value)
  }
})

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>
