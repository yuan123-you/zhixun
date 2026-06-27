<template>
  <!-- 作品卡片组件 - 全宽布局 -->
  <article class="px-2.5 py-2 md:px-4 md:py-3 cursor-pointer no-tap-highlight touch-feedback" @click="navigateToDetail">
    <!-- 作者信息栏（标题上方） -->
    <div class="flex items-center gap-1.5 mb-0.5">
      <!-- 作者头像 -->
      <NuxtLink :to="`/user/${article.authorId || article.author?.id}`" class="shrink-0 flex items-center" @click.stop>
        <UserAvatar :src="article.authorAvatar || article.author?.avatar" :alt="article.authorName || article.author?.nickname" size="sm" />
      </NuxtLink>
      <div class="flex-1 min-w-0 flex items-center gap-1">
        <!-- 作者姓名 + 发布时间 -->
        <NuxtLink :to="`/user/${article.authorId || article.author?.id}`" class="text-xs md:text-sm font-medium text-slate-900 hover:text-primary transition-colors truncate max-w-[120px]" @click.stop>
          {{ article.authorName || article.author?.nickname }}
        </NuxtLink>
        <span class="text-[10px] md:text-xs text-slate-400 shrink-0">·</span>
        <time class="text-[10px] md:text-xs text-slate-400 shrink-0">{{ formatTimestamp(article.createdAt) }}</time>
        <template v-if="article.location">
          <span class="text-[10px] md:text-xs text-slate-400 shrink-0">·</span>
          <span class="text-[10px] md:text-xs text-slate-400 shrink-0 inline-flex items-center gap-0.5">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {{ article.location }}
          </span>
        </template>
        <span v-if="article.deviceInfo" class="text-[10px] md:text-xs text-slate-400 shrink-0 hidden sm:inline">来自{{ article.deviceInfo }}</span>
        <span v-if="article.matchType" class="ml-auto inline-flex items-center px-1 py-0.5 rounded text-[10px] font-medium shrink-0"
          :class="matchTypeStyle">
          {{ matchTypeLabel }}
        </span>
      </div>
    </div>

    <!-- 作品内容区 -->
    <div class="flex gap-1.5">
      <div class="flex-1 min-w-0">
        <!-- 标题（支持高亮HTML） -->
        <h3 v-if="article.matchType === 'title'" class="text-sm md:text-base font-semibold text-slate-900 line-clamp-2 mb-0.5" v-html="article.title" />
        <h3 v-else class="text-sm md:text-base font-semibold text-slate-900 line-clamp-2 mb-0.5">
          {{ article.title }}
        </h3>

        <!-- 正文内容（溢出省略+全文按钮，全文按钮紧挨正文内联） -->
        <div
          v-if="article.matchType === 'content' && article.contentSnippet"
          class="text-xs md:text-sm text-slate-500 mb-0 article-content-wrap"
        >
          <span class="article-text-inline search-snippet" v-html="article.contentSnippet" />
          <button
            v-if="hasContent"
            class="inline-flex text-xs text-primary hover:text-primary-600 font-medium transition-colors align-baseline"
            @click.stop="navigateToDetail"
          >
            全文
          </button>
        </div>
        <div
          v-else-if="displayContent"
          class="text-xs md:text-sm text-slate-500 mb-0 article-content-wrap"
        >
          <span class="article-text-inline">
            {{ displayContent }}
            <button
              v-if="hasContent"
              class="inline-flex text-xs text-primary hover:text-primary-600 font-medium transition-colors align-baseline"
              @click.stop="navigateToDetail"
            >
              全文
            </button>
          </span>
        </div>
      </div>

      <!-- 封面图（右侧，sm以上显示） -->
      <div v-if="article.coverImage" class="hidden sm:block w-20 md:w-28 h-16 md:h-24 shrink-0">
        <img v-if="autoLoadImages" :src="resolveUrl(article.coverImage) || ''" :alt="typeof article.title === 'string' ? article.title : ''" class="w-full h-full object-cover rounded-lg" loading="lazy" />
        <div v-else class="w-full h-full bg-slate-100 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
      </div>
    </div>

    <!-- 话题标签 -->
    <div v-if="article.topicTags?.length" class="flex flex-wrap gap-1 mt-1">
      <TopicBadge v-for="t in article.topicTags" :key="t.id" :id="t.id" :name="t.name" :is-official="t.isOfficial" />
    </div>

    <!-- 作品互动部分（内容下方）- 三列 grid 两端对齐 -->
    <div class="grid grid-cols-3 mt-0.5">
      <!-- 点赞 -->
      <button
        class="flex items-center justify-self-start rounded-full text-xs transition-colors active:scale-95"
        :class="article.isLiked ? 'text-primary' : 'text-slate-400 hover:text-primary'"
        @click.stop="handleToggleLike"
      >
        <svg class="w-5 h-5 md:w-6 md:h-6" :class="article.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
        <span class="text-[10px] md:text-xs">{{ formatCount(article.likeCount) }}</span>
      </button>

      <!-- 评论 - 跳转到作品评论区 -->
      <button
        class="flex items-center justify-self-center rounded-full text-xs text-slate-400 hover:text-primary transition-colors active:scale-95"
        @click.stop="navigateToComments"
      >
        <svg class="w-5 h-5 md:w-6 md:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span class="text-[10px] md:text-xs">{{ formatCount(article.commentCount) }}</span>
      </button>

      <!-- 分享/转发 - 弹出分享面板 -->
      <div class="justify-self-end" @click.stop>
        <button
          class="flex items-center rounded-full text-xs text-slate-400 hover:text-primary transition-colors active:scale-95"
          @click="showShareMenu = !showShareMenu"
        >
          <svg class="w-5 h-5 md:w-6 md:h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          <span class="text-[10px] md:text-xs">{{ formatCount(article.shareCount) }}</span>
        </button>

        <!-- 分享弹出面板 - 固定定位居中，避免移动端溢出 -->
        <Teleport to="body">
          <Transition name="share-menu">
            <div
              v-if="showShareMenu"
              class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/30"
              @click.self="showShareMenu = false"
            >
              <div class="w-full sm:w-auto sm:min-w-[180px] mx-4 sm:mx-0 bg-white rounded-t-xl sm:rounded-xl shadow-lg border border-slate-200 py-1">
                <!-- 复制链接 -->
                <button class="flex items-center gap-3 w-full px-4 py-3 sm:px-3 sm:py-2 text-sm sm:text-xs text-slate-700 hover:bg-slate-50 transition-colors" @click="shareCopyLink">
                  <svg class="w-5 h-5 sm:w-4 sm:h-4 text-gray-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                  </svg>
                  <span>复制链接</span>
                </button>
                <!-- 微信 -->
                <button class="flex items-center gap-3 w-full px-4 py-3 sm:px-3 sm:py-2 text-sm sm:text-xs text-slate-700 hover:bg-slate-50 transition-colors" @click="shareToWechat">
                  <svg class="w-5 h-5 sm:w-4 sm:h-4 text-green-500 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 01.598.082l1.584.926a.272.272 0 00.14.045c.133 0 .241-.11.241-.246 0-.06-.024-.12-.04-.178l-.325-1.233a.492.492 0 01.177-.554C23.025 18.265 24 16.572 24 14.71c0-3.38-3.126-5.852-7.062-5.852zm-2.095 2.99c.535 0 .969.44.969.983a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.544.434-.983.97-.983zm4.19 0c.535 0 .969.44.969.983a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.544.434-.983.97-.983z"/>
                  </svg>
                  <span>微信</span>
                </button>
                <!-- 微博 -->
                <button class="flex items-center gap-3 w-full px-4 py-3 sm:px-3 sm:py-2 text-sm sm:text-xs text-slate-700 hover:bg-slate-50 transition-colors" @click="shareToWeibo">
                  <svg class="w-5 h-5 sm:w-4 sm:h-4 text-red-500 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zm-1.409-7.498c-2.623.269-4.587 2.108-4.387 4.11.193 1.99 2.47 3.349 5.09 3.072 2.624-.269 4.588-2.108 4.388-4.107-.196-2-2.473-3.349-5.091-3.075zm1.172 4.664c-.251.623-.929.903-1.513.627-.577-.271-.789-.972-.537-1.584.249-.609.913-.892 1.497-.625.585.27.804.967.553 1.582zm1.427-1.747c-.1.233-.335.349-.524.259-.186-.09-.258-.342-.164-.569.097-.228.327-.344.516-.256.189.087.268.336.172.566zm.757-7.586c-2.875-.798-6.114.077-7.353 2.096-.478.778-.424 1.508.017 2.043.826 1.001 2.553.321 2.553.321s-.945.312-1.566-.156c-.459-.346-.356-.935.015-1.468.843-1.22 3.086-1.874 5.259-1.332 2.171.542 3.583 2.067 3.583 3.412 0 .699-.541 1.047-1.123 1.079-.001 0-.729.045-.729.045s.445.186.938.186c.932 0 1.657-.623 1.657-1.578 0-2.096-1.595-3.846-3.251-4.648zM20.5 7.908c-.562-1.396-1.836-2.181-3.149-2.181-.277 0-.557.037-.832.113a.75.75 0 00-.499.936.762.762 0 00.941.493 2.078 2.078 0 012.56 1.246c.349.866-.009 1.896-.834 2.393a.752.752 0 00-.263 1.032.764.764 0 001.04.261c1.289-.776 1.848-2.316 1.336-3.293z"/>
                  </svg>
                  <span>微博</span>
                </button>
                <!-- QQ -->
                <button class="flex items-center gap-3 w-full px-4 py-3 sm:px-3 sm:py-2 text-sm sm:text-xs text-slate-700 hover:bg-slate-50 transition-colors" @click="shareToQQ">
                  <svg class="w-5 h-5 sm:w-4 sm:h-4 text-blue-500 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12.003 2c-2.265 0-6.29 1.364-6.29 7.325v1.195S3.55 14.96 3.55 17.474c0 .665.17 1.025.396 1.025.116 0 .263-.072.42-.215.83-.798 2.616-2.478 3.145-2.937.078-.067.16-.1.24-.1h8.502c.08 0 .16.033.24.1.529.46 2.315 2.14 3.145 2.937.157.143.304.215.42.215.227 0 .396-.36.396-1.025 0-2.514-2.163-6.954-2.163-6.954V9.325C18.292 3.364 14.268 2 12.003 2zm-2.34 5.21a1.21 1.21 0 110-2.42 1.21 1.21 0 010 2.42zm4.68 0a1.21 1.21 0 110-2.42 1.21 1.21 0 010 2.42z"/>
                  </svg>
                  <span>QQ</span>
                </button>
              </div>
            </div>
          </Transition>
        </Teleport>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
/** 作品卡片组件 - 微博风格布局，支持搜索结果高亮 */
import type { Article } from '~/types'

const props = defineProps<{
  article: Article
}>()

const { resolveUrl } = useResourceUrl()
const userStore = useUserStore()
const { invalidateArticle } = useCacheInvalidation()
const { autoLoadImages } = useLocalSettings()

// 分享菜单状态
const showShareMenu = ref(false)

// 去除 HTML 标签，获取纯文本
const stripHtml = (html: string): string => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '')
}

// 显示的正文内容（纯文本）
const displayContent = computed(() => {
  const content = props.article.content
  if (!content) {
    // 降级到摘要
    return props.article.summary || ''
  }
  return stripHtml(content)
})

// 是否有内容可展示
const hasContent = computed(() => {
  return !!displayContent.value || !!props.article.contentSnippet
})

// 点击外部关闭分享菜单
onMounted(() => {
  const closeMenu = (e: MouseEvent) => {
    if (showShareMenu.value) showShareMenu.value = false
  }
  document.addEventListener('click', closeMenu)
  onUnmounted(() => document.removeEventListener('click', closeMenu))
})

// 匹配类型标签
const matchTypeLabel = computed(() => {
  switch (props.article.matchType) {
    case 'title': return '标题匹配'
    case 'summary': return '摘要匹配'
    case 'content': return '正文匹配'
    default: return ''
  }
})

// 匹配类型样式
const matchTypeStyle = computed(() => {
  switch (props.article.matchType) {
    case 'title': return 'bg-blue-50 text-blue-700'
    case 'summary': return 'bg-emerald-50 text-emerald-700'
    case 'content': return 'bg-amber-50 text-amber-700'
    default: return ''
  }
})

// 跳转到作品详情
const navigateToDetail = () => {
  navigateTo(`/articles/${props.article.id}`)
}

// 跳转到作品评论区
const navigateToComments = () => {
  navigateTo(`/articles/${props.article.id}#comments`)
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

// 获取作品链接（SSR 安全：仅在客户端事件处理器中调用）
const getArticleUrl = () => {
  if (import.meta.server) return `/articles/${props.article.id}`
  return `${window.location.origin}/articles/${props.article.id}`
}

// 复制链接
const shareCopyLink = async () => {
  showShareMenu.value = false
  try {
    await navigator.clipboard.writeText(getArticleUrl())
    const { articleApi } = await import('~/api')
    articleApi.recordShare(props.article.id, 'copy')
  } catch {
    // 复制失败
  }
}

// 分享到微信
const shareToWechat = async () => {
  showShareMenu.value = false
  try {
    await navigator.clipboard.writeText(getArticleUrl())
    const { articleApi } = await import('~/api')
    articleApi.recordShare(props.article.id, 'wechat')
  } catch {
    // 复制失败
  }
}

// 分享到微博
const shareToWeibo = async () => {
  showShareMenu.value = false
  const url = encodeURIComponent(getArticleUrl())
  const title = encodeURIComponent(typeof props.article.title === 'string' ? props.article.title : '')
  window.open(`https://service.weibo.com/share/share.php?url=${url}&title=${title}`, '_blank', 'width=600,height=500')
  try {
    const { articleApi } = await import('~/api')
    articleApi.recordShare(props.article.id, 'weibo')
  } catch {
    // 记录失败
  }
}

// 分享到QQ
const shareToQQ = async () => {
  showShareMenu.value = false
  const url = encodeURIComponent(getArticleUrl())
  const title = encodeURIComponent(typeof props.article.title === 'string' ? props.article.title : '')
  const summary = encodeURIComponent(props.article.summary || '')
  window.open(`https://connect.qq.com/widget/shareqq/index.html?url=${url}&title=${title}&summary=${summary}`, '_blank', 'width=600,height=500')
  try {
    const { articleApi } = await import('~/api')
    articleApi.recordShare(props.article.id, 'qq')
  } catch {
    // 记录失败
  }
}
</script>

<style scoped>
/* 作品正文 - 溢出省略6行，max-height作为后备确保移动端正确截断 */
.article-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 6;
  line-clamp: 6;
  overflow: hidden;
  word-break: break-word;
  /* 后备方案：6行 × 1.5行高 × 字号 ≈ 9em */
  max-height: 9em;
  /* 防止v-html中的块级元素破坏line-clamp计数 */
  text-overflow: ellipsis;
}

/* 内联正文容器（全文按钮紧跟其后，使用max-height截断） */
.article-content-wrap {
  overflow: hidden;
  max-height: 5.4em; /* ~3-4行 */
  word-break: break-word;
}

/* 内联正文（全文按钮紧跟其后） */
.article-text-inline {
  display: inline;
}


/* 搜索结果高亮样式 */
.search-snippet :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb, 99, 102, 241), 0.1);
  border-radius: 2px;
  padding: 0 2px;
}

/* 标题高亮样式 */
h3 :deep(em) {
  font-style: normal;
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: underline;
  text-decoration-color: rgba(var(--color-primary-rgb, 99, 102, 241), 0.3);
  text-underline-offset: 2px;
}

/* 正文高亮样式 */
.article-text :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb, 99, 102, 241), 0.1);
  border-radius: 2px;
  padding: 0 2px;
}

/* v-html内容中所有元素转为inline，确保line-clamp正确计数 */
.article-text :deep(*),
.article-text-inline :deep(*) {
  display: inline;
}

/* 确保br标签不产生额外行高 */
.article-text :deep(br),
.article-text-inline :deep(br) {
  display: none;
}

/* 分享菜单动画 - 移动端底部上滑，桌面端缩放淡入 */
.share-menu-enter-active,
.share-menu-leave-active {
  transition: opacity 0.2s ease;
}
.share-menu-enter-active > div,
.share-menu-leave-active > div {
  transition: transform 0.2s ease;
}
.share-menu-enter-from,
.share-menu-leave-to {
  opacity: 0;
}
.share-menu-enter-from > div {
  transform: translateY(100%);
}
.share-menu-leave-to > div {
  transform: translateY(100%);
}

@media (min-width: 640px) {
  .share-menu-enter-from > div {
    transform: scale(0.95) translateY(0);
  }
  .share-menu-leave-to > div {
    transform: scale(0.95) translateY(0);
  }
}

/* ===== 移动端紧凑对齐 (与 Tailwind md:768px 断点一致) ===== */
@media (max-width: 767.98px) {
  /* 移动端缩小标题与正文间距 */
  h3 {
    margin-bottom: 2px;
  }

  /* 移动端互动按钮紧凑间距 */
  .grid.grid-cols-3.mt-0\.5 {
    margin-top: 2px;
  }

}
</style>
