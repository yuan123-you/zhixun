<template>
  <!-- 分类页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <div class="flex items-center gap-3 mb-6">
      <button class="p-1.5 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="navigateTo('/')">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <span class="w-3 h-3 rounded-full shrink-0" :class="categoryColor"></span>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ categoryLabel }}</h1>
    </div>

    <!-- 分类Tab -->
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

    <!-- 文章列表 -->
    <ArticleList
      :articles="articles"
      :loading="loading"
      :has-more="hasMore"
      :error="error"
      @load-more="loadMore"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
/** 分类页：按分类显示文章列表 */
import type { Article, PageResult, ApiResponse } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const { t } = useI18n()

const slug = computed(() => route.params.slug as string)

// 分类映射
const categoryMap: Record<string, { label: string; color: string; id: number }> = {
  tech: { label: t('nav.tech'), color: 'bg-blue-500', id: 1 },
  design: { label: t('nav.design'), color: 'bg-purple-500', id: 2 },
  product: { label: t('nav.product'), color: 'bg-green-500', id: 3 },
}

const categoryLabel = computed(() => categoryMap[slug.value]?.label || slug.value)
const categoryColor = computed(() => categoryMap[slug.value]?.color || 'bg-gray-500')
const categoryId = computed(() => categoryMap[slug.value]?.id)

const tabs = [
  { key: 'latest', label: computed(() => t('nav.latest')) },
  { key: 'hot', label: computed(() => t('nav.hot')) },
]

const activeTab = ref('latest')
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)

// 构建API基础URL
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  articles.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  fetchArticles()
}

// 获取文章列表
const fetchArticles = async () => {
  if (loading.value || !categoryId.value) return
  loading.value = true
  error.value = null

  try {
    const base = getApiBase()
    const sort = activeTab.value === 'hot' ? 'hot' : 'latest'
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/category/${categoryId.value}`, {
      params: { page: page.value, pageSize: 10, sort },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    const data = res.data
    const items = data?.list || []
    if (page.value === 1) {
      articles.value = items
    } else {
      articles.value.push(...items)
    }
    hasMore.value = items.length >= 10
  } catch {
    error.value = t('common.loadFailed')
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  page.value++
  try {
    await fetchArticles()
  } catch {
    page.value--
  }
}

// 重试
const handleRetry = () => {
  error.value = null
  fetchArticles()
}

// 监听slug变化
watch(slug, () => {
  articles.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  fetchArticles()
})

// SSR数据获取
const { data: initialData } = await useAsyncData(`category-${slug.value}`, async () => {
  if (!categoryId.value) return []
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/category/${categoryId.value}`, {
      params: { page: 1, pageSize: 10, sort: 'latest' },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res.data?.list || []
  } catch {
    return []
  }
}, { default: () => [] as Article[] })

articles.value = initialData.value

useHead({
  title: () => `${categoryLabel.value} - 知讯`,
})
</script>
