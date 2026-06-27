<template>
  <!-- 作品详情页 -->
  <div class="max-w-[800px] md:max-w-[900px] 2xl:max-w-[1200px] mx-auto px-1.5 2xl:px-2 py-1.5">
    <!-- 加载状态 -->
    <LoadingSkeleton v-if="pending" type="article" />

    <!-- 错误状态 -->
    <ErrorRetry v-else-if="articleError" :message="articleError?.message || '作品加载失败'" :on-retry="() => refresh()" />

    <!-- 作品内容 -->
    <SwipeArticle v-else-if="article" :prev-id="prevArticleId" :next-id="nextArticleId">
    <article class="animate-fade-in 2xl:flex 2xl:gap-8">
      <!-- 作品主体 -->
      <div class="2xl:flex-1 2xl:min-w-0 2xl:max-w-[800px]">
      <!-- 作品标题 -->
      <h1 class="text-2xl md:text-3xl font-bold text-slate-900 mb-2">
        {{ article.title || '加载中...' }}
      </h1>

      <!-- 作者信息栏（微博风格） -->
      <div class="flex items-center gap-2 mb-2">
        <NuxtLink :to="`/user/${article.author?.id}`" class="shrink-0">
          <UserAvatar :src="article.author?.avatar" :alt="article.author?.nickname || '用户'" size="md" />
        </NuxtLink>
        <div class="flex-1 min-w-0">
          <NuxtLink :to="`/user/${article.author?.id}`" class="text-sm font-medium text-slate-900 hover:text-primary transition-colors block">
            {{ article.author?.nickname || article.authorName || '用户' }}
          </NuxtLink>
          <p class="text-xs text-slate-400">
            <time>{{ formatTimestamp(article.createdAt) }}</time>
            <template v-if="article.location">
              <span class="mx-1">·</span>
              <span class="inline-flex items-center gap-0.5">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {{ article.location }}
              </span>
            </template>
            <template v-if="article.deviceInfo">
              <span class="mx-1">·</span>
              <span>来自{{ article.deviceInfo }}</span>
            </template>
          </p>
        </div>
        <ClientOnly>
          <button
            v-if="article.author?.id !== userStore.userInfo?.id"
            class="ml-auto btn text-sm"
            :class="article.author?.isFollowing ? 'btn-secondary' : 'btn-primary'"
            @click="toggleFollowAuthor"
          >
            {{ article.author?.isFollowing ? '已关注' : '关注' }}
          </button>
        </ClientOnly>
      </div>

      <!-- 作品内容（富文本渲染） -->
      <div class="relative">
        <div ref="contentRef" class="prose prose-slate max-w-none mb-4" v-html="article.content" @click="handleContentClick"></div>
      </div>

      <!-- 图片网格（最多9张，3列正方形，无间距，撑满屏幕） -->
      <div v-if="article.images?.length" class="mb-4 -mx-1.5 md:-mx-0">
        <div class="grid grid-cols-3 gap-0 w-full">
          <div
            v-for="(img, idx) in article.images.slice(0, 9)"
            :key="idx"
            class="aspect-square overflow-hidden cursor-pointer"
            @click="openImageZoom(resolveUrl(img) || img, article.title)"
          >
            <img :src="resolveUrl(img) || img" alt="" class="w-full h-full object-cover" />
          </div>
        </div>
      </div>

      <!-- 标签 -->
      <div v-if="article.tags?.length" class="flex flex-wrap gap-1.5 mb-3">
        <TopicBadge v-for="tag in article.tags" :key="tag.id" :id="tag.id" :name="tag.name" />
      </div>

      <!-- 互动按钮栏（微博风格） -->
      <div class="flex items-center justify-around py-4 border-y border-slate-200 mb-6">
        <!-- 点赞 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full transition-colors touch-target" :class="article.isLiked ? 'text-primary bg-primary-50/50' : 'text-slate-500 hover:text-primary hover:bg-primary-50/50'" @click="toggleLike">
          <svg class="w-5 h-5" :class="article.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span class="text-sm">{{ article.likeCount }}</span>
        </button>

        <!-- 收藏 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full transition-colors touch-target" :class="article.isCollected ? 'text-yellow-500 bg-yellow-50' : 'text-slate-500 hover:text-yellow-500 hover:bg-yellow-50'" @click="toggleCollect">
          <svg class="w-5 h-5" :class="article.isCollected ? 'fill-yellow-500' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
          </svg>
          <span class="text-sm">{{ article.collectCount }}</span>
        </button>

        <!-- 评论 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full text-slate-500 hover:text-primary hover:bg-primary-50/50 transition-colors touch-target" @click="scrollToComments">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span class="text-sm">{{ article.commentCount }}</span>
        </button>

        <!-- 分享 -->
        <button class="flex items-center gap-1.5 px-4 py-2 rounded-full text-slate-500 hover:text-primary hover:bg-primary-50/50 transition-colors touch-target" @click="showShareDialog = true">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          <span class="text-sm">{{ article.shareCount ?? '分享' }}</span>
        </button>

        <!-- 举报 -->
        <ClientOnly>
          <button class="flex items-center gap-1.5 px-4 py-2 rounded-full text-slate-500 hover:text-red-500 hover:bg-red-50 transition-colors touch-target" @click="reportVisible = true">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <span class="text-sm">举报</span>
          </button>
        </ClientOnly>
      </div>

      <!-- 评论区 -->
      <section id="comments">
        <!-- 评论懒加载触发器 -->
        <div :ref="(el: any) => commentsLazyTrigger = el" class="h-0"></div>

        <h2 class="text-lg font-semibold text-slate-900 mb-2">评论 ({{ commentTotal }})</h2>

        <!-- 评论加载中 -->
        <div v-if="commentsLoading" class="flex items-center justify-center py-8">
          <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          <span class="ml-2 text-sm text-gray-500">{{ '加载中...' }}</span>
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
      <section class="mt-4">
        <!-- 相关推荐懒加载触发器 -->
        <div :ref="(el: any) => relatedLazyTrigger = el" class="h-0"></div>

        <template v-if="relatedLoading">
          <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '相关推荐' }}</h2>
          <div class="flex items-center justify-center py-8">
            <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            <span class="ml-2 text-sm text-gray-500">{{ '加载中...' }}</span>
          </div>
        </template>

        <ErrorRetry v-else-if="relatedError" :message="relatedError" :on-retry="retryRelated" />

        <template v-else-if="relatedLoaded && relatedArticles.length">
          <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '相关推荐' }}</h2>
          <div class="space-y-2">
            <ArticleCard v-for="a in relatedArticles" :key="a.id" :article="a" />
          </div>
        </template>
      </section>
      </div>

      <!-- 2xl断点右侧目录/推荐栏 -->
      <aside class="hidden 2xl:block w-72 shrink-0">
        <div class="sticky top-20 space-y-3">
          <!-- 目录 -->
          <div v-if="tocItems.length" class="card">
            <div class="p-2 border-b border-slate-200">
              <h3 class="font-semibold text-slate-900">{{ '目录' }}</h3>
            </div>
            <nav class="p-2 space-y-1 max-h-[60vh] overflow-y-auto">
              <a
                v-for="item in tocItems"
                :key="item.id"
                :href="'#' + item.id"
                class="block text-sm text-slate-500 hover:text-primary-600 transition-colors truncate"
                :style="{ paddingLeft: (item.level - 1) * 12 + 'px' }"
              >
                {{ item.text }}
              </a>
            </nav>
          </div>

          <!-- 相关推荐 -->
          <div v-if="relatedArticles.length" class="card">
            <div class="p-2 border-b border-slate-200">
              <h3 class="font-semibold text-slate-900">{{ '相关推荐' }}</h3>
            </div>
            <div class="p-2 space-y-2">
              <NuxtLink
                v-for="a in relatedArticles.slice(0, 5)"
                :key="a.id"
                :to="`/articles/${a.id}`"
                class="block text-sm text-slate-700 hover:text-primary-600 transition-colors line-clamp-2"
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

    <!-- 分享弹框（QQ/微信/微博/链接） -->
    <ShareDialog
      :visible="showShareDialog"
      :title="'分享「' + (article?.title || '') + '」'"
      :params="shareParams"
      @close="showShareDialog = false"
      @shared="onShareToPlatform"
    />

    <!-- 图片缩放查看器 -->
    <ImageZoom v-model:visible="imageZoomVisible" :src="imageZoomSrc" :alt="imageZoomAlt" />

    <!-- 举报弹窗 -->
    <ClientOnly>
      <ReportDialog
        :visible="reportVisible"
        type="article"
        :target-id="article?.id || 0"
        @close="reportVisible = false"
        @reported="reportVisible = false"
      />
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
/** 作品详情页：SSR渲染 */
import type { Article, Comment } from '~/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { setTitle } = usePageHeaderTitle()
const { resolveUrl } = useResourceUrl()
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
const articleId = computed(() => {
  const id = Number(route.params.id)
  return isNaN(id) ? null : id
})

// 作品数据
const comments = ref<Comment[]>([])
const hasMoreComments = ref(true)
const commentTotal = ref(0)
const commentSort = ref('latest')
const relatedArticles = ref<Article[]>([])

// 上一篇/下一篇作品ID（从相关推荐中取）
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

// 从作品内容中提取目录
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
const showShareDialog = ref(false)
const showPoster = ref(false)

// 举报
const reportVisible = ref(false)

// 分享参数（传给 ShareDialog）
const requestUrl = useRequestURL()
const shareBaseUrl = computed(() => {
  if (import.meta.server) {
    return `${requestUrl.protocol}//${requestUrl.host}`
  }
  return window.location.origin
})
const shareParams = computed(() => ({
  url: `${shareBaseUrl.value}/articles/${articleId.value}`,
  title: article.value?.title || '',
  summary: article.value?.summary || '',
  image: article.value?.coverImage || '',
}))

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

// SSR获取作品详情
const { data: article, pending, error: articleError, refresh } = await useAsyncData(
  `article-${articleId.value}`,
  async () => {
    if (articleId.value === null) {
      throw new Error('无效的作品ID')
    }
    const { articleApi } = await import('~/api')
    const response = await articleApi.getArticleDetail(articleId.value!)
    return response.data.data
  },
  {
    // 确保服务端和客户端都正确获取数据
    server: true,
    lazy: false,
  }
)

// 客户端挂载后检查数据完整性，防止 SSR 水合后数据丢失导致标题/用户名为空
onMounted(() => {
  if (import.meta.client && article.value) {
    // 更全面的数据完整性检查：标题缺失，或者作者信息完全缺失（既无 author 对象也无扁平字段）
    const hasIncompleteData =
      !article.value.title ||
      !article.value.content ||
      (!article.value.author?.nickname && !article.value.authorName)
    if (hasIncompleteData) {
      // SSR 水合后数据不完整，重新获取
      refresh()
    }
  }
})

watch(article, (val) => { if (val?.title) setTitle(val.title) }, { immediate: true })

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
  // 失效作品列表缓存，确保列表页数据一致
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
  // 失效作品列表缓存，确保列表页数据一致
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
    console.error('关注操作失败' + ':', error.message)
  }
}

// 提交评论
const submitComment = async (data: { content: string; parentId?: number; replyUserId?: number }) => {
  if (!article.value) return
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  const { interactionApi } = await import('~/api')
  const response = await interactionApi.createComment(article.value.id, data)
  comments.value.unshift(response.data.data)
  article.value.commentCount++
  // 失效作品列表缓存，确保列表页数据一致
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
  showPoster.value = true
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

// ShareDialog 分享回调
const onShareToPlatform = (platform: string) => {
  recordShareStat(platform)
}

// 页面挂载：记录浏览、启动计时器、加载评论和推荐
onMounted(() => {
  // 处理URL hash滚动到评论区
  if (window.location.hash === '#comments') {
    setTimeout(() => {
      const el = document.getElementById('comments')
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 500)
  }

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
  title: () => article.value ? `${article.value.title} - 知讯` : '作品加载失败，请稍后重试' + ' - 知讯',
})
</script>
