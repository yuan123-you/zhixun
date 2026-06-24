<template>
  <!-- 首页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-4 2xl:px-8 py-6">
    <PullToRefresh :on-refresh="handleRefresh">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- 轮播图 -->
        <BannerCarousel :banners="bannerList" />

        <!-- 公告栏 -->
        <div v-if="announcementList.length > 0" class="mt-4">
          <AnnouncementBar :announcements="announcementList" />
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
            <span>{{ refreshing ? $t('common.refreshing') : $t('common.refresh') }}</span>
          </button>
        </div>

        <!-- 文章列表 - 平板端双列 -->
        <ArticleList
          :articles="articles"
          :loading="loading"
          :has-more="hasMore"
          @load-more="loadMore"
        />
      </div>

      <!-- 右侧栏（桌面端可见，平板端通过侧边栏展示） -->
      <aside class="hidden lg:block w-80 2xl:w-96 shrink-0 space-y-6">
        <!-- 热榜 -->
        <HotRank :items="hotRankItems" />

        <!-- 热门标签 -->
        <div class="card">
          <div class="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <h3 class="font-semibold text-gray-900 dark:text-white">{{ $t('hotTags.title') }}</h3>
            <NuxtLink to="/tags" class="text-xs text-primary hover:text-primary-dark transition-colors">{{ $t('hotTags.viewAll') }}</NuxtLink>
          </div>
          <div class="p-4 flex flex-wrap gap-2">
            <NuxtLink
              v-for="tag in hotTags"
              :key="tag.id"
              :to="`/tags`"
              class="px-3 py-1 text-sm rounded-full bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-primary/10 hover:text-primary dark:hover:text-primary transition-colors"
            >
              {{ tag.name }}
            </NuxtLink>
          </div>
        </div>

        <!-- 推荐用户 -->
        <div class="card">
          <div class="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="font-semibold text-gray-900 dark:text-white">{{ $t('recommendUsers.title') }}</h3>
          </div>
          <div class="p-4 space-y-4">
            <UserCard v-for="user in recommendUsers" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
        </div>
      </aside>
    </div>

    <!-- 移动端/平板端：热榜和推荐用户入口（右侧栏在小屏不可见，补充展示） -->
    <div class="lg:hidden mt-6 space-y-6">
      <!-- 热榜 -->
      <HotRank :items="hotRankItems" />

      <!-- 热门标签 -->
      <div class="card">
        <div class="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <h3 class="font-semibold text-gray-900 dark:text-white">热门标签</h3>
          <NuxtLink to="/tags" class="text-xs text-primary hover:text-primary-dark transition-colors">查看全部</NuxtLink>
        </div>
        <div class="p-4 flex flex-wrap gap-2">
          <NuxtLink
            v-for="tag in hotTags"
            :key="tag.id"
            to="/tags"
            class="px-3 py-1 text-sm rounded-full bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-primary/10 hover:text-primary dark:hover:text-primary transition-colors"
          >
            {{ tag.name }}
          </NuxtLink>
        </div>
      </div>

      <!-- 推荐用户 -->
      <div class="card">
        <div class="p-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="font-semibold text-gray-900 dark:text-white">推荐关注</h3>
        </div>
        <div class="p-4 space-y-4">
          <UserCard v-for="user in recommendUsers" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
        </div>
      </div>
    </div>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/** 首页：推荐/热门/最新/关注四个Tab，文章卡片列表，右侧热榜/推荐栏 */

import type { Article, User, RankItem, PageResult, ApiResponse, Tag } from '~/types'
import type { BannerItem, AnnouncementItem } from '~/api/banner'

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
const refreshing = ref(false)
const hasMore = ref(true)
const page = ref(1)
const hotRankItems = ref<RankItem[]>([])
const recommendUsers = ref<User[]>([])
const bannerList = ref<BannerItem[]>([])
const announcementList = ref<AnnouncementItem[]>([])
const hotTags = ref<Tag[]>([])

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
  // 切换到推荐Tab时重置refreshKey
  if (key === 'recommend') {
    refreshKey.value = ''
  }
  fetchArticles()
}

// 推荐刷新：换一批
const handleRefresh = async () => {
  if (refreshing.value) return
  refreshing.value = true
  page.value = 1
  hasMore.value = true

  try {
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
  } catch {
    hasMore.value = false
  } finally {
    refreshing.value = false
  }
}

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
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  fetchArticles()
}

// 关注/取关
const toggleFollow = async (userId: number) => {
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(userId)
    // 更新本地用户关注状态
    const user = recommendUsers.value.find(u => u.id === userId)
    if (user) {
      user.isFollowing = !user.isFollowing
      user.followerCount += user.isFollowing ? 1 : -1
    }
  } catch (error: any) {
    console.error('关注操作失败:', error.message)
  }
}

// 加载推荐用户
const fetchRecommendUsers = async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<User[]>>(`${base}/users/recommend`, {
      params: { limit: 5 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    recommendUsers.value = res?.data || []
  } catch {
    recommendUsers.value = []
  }
}

// SSR数据获取 - 使用 useAsyncData + $fetch 确保SSR正确渲染
// SSR时通过 nitro routeRules proxy /api -> http://server:8080/api
// 客户端时通过 nginx 代理 /api -> http://server:8080/api
const { data: feedData } = await useAsyncData('feed-recommend', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/recommend`, {
      params: { page: 1, pageSize: 20 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    // 保存首次加载的refresh_key
    const list = res?.data?.list || []
    refreshKey.value = (res?.data as any)?.refresh_key || ''
    return list
  } catch {
    return []
  }
}, { default: () => [] })

const { data: rankData } = await useAsyncData('rank-hot', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<RankItem[]>>(`${base}/rank/hot`, {
      params: { period: 'weekly' },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: userData } = await useAsyncData('users-recommend', async () => {
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

const { data: bannerData } = await useAsyncData('banners-active', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<BannerItem[]>>(`${base}/banners`, {
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: announcementData } = await useAsyncData('announcements-active', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<AnnouncementItem[]>>(`${base}/announcements`, {
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: tagData } = await useAsyncData('tags-hot-home', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/hot`, {
      params: { limit: 10 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

articles.value = feedData.value
hotRankItems.value = rankData.value
recommendUsers.value = userData.value
bannerList.value = bannerData.value
announcementList.value = announcementData.value
hotTags.value = tagData.value

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>
