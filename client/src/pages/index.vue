<template>
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto py-1 px-2 md:px-0">
    <PullToRefresh ref="ptrRef" :on-refresh="handleRefresh" :error="error || undefined">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- Tab切换 - 移动端紧凑布局确保单行显示 -->
        <div class="flex items-center border-b border-[var(--zh-border)] overflow-x-auto no-scrollbar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="text-[13px] font-medium border-b-2 transition-colors whitespace-nowrap no-tap-highlight touch-target px-2 py-1.5"
            :class="activeTab === tab.key
                ? 'border-primary text-primary'
                : 'border-transparent text-[var(--zh-text-secondary)] hover:text-[var(--zh-text-secondary)]'"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>

          <!-- 刷新按钮（桌面端下拉刷新的备选入口） -->
          <button
            class="ml-auto p-1.5 text-[var(--zh-text-secondary)] dark:text-gray-300 hover:bg-[var(--zh-bg-hover)] dark:hover:bg-gray-700 rounded-lg shrink-0 no-tap-highlight touch-target"
            :class="{ 'home-refresh-btn--spinning': refreshing }"
            :disabled="refreshing"
            :aria-label="refreshing ? '刷新中' : '刷新推荐内容'"
            @click="onRefreshClick"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>

          <!-- 未登录时显示登录按钮（所有Tab） -->
          <RouterLink
              v-if="!userStore.isLoggedIn"
              to="/login"
              class="ml-1 flex items-center gap-1 text-[13px] text-primary hover:text-primary-dark transition-colors rounded-lg hover:bg-primary-50/50 no-tap-highlight shrink-0 px-2 py-1"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
              </svg>
              <span>登录</span>
            </RouterLink>
          </div>

        <!-- 关注Tab未登录提示 -->
        <div v-if="activeTab === 'following' && !userStore.isLoggedIn && !loading" class="flex flex-col items-center justify-center py-16 text-center">
          <svg class="w-16 h-16 text-[var(--zh-text-tertiary)] mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          <p class="text-[var(--zh-text-secondary)] mb-4">登录后可查看关注内容</p>
          <RouterLink to="/login" class="inline-flex items-center gap-2 px-6 py-2.5 bg-primary text-white text-sm font-medium rounded-full hover:bg-primary-600 transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
            </svg>
            去登录
          </RouterLink>
        </div>

        <!-- 作品列表 - 平板端双列 -->
        <ArticleList
          v-else
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

import type { Article, PageResult } from '@/types'

const userStore = useUserStore()
const { defaultSort: savedDefaultSort } = useLocalSettings()
const route = useRoute()

// Tab配置
const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'hot', label: '排行' },
  { key: 'latest', label: '最新' },
  { key: 'following', label: '关注' },
]

// 初始Tab：根据 localStorage 中保存的默认排序确定
const tabMap: Record<string, string> = {
  latest: 'latest',
  hot: 'hot',
}
const activeTab = ref(tabMap[savedDefaultSort.value] || 'recommend')
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const refreshKey = ref('')

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

// 下拉/换一批刷新：按当前 Tab 分发，确保推荐走算法换批、其余 Tab 走各自接口重拉
// 修复：此前所有 Tab 下拉刷新都调推荐接口，导致"最新"刷新后内容变成推荐排序、不再按时间排序
const { loading: refreshing, refresh: handleRefresh } = useRefresh({
  onRefresh: async () => {
    error.value = null
    page.value = 1
    hasMore.value = true

    // 推荐 Tab：调用算法推荐接口并 refresh=1 换一批新内容
    if (activeTab.value === 'recommend') {
      const api = useApi()
      const res = await api.get<PageResult<Article>>('/feed/recommend', {
        page: 1,
        pageSize: 10,
        refresh: 1,
      })
      const data = res.data.data
      const items = data?.list || []
      articles.value = items
      refreshKey.value = (data as any)?.refresh_key || ''
      hasMore.value = items.length >= 10
      return
    }

    // 其余 Tab（最新/热门/关注）：重置到第一页，走各自接口重新拉取
    // 最新 Tab 走 /feed/latest，保持按发布时间倒序；不再误调推荐接口
    await fetchArticles()
  },
  debounceMs: 300,
  showError: true,
  errorMessage: undefined, // 使用默认错误消息
  onError: () => {
    hasMore.value = false
  },
})

// PullToRefresh 组件引用：用于在桌面端通过按钮触发刷新并显示顶部指示器
const ptrRef = ref<{ triggerRefresh: () => Promise<void>; isRefreshing: Ref<boolean> } | null>(null)

// 桌面端"点击刷新"按钮的处理：
// - 移动端以下拉为主，桌面端以下拉 + 按钮双入口，确保所有设备均可触发刷新
// - 委托给 PullToRefresh 组件统一管理状态，避免重复实现
const onRefreshClick = () => {
  if (refreshing.value) return
  ptrRef.value?.triggerRefresh()
}

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

// 初始加载：根据默认Tab直接获取数据
fetchArticles()

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>

<style scoped>
/* 顶部刷新按钮：加载时持续旋转动画 */
.home-refresh-btn--spinning svg {
  animation: home-refresh-spin 0.8s linear infinite;
}
@keyframes home-refresh-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
