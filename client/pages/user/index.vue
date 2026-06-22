<template>
  <!-- 个人中心 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 个人资料卡 -->
    <div class="card p-6 mb-6">
      <div class="flex items-start space-x-4">
        <!-- 头像 -->
        <img :src="userStore.userInfo?.avatar || '/default-avatar.png'" class="w-20 h-20 rounded-full object-cover shrink-0" alt="头像" />

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-bold text-gray-900 dark:text-white">{{ userStore.userInfo?.nickname }}</h2>
            <NuxtLink to="/user/settings" class="btn-secondary text-sm">编辑资料</NuxtLink>
          </div>
          <p v-if="userStore.userInfo?.bio" class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ userStore.userInfo?.bio }}</p>
          <div class="flex items-center space-x-6 mt-3 text-sm text-gray-500 dark:text-gray-400">
            <span><strong class="text-gray-900 dark:text-white">{{ userStore.userInfo?.articleCount }}</strong> 文章</span>
            <span><strong class="text-gray-900 dark:text-white">{{ userStore.userInfo?.followCount }}</strong> 关注</span>
            <span><strong class="text-gray-900 dark:text-white">{{ userStore.userInfo?.followerCount }}</strong> 粉丝</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab切换 -->
    <div class="flex items-center border-b border-gray-200 dark:border-gray-700 mb-6 overflow-x-auto no-scrollbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors shrink-0"
        :class="activeTab === tab.key
          ? 'border-primary text-primary'
          : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab内容 -->
    <div>
      <!-- 我发布的 -->
      <ArticleList v-if="activeTab === 'published'" :articles="publishedArticles" :loading="loading" :has-more="hasMore" @load-more="loadMore" />

      <!-- 我的收藏 -->
      <ArticleList v-if="activeTab === 'collected'" :articles="collectedArticles" :loading="loading" :has-more="hasMore" @load-more="loadMore" />

      <!-- 我的点赞 -->
      <ArticleList v-if="activeTab === 'liked'" :articles="likedArticles" :loading="loading" :has-more="hasMore" @load-more="loadMore" />

      <!-- 我的评论 -->
      <div v-if="activeTab === 'comments'" class="space-y-4">
        <div v-for="comment in myComments" :key="comment.id" class="card p-4">
          <p class="text-sm text-gray-700 dark:text-gray-300">{{ comment.content }}</p>
          <div class="flex items-center justify-between mt-2">
            <span class="text-xs text-gray-400">{{ formatDate(comment.createdAt) }}</span>
            <NuxtLink :to="`/articles/${comment.articleId}`" class="text-xs text-primary hover:text-primary-600">查看文章</NuxtLink>
          </div>
        </div>
        <EmptyState v-if="!loading && myComments.length === 0" title="暂无评论" />
      </div>

      <!-- 浏览历史 -->
      <ArticleList v-if="activeTab === 'history'" :articles="historyArticles" :loading="loading" :has-more="hasMore" @load-more="loadMore" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 个人中心页 */
import type { Article, Comment } from '~/types'

definePageMeta({
  middleware: 'auth',
})

const userStore = useUserStore()

const tabs = [
  { key: 'published', label: '我发布的' },
  { key: 'collected', label: '我的收藏' },
  { key: 'liked', label: '我的点赞' },
  { key: 'comments', label: '我的评论' },
  { key: 'history', label: '浏览历史' },
]

const activeTab = ref('published')
const loading = ref(false)
const hasMore = ref(true)
const publishedArticles = ref<Article[]>([])
const collectedArticles = ref<Article[]>([])
const likedArticles = ref<Article[]>([])
const myComments = ref<Comment[]>([])
const historyArticles = ref<Article[]>([])

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  // 加载对应Tab数据
  loadTabData()
}

// 加载Tab数据
const loadTabData = async () => {
  loading.value = true
  try {
    // TODO: 根据activeTab加载不同数据
  } catch {
    // 加载失败处理
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  // 加载更多数据
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面元信息
useHead({
  title: '个人中心 - 知讯',
})
</script>
