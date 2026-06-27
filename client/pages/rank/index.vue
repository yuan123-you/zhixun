<template>
  <!-- 排行榜页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">

    <!-- 日榜/周榜/月榜切换 -->
    <div class="flex items-center space-x-2 mb-2">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
        :class="activeTab === tab.key
          ? 'bg-primary text-white'
          : 'bg-slate-50 text-slate-700 hover:bg-slate-200'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 排行列表 -->
    <div v-if="rankItems.length > 0" class="space-y-1.5">
      <div
        v-for="(item, index) in rankItems"
        :key="item.id"
        class="card px-2 py-1.5 flex items-center space-x-3 hover:shadow-[var(--shadow-md)] transition-shadow cursor-pointer"
        @click="navigateTo(`/articles/${item.id}`)"
      >
        <!-- 排名 -->
        <div class="w-8 h-8 rounded-full flex items-center justify-center shrink-0 font-bold text-sm" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>

        <!-- 作品信息 -->
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-slate-900 line-clamp-1">{{ item.title }}</h3>
          <div class="flex items-center space-x-4 mt-1 text-xs text-gray-400">
            <span>{{ item.authorNickname }}</span>
            <span class="flex items-center space-x-0.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              <span>{{ item.likeCount }}</span>
            </span>
            <span class="flex items-center space-x-0.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span>{{ item.commentCount }}</span>
            </span>
          </div>
        </div>

        <!-- 热度值 -->
        <div class="text-right shrink-0">
          <span class="text-sm font-bold text-accent">{{ formatHeat(item.score ?? 0) }}</span>
          <p class="text-2xs text-gray-400">{{ '热度' }}</p>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <template v-if="loading">
      <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
    </template>

    <!-- 错误状态 -->
    <ErrorRetry v-if="error && !loading" :message="error" :on-retry="retryLastRequest" />

    <!-- 空状态 -->
    <EmptyState v-if="!loading && !error && rankItems.length === 0" title="暂无热榜数据" />
  </div>
</template>

<script setup lang="ts">
/** 排行榜页：日榜/周榜/月榜 */
import type { RankItem, ApiResponse } from '~/types'

const { setTitle } = usePageHeaderTitle()
setTitle('热榜')

const tabs = [
  { key: 'daily', label: computed(() => '日榜') },
  { key: 'weekly', label: computed(() => '周榜') },
  { key: 'monthly', label: computed(() => '月榜') },
]

const activeTab = ref('daily')
const rankItems = ref<RankItem[]>([])
const loading = ref(false)
const error = ref('')

const config = useRuntimeConfig()

// 构建API基础URL：SSR时使用内部地址，客户端时走Nginx代理
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  fetchRank()
}

// 获取排行数据
const fetchRank = async () => {
  loading.value = true
  error.value = ''
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<RankItem[]>>(`${base}/rank/hot`, {
      params: { period: activeTab.value },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    rankItems.value = res?.data || []
  } catch {
    error.value = '加载排行榜失败，请稍后重试'
    rankItems.value = []
  } finally {
    loading.value = false
  }
}

// 重试上次失败的请求
const retryLastRequest = () => {
  fetchRank()
}

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'bg-yellow-400 text-white'
  if (index === 1) return 'bg-gray-300 text-white'
  if (index === 2) return 'bg-orange-400 text-white'
  return 'bg-slate-50 text-slate-500'
}

// 格式化热度值
const formatHeat = (score: number) => {
  if (score >= 10000) return `${(score / 10000).toFixed(1)}万`
  if (score >= 100) return Math.round(score).toString()
  if (score >= 1) return score.toFixed(1)
  return score.toFixed(2)
}

// SSR数据获取
const { data: initialData } = await useAsyncData('rank-init', async () => {
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
})

rankItems.value = initialData.value || []

// 页面元信息
useHead({
  title: () => '热榜' + ' - 知讯',
})
</script>
