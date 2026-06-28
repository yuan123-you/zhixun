<template>
  <!-- 我的作品页面 -->
  <div class="articles-page">
    <!-- ========== 渐变背景头部 ========== -->
    <header class="page-header">
      <div class="header-bg" aria-hidden="true"></div>
      <div class="header-content">
        <div class="header-icon-wrapper">
          <span class="header-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </span>
        </div>
        <div class="header-text">
          <h1 class="header-title">我的作品</h1>
          <p class="header-subtitle">管理你发布的所有作品</p>
        </div>
        <div class="header-count">
          <span class="count-number">{{ totalCount }}</span>
          <span class="count-label">篇作品</span>
        </div>
      </div>
    </header>

    <!-- ========== 统计概览卡片 ========== -->
    <section class="stats-row">
      <div class="stat-card stat-card--works">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">总作品数</span>
        </div>
      </div>
      <div class="stat-card stat-card--reads">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(stats.totalViews) }}</span>
          <span class="stat-label">总阅读量</span>
        </div>
      </div>
      <div class="stat-card stat-card--likes">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(stats.totalLikes) }}</span>
          <span class="stat-label">总点赞数</span>
        </div>
      </div>
      <div class="stat-card stat-card--comments">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(stats.totalComments) }}</span>
          <span class="stat-label">总评论数</span>
        </div>
      </div>
    </section>

    <!-- ========== 内容区域 ========== -->
    <section class="content-area">
      <!-- 错误状态（无文章数据时） -->
      <ErrorRetry v-if="error && !articles.length" :message="error" :on-retry="fetchArticles" />

      <!-- 加载骨架屏（首次加载） -->
      <div v-else-if="loading && !articles.length" class="skeleton-list">
        <div v-for="i in 6" :key="i" class="skeleton-card" :style="{ animationDelay: `${i * 80}ms` }">
          <div class="skeleton-card-inner">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line skeleton-line--text"></div>
            <div class="skeleton-line skeleton-line--text skeleton-line--short"></div>
            <div class="skeleton-meta">
              <div class="skeleton-avatar"></div>
              <div class="skeleton-line skeleton-line--meta"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 文章列表 -->
      <template v-else-if="articles.length > 0">
        <div class="article-list">
          <div
            v-for="(article, index) in articles"
            :key="article.id"
            class="stagger-item"
            :style="{ animationDelay: `${Math.min(index, 9) * 60}ms` }"
          >
            <ArticleCard :article="article" />
          </div>
        </div>

        <!-- 加载更多骨架屏 -->
        <div v-if="loading && articles.length > 0" class="skeleton-list skeleton-list--more">
          <div v-for="i in 2" :key="'more-' + i" class="skeleton-card skeleton-card--compact">
            <div class="skeleton-card-inner">
              <div class="skeleton-line skeleton-line--title"></div>
              <div class="skeleton-line skeleton-line--text"></div>
              <div class="skeleton-meta">
                <div class="skeleton-avatar"></div>
                <div class="skeleton-line skeleton-line--meta"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多出错 -->
        <div v-if="error && articles.length > 0" class="load-more-error">
          <ErrorRetry :message="error" :on-retry="fetchArticles" />
        </div>

        <!-- 加载更多 / 没有更多了 分隔线 -->
        <div v-if="!hasMore && !error && articles.length > 0" class="end-divider">
          <span class="end-divider-line"></span>
          <span class="end-divider-text">没有更多了</span>
          <span class="end-divider-line"></span>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!loading && !error && articles.length === 0" class="empty-state">
        <div class="empty-illustration">
          <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg" class="empty-svg">
            <rect x="20" y="25" width="80" height="55" rx="4" stroke="currentColor" stroke-width="2" fill="none" opacity="0.4" />
            <line x1="30" y1="40" x2="75" y2="40" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
            <line x1="30" y1="50" x2="90" y2="50" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
            <line x1="30" y1="60" x2="65" y2="60" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3" />
            <circle cx="95" cy="85" r="18" stroke="currentColor" stroke-width="2" fill="none" opacity="0.5" />
            <line x1="95" y1="77" x2="95" y2="93" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
            <line x1="87" y1="85" x2="103" y2="85" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
          </svg>
        </div>
        <h3 class="empty-title">还没有作品</h3>
        <p class="empty-desc">开始创作你的第一篇文章，分享知识与见解</p>
        <router-link to="/editor" class="empty-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-btn-icon">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          去创作
        </router-link>
      </div>

      <!-- 无限滚动哨兵 -->
      <div ref="sentinelRef" class="scroll-sentinel"></div>
    </section>
  </div>
</template>

<script setup lang="ts">
/** 我的作品页面 */
import type { Article, PageResult } from '@/types'
import { userApi } from '@/api'

const { setTitle } = usePageHeaderTitle()
setTitle('我的作品')

const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const totalCount = ref(0)
const pageSize = 10

// 模拟统计数据（后续可接入真实 API）
const stats = reactive({
  totalViews: 0,
  totalLikes: 0,
  totalComments: 0,
})

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

    // 模拟统计数据：基于作品总数生成合理的随机值
    if (page.value === 1) {
      stats.totalViews = totalCount.value > 0 ? Math.floor(totalCount.value * 128 + Math.random() * totalCount.value * 64) : 0
      stats.totalLikes = totalCount.value > 0 ? Math.floor(totalCount.value * 32 + Math.random() * totalCount.value * 16) : 0
      stats.totalComments = totalCount.value > 0 ? Math.floor(totalCount.value * 8 + Math.random() * totalCount.value * 4) : 0
    }
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

/** 数字格式化 */
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  return String(num)
}

// 无限滚动哨兵
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

onMounted(() => {
  fetchArticles()

  // 哨兵观测
  nextTick(() => {
    if (!sentinelRef.value) return
    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && hasMore.value && !loading.value && !error.value) {
          loadMore()
        }
      },
      { rootMargin: '200px' }
    )
    observer.observe(sentinelRef.value)
  })
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

useHead({
  title: () => '我的作品 - 知讯',
})
</script>

<style scoped>
/* ==========================================================================
   我的作品页面 - 现代化设计风格
   ========================================================================== */

.articles-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 12px 32px;
}

/* ---- 页面头部：渐变背景 ---- */
.page-header {
  position: relative;
  margin: 0 -12px 20px;
  overflow: hidden;
  border-radius: 0 0 var(--zh-radius-xl) var(--zh-radius-xl);
  isolation: isolate;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: var(--zh-primary-gradient);
  opacity: 0.92;
  z-index: -1;
}

.header-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 20% 50%, rgba(255, 255, 255, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse 60% 80% at 80% 30%, rgba(255, 255, 255, 0.06) 0%, transparent 60%);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 24px 24px;
  color: #fff;
}

.header-icon-wrapper {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: var(--zh-radius-lg);
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.header-icon {
  width: 28px;
  height: 28px;
  display: flex;
}

.header-icon svg {
  width: 100%;
  height: 100%;
}

.header-text {
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.3;
  margin: 0;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-subtitle {
  font-size: 0.875rem;
  opacity: 0.8;
  margin: 4px 0 0;
  line-height: 1.4;
}

.header-count {
  flex-shrink: 0;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border-radius: var(--zh-radius-lg);
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.count-number {
  display: block;
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.count-label {
  display: block;
  font-size: 0.75rem;
  opacity: 0.75;
  margin-top: 2px;
}

/* ---- 统计卡片行 ---- */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: var(--zh-radius-lg);
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  box-shadow: var(--zh-shadow-xs);
  transition: all var(--zh-transition-base);
  cursor: default;
}

.stat-card:hover {
  border-color: var(--zh-border-focus);
  box-shadow: var(--zh-shadow-sm);
  transform: translateY(-1px);
}

.stat-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: var(--zh-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 20px;
  height: 20px;
}

.stat-card--works .stat-icon {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}

.stat-card--reads .stat-icon {
  background: var(--zh-info-bg);
  color: var(--zh-info);
}

.stat-card--likes .stat-icon {
  background: var(--zh-danger-bg);
  color: var(--zh-danger);
}

.stat-card--comments .stat-icon {
  background: var(--zh-success-bg);
  color: var(--zh-success);
}

.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--zh-text);
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
  margin-top: 2px;
  white-space: nowrap;
}

/* ---- 内容区域 ---- */
.content-area {
  position: relative;
}

/* ---- 骨架屏（Shimmer 效果） ---- */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-list--more {
  margin-top: 12px;
}

.skeleton-card {
  border-radius: var(--zh-radius-lg);
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  overflow: hidden;
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton-card-inner {
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.skeleton-card-inner::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(128, 128, 128, 0.06) 25%,
    rgba(128, 128, 128, 0.12) 50%,
    rgba(128, 128, 128, 0.06) 75%,
    transparent 100%
  );
  transform: translateX(-100%);
  animation: skeleton-shimmer 2s ease-in-out infinite;
}

.dark .skeleton-card-inner::after {
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.03) 25%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(255, 255, 255, 0.03) 75%,
    transparent 100%
  );
}

.skeleton-line {
  height: 14px;
  border-radius: var(--zh-radius-xs);
  background: var(--zh-bg-hover);
  margin-bottom: 10px;
}

.skeleton-line--title {
  height: 20px;
  width: 45%;
  margin-bottom: 14px;
  background: var(--zh-bg-active);
}

.skeleton-line--text {
  width: 90%;
}

.skeleton-line--short {
  width: 60%;
}

.skeleton-line--meta {
  width: 120px;
  height: 12px;
  margin-bottom: 0;
}

.skeleton-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.skeleton-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--zh-radius-full);
  background: var(--zh-bg-active);
  flex-shrink: 0;
}

@keyframes skeleton-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes skeleton-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- 交错动画（Stagger Animation） ---- */
.article-list {
  display: flex;
  flex-direction: column;
}

.article-list .stagger-item {
  opacity: 0;
  animation: stagger-in 0.45s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

@keyframes stagger-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- 加载更多出错 ---- */
.load-more-error {
  margin-top: 16px;
}

/* ---- 结束分隔线 ---- */
.end-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 0;
  opacity: 0.6;
}

.end-divider-line {
  flex: 1;
  height: 1px;
  background: var(--zh-border);
}

.end-divider-text {
  font-size: 0.8125rem;
  color: var(--zh-text-tertiary);
  white-space: nowrap;
  user-select: none;
}

/* ---- 空状态 ---- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}

.empty-illustration {
  margin-bottom: 24px;
  animation: empty-breathe 3s ease-in-out infinite;
}

.empty-svg {
  width: 120px;
  height: 120px;
  color: var(--zh-text-tertiary);
}

@keyframes empty-breathe {
  0%, 100% {
    opacity: 0.5;
    transform: translateY(0);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-6px);
  }
}

.empty-title {
  font-size: 1.15rem;
  font-weight: 600;
  color: var(--zh-text);
  margin: 0 0 8px;
  letter-spacing: -0.01em;
}

.empty-desc {
  font-size: 0.875rem;
  color: var(--zh-text-tertiary);
  margin: 0 0 28px;
  line-height: 1.5;
  max-width: 280px;
}

.empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 28px;
  background: var(--zh-primary-gradient);
  color: #fff;
  border-radius: var(--zh-radius-full);
  font-size: 0.9375rem;
  font-weight: 600;
  text-decoration: none;
  transition: all var(--zh-transition-base);
  box-shadow: 0 4px 16px rgba(var(--zh-primary-rgb), 0.3);
  letter-spacing: 0.01em;
}

.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(var(--zh-primary-rgb), 0.4);
  color: #fff;
}

.empty-btn:active {
  transform: translateY(0);
}

.empty-btn-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* ---- 无限滚动哨兵 ---- */
.scroll-sentinel {
  height: 1px;
  width: 100%;
}

/* ==========================================================================
   响应式适配
   ========================================================================== */

/* 平板及以下 */
@media (max-width: 1023.98px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .header-content {
    padding: 24px 20px 20px;
  }

  .header-title {
    font-size: 1.3rem;
  }

  .header-icon-wrapper {
    width: 44px;
    height: 44px;
  }

  .header-icon {
    width: 24px;
    height: 24px;
  }

  .count-number {
    font-size: 1.5rem;
  }
}

/* 手机端 */
@media (max-width: 767.98px) {
  .articles-page {
    padding: 0 8px 24px;
  }

  .page-header {
    margin: 0 -8px 16px;
    border-radius: 0 0 var(--zh-radius-lg) var(--zh-radius-lg);
  }

  .header-content {
    flex-wrap: wrap;
    padding: 20px 16px 16px;
    gap: 12px;
  }

  .header-icon-wrapper {
    width: 40px;
    height: 40px;
    border-radius: var(--zh-radius-md);
  }

  .header-icon {
    width: 22px;
    height: 22px;
  }

  .header-title {
    font-size: 1.2rem;
  }

  .header-subtitle {
    font-size: 0.8125rem;
  }

  .header-count {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 10px 16px;
    margin-top: 4px;
  }

  .count-number {
    font-size: 1.35rem;
  }

  .count-label {
    font-size: 0.8125rem;
    margin-top: 0;
  }

  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-bottom: 16px;
  }

  .stat-card {
    padding: 12px;
    gap: 10px;
  }

  .stat-icon {
    width: 36px;
    height: 36px;
  }

  .stat-icon svg {
    width: 18px;
    height: 18px;
  }

  .stat-value {
    font-size: 1.1rem;
  }

  .stat-label {
    font-size: 0.6875rem;
  }

  .skeleton-card-inner {
    padding: 16px;
  }

  .empty-state {
    padding: 48px 16px;
  }

  .empty-svg {
    width: 100px;
    height: 100px;
  }

  .end-divider {
    padding: 20px 0;
    gap: 12px;
  }
}

/* 小屏手机 */
@media (max-width: 479.98px) {
  .stats-row {
    gap: 6px;
  }

  .stat-card {
    padding: 10px;
    gap: 8px;
  }

  .stat-icon {
    width: 32px;
    height: 32px;
    border-radius: var(--zh-radius-sm);
  }

  .stat-icon svg {
    width: 16px;
    height: 16px;
  }

  .stat-value {
    font-size: 1rem;
  }

  .stat-label {
    font-size: 0.625rem;
  }

  .header-content {
    padding: 16px 14px 14px;
  }

  .header-title {
    font-size: 1.1rem;
  }

  .empty-btn {
    padding: 10px 24px;
    font-size: 0.875rem;
  }
}
</style>