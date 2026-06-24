<template>
  <!-- 排行榜页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">{{ t('hotRank.title') }}</h1>

    <!-- 日榜/周榜/月榜切换 -->
    <div class="flex items-center space-x-2 mb-6">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
        :class="activeTab === tab.key
          ? 'bg-primary text-white'
          : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 排行列表 -->
    <div v-if="rankItems.length > 0" class="space-y-3">
      <div
        v-for="(item, index) in rankItems"
        :key="item.id"
        class="card p-4 flex items-center space-x-4 hover:shadow-md transition-shadow cursor-pointer"
        @click="navigateTo(`/articles/${item.id}`)"
      >
        <!-- 排名 -->
        <div class="w-8 h-8 rounded-full flex items-center justify-center shrink-0 font-bold text-sm" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>

        <!-- 文章信息 -->
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white line-clamp-1">{{ item.title }}</h3>
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
          <p class="text-2xs text-gray-400">{{ t('article.heat') }}</p>
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
    <EmptyState v-if="!loading && !error && rankItems.length === 0" :title="t('hotRank.empty')" />
  </div>
</template>

<script setup lang="ts">
/** 排行榜页：日榜/周榜/月榜 */
import type { RankItem } from '~/types'

const { t } = useI18n()

const tabs = [
  { key: 'daily', label: computed(() => t('hotRank.daily')) },
  { key: 'weekly', label: computed(() => t('hotRank.weekly')) },
  { key: 'monthly', label: computed(() => t('hotRank.monthly')) },
]

const activeTab = ref('daily')
const rankItems = ref<RankItem[]>([])
const loading = ref(false)
const error = ref('')

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

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
    const { rankApi } = await import('~/api')
    const data = await cachedRequest(
      (params: { period: string }) => rankApi.getHotRank(params.period),
      '/rank/hot',
      { period: activeTab.value },
    )
    rankItems.value = data.data.data
  } catch {
    error.value = t('hotRank.loadFailed')
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
  return 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
}

// 格式化热度值
const formatHeat = (score: number) => {
  if (score >= 10000) return `${(score / 10000).toFixed(1)}万`
  return score.toString()
}

// SSR数据获取
const { data: initialData } = await useAsyncData('rank-init', async () => {
  const { rankApi } = await import('~/api')
  const response = await rankApi.getHotRank('daily')
  return response.data.data
})

if (initialData.value) {
  rankItems.value = initialData.value
}

// 页面元信息
useHead({
  title: () => t('hotRank.title') + ' - 知讯',
})
</script>
