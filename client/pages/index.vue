<template>
  <!-- 首页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <PullToRefresh :on-refresh="() => handleRefresh()" :error="error || undefined">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- Tab切换 - 移动端紧凑布局确保单行显示 -->
        <div class="flex items-center border-b border-slate-200 overflow-x-auto no-scrollbar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="text-sm font-medium border-b-2 transition-colors whitespace-nowrap no-tap-highlight touch-target px-2 py-2"
            :class="activeTab === tab.key
                ? 'border-primary text-primary'
                : 'border-transparent text-slate-500 hover:text-slate-700'"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>

          <!-- 推荐Tab的"换一批"刷新按钮 - 移动端仅显示图标 -->
          <button
            v-if="activeTab === 'recommend'"
            class="ml-auto flex items-center gap-1 text-sm text-primary hover:text-primary-dark transition-colors rounded-full hover:bg-primary-50/50 no-tap-highlight shrink-0 px-2 py-1"
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
            <span>{{ refreshing ? '刷新中' : '换一批' }}</span>
          </button>
        </div>

        <!-- 作品列表 - 平板端双列 -->
        <ArticleList
          :articles="articles"
          :loading="loading"
          :has-more="hasMore"
          :error="error"
          @load-more="loadMore"
          @retry="handleRetry"
        />
      </div>
    </div>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/** 首页：推荐/热门/最新/关注四个Tab，作品卡片列表 */

import type { Article, PageResult, ApiResponse } from '~/types'

const userStore = useUserStore()
const config = useRuntimeConfig()
const { defaultSort: savedDefaultSort } = useLocalSettings()

// Tab配置
const tabs = [
  { key: 'recommend', label: computed(() => '推荐') },
  { key: 'hot', label: computed(() => '排行') },
  { key: 'latest', label: computed(() => '最新') },
  { key: 'following', label: computed(() => '关注') },
]

// 初始Tab：根据用户设置的默认排序选择
const initialTab = computed(() => {
  if (savedDefaultSort.value === 'latest') return 'latest'
  if (savedDefaultSort.value === 'hot') return 'hot'
  return 'recommend'
})
const activeTab = ref(initialTab.value)
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const refreshKey = ref('')

// 构建API基础URL：SSR时使用内部地址，客户端时走Nginx代理
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 通用分页数据获取 — 使用 useApi 以支持 Token 自动刷新和 CSRF 防护
const fetchPage = async (url: string, params: Record<string, any> = {}) => {
  const api = useApi()
  const res = await api.get<PageResult<Article>>(url, { page: params.page || 1, pageSize: params.pageSize || 10, ...params })
  return res.data.data
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
// 注意：handleRefresh 内使用 useApi 替代 $fetch，确保 Token 自动刷新和 CSRF 防护生效
const { loading: refreshing, refresh: handleRefresh } = useRefresh({
  onRefresh: async () => {
    error.value = null
    page.value = 1
    hasMore.value = true

    const params: Record<string, any> = {
      page: 1,
      pageSize: 10,
      refresh: 1,
    }
    if (refreshKey.value) {
      params.refresh_key = refreshKey.value
    }

    const api = useApi()
    const res = await api.get<PageResult<Article>>('/feed/recommend', params)
    const data = res.data.data
    const items = data?.list || []

    articles.value = items
    refreshKey.value = (data as any)?.refresh_key || ''
    hasMore.value = items.length >= 10
  },
  debounceMs: 300,
  showError: true,
  errorMessage: undefined, // 使用默认错误消息
  onError: () => {
    hasMore.value = false
  },
})

// 获取作品列表
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
    const pageSize = 10
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
    error.value = '加载失败，请稍后重试'
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

// SSR数据获取 - 使用单个 useAsyncData + Promise.all 并行请求，避免串行等待
const { data: homeData } = await useAsyncData('home-init', async () => {
  const base = getApiBase()
  const headers = import.meta.server ? { 'X-SSR-Request': 'true' } : {}

  const feedRes = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/recommend`, {
    params: { page: 1, pageSize: 10 },
    headers,
  }).catch(() => null)

  return {
    feed: feedRes?.data?.list || [],
    refreshKey: (feedRes?.data as any)?.refresh_key || '',
  }
}, {
  default: () => ({ feed: [] as Article[], refreshKey: '' }),
  // 客户端缓存：60秒内使用缓存，避免导航回首页时重复请求
  // 如果缓存的feed为空（SSR失败场景），跳过缓存让客户端重新请求
  getCachedData(key, nuxtApp) {
    const cached = nuxtApp.payload.data[key] || nuxtApp.static.data[key]
    if (!cached) return undefined
    if (!cached.feed || cached.feed.length === 0) return undefined
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

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>
