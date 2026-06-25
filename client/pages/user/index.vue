<template>
  <!-- 个人中心 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 返回导航 -->
    <button class="flex items-center gap-1 text-sm text-slate-500 hover:text-primary-600 transition-colors mb-2" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      {{ '返回' }}
    </button>

    <!-- 个人资料卡 -->
    <div class="card p-3 mb-3">
      <div class="flex items-start space-x-3">
        <!-- 头像 -->
        <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" size="xl" />

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-bold text-slate-900">{{ userStore.userInfo?.nickname }}</h2>
            <NuxtLink to="/user/settings" class="btn-secondary text-sm">{{ '编辑资料' }}</NuxtLink>
          </div>
          <p v-if="userStore.userInfo?.bio" class="text-sm text-slate-500 mt-1">{{ userStore.userInfo?.bio }}</p>
          <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
            <span><strong class="text-slate-900">{{ userStore.userInfo?.articleCount }}</strong> {{ '文章' }}</span>
            <span><strong class="text-slate-900">{{ userStore.userInfo?.followCount }}</strong> {{ '关注' }}</span>
            <span><strong class="text-slate-900">{{ userStore.userInfo?.followerCount }}</strong> {{ '粉丝' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab切换 -->
    <div class="flex items-center border-b border-slate-200 mb-3 overflow-x-auto no-scrollbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-3 py-2 text-sm font-medium border-b-2 whitespace-nowrap transition-colors shrink-0"
        :class="activeTab === tab.key
          ? 'border-primary text-primary'
          : 'border-transparent text-slate-500 hover:text-slate-700'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab内容 -->
    <div>
      <!-- 我发布的 -->
      <ErrorRetry v-if="activeTab === 'published' && tabError && !publishedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <ArticleList v-if="activeTab === 'published'" :articles="publishedArticles" :loading="loading" :has-more="hasMore" :error="tabError" @load-more="loadMore" @retry="retryTabData" />

      <!-- 我的收藏 -->
      <ErrorRetry v-if="activeTab === 'collected' && tabError && !collectedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <ArticleList v-if="activeTab === 'collected'" :articles="collectedArticles" :loading="loading" :has-more="hasMore" :error="tabError" @load-more="loadMore" @retry="retryTabData" />

      <!-- 我的点赞 -->
      <ErrorRetry v-if="activeTab === 'liked' && tabError && !likedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <ArticleList v-if="activeTab === 'liked'" :articles="likedArticles" :loading="loading" :has-more="hasMore" :error="tabError" @load-more="loadMore" @retry="retryTabData" />

      <!-- 我的评论 -->
      <ErrorRetry v-if="activeTab === 'comments' && tabError && !myComments.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'comments' && !tabError" class="space-y-4">
        <div v-for="comment in myComments" :key="comment.id" class="card p-2">
          <p class="text-sm text-slate-700">{{ comment.content }}</p>
          <div class="flex items-center justify-between mt-2">
            <span class="text-xs text-gray-400">{{ formatDate(comment.createdAt) }}</span>
            <NuxtLink :to="`/articles/${comment.articleId}`" class="text-xs text-primary hover:text-primary-600">{{ '查看文章' }}</NuxtLink>
          </div>
        </div>
        <EmptyState v-if="!loading && myComments.length === 0" title="暂无评论" />
      </div>

      <!-- 浏览历史 -->
      <ErrorRetry v-if="activeTab === 'history' && tabError && !historyArticles.length" :message="tabError" :on-retry="retryTabData" />
      <ArticleList v-if="activeTab === 'history'" :articles="historyArticles" :loading="loading" :has-more="hasMore" :error="tabError" @load-more="loadMore" @retry="retryTabData" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 个人中心页 */
import type { Article, Comment } from '~/types'
import { userApi } from '~/api'

definePageMeta({
  middleware: 'auth',
})

const userStore = useUserStore()
const router = useRouter()

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}

const tabs = [
  { key: 'published', label: computed(() => '我发布的') },
  { key: 'collected', label: computed(() => '我的收藏') },
  { key: 'liked', label: computed(() => '我的点赞') },
  { key: 'comments', label: computed(() => '我的评论') },
  { key: 'history', label: computed(() => '浏览历史') },
]

const activeTab = ref('published')
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 10
const tabError = ref<string | null>(null)

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

const publishedArticles = ref<Article[]>([])
const collectedArticles = ref<Article[]>([])
const likedArticles = ref<Article[]>([])
const myComments = ref<Comment[]>([])
const historyArticles = ref<Article[]>([])

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  hasMore.value = true
  currentPage.value = 1
  loadTabData()
}

// 重试加载Tab数据
const retryTabData = () => {
  tabError.value = null
  loadTabData()
}

// 获取当前Tab对应的数据引用
const getCurrentData = (): { articles?: Ref<Article[]>; comments?: Ref<Comment[]> } => {
  switch (activeTab.value) {
    case 'published': return { articles: publishedArticles }
    case 'collected': return { articles: collectedArticles }
    case 'liked': return { articles: likedArticles }
    case 'comments': return { comments: myComments }
    case 'history': return { articles: historyArticles }
    default: return {}
  }
}

// 加载Tab数据
const loadTabData = async () => {
  loading.value = true
  tabError.value = null
  try {
    const params = { page: currentPage.value, pageSize }
    switch (activeTab.value) {
      case 'published': {
        const { data } = await cachedRequest(userApi.getMyArticles, '/user/articles', params)
        publishedArticles.value = data.data.list || []
        hasMore.value = publishedArticles.value.length < data.data.total
        break
      }
      case 'collected': {
        const { data } = await cachedRequest(userApi.getMyCollections, '/user/collections', params)
        collectedArticles.value = data.data.list || []
        hasMore.value = collectedArticles.value.length < data.data.total
        break
      }
      case 'liked': {
        const { data } = await cachedRequest(userApi.getMyLikes, '/user/likes', params)
        likedArticles.value = data.data.list || []
        hasMore.value = likedArticles.value.length < data.data.total
        break
      }
      case 'comments': {
        const { data } = await cachedRequest(userApi.getMyComments, '/user/comments', params)
        myComments.value = data.data.list || []
        hasMore.value = myComments.value.length < data.data.total
        break
      }
      case 'history': {
        const { data } = await cachedRequest(userApi.getViewHistory, '/user/history', params)
        historyArticles.value = data.data.list || []
        hasMore.value = historyArticles.value.length < data.data.total
        break
      }
    }
  } catch {
    tabError.value = '数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  loading.value = true
  try {
    const params = { page: currentPage.value, pageSize }
    switch (activeTab.value) {
      case 'published': {
        const { data } = await userApi.getMyArticles(params)
        publishedArticles.value.push(...(data.data.list || []))
        hasMore.value = publishedArticles.value.length < data.data.total
        break
      }
      case 'collected': {
        const { data } = await userApi.getMyCollections(params)
        collectedArticles.value.push(...(data.data.list || []))
        hasMore.value = collectedArticles.value.length < data.data.total
        break
      }
      case 'liked': {
        const { data } = await userApi.getMyLikes(params)
        likedArticles.value.push(...(data.data.list || []))
        hasMore.value = likedArticles.value.length < data.data.total
        break
      }
      case 'comments': {
        const { data } = await userApi.getMyComments(params)
        myComments.value.push(...(data.data.list || []))
        hasMore.value = myComments.value.length < data.data.total
        break
      }
      case 'history': {
        const { data } = await userApi.getViewHistory(params)
        historyArticles.value.push(...(data.data.list || []))
        hasMore.value = historyArticles.value.length < data.data.total
        break
      }
    }
  } catch {
    currentPage.value--
    tabError.value = '加载更多失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面加载时获取默认Tab数据
onMounted(() => {
  loadTabData()
})

// 页面元信息
useHead({
  title: () => '个人中心' + ' - 知讯',
})
</script>
