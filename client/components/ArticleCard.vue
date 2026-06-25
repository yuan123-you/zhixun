<template>
  <!-- 文章卡片组件 - 微博风格布局 -->
  <article class="card px-3 py-2.5 md:px-4 md:py-3 hover:shadow-md transition-shadow cursor-pointer no-tap-highlight touch-feedback" @click="navigateToDetail">
    <!-- 作者信息栏（标题上方） -->
    <div class="flex items-center gap-2.5 mb-2">
      <!-- 作者头像：大小等于姓名盒子+发布信息盒子的高度 -->
      <NuxtLink :to="`/user/${article.author?.id}`" class="shrink-0" @click.stop>
        <UserAvatar :src="article.authorAvatar || article.author?.avatar" :alt="article.authorName || article.author?.nickname" size="md" />
      </NuxtLink>
      <div class="flex-1 min-w-0">
        <!-- 作者姓名 -->
        <NuxtLink :to="`/user/${article.author?.id}`" class="text-sm font-medium text-gray-900 dark:text-white hover:text-primary transition-colors truncate block" @click.stop>
          {{ article.authorName || article.author?.nickname }}
        </NuxtLink>
        <!-- 发布时间 + 设备信息 + 匹配类型标签 -->
        <p class="text-xs text-gray-400 dark:text-gray-500 truncate">
          <time>{{ formatTimestamp(article.createdAt) }}</time>
          <span v-if="article.deviceInfo" class="ml-1">来自{{ article.deviceInfo }}</span>
          <span v-if="article.matchType" class="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium"
            :class="matchTypeStyle">
            {{ matchTypeLabel }}
          </span>
        </p>
      </div>
    </div>

    <!-- 文章内容区 -->
    <div class="flex gap-3">
      <div class="flex-1 min-w-0">
        <!-- 标题（支持高亮HTML） -->
        <h3 v-if="article.matchType === 'title'" class="text-sm md:text-base font-semibold text-gray-900 dark:text-white line-clamp-2 mb-1.5" v-html="article.title" />
        <h3 v-else class="text-sm md:text-base font-semibold text-gray-900 dark:text-white line-clamp-2 mb-1.5">
          {{ article.title }}
        </h3>

        <!-- 摘要（支持高亮HTML） -->
        <p v-if="article.matchType === 'summary' && article.summary" class="text-xs md:text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-2" v-html="article.summary" />
        <p v-else-if="article.summary" class="text-xs md:text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-2">
          {{ article.summary }}
        </p>

        <!-- 正文内容片段（搜索结果中显示，支持高亮HTML） -->
        <div v-if="article.contentSnippet" class="text-xs md:text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-2 search-snippet" v-html="article.contentSnippet" />
      </div>

      <!-- 封面图（右侧，sm以上显示） -->
      <div v-if="article.coverImage" class="hidden sm:block w-24 md:w-28 h-20 md:h-24 shrink-0">
        <img :src="resolveUrl(article.coverImage) || ''" :alt="typeof article.title === 'string' ? article.title : ''" class="w-full h-full object-cover rounded-lg" />
      </div>
    </div>

    <!-- 文章互动部分（内容下方） -->
    <div class="flex items-center mt-2 -ml-2">
      <!-- 点赞 -->
      <button
        class="flex items-center gap-1 px-2 py-1.5 rounded-full text-xs transition-colors touch-target"
        :class="article.isLiked ? 'text-primary' : 'text-gray-400 dark:text-gray-500 hover:text-primary'"
        @click.stop="handleToggleLike"
      >
        <svg class="w-4 h-4" :class="article.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
        <span>{{ formatCount(article.likeCount) }}</span>
      </button>

      <!-- 评论 -->
      <button
        class="flex items-center gap-1 px-2 py-1.5 rounded-full text-xs text-gray-400 dark:text-gray-500 hover:text-primary transition-colors touch-target"
        @click.stop="navigateToDetail"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span>{{ formatCount(article.commentCount) }}</span>
      </button>

      <!-- 分享/转发 -->
      <button
        class="flex items-center gap-1 px-2 py-1.5 rounded-full text-xs text-gray-400 dark:text-gray-500 hover:text-primary transition-colors touch-target"
        @click.stop="handleShare"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
        </svg>
        <span>{{ formatCount(article.shareCount) }}</span>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
/** 文章卡片组件 - 微博风格布局，支持搜索结果高亮 */
import type { Article } from '~/types'

const props = defineProps<{
  article: Article
}>()

const { t } = useI18n()
const { resolveUrl } = useResourceUrl()
const userStore = useUserStore()
const { invalidateArticle } = useCacheInvalidation()

// 匹配类型标签
const matchTypeLabel = computed(() => {
  switch (props.article.matchType) {
    case 'title': return t('search.matchTitle')
    case 'summary': return t('search.matchSummary')
    case 'content': return t('search.matchContent')
    default: return ''
  }
})

// 匹配类型样式
const matchTypeStyle = computed(() => {
  switch (props.article.matchType) {
    case 'title': return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
    case 'summary': return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
    case 'content': return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300'
    default: return ''
  }
})

// 跳转到文章详情
const navigateToDetail = () => {
  navigateTo(`/articles/${props.article.id}`)
}

// 格式化标准时间戳
const formatTimestamp = (time: string | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

// 格式化数量
const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}

// 点赞
const handleToggleLike = async () => {
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.toggleLike(props.article.id)
    props.article.isLiked = response.data.data.isLiked
    props.article.likeCount = response.data.data.likeCount
    invalidateArticle()
  } catch {
    // 点赞失败静默处理
  }
}

// 分享
const handleShare = async () => {
  try {
    const url = `${window.location.origin}/articles/${props.article.id}`
    await navigator.clipboard.writeText(url)
    // 记录分享统计
    const { articleApi } = await import('~/api')
    await articleApi.recordShare(props.article.id, 'link')
  } catch {
    // 分享失败静默处理
  }
}
</script>

<style scoped>
/* 搜索结果高亮样式 */
.search-snippet :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb, 59, 130, 246), 0.1);
  border-radius: 2px;
  padding: 0 2px;
}

/* 标题高亮样式 */
h3 :deep(em) {
  font-style: normal;
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: underline;
  text-decoration-color: rgba(var(--color-primary-rgb, 59, 130, 246), 0.3);
  text-underline-offset: 2px;
}

/* 摘要高亮样式 */
p :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb, 59, 130, 246), 0.1);
  border-radius: 2px;
  padding: 0 2px;
}
</style>
