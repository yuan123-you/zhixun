<template>
  <!-- 首页 -->
  <div class="max-w-7xl mx-auto px-4 py-6">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- Tab切换 -->
        <div class="flex items-center border-b border-gray-200 dark:border-gray-700 mb-6">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-4 py-3 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === tab.key
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 文章列表 -->
        <ArticleList
          :articles="articles"
          :loading="loading"
          :has-more="hasMore"
          @load-more="loadMore"
        />
      </div>

      <!-- 右侧栏（桌面端） -->
      <aside class="hidden lg:block w-80 shrink-0 space-y-6">
        <!-- 热榜 -->
        <HotRank :items="hotRankItems" />

        <!-- 推荐用户 -->
        <div class="card">
          <div class="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="font-semibold text-gray-900 dark:text-white">推荐关注</h3>
          </div>
          <div class="p-4 space-y-4">
            <UserCard v-for="user in recommendUsers" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 首页：推荐/最新/关注三个Tab，文章卡片列表，右侧热榜/推荐栏 */

import type { Article, User, RankItem } from '~/types'

const userStore = useUserStore()
const config = useRuntimeConfig()

// Tab配置
const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'latest', label: '最新' },
  { key: 'following', label: '关注' },
]

const activeTab = ref('recommend')
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)
const hotRankItems = ref<RankItem[]>([])
const recommendUsers = ref<User[]>([])

// 构建API基础URL
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 通用分页数据获取
const fetchPage = async (url: string, params: Record<string, any> = {}) => {
  const base = getApiBase()
  const query = new URLSearchParams({ page: String(params.page || 1), pageSize: String(params.pageSize || 20), ...params })
  const res = await $fetch<{ code: number; data: { list?: any[]; items?: any[]; total: number; page: number; page_size: number } }>(`${base}${url}`, { params: Object.fromEntries(query) })
  return res.data
}

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  articles.value = []
  page.value = 1
  hasMore.value = true
  fetchArticles()
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
    switch (activeTab.value) {
      case 'latest':
        url = '/feed/latest'
        break
      case 'following':
        url = '/feed/following'
        break
      default:
        url = '/feed/recommend'
    }

    const data = await fetchPage(url, { page: page.value, pageSize })
    const items = data?.list || data?.items || []
    if (page.value === 1) {
      articles.value = items
    } else {
      articles.value.push(...items)
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
  const { socialApi } = await import('~/api')
  await socialApi.toggleFollow(userId)
}

// SSR数据获取 - 使用 useAsyncData + $fetch 确保SSR正确渲染
// SSR时通过 nitro routeRules proxy /api -> http://server:8080/api
// 客户端时通过 nginx 代理 /api -> http://server:8080/api
const { data: feedData } = await useAsyncData('feed-recommend', async () => {
  try {
    const res = await $fetch<{ code: number; data: { list?: any[]; items?: any[] } }>('/api/v1/feed/recommend', {
      params: { page: 1, pageSize: 20 },
    })
    return res?.data?.list || res?.data?.items || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: rankData } = await useAsyncData('rank-hot', async () => {
  try {
    const res = await $fetch<{ code: number; data: any[] }>('/api/v1/rank/hot', {
      params: { period: 'daily' },
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

articles.value = feedData.value
hotRankItems.value = rankData.value

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>
