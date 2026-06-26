<template>
  <!-- 我的作品列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <div class="flex items-center justify-between mb-3">
      <h1 class="text-xl font-bold text-slate-900">我的作品</h1>
      <span class="text-sm text-slate-500">共 {{ totalCount }} 篇</span>
    </div>
    <ErrorRetry v-if="error && !articles.length" :message="error" :on-retry="fetchArticles" />
    <ArticleList
      :articles="articles"
      :loading="loading"
      :has-more="hasMore"
      :error="error"
      @load-more="loadMore"
      @retry="fetchArticles"
    />
  </div>
</template>

<script setup lang="ts">
/** 我的作品页面 */
import type { Article, PageResult } from '~/types'
import { userApi } from '~/api'

definePageMeta({
  middleware: 'auth',
})

const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const totalCount = ref(0)
const pageSize = 10

const fetchArticles = async () => {
  if (loading.value) return
  loading.value = true
  error.value = null
  try {
    const { data } = await userApi.getMyArticles({ page: page.value, pageSize })
    const result = data.data as PageResult<Article>
    if (page.value === 1) {
      articles.value = result.list || []
    } else {
      articles.value.push(...(result.list || []))
    }
    totalCount.value = result.total || 0
    hasMore.value = articles.value.length < totalCount.value
  } catch {
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  page.value++
  await fetchArticles()
  if (error.value) page.value--
}

onMounted(() => {
  fetchArticles()
})

useHead({
  title: () => '我的作品 - 知讯',
})
</script>
