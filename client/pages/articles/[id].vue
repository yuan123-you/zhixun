<template>
  <!-- 文章详情页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 加载状态 -->
    <LoadingSkeleton v-if="pending" type="article" />

    <!-- 文章内容 -->
    <article v-else-if="article" class="animate-fade-in">
      <!-- 文章标题 -->
      <h1 class="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white mb-4">
        {{ article.title }}
      </h1>

      <!-- 作者信息 -->
      <div class="flex items-center space-x-3 mb-6">
        <NuxtLink :to="`/user/${article.author?.id}`">
          <img :src="article.author?.avatar || '/default-avatar.png'" :alt="article.author?.nickname" class="w-10 h-10 rounded-full object-cover" />
        </NuxtLink>
        <div>
          <NuxtLink :to="`/user/${article.author?.id}`" class="text-sm font-medium text-gray-900 dark:text-white hover:text-primary transition-colors">
            {{ article.author?.nickname }}
          </NuxtLink>
          <p class="text-xs text-gray-500 dark:text-gray-400">
            {{ formatDate(article.createdAt) }} · 阅读 {{ article.viewCount }}
          </p>
        </div>
        <button
          v-if="article.author?.id !== userStore.userInfo?.id"
          class="ml-auto btn text-sm"
          :class="article.author?.isFollowing ? 'btn-secondary' : 'btn-primary'"
          @click="toggleFollowAuthor"
        >
          {{ article.author?.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>

      <!-- 封面图 -->
      <div v-if="article.coverImage" class="mb-6 rounded-lg overflow-hidden">
        <img :src="article.coverImage" :alt="article.title" class="w-full max-h-96 object-cover" />
      </div>

      <!-- 文章内容（富文本渲染） -->
      <div class="prose dark:prose-invert max-w-none mb-8" v-html="article.content"></div>

      <!-- 标签 -->
      <div v-if="article.tags?.length" class="flex flex-wrap gap-2 mb-6">
        <span v-for="tag in article.tags" :key="tag.id" class="badge-primary">
          # {{ tag.name }}
        </span>
      </div>

      <!-- 互动按钮栏 -->
      <div class="flex items-center justify-center space-x-8 py-6 border-y border-gray-200 dark:border-gray-700 mb-8">
        <!-- 点赞 -->
        <button class="flex flex-col items-center space-y-1 transition-colors" :class="article.isLiked ? 'text-primary' : 'text-gray-500 dark:text-gray-400 hover:text-primary'" @click="toggleLike">
          <svg class="w-6 h-6" :class="article.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span class="text-xs">{{ article.likeCount }}</span>
        </button>

        <!-- 收藏 -->
        <button class="flex flex-col items-center space-y-1 transition-colors" :class="article.isCollected ? 'text-accent' : 'text-gray-500 dark:text-gray-400 hover:text-accent'" @click="toggleCollect">
          <svg class="w-6 h-6" :class="article.isCollected ? 'fill-accent' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
          </svg>
          <span class="text-xs">{{ article.collectCount }}</span>
        </button>

        <!-- 评论 -->
        <button class="flex flex-col items-center space-y-1 text-gray-500 dark:text-gray-400 hover:text-primary transition-colors">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span class="text-xs">{{ article.commentCount }}</span>
        </button>
      </div>

      <!-- 评论区 -->
      <section>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">评论 ({{ article.commentCount }})</h2>
        <CommentSection
          :article-id="article.id"
          :comments="comments"
          :has-more="hasMoreComments"
          @submit="submitComment"
          @like="likeComment"
          @load-more="loadMoreComments"
        />
      </section>

      <!-- 相关推荐 -->
      <section v-if="relatedArticles.length" class="mt-8">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">相关推荐</h2>
        <div class="space-y-4">
          <ArticleCard v-for="a in relatedArticles" :key="a.id" :article="a" />
        </div>
      </section>
    </article>
  </div>
</template>

<script setup lang="ts">
/** 文章详情页：SSR渲染 */
import type { Article, Comment } from '~/types'

const route = useRoute()
const userStore = useUserStore()
const articleId = computed(() => Number(route.params.id))

// 文章数据
const comments = ref<Comment[]>([])
const hasMoreComments = ref(true)
const relatedArticles = ref<Article[]>([])

// SSR获取文章详情
const { data: article, pending } = await useAsyncData(
  `article-${articleId.value}`,
  async () => {
    const { articleApi } = await import('~/api')
    const response = await articleApi.getArticleDetail(articleId.value)
    return response.data.data
  }
)

// 点赞
const toggleLike = async () => {
  if (!article.value) return
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.toggleLike(article.value.id)
  article.value.isLiked = response.data.data.isLiked
  article.value.likeCount = response.data.data.likeCount
}

// 收藏
const toggleCollect = async () => {
  if (!article.value) return
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.toggleCollect(article.value.id)
  article.value.isCollected = response.data.data.isCollected
  article.value.collectCount = response.data.data.collectCount
}

// 关注作者
const toggleFollowAuthor = async () => {
  if (!article.value?.author) return
  const { socialApi } = await import('~/api')
  await socialApi.toggleFollow(article.value.author.id)
  article.value.author.isFollowing = !article.value.author.isFollowing
}

// 提交评论
const submitComment = async (data: { content: string; parentId?: number }) => {
  if (!article.value) return
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.createComment(article.value.id, data)
  comments.value.unshift(response.data.data)
  article.value.commentCount++
}

// 点赞评论
const likeComment = async (commentId: number) => {
  // 评论点赞逻辑
}

// 加载更多评论
const loadMoreComments = () => {
  // 加载更多评论
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// 页面元信息
useHead({
  title: () => article.value ? `${article.value.title} - 知讯` : '文章详情 - 知讯',
})
</script>
