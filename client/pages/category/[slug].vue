<template>
  <!-- 分类页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">

    <!-- 分类Tab -->
    <div class="flex items-center space-x-2 mb-3">
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

    <!-- 作品列表 -->
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
/** 分类页：按分类显示作品列表 */
import type { Article, PageResult, ApiResponse } from '~/types'

const route = useRoute()
const config = useRuntimeConfig()
const slug = computed(() => route.params.slug as string)

// 分类映射
const categoryMap: Record<string, { label: string; color: string; id: number }> = {
  tech: { label: '技术', color: 'bg-blue-500', id: 1 },
  design: { label: '设计', color: 'bg-purple-500', id: 2 },
  product: { label: '产品', color: 'bg-green-500', id: 3 },
}

const categoryLabel = computed(() => categoryMap[slug.value]?.label || slug.value)
const { setTitle } = usePageHeaderTitle()
watchEffect(() => { setTitle(categoryLabel.value) })
const categoryColor = computed(() => categoryMap[slug.value]?.color || 'bg-gray-500')
const categoryId = computed(() => categoryMap[slug.value]?.id)

const tabs = [
  { key: 'latest', label: computed(() => '最新') },
  { key: 'hot', label: computed(() => '排行') },
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

// 获取作品列表 - 使用 /articles?categoryId= 接口
const fetchArticles = async () => {
  if (loading.value || !categoryId.value) return
  loading.value = true
  error.value = null

  try {
    const base = getApiBase()
    const sort = activeTab.value === 'hot' ? 'view_count' : 'created_at'
    const order = 'desc'
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/articles`, {
      params: {
        categoryId: categoryId.value,
        pageNum: page.value,
        pageSize: 10,
        sortBy: sort,
        sortOrder: order,
      },
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
    error.value = '加载失败，请稍后重试'
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
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/articles`, {
      params: {
        categoryId: categoryId.value,
        pageNum: 1,
        pageSize: 10,
        sortBy: 'created_at',
        sortOrder: 'desc',
      },
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
