<template>
  <!-- 排行榜页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">

    <!-- 页面头部 -->
    <section class="rank-hero">
      <div class="rank-hero-bg"></div>
      <div class="rank-hero-glow"></div>
      <div class="rank-hero-content">
        <div class="rank-hero-icon-wrap">
          <svg class="rank-hero-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8.5 14.5A2.5 2.5 0 0011 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 11-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 002.5 2.5z" />
          </svg>
        </div>
        <div>
          <h1 class="rank-hero-title">热榜</h1>
          <p class="rank-hero-desc">实时热门内容排行</p>
        </div>
      </div>
    </section>

    <!-- 说明文字 -->
    <div class="rank-section-desc">
      <span class="rank-desc-text">基于阅读、点赞、评论等维度的综合热度排行</span>
      <span class="rank-update-time">实时更新</span>
    </div>

    <!-- 日榜/周榜/月榜切换 -->
    <div class="rank-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="rank-tab"
        :class="{ 'rank-tab--active': activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 排行列表 - 三态切换：loading / list / empty -->
    <div class="rank-content-wrap">
      <Transition name="rank-fade" mode="out-in">
        <div v-if="loading" key="loading" class="rank-loading-wrap">
          <div v-for="i in 5" :key="i" class="rank-skeleton" :style="{ animationDelay: `${i * 0.08}s` }">
            <div class="rank-skeleton-badge"></div>
            <div class="rank-skeleton-content">
              <div class="rank-skeleton-line w-3/4"></div>
              <div class="rank-skeleton-line w-1/3 mt-1.5"></div>
            </div>
            <div class="rank-skeleton-heat"></div>
          </div>
        </div>

        <div v-else-if="rankItems.length > 0" key="list" class="rank-list-container">
          <TransitionGroup name="rank-list-item" tag="div" class="contents">
            <div
              v-for="(item, index) in rankItems"
              :key="item.id"
              class="rank-list-item"
              :class="getRankItemClass(index)"
              :style="{ animationDelay: `${index * 0.06}s` }"
              @click="goToArticle(item.id)"
            >
              <!-- 左侧渐变装饰条 -->
              <div v-if="index < 3" class="rank-accent-bar" :class="`rank-accent-bar--${index}`"></div>

              <!-- 排名徽章 -->
              <div class="rank-pos" :class="getRankPosClass(index)">
                <template v-if="index === 0">
                  <span class="rank-medal-icon">🥇</span>
                </template>
                <template v-else-if="index === 1">
                  <span class="rank-medal-icon">🥈</span>
                </template>
                <template v-else-if="index === 2">
                  <span class="rank-medal-icon">🥉</span>
                </template>
                <template v-else>
                  {{ index + 1 }}
                </template>
              </div>

              <!-- 作品信息 -->
              <div class="rank-info">
                <div class="rank-info-title" :class="{ 'rank-info-title--top': index < 3 }">{{ item.title }}</div>
                <div class="rank-info-meta">
                  <span>{{ item.authorNickname }}</span>
                  <span>{{ item.likeCount }}</span>
                  <span>{{ item.commentCount }}</span>
                </div>
              </div>

              <!-- 趋势指示器 -->
              <div class="rank-trend" :class="getTrendClass(index)">
                {{ ['↑', '↓', '→'][index % 3] }}
              </div>

              <!-- 热度值 -->
              <div class="rank-heat-value" :class="getHeatClass(item.score ?? 0)">
                <span class="heat-num">{{ formatHeat(item.score ?? 0) }}</span>
                <span class="heat-label">热度</span>
              </div>
            </div>
          </TransitionGroup>
        </div>

        <div v-else key="empty" class="rank-empty">
          <svg class="rank-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
          <p class="rank-empty-title">暂无热榜数据</p>
          <p class="rank-empty-desc">当前时段暂无排行内容，请稍后再来</p>
        </div>
      </Transition>
    </div>

    <!-- 错误状态 -->
    <ErrorRetry v-if="error && !loading" :message="error" :on-retry="retryLastRequest" />
  </div>
</template>

<script setup lang="ts">
/** 排行榜页：日榜/周榜/月榜 */
import type { RankItem, ApiResponse } from '@/types'

// 跳转到作品详情页（使用 router 实例而非 navigateTo 自动导入，避免模板事件中未定义）
const router = useRouter()
const goToArticle = (id: number | string) => router.push(`/articles/${id}`)

const { setTitle } = usePageHeaderTitle()
setTitle('热榜')

const tabs = [
  { key: 'daily', label: '日榜' },
  { key: 'weekly', label: '周榜' },
  { key: 'monthly', label: '月榜' },
]

const activeTab = ref('daily')
const rankItems = ref<RankItem[]>([])
const loading = ref(false)
const error = ref('')

// 切换Tab - 修复：
// 1. 旧实现 label 用 computed(()=>'日榜')，模板里 {{ tab.label }} 渲染成 RefImpl 对象，文字不显示
// 2. 旧实现切 Tab 时未清空 rankItems，导致新数据加载完成前仍显示旧榜单，造成"内容错乱"
// 3. 旧实现无过渡动画，Tab 切换体验生硬
const switchTab = (key: string) => {
  if (activeTab.value === key || loading.value) return
  activeTab.value = key
  // 立即清空当前列表，使新数据加载期间显示骨架屏而非旧内容
  rankItems.value = []
  error.value = ''
  fetchRank()
}

// 获取排行数据
const fetchRank = async () => {
  // 同一 Tab 重复点击时，若已有数据则不重新请求（避免无谓请求）
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    const api = useApi()
    const res = await api.get<RankItem[]>('/rank/hot', { period: activeTab.value })
    // 仅当 Tab 未被切换时才更新数据（避免用户快速切换时旧 Tab 数据覆盖新 Tab）
    if (res.data?.data) {
      rankItems.value = res.data.data
    } else {
      rankItems.value = []
    }
  } catch {
    error.value = '加载排行榜失败，请稍后重试'
    rankItems.value = []
  } finally {
    loading.value = false
  }
}

// 重试上次失败的请求
const retryLastRequest = () => {
  fetchRank()
}

// 获取排行卡片样式
const getRankItemClass = (index: number) => {
  if (index === 0) return 'rank-list-item--gold'
  if (index === 1) return 'rank-list-item--silver'
  if (index === 2) return 'rank-list-item--bronze'
  return ''
}

// 获取排名徽章样式
const getRankPosClass = (index: number) => {
  if (index === 0) return 'rank-pos--gold'
  if (index === 1) return 'rank-pos--silver'
  if (index === 2) return 'rank-pos--bronze'
  return 'rank-pos--other'
}

// 获取趋势样式
const getTrendClass = (index: number) => {
  const types = ['rank-trend--up', 'rank-trend--down', 'rank-trend--same']
  return types[index % 3]
}

// 获取热度等级样式
const getHeatClass = (score: number) => {
  if (score >= 10000) return 'rank-heat-value--hot'
  if (score >= 1000) return 'rank-heat-value--warm'
  return ''
}

// 格式化热度值
const formatHeat = (score: number) => {
  if (score >= 10000) return `${(score / 10000).toFixed(1)}万`
  if (score >= 100) return Math.round(score).toString()
  if (score >= 1) return score.toFixed(1)
  return score.toFixed(2)
}

// 初始化数据获取
const { data: initialData } = useAsyncData('rank-init', async () => {
  try {
    const api = useApi()
    const res = await api.get<RankItem[]>('/rank/hot', { period: 'daily' })
    return res.data?.data || []
  } catch {
    return []
  }
})

rankItems.value = initialData.value || []

// 如果初始数据为空，在客户端重新获取
onMounted(() => {
  if (rankItems.value.length === 0 && !loading.value) {
    fetchRank()
  }
})

// 页面元信息
useHead({
  title: () => '热榜' + ' - 知讯',
})
</script>

<style scoped>
/* ==========================================================================
   热榜页样式 —— 知讯 (Zhixun)
   ========================================================================== */

/* ========== 页面头部渐变 ========== */
.rank-hero {
  position: relative;
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  padding: 1.5rem 1.5rem;
  margin-bottom: 0.75rem;
}

.rank-hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 50%, #f97316 100%);
  opacity: 0.08;
  pointer-events: none;
}

.rank-hero-glow {
  position: absolute;
  top: -30%;
  right: -10%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.rank-hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.rank-hero-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--zh-radius-md);
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
  animation: fire-glow 2s ease-in-out infinite;
}

@keyframes fire-glow {
  0%, 100% { box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35); }
  50% { box-shadow: 0 4px 22px rgba(245, 158, 11, 0.55), 0 0 0 4px rgba(245, 158, 11, 0.08); }
}

.rank-hero-icon {
  width: 22px;
  height: 22px;
  color: #fff;
}

.rank-hero-title {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--zh-text);
  margin: 0;
  line-height: 1.2;
}

.rank-hero-desc {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  color: var(--zh-text-secondary);
  line-height: 1.5;
}

/* ========== 说明文字 ========== */
.rank-section-desc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.875rem;
  padding: 0 2px;
}

.rank-desc-text {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
}

.rank-update-time {
  font-size: 0.6875rem;
  color: var(--zh-text-tertiary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.rank-update-time::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: update-dot-pulse 1.5s ease-in-out infinite;
}

@keyframes update-dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ========== Tab 切换器 Pill ========== */
.rank-tabs {
  display: inline-flex;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-full);
  padding: 0.25rem;
  gap: 0.125rem;
  border: 1px solid var(--zh-border);
  margin-bottom: 1rem;
}

.rank-tab {
  position: relative;
  padding: 0.5rem 1.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: var(--zh-radius-full);
  border: none;
  background: transparent;
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  outline: none;
}

.rank-tab:hover:not(.rank-tab--active) {
  color: var(--zh-text);
}

.rank-tab--active {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.35);
}

/* ========== 列表容器 ========== */
.rank-list-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* ========== 列表项卡片 ========== */
.rank-list-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color var(--zh-transition-base);
  position: relative;
  overflow: hidden;
  animation: rank-stagger-in 0.5s ease-out both;
}

@keyframes rank-stagger-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rank-list-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--zh-shadow-md), 0 6px 20px rgba(245, 158, 11, 0.06);
  border-color: var(--zh-border-focus);
}

/* 前三名卡片 */
.rank-list-item--gold {
  border-left: 3px solid #f59e0b;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(245, 158, 11, 0.04) 100%);
}

.rank-list-item--silver {
  border-left: 3px solid #94a3b8;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(148, 163, 184, 0.04) 100%);
}

.rank-list-item--bronze {
  border-left: 3px solid #d97706;
  background: linear-gradient(135deg, var(--zh-bg-elevated) 0%, rgba(217, 119, 6, 0.04) 100%);
}

/* 左侧渐变装饰条 */
.rank-accent-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: var(--zh-radius-md) 0 0 var(--zh-radius-md);
}

.rank-accent-bar--0 {
  background: linear-gradient(180deg, #fbbf24, #f59e0b, #d97706);
}

.rank-accent-bar--1 {
  background: linear-gradient(180deg, #e2e8f0, #94a3b8, #64748b);
}

.rank-accent-bar--2 {
  background: linear-gradient(180deg, #fb923c, #f97316, #ea580c);
}

/* ========== 排名徽章 ========== */
.rank-pos {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 0.75rem;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}

.rank-list-item:hover .rank-pos {
  transform: scale(1.12);
}

/* 前三名大号渐变徽章 */
.rank-pos--gold {
  width: 2.5rem;
  height: 2.5rem;
  font-size: 0;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%);
  box-shadow: 0 3px 12px rgba(245, 158, 11, 0.45), 0 0 0 1px rgba(245, 158, 11, 0.2);
}

.rank-pos--silver {
  width: 2.5rem;
  height: 2.5rem;
  font-size: 0;
  background: linear-gradient(135deg, #e2e8f0 0%, #94a3b8 50%, #64748b 100%);
  box-shadow: 0 3px 12px rgba(148, 163, 184, 0.45), 0 0 0 1px rgba(148, 163, 184, 0.2);
}

.rank-pos--bronze {
  width: 2.5rem;
  height: 2.5rem;
  font-size: 0;
  background: linear-gradient(135deg, #fb923c 0%, #f97316 50%, #ea580c 100%);
  box-shadow: 0 3px 12px rgba(249, 115, 22, 0.45), 0 0 0 1px rgba(249, 115, 22, 0.2);
}

.rank-pos--other {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}

.rank-medal-icon {
  font-size: 1.25rem;
  line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
}

/* ========== 内容区域 ========== */
.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-info-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--zh-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.2rem;
  transition: color 0.2s ease;
}

.rank-list-item:hover .rank-info-title {
  color: #f59e0b;
}

.rank-info-title--top {
  font-size: 0.9375rem;
  font-weight: 700;
}

.rank-info-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.7rem;
  color: var(--zh-text-tertiary);
}

/* ========== 趋势指示器 ========== */
.rank-trend {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}

.rank-trend--up {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.rank-trend--down {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.rank-trend--same {
  background: var(--zh-bg-hover);
  color: var(--zh-text-tertiary);
}

/* ========== 热度值渐变 ========== */
.rank-heat-value {
  text-align: right;
  flex-shrink: 0;
  min-width: 3.5rem;
}

.heat-num {
  display: block;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--zh-warning);
  line-height: 1.2;
  transition: all 0.3s ease;
}

.rank-heat-value--warm .heat-num {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.rank-heat-value--hot .heat-num {
  font-size: 1rem;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.heat-label {
  display: block;
  font-size: 0.6rem;
  color: var(--zh-text-tertiary);
}

/* ========== 骨架屏 ========== */
.rank-skeleton {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  overflow: hidden;
  animation: rank-stagger-in 0.4s ease-out both;
}

.rank-skeleton-badge {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  flex-shrink: 0;
}

.rank-skeleton-content {
  flex: 1;
  min-width: 0;
}

.rank-skeleton-line {
  height: 0.75rem;
  border-radius: var(--zh-radius-xs);
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}

.rank-skeleton-heat {
  width: 2.5rem;
  height: 1.25rem;
  border-radius: var(--zh-radius-xs);
  background: linear-gradient(90deg, var(--zh-bg-hover) 25%, var(--zh-bg-active) 37%, var(--zh-bg-hover) 63%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ========== 空状态 ========== */
.rank-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
  text-align: center;
}

.rank-empty-icon {
  width: 3rem;
  height: 3rem;
  color: var(--zh-text-tertiary);
  margin-bottom: 0.75rem;
  opacity: 0.5;
}

.rank-empty-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--zh-text-secondary);
  margin: 0 0 0.25rem;
}

.rank-empty-desc {
  font-size: 0.8125rem;
  color: var(--zh-text-tertiary);
  margin: 0;
}

/* ========== Tab 切换列表过渡动画 ========== */
.rank-list-item-enter-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.rank-list-item-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
  position: absolute;
  width: 100%;
}

.rank-list-item-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.rank-list-item-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.rank-list-item-move {
  transition: transform 0.35s ease;
}

/* ========== Tab 内容切换过渡（loading/list/empty 之间） ========== */
.rank-fade-enter-active,
.rank-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.rank-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.rank-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
.rank-loading-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.rank-content-wrap {
  position: relative;
  min-height: 200px;
}
.rank-content-wrap .contents {
  display: contents;
}

/* ========== 响应式 ========== */
@media (max-width: 640px) {
  .rank-hero {
    padding: 1rem 1.25rem;
  }

  .rank-hero-icon-wrap {
    width: 36px;
    height: 36px;
    border-radius: var(--zh-radius-sm);
  }

  .rank-hero-icon {
    width: 18px;
    height: 18px;
  }

  .rank-hero-title {
    font-size: 1.25rem;
  }

  .rank-hero-desc {
    font-size: 0.8125rem;
  }

  .rank-tab {
    padding: 0.4375rem 1rem;
    font-size: 0.8125rem;
  }

  .rank-list-item {
    padding: 0.65rem 0.75rem;
    gap: 0.625rem;
  }

  .rank-pos--gold,
  .rank-pos--silver,
  .rank-pos--bronze {
    width: 2.125rem;
    height: 2.125rem;
  }

  .rank-medal-icon {
    font-size: 1rem;
  }

  .rank-info-title {
    font-size: 0.8125rem;
  }

  .rank-info-title--top {
    font-size: 0.875rem;
  }

  .rank-info-meta {
    gap: 0.375rem;
    font-size: 0.6875rem;
  }

  .heat-num {
    font-size: 0.8125rem;
  }

  .rank-heat-value--hot .heat-num {
    font-size: 0.9rem;
  }

  .rank-trend {
    width: 1.25rem;
    height: 1.25rem;
    font-size: 0.625rem;
  }
}
</style>