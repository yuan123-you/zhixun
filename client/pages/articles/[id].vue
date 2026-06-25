<template>
  <!-- 文章详情页 -->
  <div class="max-w-[800px] md:max-w-[900px] 2xl:max-w-[1200px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 返回导航 -->
    <button class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-primary-400 transition-colors mb-4" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      {{ t('common.back') }}
    </button>

    <!-- 加载状态 -->
    <LoadingSkeleton v-if="pending" type="article" />

    <!-- 文章内容 -->
    <SwipeArticle v-else-if="article" :prev-id="prevArticleId" :next-id="nextArticleId">
    <article class="animate-fade-in 2xl:flex 2xl:gap-8">
      <!-- 文章主体 -->
      <div class="2xl:flex-1 2xl:min-w-0 2xl:max-w-[800px]">
      <!-- 文章标题 -->
      <h1 class="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white mb-4">
        {{ article.title }}
      </h1>

      <!-- 作者信息栏（微博风格） -->
      <div class="flex items-center gap-2.5 mb-4">
        <NuxtLink :to="`/user/${article.author?.id}`" class="shrink-0">
          <UserAvatar :src="article.author?.avatar" :alt="article.author?.nickname" size="md" />
        </NuxtLink>
        <div class="flex-1 min-w-0">
          <NuxtLink :to="`/user/${article.author?.id}`" class="text-sm font-medium text-gray-900 dark:text-white hover:text-primary transition-colors block">
            {{ article.author?.nickname }}
          </NuxtLink>
          <p class="text-xs text-gray-400 dark:text-gray-500">
            <time>{{ formatTimestamp(article.createdAt) }}</time>
            <span v-if="article.deviceInfo" class="ml-1">来自{{ article.deviceInfo }}</span>
          </p>
        </div>
        <button
          v-if="article.author?.id !== userStore.userInfo?.id"
          class="ml-auto btn text-sm"
          :class="article.author?.isFollowing ? 'btn-secondary' : 'btn-primary'"
          @click="toggleFollowAuthor"
        >
          {{ article.author?.isFollowing ? t('article.followed') : t('article.followBtn') }}
        </button>
      </div>

      <!-- 封面图 -->
      <div v-if="article.coverImage" class="mb-6 rounded-lg overflow-hidden cursor-pointer" @click="openImageZoom(resolveUrl(article.coverImage) || '', article.title)">
        <img :src="resolveUrl(article.coverImage) || ''" :alt="article.title" class="w-full max-h-96 object-cover" />
      </div>

      <!-- 文章内容（富文本渲染） -->
      <div ref="contentRef" class="prose dark:prose-invert max-w-none mb-8" v-html="article.content" @click="handleContentClick"></div>

      <!-- 标签 -->
      <div v-if="article.tags?.length" class="flex flex-wrap gap-2 mb-6">
        <span v-for="tag in article.tags" :key="tag.id" class="badge-primary">
          # {{ tag.name }}
        </span>
      </div>

      <!-- 互动按钮栏（微博风格） -->
      <div class="flex items-center justify-around py-4 border-y border-gray-200 dark:border-gray-700 mb-6">
        <!-- 点赞 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full transition-colors touch-target" :class="article.isLiked ? 'text-primary bg-primary/5' : 'text-gray-500 dark:text-gray-400 hover:text-primary hover:bg-primary/5'" @click="toggleLike">
          <svg class="w-5 h-5" :class="article.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span class="text-sm">{{ article.likeCount }}</span>
        </button>

        <!-- 评论 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full text-gray-500 dark:text-gray-400 hover:text-primary hover:bg-primary/5 transition-colors touch-target" @click="scrollToComments">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span class="text-sm">{{ article.commentCount }}</span>
        </button>

        <!-- 分享/转发 -->
        <div class="relative">
          <button class="flex items-center gap-1.5 px-4 py-2 rounded-full text-gray-500 dark:text-gray-400 hover:text-primary hover:bg-primary/5 transition-colors touch-target" @click="showSharePanel = !showSharePanel">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            <span class="text-sm">{{ article.shareCount || t('article.share') }}</span>
          </button>

          <!-- 分享面板 -->
          <Transition name="fade">
            <div v-if="showSharePanel" class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 p-3 w-56 z-10">
              <!-- 分享方式 -->
              <div class="grid grid-cols-4 gap-3 mb-3">
                <!-- 生成海报 -->
                <button class="flex flex-col items-center space-y-1" @click="openSharePoster">
                  <div class="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                    <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <span class="text-xs text-gray-600 dark:text-gray-300">海报</span>
                </button>

                <!-- 复制链接 -->
                <button class="flex flex-col items-center space-y-1" @click="copyLink">
                  <div class="w-10 h-10 bg-green-50 dark:bg-green-900/30 rounded-full flex items-center justify-center">
                    <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                    </svg>
                  </div>
                  <span class="text-xs text-gray-600 dark:text-gray-300">链接</span>
                </button>

                <!-- 微信 -->
                <button class="flex flex-col items-center space-y-1" @click="shareToPlatform('wechat')">
                  <div class="w-10 h-10 bg-green-50 dark:bg-green-900/30 rounded-full flex items-center justify-center">
                    <svg class="w-5 h-5 text-green-600" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05a6.42 6.42 0 01-.246-1.79c0-3.558 3.39-6.441 7.573-6.441.258 0 .509.025.764.042C16.626 4.834 13.004 2.188 8.691 2.188zm-2.6 4.408c.56 0 1.015.46 1.015 1.025 0 .566-.455 1.025-1.014 1.025-.56 0-1.015-.46-1.015-1.025 0-.566.456-1.025 1.015-1.025zm5.144 0c.56 0 1.015.46 1.015 1.025 0 .566-.456 1.025-1.015 1.025-.56 0-1.014-.46-1.014-1.025 0-.566.455-1.025 1.014-1.025zM16.1 9.273c-3.68 0-6.667 2.488-6.667 5.558 0 3.07 2.987 5.558 6.667 5.558.715 0 1.404-.108 2.055-.293a.697.697 0 01.58.08l1.377.807a.262.262 0 00.135.043c.13 0 .235-.108.235-.24 0-.059-.024-.116-.04-.173l-.282-1.07a.477.477 0 01.173-.539C21.913 18.478 22.9 16.77 22.9 14.83c0-3.07-2.988-5.558-6.668-5.558h-.132zm-2.3 3.283c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833zm4.6 0c.454 0 .822.373.822.833 0 .46-.368.833-.822.833a.828.828 0 01-.822-.833c0-.46.368-.833.822-.833z"/>
                    </svg>
                  </div>
                  <span class="text-xs text-gray-600 dark:text-gray-300">微信</span>
                </button>

                <!-- QQ -->
                <button class="flex flex-col items-center space-y-1" @click="shareToPlatform('qq')">
                  <div class="w-10 h-10 bg-blue-50 dark:bg-blue-900/30 rounded-full flex items-center justify-center">
                    <svg class="w-5 h-5 text-blue-500" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 13.2c-.18.53-.5.98-.93 1.33.27.12.58.2.9.2.16 0 .28-.01.39-.04-.32.3-.74.5-1.2.56-.16.02-.3.03-.45.03-.36 0-.7-.08-1.01-.22-.32.08-.65.13-1 .13s-.68-.05-1-.13c-.31.14-.65.22-1.01.22-.15 0-.3-.01-.45-.03-.46-.06-.88-.26-1.2-.56.11.03.23.04.39.04.32 0 .63-.08.9-.2-.43-.35-.75-.8-.93-1.33.38.32.86.52 1.39.52.24 0 .47-.04.68-.12-.3-.28-.5-.66-.5-1.09 0-.36.13-.69.35-.95-.53-.19-.91-.69-.91-1.28 0-.3.1-.58.28-.81-.1-.33-.16-.69-.16-1.07 0-1.82 1.17-3.29 2.61-3.29s2.61 1.47 2.61 3.29c0 .38-.06.74-.16 1.07.18.23.28.51.28.81 0 .59-.38 1.09-.91 1.28.22.26.35.59.35.95 0 .43-.2.81-.5 1.09.21.08.44.12.68.12.53 0 1.01-.2 1.39-.52z"/>
                    </svg>
                  </div>
                  <span class="text-xs text-gray-600 dark:text-gray-300">QQ</span>
                </button>
              </div>

              <!-- 微博分享 -->
              <button class="w-full flex items-center space-x-2 px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors" @click="shareToPlatform('weibo')">
                <div class="w-8 h-8 bg-red-50 dark:bg-red-900/30 rounded-full flex items-center justify-center">
                  <svg class="w-4 h-4 text-red-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10.09 16.82c-2.83.3-5.27-1-5.46-2.9-.19-1.9 1.95-3.69 4.78-3.99 2.83-.3 5.27 1 5.46 2.9.19 1.9-1.95 3.69-4.78 3.99zm7.71-5.1c-.23-.7-.92-1.08-1.54-.85-.62.23-.94.94-.71 1.59.23.65.92 1.03 1.54.8.62-.22.94-.89.71-1.54zM17.6 3.4C15.8 1.6 13.3.8 10.9 1.1c-.8.1-1.4.8-1.3 1.6.1.8.8 1.4 1.6 1.3 1.6-.2 3.3.3 4.5 1.5 1.2 1.2 1.7 2.9 1.5 4.5-.1.8.5 1.5 1.3 1.6.8.1 1.5-.5 1.6-1.3.3-2.4-.5-4.9-2.5-6.9z"/>
                  </svg>
                </div>
                <span class="text-sm text-gray-700 dark:text-gray-300">分享到微博</span>
              </button>
            </div>
          </Transition>
        </div>
      </div>

      <!-- 评论区 -->
      <section>
        <!-- 评论懒加载触发器 -->
        <div :ref="(el: any) => commentsLazyTrigger = el" class="h-0"></div>

        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">评论 ({{ commentTotal }})</h2>

        <!-- 评论加载中 -->
        <div v-if="commentsLoading" class="flex items-center justify-center py-8">
          <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          <span class="ml-2 text-sm text-gray-500">{{ t('common.loading') }}</span>
        </div>

        <!-- 评论加载失败 -->
        <ErrorRetry v-else-if="commentsError" :message="commentsError" :on-retry="retryComments" />

        <CommentSection
          v-else-if="commentsLoaded"
          :article-id="article.id"
          :comments="comments"
          :has-more="hasMoreComments"
          :total="commentTotal"
          @submit="submitComment"
          @like="likeComment"
          @delete="deleteComment"
          @report="reportComment"
          @sort-change="handleSortChange"
          @load-more="loadMoreComments"
        />
      </section>

      <!-- 相关推荐 -->
      <section class="mt-8">
        <!-- 相关推荐懒加载触发器 -->
        <div :ref="(el: any) => relatedLazyTrigger = el" class="h-0"></div>

        <template v-if="relatedLoading">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">{{ t('article.relatedArticles') }}</h2>
          <div class="flex items-center justify-center py-8">
            <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            <span class="ml-2 text-sm text-gray-500">{{ t('common.loading') }}</span>
          </div>
        </template>

        <ErrorRetry v-else-if="relatedError" :message="relatedError" :on-retry="retryRelated" />

        <template v-else-if="relatedLoaded && relatedArticles.length">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">{{ t('article.relatedArticles') }}</h2>
          <div class="space-y-4">
            <ArticleCard v-for="a in relatedArticles" :key="a.id" :article="a" />
          </div>
        </template>
      </section>
      </div>

      <!-- 2xl断点右侧目录/推荐栏 -->
      <aside class="hidden 2xl:block w-72 shrink-0">
        <div class="sticky top-20 space-y-6">
          <!-- 目录 -->
          <div v-if="tocItems.length" class="card">
            <div class="p-4 border-b border-gray-200 dark:border-gray-700">
              <h3 class="font-semibold text-gray-900 dark:text-white">{{ t('article.toc') }}</h3>
            </div>
            <nav class="p-4 space-y-1 max-h-[60vh] overflow-y-auto">
              <a
                v-for="item in tocItems"
                :key="item.id"
                :href="'#' + item.id"
                class="block text-sm text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-primary-400 transition-colors truncate"
                :style="{ paddingLeft: (item.level - 1) * 12 + 'px' }"
              >
                {{ item.text }}
              </a>
            </nav>
          </div>

          <!-- 相关推荐 -->
          <div v-if="relatedArticles.length" class="card">
            <div class="p-4 border-b border-gray-200 dark:border-gray-700">
              <h3 class="font-semibold text-gray-900 dark:text-white">{{ t('article.relatedArticles') }}</h3>
            </div>
            <div class="p-4 space-y-3">
              <NuxtLink
                v-for="a in relatedArticles.slice(0, 5)"
                :key="a.id"
                :to="`/articles/${a.id}`"
                class="block text-sm text-gray-700 dark:text-gray-300 hover:text-primary dark:hover:text-primary-400 transition-colors line-clamp-2"
              >
                {{ a.title }}
              </NuxtLink>
            </div>
          </div>
        </div>
      </aside>
    </article>
    </SwipeArticle>

    <!-- 分享海报弹窗 -->
    <SharePoster :visible="showPoster" :article="article" @close="showPoster = false" @shared="onShareCompleted" />

    <!-- 图片缩放查看器 -->
    <ImageZoom v-model:visible="imageZoomVisible" :src="imageZoomSrc" :alt="imageZoomAlt" />
  </div>
</template>

<script setup lang="ts">
/** 文章详情页：SSR渲染 */
import type { Article, Comment } from '~/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { resolveUrl } = useResourceUrl()
const { t } = useI18n()
const { invalidateArticle, invalidateUser } = useCacheInvalidation()

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}
const { recordView, updateDuration } = useViewHistory()
const { isTablet, isLandscape } = useBreakpoints()
const { promptOrientationLock, dismissOrientationPrompt, showOrientationPrompt } = useOrientation()
const articleId = computed(() => Number(route.params.id))

// 文章数据
const comments = ref<Comment[]>([])
const hasMoreComments = ref(true)
const commentTotal = ref(0)
const commentSort = ref('latest')
const relatedArticles = ref<Article[]>([])

// 上一篇/下一篇文章ID（从相关推荐中取）
const prevArticleId = computed(() => {
  if (!relatedArticles.value.length) return null
  const idx = relatedArticles.value.findIndex(a => a.id === articleId.value)
  if (idx > 0) return relatedArticles.value[idx - 1].id
  return relatedArticles.value[relatedArticles.value.length - 1]?.id ?? null
})
const nextArticleId = computed(() => {
  if (!relatedArticles.value.length) return null
  const idx = relatedArticles.value.findIndex(a => a.id === articleId.value)
  if (idx >= 0 && idx < relatedArticles.value.length - 1) return relatedArticles.value[idx + 1].id
  return relatedArticles.value[0]?.id ?? null
})

// 目录项
interface TocItem {
  id: string
  text: string
  level: number
}
const tocItems = ref<TocItem[]>([])

// 从文章内容中提取目录
const extractToc = (html: string) => {
  const items: TocItem[] = []
  const regex = /<h([1-3])[^>]*id=["']([^"']+)["'][^>]*>(.*?)<\/h\1>/gi
  let match
  while ((match = regex.exec(html)) !== null) {
    const level = parseInt(match[1])
    const id = match[2]
    const text = match[3].replace(/<[^>]+>/g, '').trim()
    if (id && text) {
      items.push({ id, text, level })
    }
  }
  return items
}

// 浏览时长计时器
let viewStartTime = 0
let durationTimer: ReturnType<typeof setInterval> | null = null
let accumulatedDuration = 0

// 分享相关状态
const showSharePanel = ref(false)
const showPoster = ref(false)

// 图片缩放查看器
const contentRef = ref<HTMLElement>()
const imageZoomVisible = ref(false)
const imageZoomSrc = ref('')
const imageZoomAlt = ref('')

const openImageZoom = (src: string, alt?: string) => {
  imageZoomSrc.value = src
  imageZoomAlt.value = alt || ''
  imageZoomVisible.value = true
}

const handleContentClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    e.preventDefault()
    const img = target as HTMLImageElement
    openImageZoom(img.src, img.alt)
  }
}

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
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.toggleLike(article.value.id)
  article.value.isLiked = response.data.data.isLiked
  article.value.likeCount = response.data.data.likeCount
  // 失效文章列表缓存，确保列表页数据一致
  invalidateArticle()
}

// 收藏
const toggleCollect = async () => {
  if (!article.value) return
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.toggleCollect(article.value.id)
  article.value.isCollected = response.data.data.isCollected
  article.value.collectCount = response.data.data.collectCount
  // 失效文章列表缓存，确保列表页数据一致
  invalidateArticle()
}

// 关注作者
const toggleFollowAuthor = async () => {
  if (!article.value?.author) return
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(article.value.author.id)
    article.value.author.isFollowing = !article.value.author.isFollowing
    // 失效用户相关缓存
    invalidateUser()
  } catch (error: any) {
    console.error(t('article.followFailed') + ':', error.message)
  }
}

// 提交评论
const submitComment = async (data: { content: string; parentId?: number }) => {
  if (!article.value) return
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.createComment(article.value.id, data)
  comments.value.unshift(response.data.data)
  article.value.commentCount++
  // 失效文章列表缓存，确保列表页数据一致
  invalidateArticle()
}

// 点赞评论
const likeComment = async (commentId: number) => {
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.likeComment(commentId)
    const { isLiked, likeCount } = response.data.data
    // 递归查找并更新评论
    const updateCommentInList = (list: Comment[]) => {
      for (const comment of list) {
        if (comment.id === commentId) {
          comment.isLiked = isLiked
          comment.likeCount = likeCount
          return true
        }
        if (comment.replies?.length && updateCommentInList(comment.replies)) {
          return true
        }
      }
      return false
    }
    updateCommentInList(comments.value)
  } catch {
    // 评论点赞失败，静默处理
  }
}

// 删除评论
const deleteComment = async (commentId: number) => {
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.deleteComment(commentId)
    // 递归查找并移除评论
    const removeCommentFromList = (list: Comment[]): Comment[] => {
      return list.filter(comment => {
        if (comment.id === commentId) return false
        if (comment.replies?.length) {
          comment.replies = removeCommentFromList(comment.replies)
        }
        return true
      })
    }
    comments.value = removeCommentFromList(comments.value)
    if (article.value) article.value.commentCount--
    commentTotal.value = Math.max(0, commentTotal.value - 1)
  } catch {
    // 删除评论失败
  }
}

// 举报评论
const reportComment = async (data: { commentId: number; reason?: string }) => {
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.reportComment(data.commentId, { reason: data.reason })
  } catch {
    // 举报评论失败
  }
}

// 切换评论排序
const handleSortChange = async (sort: string) => {
  commentSort.value = sort
  if (!article.value) return
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value.id, {
      page: 1,
      pageSize: 10,
      sort,
    })
    const data = response.data.data
    comments.value = data.list
    commentTotal.value = data.total
    hasMoreComments.value = comments.value.length < data.total
  } catch {
    // 切换排序加载失败
  }
}

// 加载更多评论
const loadMoreComments = async () => {
  if (!article.value) return
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value.id, {
      page: Math.ceil(comments.value.length / 10) + 1,
      pageSize: 10,
      sort: commentSort.value,
    })
    const data = response.data.data
    comments.value.push(...data.list)
    hasMoreComments.value = comments.value.length < data.total
  } catch {
    // 加载更多评论失败
  }
}

// 评论懒加载
const {
  data: commentsData,
  loading: commentsLoading,
  error: commentsError,
  loaded: commentsLoaded,
  triggerRef: commentsLazyTrigger,
  retry: retryComments,
} = useLazyData<{
  list: Comment[]
  total: number
}>({
  fetchFn: async () => {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value!.id, { page: 1, pageSize: 10, sort: commentSort.value })
    return response.data.data
  },
  defaultData: { list: [], total: 0 },
})

// 监听评论数据变化
watch(commentsData, (val) => {
  if (val) {
    comments.value = val.list
    commentTotal.value = val.total
    hasMoreComments.value = val.list.length < val.total
  }
})

// 相关推荐懒加载
const {
  data: relatedData,
  loading: relatedLoading,
  error: relatedError,
  loaded: relatedLoaded,
  triggerRef: relatedLazyTrigger,
  retry: retryRelated,
} = useLazyData<Article[]>({
  fetchFn: async () => {
    const { get } = useApi()
    const response = await get<Article[]>(`/articles/${articleId.value}/related`)
    return response.data.data || []
  },
  defaultData: [],
})

// 监听相关推荐数据变化
watch(relatedData, (val) => {
  if (val) {
    relatedArticles.value = val
  }
})

// 格式化标准时间戳
const formatTimestamp = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

// 滚动到评论区
const scrollToComments = () => {
  const commentSection = document.querySelector('section')
  if (commentSection) {
    commentSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// 打开分享海报
const openSharePoster = () => {
  showSharePanel.value = false
  showPoster.value = true
}

// 复制链接
const copyLink = async () => {
  if (!article.value) return
  try {
    const url = `${window.location.origin}/articles/${article.value.id}`
    await navigator.clipboard.writeText(url)
    showSharePanel.value = false
    // 记录分享
    recordShareStat('link')
  } catch {
    // 复制失败
  }
}

// 分享到第三方平台
const shareToPlatform = (platform: string) => {
  if (!article.value) return
  const url = `${window.location.origin}/articles/${article.value.id}`
  const title = article.value.title
  const summary = article.value.summary || ''

  switch (platform) {
    case 'wechat':
      // 微信分享需要用户手动操作，提示用户复制链接后在微信中打开
      copyLink()
      break
    case 'qq':
      window.open(`https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}&summary=${encodeURIComponent(summary)}`, '_blank')
      break
    case 'weibo':
      window.open(`https://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}`, '_blank')
      break
  }

  showSharePanel.value = false
  recordShareStat(platform)
}

// 记录分享统计
const recordShareStat = async (platform: string) => {
  if (!article.value) return
  try {
    const { articleApi } = await import('~/api')
    await articleApi.recordShare(article.value.id, platform)
  } catch {
    // 分享统计记录失败，静默处理
  }
}

// 海报分享完成回调
const onShareCompleted = () => {
  recordShareStat('poster')
}

// 点击外部关闭分享面板
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (showSharePanel.value && !target.closest('.relative')) {
    showSharePanel.value = false
  }
}

// 页面挂载：记录浏览、启动计时器、加载评论和推荐
onMounted(() => {
  // 点击页面其他区域关闭分享面板
  document.addEventListener('click', handleClickOutside)

  if (article.value) {
    // 记录浏览历史
    recordView(article.value.id, article.value.title)

    // 提取目录
    if (article.value.content) {
      tocItems.value = extractToc(article.value.content)
    }

    // 启动浏览时长计时器
    viewStartTime = Date.now()
    durationTimer = setInterval(() => {
      accumulatedDuration = Math.floor((Date.now() - viewStartTime) / 1000)
    }, 1000)
  }

  // 评论和相关推荐使用 useLazyData 懒加载，由 IntersectionObserver 触发

  // 平板竖屏时提示横屏阅读
  if (isTablet.value && !isLandscape.value) {
    promptOrientationLock('landscape')
  }
})

// 页面卸载：更新浏览时长
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
  }
  if (article.value && accumulatedDuration > 0) {
    updateDuration(article.value.id, accumulatedDuration)
  }
})

// 页面元信息
useHead({
  title: () => article.value ? `${article.value.title} - 知讯` : t('article.loadArticleFailed') + ' - 知讯',
})
</script>
