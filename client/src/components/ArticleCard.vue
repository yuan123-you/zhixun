<template>
  <!-- 作品卡片 - 极简内容优先 + 微交互 -->
  <article class="article-card card-base hover-lift" @click="navigateToDetail">
    <!-- 作者信息栏 -->
    <div class="card-author">
      <RouterLink :to="`/user/${article.authorId || article.author?.id}`" class="author-avatar" @click.stop>
        <UserAvatar :src="article.authorAvatar || article.author?.avatar" :alt="article.authorName || article.author?.nickname" size="sm" />
      </RouterLink>
      <div class="author-meta">
        <RouterLink :to="`/user/${article.authorId || article.author?.id}`" class="author-name" @click.stop>
          {{ article.authorName || article.author?.nickname }}
        </RouterLink>
        <span class="meta-dot">·</span>
        <time class="meta-time">{{ formatTimestamp(article.createdAt) }}</time>
        <template v-if="article.location">
          <span class="meta-dot">·</span>
          <span class="meta-loc">
            <el-icon :size="12"><Location /></el-icon>
            {{ article.location }}
          </span>
        </template>
        <span v-if="article.deviceInfo" class="meta-device">· {{ article.deviceInfo }}</span>
        <el-tag v-if="article.matchType" size="small" :type="matchTagType" class="match-tag" effect="plain">
          {{ matchTypeLabel }}
        </el-tag>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="card-content">
      <div class="content-text">
        <h3 v-if="article.matchType === 'title'" class="content-title" v-html="sanitizeHtml(article.title)" />
        <h3 v-else class="content-title">{{ article.title }}</h3>

        <p v-if="article.matchType === 'content' && article.contentSnippet" class="content-snippet search-snippet" v-html="sanitizeHtml(article.contentSnippet)" />
        <p v-else-if="displayContent" class="content-snippet">
          <span class="snippet-text">{{ displayContent }}</span><button v-if="hasContent" class="read-more" @click.stop="navigateToDetail">查看全文</button>
        </p>
      </div>

      <!-- 封面图 -->
      <div v-if="article.coverImage && !coverImageError" class="cover-image-wrap">
        <img v-if="autoLoadImages" :src="resolveUrl(article.coverImage) || ''" :alt="typeof article.title === 'string' ? article.title : ''" class="cover-image" loading="lazy" @error="coverImageError = true" />
        <div v-else class="cover-placeholder">
          <el-icon :size="18"><PictureFilled /></el-icon>
        </div>
      </div>
      <div v-else-if="article.coverImage && coverImageError" class="cover-image-wrap">
        <div class="cover-placeholder cover-placeholder--error">
          <el-icon :size="18"><PictureFilled /></el-icon>
        </div>
      </div>
    </div>

    <!-- 话题标签 -->
    <div v-if="article.topicTags?.length" class="topic-tags">
      <TopicBadge v-for="t in article.topicTags" :key="t.id" :id="t.id" :name="t.name" :is-official="t.isOfficial" />
    </div>

    <!-- 互动栏 -->
    <div class="card-actions">
      <button class="action-btn" :class="{ 'action-btn--active': article.isLiked }" @click.stop="handleToggleLike">
        <el-icon :size="18"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg></el-icon>
        <span>{{ formatCount(article.likeCount) }}</span>
      </button>
      <button class="action-btn" @click.stop="navigateToComments">
        <el-icon :size="18"><ChatDotRound /></el-icon>
        <span>{{ formatCount(article.commentCount) }}</span>
      </button>
      <button class="action-btn" @click.stop="handleShareClick">
        <el-icon :size="18"><Share /></el-icon>
        <span>{{ formatCount(article.shareCount) }}</span>
      </button>
    </div>

    <!-- 分享面板 -->
    <Teleport to="body">
      <Transition name="panel-slide">
        <div v-if="showShareMenu" class="share-overlay" @click.self="showShareMenu = false">
          <div class="share-panel">
            <button class="share-item" @click="shareCopyLink">
              <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/></svg>
              <span>复制链接</span>
            </button>
            <button class="share-item" @click="shareToWechat">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm3.024 4.22c-2.538 0-4.595 2.094-4.595 4.677 0 2.582 2.057 4.677 4.595 4.677.462 0 .92-.07 1.358-.203a.494.494 0 01.408.055l1.09.638a.186.186 0 00.095.03c.091 0 .166-.074.166-.168 0-.04-.015-.081-.027-.122l-.223-.846a.338.338 0 01.122-.38c1.048-.77 1.666-1.92 1.666-3.681 0-2.583-2.057-4.677-4.595-4.677zm-1.5 2.692c.367 0 .665.302.665.674a.67.67 0 01-.665.674.67.67 0 01-.665-.674c0-.372.298-.674.665-.674zm3.006 0c.367 0 .665.302.665.674a.67.67 0 01-.665.674.67.67 0 01-.665-.674c0-.372.298-.674.665-.674z"/></svg>
              <span>微信分享</span>
            </button>
            <button class="share-item" @click="shareToWeibo">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zm-3.522-4.955c-.543.574-.694 1.682-.367 2.3.334.635 1.116.927 2.094.751.484-.087.957-.285 1.275-.514.12-.085.184-.192.16-.278-.068-.247-.46-.363-1.002-.332-.387.022-.81-.088-1.044-.314-.285-.276-.098-.662.465-.893.26-.108.542-.153.817-.153h.003c.436 0 .72.135.781.283.036.087.009.156-.044.212-.225.254-.535.449-.535.449s.386-.06.876-.06h.071c.616 0 1.234.155 1.67.414.43.254.65.57.596.808-.035.163-.178.297-.435.374.219.066.392.19.48.37.156.318.03.798-.35 1.202-.617.652-1.72 1.148-3.035 1.34-1.306.189-2.5.018-3.068-.465-.57-.484-.44-1.222.329-1.898zm9.453-5.415c-.355.053-.665-.248-.693-.673-.026-.427.23-.807.585-.86.353-.054.664.248.69.673.027.427-.229.806-.582.86zm.368-1.563c-.534.14-1.1-.303-1.261-.99-.162-.688.14-1.355.674-1.494.534-.14 1.1.302 1.263.988.16.688-.143 1.355-.676 1.496zm3.233-.18c-.142.102-.33.04-.432-.177-.505-1.05-1.416-2.025-2.811-2.52-1.394-.495-2.748-.447-3.636.066-.219.125-.478.025-.58-.224-.1-.248.002-.541.222-.666.974-.562 2.512-.66 4.114-.093 2.201.78 3.817 2.601 3.995 3.704.044.271-.182.507-.43.489-.168-.012-.284-.127-.31-.28-.102-.642-.604-1.367-1.286-1.97-.681-.604-1.5-1.04-2.196-1.242-.17-.048-.87-.22-1.1-.303-.174-.062-.328-.174-.468-.317-.003-.002-.005-.006-.006-.008.002-.002.003-.004.005-.006.034-.007.066-.014.09-.02.262-.09.5-.164.796-.214.002-.001.003-.001.005-.001.005 0 .01.001.014.004.061.01.11.056.123.102.002.006.004.011.004.017.003.018.003.036.002.052-.002.094-.054.165-.13.196-1.01.425-1.696 1.043-2.147 1.788-.226.377-.367.79-.448 1.165-.029.133-.006.238.07.291z"/></svg>
              <span>微博分享</span>
            </button>
            <button class="share-item" @click="shareToQQ">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12.003 2c-2.265 0-4.103 1.783-4.103 3.997 0 .912.318 1.747.846 2.412-1.597.965-2.978 2.147-4.058 3.472-.813.998-1.42 2.085-1.706 3.127-.155.564-.07 1.088.274 1.488.342.398.88.607 1.496.607.21 0 .424-.03.64-.092.622-.178 1.2-.386 1.76-.608.47-.187.933-.37 1.41-.53l.068-.023c-.025.533-.045 1.08-.06 1.638-.032 1.22.03 2.484.3 3.533.3 1.166.85 1.99 1.67 2.413.83.425 1.96.66 3.334.66h.04c1.37 0 2.5-.235 3.334-.66.82-.424 1.374-1.247 1.672-2.413.27-1.05.333-2.313.3-3.533-.015-.558-.035-1.105-.06-1.638l.068.023c.477.16.94.343 1.41.53.56.222 1.138.43 1.76.608.216.062.43.092.64.092.617 0 1.154-.21 1.496-.607.345-.4.43-.924.274-1.488-.286-1.042-.893-2.13-1.706-3.127-1.08-1.325-2.46-2.507-4.058-3.472.528-.665.846-1.5.846-2.412C16.106 3.783 14.268 2 12.003 2z"/></svg>
              <span>QQ 分享</span>
            </button>
            <button class="share-item share-cancel" @click="showShareMenu = false">取消</button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </article>
</template>

<script setup lang="ts">
import type { Article } from '@/types'
import { Location, PictureFilled, ChatDotRound, Share } from '@element-plus/icons-vue'
import { sanitizeHtml } from '@/utils/sanitize'

const props = defineProps<{ article: Article }>()

const { resolveUrl } = useResourceUrl()
const userStore = useUserStore()
const { invalidateArticle } = useCacheInvalidation()
const { autoLoadImages } = useLocalSettings()
const router = useRouter()

const showShareMenu = ref(false)
// 跟踪封面图加载失败状态，加载失败后展示占位符以避免重复请求
const coverImageError = ref(false)

const stripHtml = (html: string): string => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '')
}

const displayContent = computed(() => {
  const content = props.article.content
  if (!content) return props.article.summary || ''
  return stripHtml(content)
})

const hasContent = computed(() => !!displayContent.value || !!props.article.contentSnippet)

onMounted(() => {
  const close = () => { showShareMenu.value = false }
  document.addEventListener('click', close, { once: true })
})

const matchTypeLabel = computed(() => {
  switch (props.article.matchType) {
    case 'title': return '标题匹配'
    case 'summary': return '摘要匹配'
    case 'content': return '正文匹配'
    default: return ''
  }
})

const matchTagType = computed(() => {
  switch (props.article.matchType) {
    case 'title': return 'primary'
    case 'summary': return 'success'
    case 'content': return 'warning'
    default: return 'info'
  }
})

const navigateToDetail = () => navigateTo(`/articles/${props.article.id}`)
const navigateToComments = () => navigateTo(`/articles/${props.article.id}#comments`)

const formatTimestamp = (time: string | undefined) => {
  if (!time) return ''
  const d = new Date(time)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}

const handleToggleLike = async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const { interactionApi } = await import('@/api')
    const r = await interactionApi.toggleLike(props.article.id)
    props.article.isLiked = r.data.data.isLiked
    props.article.likeCount = r.data.data.likeCount
    invalidateArticle()
  } catch (e: any) {
    console.error('点赞操作失败:', e?.message || e)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  }
}

const getArticleUrl = () => `${window.location.origin}/articles/${props.article.id}`

const handleShareClick = (e: Event) => {
  e.stopPropagation()
  showShareMenu.value = !showShareMenu.value
}

const shareCopyLink = async () => {
  showShareMenu.value = false
  await navigator.clipboard.writeText(getArticleUrl()).catch(() => {})
  ElMessage.success('链接已复制')
}

const shareToWechat = () => { showShareMenu.value = false; ElMessage.info('请复制链接到微信分享') }
const shareToWeibo = () => { showShareMenu.value = false; window.open(`https://service.weibo.com/share/share.php?url=${encodeURIComponent(getArticleUrl())}`, '_blank') }
const shareToQQ = () => { showShareMenu.value = false; window.open(`https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(getArticleUrl())}`, '_blank') }
</script>

<style scoped>
.article-card {
  padding: 12px 12px;
  cursor: pointer;
  transition: transform var(--zh-transition-base), box-shadow var(--zh-transition-base), border-color var(--zh-transition-base);
  margin-bottom: 6px;
  /* 修复子元素(尤其 grid 的 .card-actions)在 flex/grid 父级中可能撑出容器 */
  box-sizing: border-box;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}
@media (max-width: 767.98px) {
  .article-card { padding: 8px 10px; margin-bottom: 10px; margin-top: 6px; }
  .article-card:first-child { margin-top: 6px; }
}

/* 作者行 */
.card-author {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 2px;
  min-height: 0; /* 兜底防止被全局 pointer:coarse 规则撑高 */
}
.card-author a {
  min-height: 0;
  min-width: 0;
}
.author-avatar {
  flex-shrink: 0;
  display: flex;
  min-height: 0;
  min-width: 0;
  transform: scale(0.85);
  transform-origin: left center;
  margin-right: -5px; /* 抵消 scale 造成的视觉间距 */
}
.author-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 3px;
  flex-wrap: wrap;
  min-height: 0;
  line-height: 1.2;
}
.author-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--zh-text);
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-decoration: none;
  transition: color var(--zh-transition-fast);
}
.author-name:hover { color: var(--zh-primary); }
@media (min-width: 768px) { .author-name { font-size: 14px; } }

.meta-dot, .meta-time, .meta-loc {
  font-size: 11px;
  color: var(--zh-text-tertiary);
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.meta-loc { gap: 2px; }
.meta-device { font-size: 11px; color: var(--zh-text-tertiary); display: none; }
@media (min-width: 640px) { .meta-device { display: inline; } }

.match-tag { margin-left: auto; flex-shrink: 0; }

/* 内容区 */
.card-content { display: flex; gap: 10px; box-sizing: border-box; min-width: 0; }
.content-text { flex: 1; min-width: 0; }

.content-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--zh-text);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin-bottom: 2px;
  letter-spacing: -0.01em;
}
@media (min-width: 768px) { .content-title { font-size: 17px; margin-bottom: 8px; } }

.content-snippet {
  font-size: 12.5px;
  color: var(--zh-text-secondary);
  line-height: 1.35;
  word-break: break-word;
  max-height: 2.7em; /* 2 行高(13 × 1.35 × 2) */
  overflow: hidden;
}
.snippet-text {
  display: inline;
  word-break: break-word;
}
.content-snippet .read-more {
  /* inline-block 紧跟文本流,不会强制换行(满足"不占用新的一行");
     float: right 让按钮始终贴右显示,文本会自动在按钮前换行;
     配合父级 max-height:2.7em + overflow:hidden 实现 2 行截断;
     不再用 position:absolute 避免遮挡文字 */
  float: right;
  display: inline-block;
  font-size: 11px;
  line-height: 1.35;
  color: var(--zh-primary);
  font-weight: 500;
  background: var(--zh-bg-elevated);
  border: none;
  padding: 0;
  margin: 0;
  cursor: pointer;
  white-space: nowrap;
}
.content-snippet .read-more:hover { text-decoration: underline; }
@media (min-width: 768px) { .content-snippet .read-more { font-size: 12px; } }

/* 独立 .read-more 规则:未在 .content-snippet 内时也使用相同基础样式(避免被全局样式覆盖) */
.read-more {
  display: inline-block;
  font-size: 12px;
  color: var(--zh-primary);
  font-weight: 500;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  white-space: nowrap;
  line-height: 1.35;
}

/* 封面图 */
.cover-image-wrap {
  display: none;
  width: 72px;
  height: 56px;
  flex-shrink: 0;
}
@media (min-width: 640px) { .cover-image-wrap { display: block; } }
@media (min-width: 768px) { .cover-image-wrap { width: 120px; height: 88px; } }

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--zh-radius-md);
  transition: transform var(--zh-transition-slow);
}
.article-card:hover .cover-image { transform: scale(1.04); }

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--zh-text-placeholder);
}

/* 话题标签 */
.topic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
  box-sizing: border-box;
  min-width: 0;
  max-width: 100%;
}

/* 互动栏 - 移动端 fix:
   父容器宽 375px,DPR=2 时 3 个 flex: 1 子项的均分会出现
   0.01px 舍入误差,导致按钮宽度轻微不一致。改为 grid 避免。
   显式 box-sizing + width:100% + min-width:0 防止子元素 grid 把容器撑出。 */
.card-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
  gap: 2px;
  margin: 0;
  padding: 0;
  line-height: 1;
  min-height: 0;
  height: 28px;
  border-top: 1px solid var(--zh-border-light);
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.action-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0 4px;
  line-height: 1;
  height: 100%;
  font-size: 12px;
  color: var(--zh-text-tertiary);
  background: none;
  border: none;
  border-radius: var(--zh-radius-md);
  cursor: pointer;
  transition: all var(--zh-transition-fast);
}
.action-btn:hover { color: var(--zh-primary); background: var(--zh-primary-bg); }
.action-btn:active { transform: scale(0.96); }
.action-btn--active { color: var(--zh-danger) !important; }
.action-btn--active svg { fill: var(--zh-danger); }

/* 分享面板 */
.share-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
@media (min-width: 640px) { .share-overlay { align-items: center; } }

.share-panel {
  width: 100%;
  max-width: 360px;
  margin: 0 12px 12px;
  background: var(--zh-bg-elevated);
  border-radius: var(--zh-radius-xl);
  box-shadow: var(--zh-shadow-xl);
  padding: 8px;
  overflow: hidden;
}
@media (min-width: 640px) {
  .share-panel { margin: 0; border-radius: var(--zh-radius-xl); }
}

.share-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 14px;
  font-size: 15px;
  font-weight: 500;
  color: var(--zh-text);
  background: none;
  border: none;
  border-radius: var(--zh-radius-md);
  cursor: pointer;
  transition: background var(--zh-transition-fast);
}
.share-item:hover { background: var(--zh-bg-hover); }
.share-item:active { transform: scale(0.98); }
.share-cancel {
  color: var(--zh-text-tertiary);
  font-weight: 400;
  margin-top: 4px;
  border-top: 1px solid var(--zh-border-light);
  border-radius: 0;
  padding-top: 16px;
}

.panel-slide-enter-active { transition: opacity var(--zh-transition-fast); }
.panel-slide-enter-active .share-panel { transition: transform var(--zh-transition-spring); }
.panel-slide-leave-active { transition: opacity var(--zh-transition-fast); }
.panel-slide-leave-active .share-panel { transition: transform var(--zh-transition-base); }
.panel-slide-enter-from { opacity: 0; }
.panel-slide-enter-from .share-panel { transform: translateY(100%); }
.panel-slide-leave-to { opacity: 0; }
.panel-slide-leave-to .share-panel { transform: translateY(100%); }
@media (min-width: 640px) {
  .panel-slide-enter-from .share-panel,
  .panel-slide-leave-to .share-panel {
    transform: scale(0.92) translateY(0);
  }
}

/* 搜索高亮 */
.search-snippet :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--zh-primary);
  background: var(--zh-primary-bg);
  border-radius: 2px;
  padding: 0 2px;
}
.content-title :deep(em) {
  font-style: normal;
  font-weight: 700;
  color: var(--zh-primary);
}
</style>
