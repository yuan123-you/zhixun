<template>
  <!-- 作品详情页 - 现代编辑感设计 -->
  <div class="article-page">
    <!-- 阅读进度条（顶部固定） -->
    <div class="read-progress" :class="{ visible: scrollProgress > 0.02 }">
      <div class="read-progress-bar" :style="{ width: `${scrollProgress * 100}%` }"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="pending" class="article-skeleton">
      <LoadingSkeleton type="article" />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="articleError" class="article-error">
      <ErrorRetry :message="articleError?.message || '作品加载失败，请稍后重试'" :on-retry="() => refresh()" />
    </div>

    <!-- 作品内容 -->
    <SwipeArticle v-else-if="article">
      <article class="article-content">
        <!-- 作品主体 -->
        <div class="article-main">
          <!-- 顶部元信息：返回按钮 + 分类 + 日期 -->
          <div class="article-meta-top">
            <button
              v-if="canGoBack"
              class="article-back-btn"
              type="button"
              aria-label="返回"
              @click="goBack"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <line x1="19" y1="12" x2="5" y2="12"/>
                <polyline points="12 19 5 12 12 5"/>
              </svg>
              <span class="article-back-btn-text">返回</span>
            </button>
            <span v-if="article.categoryName" class="article-category-badge">
              <span class="article-category-dot"></span>
              {{ article.categoryName }}
            </span>
            <span v-if="article.location" class="article-meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              {{ article.location }}
            </span>
            <span class="article-meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              {{ formatTimestamp(article.createdAt) }}
            </span>
          </div>

          <!-- 标题 -->
          <h1 class="article-title">
            {{ article.title || '加载中...' }}
          </h1>

          <!-- 副标题/摘要 -->
          <p v-if="article.summary" class="article-summary">
            {{ article.summary }}
          </p>

          <!-- 作者信息栏（精致卡片） -->
          <div class="author-bar">
            <RouterLink :to="`/user/${article.author?.id}`" class="author-avatar-link">
              <UserAvatar :src="article.author?.avatar" :alt="article.author?.nickname || '用户'" size="lg" />
            </RouterLink>
            <div class="author-info">
              <RouterLink :to="`/user/${article.author?.id}`" class="author-name">
                {{ article.author?.nickname || article.authorName || '用户' }}
                <svg v-if="article.author?.role === 'ADMIN' || article.author?.role === 'SUPER_ADMIN'" class="author-badge" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2l2.39 4.84 5.34.78-3.86 3.77.91 5.32L12 14.27l-4.78 2.51.91-5.32-3.86-3.77 5.34-.78L12 2z"/>
                </svg>
              </RouterLink>
              <div class="author-meta">
                <span v-if="article.deviceInfo" class="author-meta-item">
                  来自 {{ article.deviceInfo }}
                </span>
                <span class="author-meta-item">·</span>
                <span class="author-meta-item">{{ readingTime }} 分钟阅读</span>
                <span class="author-meta-item">·</span>
                <span class="author-meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="meta-icon">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
                  {{ formatViewCount(article.viewCount) }} 浏览
                </span>
              </div>
            </div>
            <button
              v-if="article.author?.id !== userStore.userInfo?.id"
              class="follow-btn"
              :class="{ 'follow-btn--active': article.author?.isFollowing }"
              type="button"
              @click="toggleFollowAuthor"
            >
              <span v-if="article.author?.isFollowing" class="follow-btn-icon">✓</span>
              <span v-else class="follow-btn-icon">+</span>
              <span>{{ article.author?.isFollowing ? '已关注' : '关注' }}</span>
            </button>
          </div>

          <!-- 作品内容（富文本渲染） -->
          <div ref="contentRef" class="article-prose" v-html="sanitizedContent" @click="handleContentClick"></div>

          <!-- 图片网格（最多9张，3列） -->
          <div v-if="article.images?.length" class="article-images">
            <div
              v-for="(img, idx) in article.images.slice(0, 9)"
              :key="idx"
              class="article-image-item"
              @click="openImageZoom(resolveUrl(img) || img, article.title)"
            >
              <img :src="resolveUrl(img) || img" :alt="`配图${idx + 1}`" loading="lazy" />
            </div>
          </div>

          <!-- 标签 -->
          <div v-if="article.tags?.length" class="article-tags">
            <TopicBadge v-for="tag in article.tags" :key="tag.id" :id="tag.id" :name="tag.name" />
          </div>

          <!-- 互动按钮栏（现代悬浮设计） -->
          <div class="interaction-bar">
            <button
              class="interaction-btn"
              :class="{ active: article.isLiked }"
              type="button"
              @click="toggleLike"
            >
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.likeCount || '点赞' }}</span>
            </button>
            <button
              class="interaction-btn"
              :class="{ active: article.isCollected }"
              type="button"
              @click="toggleCollect"
            >
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" :fill="article.isCollected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.collectCount || '收藏' }}</span>
            </button>
            <button class="interaction-btn" type="button" @click="scrollToComments">
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </span>
              <span class="interaction-btn-count">{{ article.commentCount || '评论' }}</span>
            </button>
            <button class="interaction-btn" type="button" @click="openReport">
              <span class="interaction-btn-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </span>
              <span class="interaction-btn-count">举报</span>
            </button>
          </div>

          <!-- 评论区 -->
          <section id="comments" class="comments-section">
            <div :ref="(el: any) => commentsLazyTrigger = el" class="h-0"></div>

            <!-- 评论加载中 -->
            <div v-if="commentsLoading" class="comment-status">
              <div class="comment-status-spinner"></div>
              <span>评论加载中…</span>
            </div>

            <!-- 评论加载失败 -->
            <ErrorRetry v-else-if="commentsError" :message="commentsError" :on-retry="retryComments" />

            <CommentSection
              v-else-if="commentsLoaded"
              :article-id="article.id"
              :comments="comments"
              :has-more="hasMoreComments"
              :total="commentTotal"
              :loading="false"
              @submit="submitComment"
              @like="likeComment"
              @delete="deleteComment"
              @report="reportComment"
              @sort-change="handleSortChange"
              @load-more="loadMoreComments"
            />
          </section>

          
        </div>

        <!-- 2xl断点右侧目录/推荐栏 -->
        <aside v-if="tocItems.length" class="article-aside">
          <div class="article-aside-sticky">
            <!-- 目录 -->
            <div v-if="tocItems.length" class="aside-card">
              <header class="aside-card-header">
                <h3 class="aside-card-title">目录</h3>
                <span class="aside-card-badge">{{ tocItems.length }}</span>
              </header>
              <nav class="aside-card-body aside-toc">
                <a
                  v-for="item in tocItems"
                  :key="item.id"
                  :href="'#' + item.id"
                  class="aside-toc-item"
                  :style="{ paddingLeft: (item.level - 1) * 10 + 'px' }"
                >
                  {{ item.text }}
                </a>
              </nav>
            </div>

            
          </div>
        </aside>
      </article>
    </SwipeArticle>

    <!-- 分享海报弹窗 -->
    <ImageZoom v-model:visible="imageZoomVisible" :src="imageZoomSrc" :alt="imageZoomAlt" />

    <!-- 举报弹窗 -->
    <ReportDialog
      :visible="reportVisible"
      type="article"
      :target-id="article?.id || 0"
      @close="reportVisible = false"
      @reported="reportVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
/** 作品详情页：SSR渲染 - 现代编辑感设计 */
import type { Comment } from '@/types'
import { formatTimestamp } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 返回按钮：优先使用浏览器历史回退，若无历史或来自外部则返回首页
const canGoBack = ref(false)
const goBack = () => {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
const { setTitle } = usePageHeaderTitle()
const { resolveUrl } = useResourceUrl()
const { invalidateArticle, invalidateUser } = useCacheInvalidation()
const { recordView, updateDuration } = useViewHistory()
const { isTablet, isLandscape } = useBreakpoints()
const { promptOrientationLock, showOrientationPrompt } = useOrientation()

const articleId = computed(() => {
  const id = Number(route.params.id)
  return isNaN(id) ? null : id
})

// 作品数据
const comments = ref<Comment[]>([])
const hasMoreComments = ref(true)
const commentTotal = ref(0)
const commentSort = ref('latest')

interface TocItem { id: string; text: string; level: number }
const tocItems = ref<TocItem[]>([])

const extractToc = (html: string) => {
  const items: TocItem[] = []
  const regex = /<h([1-3])[^>]*id=["']([^"']+)["'][^>]*>(.*?)<\/h\1>/gi
  let match
  while ((match = regex.exec(html)) !== null) {
    const level = parseInt(match[1])
    const id = match[2]
    const text = match[3].replace(/<[^>]+>/g, '').trim()
    if (id && text) items.push({ id, text, level })
  }
  return items
}

let viewStartTime = 0
let durationTimer: ReturnType<typeof setInterval> | null = null
let accumulatedDuration = 0
const scrollProgress = ref(0)

const reportVisible = ref(false)
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

// 估算阅读时长（按 300 字/分钟）
const readingTime = computed(() => {
  if (!article.value?.content) return 1
  const text = article.value.content.replace(/<[^>]+>/g, '').trim()
  const minutes = Math.max(1, Math.ceil(text.length / 300))
  return minutes
})

// 浏览量格式化
const formatViewCount = (n: number | undefined): string => {
  if (!n) return '0'
  if (n >= 10000) return `${(n / 10000).toFixed(1)}w`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}

// SSR获取作品详情
const { data: article, pending, error: articleError, refresh } = useAsyncData(
  () => `article-${articleId.value}`,
  async () => {
    if (articleId.value === null) throw new Error('作品信息有误，请返回首页')
    const { articleApi } = await import('~/api')
    const response = await articleApi.getArticleDetail(articleId.value!)
    return response.data.data
  },
  { server: true, lazy: false, watch: true },
)

const sanitizedContent = computed(() => {
  return article.value?.content ? sanitizeHtml(article.value.content) : ''
})

onMounted(() => {
  if (true && article.value) {
    const hasIncompleteData =
      !article.value.title ||
      !article.value.content ||
      (!article.value.author?.nickname && !article.value.authorName)
    if (hasIncompleteData) refresh()
  }
})

watch(article, (val) => { if (val?.title) setTitle(val.title) }, { immediate: true })

// 核心修复：动态路由 articleId 变化时（点击作品切换详情）重置所有相关本地状态
watch(() => articleId.value, async (newId, oldId) => {
  if (newId === oldId || newId == null) return
  // 重置评论相关状态（避免上一个作品的评论残留）
  comments.value = []
  hasMoreComments.value = true
  commentTotal.value = 0
  commentSort.value = 'latest'
  tocItems.value = []
  imageZoomVisible.value = false
  scrollProgress.value = 0
  // 重置懒加载评论：清空 loaded 标志，下次进入视口会重新加载
  resetComments()
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
})

// 点赞
const toggleLike = async () => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.toggleLike(article.value.id)
    article.value.isLiked = response.data.data.isLiked
    article.value.likeCount = response.data.data.likeCount
    invalidateArticle()
  } catch (e: any) {
    console.error('点赞操作失败:', e?.message || e)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  }
}

// 收藏
const toggleCollect = async () => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.toggleCollect(article.value.id)
    article.value.isCollected = response.data.data.isCollected
    article.value.collectCount = response.data.data.collectCount
    invalidateArticle()
  } catch (e: any) {
    console.error('收藏操作失败:', e?.message || e)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  }
}

// 关注作者
const toggleFollowAuthor = async () => {
  if (!article.value?.author) return
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(article.value.author.id)
    article.value.author.isFollowing = !article.value.author.isFollowing
    invalidateUser()
  } catch (error: any) {
    console.error('关注操作失败，请稍后重试')
  }
}

// 提交评论
const submitComment = async (data: { content?: string; parentId?: number; replyUserId?: number; images?: string[] }) => {
  if (!article.value) return
  if (!userStore.isLoggedIn) { navigateTo({ path: '/login' }); return }
  const { interactionApi } = await import('~/api')
  const payload: { content?: string; parentId?: number; replyUserId?: number; images?: string[] } = {}
  if (data.content) payload.content = data.content
  if (data.parentId) payload.parentId = data.parentId
  if (data.replyUserId) payload.replyUserId = data.replyUserId
  if (data.images && data.images.length) payload.images = data.images
  const response = await interactionApi.createComment(article.value.id, payload)
  comments.value.unshift(response.data.data)
  article.value.commentCount++
  invalidateArticle()
}

const likeComment = async (commentId: number) => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.likeComment(commentId)
    const { isLiked, likeCount } = response.data.data
    const updateCommentInList = (list: Comment[]) => {
      for (const comment of list) {
        if (comment.id === commentId) {
          comment.isLiked = isLiked
          comment.likeCount = likeCount
          return true
        }
        if (comment.replies?.length && updateCommentInList(comment.replies)) return true
      }
      return false
    }
    updateCommentInList(comments.value)
  } catch { /* 静默 */ }
}

const deleteComment = async (commentId: number) => {
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.deleteComment(commentId)
    const removeCommentFromList = (list: Comment[]): Comment[] => {
      return list.filter(comment => {
        if (comment.id === commentId) return false
        if (comment.replies?.length) comment.replies = removeCommentFromList(comment.replies)
        return true
      })
    }
    comments.value = removeCommentFromList(comments.value)
    if (article.value) article.value.commentCount--
    commentTotal.value = Math.max(0, commentTotal.value - 1)
  } catch { /* 静默 */ }
}

const openReport = () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  reportVisible.value = true
}

const reportComment = async (data: { commentId: number; reason?: string }) => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('~/api')
    await interactionApi.reportComment(data.commentId, { reason: data.reason })
  } catch { /* 静默 */ }
}

const handleSortChange = async (sort: string) => {
  commentSort.value = sort
  if (!article.value) return
  try {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value.id, {
      page: 1, pageSize: 10, sort,
    })
    const data = response.data.data
    comments.value = data.list
    commentTotal.value = data.total
    hasMoreComments.value = comments.value.length < data.total
  } catch { /* 静默 */ }
}

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
  } catch { /* 静默 */ }
}

const {
  data: commentsData,
  loading: commentsLoading,
  error: commentsError,
  loaded: commentsLoaded,
  triggerRef: commentsLazyTrigger,
  retry: retryComments,
  reset: resetComments,
} = useLazyData<{ list: Comment[]; total: number }>({
  fetchFn: async () => {
    const { interactionApi } = await import('~/api')
    const response = await interactionApi.getComments(article.value!.id, { page: 1, pageSize: 10, sort: commentSort.value })
    return response.data.data
  },
  defaultData: { list: [], total: 0 },
})

watch(commentsData, (val) => {
  if (val) {
    comments.value = val.list
    commentTotal.value = val.total
    hasMoreComments.value = val.list.length < val.total
  }
})

const scrollToComments = () => {
  const el = document.getElementById('comments')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 滚动监听：阅读进度 + 滚动保存
const handleScroll = () => {
  const docEl = document.documentElement
  const totalHeight = docEl.scrollHeight - docEl.clientHeight
  scrollProgress.value = totalHeight > 0 ? Math.min(1, docEl.scrollTop / totalHeight) : 0
}

let scrollHandler: (() => void) | null = null
onMounted(() => {
  // 初始化返回按钮状态：只要有历史就允许返回
  canGoBack.value = window.history.length > 1

  if (window.location.hash === '#comments') {
    setTimeout(() => {
      const el = document.getElementById('comments')
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 500)
  }

  if (article.value) {
    recordView(article.value.id, article.value.title)
    if (article.value.content) tocItems.value = extractToc(article.value.content)

    viewStartTime = Date.now()
    durationTimer = setInterval(() => {
      accumulatedDuration = Math.floor((Date.now() - viewStartTime) / 1000)
    }, 1000)
  }

  scrollHandler = handleScroll
  window.addEventListener('scroll', scrollHandler, { passive: true })

  if (isTablet.value && !isLandscape.value) {
    promptOrientationLock('landscape')
  }
})

onUnmounted(() => {
  if (durationTimer) { clearInterval(durationTimer); durationTimer = null }
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler)
  if (article.value && accumulatedDuration > 0) {
    updateDuration(article.value.id, accumulatedDuration)
  }
})

useHead({
  title: () => article.value ? `${article.value.title} - 知讯` : '作品无法加载 - 知讯',
})
</script>

<style scoped>
/* ========== 页面容器 ========== */
.article-page {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 4px 16px 12px;
  /* 移动端为底部评论输入框预留空间（输入框约 56px + tabbar 60px + 安全区） */
  padding-bottom: calc(140px + env(safe-area-inset-bottom, 0px));
}
@media (min-width: 768px) {
  .article-page {
    padding: 8px 24px 16px;
    padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  }
}
@media (min-width: 1536px) {
  .article-page {
    max-width: 1320px;
  }
}

/* ========== 顶部阅读进度条 ========== */
.read-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 90;
  pointer-events: none;
  background: transparent;
  transition: opacity 200ms ease;
  opacity: 0;
}
.read-progress.visible { opacity: 1; }
.read-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--zh-primary) 0%, #a78bfa 100%);
  transition: width 80ms linear;
  box-shadow: 0 0 8px rgba(var(--zh-primary-rgb), 0.4);
}

/* ========== 加载/错误状态 ========== */
.article-skeleton,
.article-error {
  padding: 16px 0;
}

/* ========== 作品主体布局 ========== */
.article-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: article-in 400ms cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes article-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-main {
  flex: 1;
  min-width: 0;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
}

@media (min-width: 1536px) {
  .article-content {
    flex-direction: row;
    align-items: flex-start;
  }
  .article-main {
    margin: 0;
  }
}

/* ========== 顶部元信息 ========== */
.article-meta-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--zh-text-tertiary);
}
/* 返回按钮 */
.article-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px 3px 6px;
  background: var(--zh-bg-elevated);
  color: var(--zh-text-secondary);
  border: 1px solid var(--zh-border);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease, border-color 150ms ease, transform 120ms ease;
}
.article-back-btn:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
  border-color: var(--zh-primary);
}
.article-back-btn:active {
  transform: scale(0.96);
}
.article-back-btn svg {
  width: 12px;
  height: 12px;
}
.article-back-btn-text {
  letter-spacing: 0.02em;
}
.article-category-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
  border-radius: 999px;
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.02em;
}
.article-category-dot {
  width: 5px;
  height: 5px;
  background: var(--zh-primary);
  border-radius: 50%;
  animation: dot-pulse 2s ease-in-out infinite;
}
@keyframes dot-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
}
.article-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.article-meta-item svg {
  width: 11px;
  height: 11px;
  opacity: 0.7;
}

/* ========== 标题 ========== */
.article-title {
  font-size: clamp(1.25rem, 4vw, 1.875rem);
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.025em;
  color: var(--zh-text);
  margin: 0 0 8px;
  font-feature-settings: 'kern' 1, 'liga' 1;
  text-wrap: balance;
}

/* ========== 副标题/摘要 ========== */
.article-summary {
  font-size: 13px;
  line-height: 1.55;
  color: var(--zh-text-secondary);
  margin: 0 0 14px;
  padding: 8px 12px;
  background: var(--zh-bg-hover);
  border-left: 3px solid var(--zh-primary);
  border-radius: 0 var(--zh-radius-md) var(--zh-radius-md) 0;
  font-weight: 400;
}

/* ========== 作者信息栏 ========== */
.author-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 14px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-xs);
  transition: box-shadow 200ms ease, border-color 200ms ease;
}
.author-bar:hover {
  border-color: var(--zh-border-focus);
  box-shadow: var(--zh-shadow-sm);
}
.author-avatar-link {
  flex-shrink: 0;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.author-avatar-link:hover { transform: scale(1.05); }
.author-info { flex: 1; min-width: 0; }
.author-name {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 700;
  color: var(--zh-text);
  text-decoration: none;
  margin-bottom: 2px;
  letter-spacing: -0.01em;
}
.author-name:hover { color: var(--zh-primary); }
.author-badge {
  width: 12px;
  height: 12px;
  color: var(--zh-warning);
}
.author-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  font-size: 11px;
  color: var(--zh-text-tertiary);
}
.author-meta-item { display: inline-flex; align-items: center; gap: 3px; }
.meta-icon { width: 11px; height: 11px; opacity: 0.7; }

.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 12px;
  background: var(--zh-primary);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 200ms ease, transform 150ms ease, box-shadow 200ms ease;
  box-shadow: 0 2px 8px rgba(var(--zh-primary-rgb), 0.25);
}
.follow-btn:hover {
  background: var(--zh-primary-light);
  box-shadow: 0 4px 12px rgba(var(--zh-primary-rgb), 0.35);
}
.follow-btn:active { transform: scale(0.97); }
.follow-btn--active {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  box-shadow: none;
  border: 1px solid var(--zh-border);
}
.follow-btn--active:hover {
  background: var(--zh-bg-active);
  color: var(--zh-text);
}
.follow-btn-icon {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

/* ========== 作品正文 ========== */
.article-prose {
  font-size: 14px;
  line-height: 1.75;
  color: var(--zh-text);
  word-wrap: break-word;
  font-feature-settings: 'kern' 1, 'liga' 1;
  margin-bottom: 14px;
}
.article-prose :deep(p) {
  margin: 0 0 0.9em;
  letter-spacing: 0.01em;
}
.article-prose :deep(h1),
.article-prose :deep(h2),
.article-prose :deep(h3),
.article-prose :deep(h4) {
  font-weight: 700;
  letter-spacing: -0.015em;
  margin: 1.3em 0 0.5em;
  scroll-margin-top: 80px;
}
.article-prose :deep(h1) { font-size: 1.4em; }
.article-prose :deep(h2) { font-size: 1.25em; }
.article-prose :deep(h3) { font-size: 1.1em; }
.article-prose :deep(a) {
  color: var(--zh-primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(var(--zh-primary-rgb), 0.3);
  transition: border-color 150ms ease, color 150ms ease;
}
.article-prose :deep(a:hover) {
  color: var(--zh-primary-light);
  border-bottom-color: var(--zh-primary);
}
.article-prose :deep(blockquote) {
  margin: 1.2em 0;
  padding: 8px 14px;
  border-left: 3px solid var(--zh-primary);
  background: var(--zh-primary-bg);
  border-radius: 0 var(--zh-radius-md) var(--zh-radius-md) 0;
  color: var(--zh-text-secondary);
  font-style: italic;
}
.article-prose :deep(blockquote p) { margin: 0; }
.article-prose :deep(code) {
  padding: 1px 5px;
  background: var(--zh-bg-hover);
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 0.88em;
  color: var(--zh-primary-dark);
}
:global(.dark) .article-prose :deep(code) { color: var(--zh-primary-light); }
.article-prose :deep(pre) {
  margin: 1.2em 0;
  padding: 12px;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-md);
  overflow-x: auto;
  font-size: 0.85em;
  line-height: 1.5;
}
.article-prose :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}
.article-prose :deep(ul),
.article-prose :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.4em;
}
.article-prose :deep(li) {
  margin: 0.35em 0;
}
.article-prose :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--zh-radius-md);
  margin: 0.8em 0;
  display: block;
}
.article-prose :deep(strong) {
  color: var(--zh-text);
  font-weight: 700;
}
.article-prose :deep(hr) {
  border: none;
  height: 1px;
  background: var(--zh-border);
  margin: 1.6em 0;
}
.article-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.2em 0;
  font-size: 0.9em;
}
.article-prose :deep(th),
.article-prose :deep(td) {
  padding: 6px 10px;
  border: 1px solid var(--zh-border);
  text-align: left;
}
.article-prose :deep(th) {
  background: var(--zh-bg-hover);
  font-weight: 600;
}

/* ========== 图片网格 ========== */
.article-images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  margin: 12px 0 14px;
  border-radius: var(--zh-radius-md);
  overflow: hidden;
}
.article-image-item {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--zh-bg-hover);
  cursor: pointer;
  transition: opacity 200ms ease;
}
.article-image-item:hover { opacity: 0.92; }
.article-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 400ms cubic-bezier(0.16, 1, 0.3, 1);
}
.article-image-item:hover img { transform: scale(1.05); }

/* ========== 标签 ========== */
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 12px 0 14px;
}

/* ========== 互动按钮栏 ========== */
.interaction-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 8px;
  margin: 16px 0;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-xs);
}
.interaction-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 6px 4px;
  background: transparent;
  border: none;
  border-radius: var(--zh-radius-md);
  color: var(--zh-text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease, transform 120ms ease;
  min-height: 44px;
}
.interaction-btn:hover {
  background: var(--zh-bg-hover);
  color: var(--zh-text);
}
.interaction-btn:active { transform: scale(0.96); }
.interaction-btn.active {
  color: var(--zh-primary);
  background: var(--zh-primary-bg);
}
.interaction-btn-icon svg {
  width: 18px;
  height: 18px;
  stroke-width: 2;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.interaction-btn:hover .interaction-btn-icon svg {
  transform: translateY(-1px);
}
.interaction-btn.active .interaction-btn-icon svg {
  transform: scale(1.1);
}
.interaction-btn-count {
  font-weight: 500;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

/* ========== 评论区 ========== */
.comments-section {
  margin: 16px 0;
  /* 移动端需要为底部输入框预留空间 */
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}
@media (min-width: 768px) {
  .comments-section {
    padding-bottom: 0;
  }
}

.comment-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--zh-text-tertiary);
  font-size: 12px;
}
.comment-status-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ========== 2xl 侧边栏 ========== */
.article-aside {
  width: 100%;
}
@media (min-width: 1536px) {
  .article-aside {
    width: 280px;
    flex-shrink: 0;
  }
}
.article-aside-sticky {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.aside-card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  box-shadow: var(--zh-shadow-xs);
}
.aside-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--zh-border-light);
  background: var(--zh-bg-hover);
}
.aside-card-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.aside-card-badge {
  font-size: 10px;
  color: var(--zh-text-tertiary);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}
.aside-card-body {
  padding: 6px;
  max-height: 60vh;
  overflow-y: auto;
}
.aside-toc {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.aside-toc-item {
  display: block;
  padding: 4px 8px;
  font-size: 11.5px;
  color: var(--zh-text-secondary);
  text-decoration: none;
  border-radius: var(--zh-radius-sm);
  transition: background 150ms ease, color 150ms ease;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.aside-toc-item:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}
</style>