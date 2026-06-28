<template>
  <div class="zh-tags-page">
    <!-- 页面头部 - 渐变背景 -->
    <div class="page-header">
      <div class="header-bg-decor"></div>
      <div class="header-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
          <line x1="7" y1="7" x2="7.01" y2="7" />
        </svg>
      </div>
      <div class="header-text">
        <h1 class="header-title">标签</h1>
        <p class="header-subtitle">探索所有话题标签，发现感兴趣的内容</p>
      </div>
    </div>

    <!-- 标签统计 -->
    <div class="stats-bar">
      <div class="stats-item">
        <svg class="stats-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>
        <span class="stats-label">标签云</span>
        <span class="stats-value">{{ cloudTags.length }}</span>
      </div>
      <div class="stats-divider"></div>
      <div class="stats-item">
        <svg class="stats-icon hot" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" /></svg>
        <span class="stats-label">热门标签</span>
        <span class="stats-value">{{ hotTags.length }}</span>
      </div>
      <div class="stats-divider"></div>
      <div class="stats-item">
        <svg class="stats-icon followed" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><polyline points="17 11 19 13 23 9" /></svg>
        <span class="stats-label">已关注</span>
        <span class="stats-value">{{ followedTags.length }}</span>
      </div>
    </div>

    <!-- Tab切换 - pill风格 -->
    <div class="tabs-wrapper">
      <div class="tags-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tags-tab"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <span class="tab-btn-icon" v-if="tab.key === 'cloud'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" /></svg>
          </span>
          <span class="tab-btn-icon" v-if="tab.key === 'hot'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" /></svg>
          </span>
          <span class="tab-btn-icon" v-if="tab.key === 'followed'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><polyline points="17 11 19 13 23 9" /></svg>
          </span>
          <span>{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <div class="content-layout">
      <!-- 左侧主内容区 -->
      <div class="main-content">
        <!-- 标签云 -->
        <div v-if="activeTab === 'cloud'" class="card cloud-card">
          <div v-if="cloudLoading" class="flex items-center justify-center py-12">
            <LoadingSkeleton v-for="i in 12" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="cloudError" :message="cloudError" :on-retry="retryCloud" />
          <div v-else-if="cloudTags.length > 0" class="cloud-container">
            <button
              v-for="(tag, idx) in cloudTags"
              :key="tag.id"
              class="cloud-tag tag-cloud-item"
              :style="{
                ...getTagStyle(tag),
                animationDelay: `${idx * 0.04}s`,
              }"
              @click="selectTag(tag)"
            >
              <span class="cloud-tag-name">{{ tag.name }}</span>
              <span class="cloud-tag-count">{{ tag.articleCount }}</span>
            </button>
          </div>
          <div v-else class="empty-cloud">
            <svg class="empty-illustration" viewBox="0 0 200 120" fill="none">
              <circle cx="100" cy="50" r="30" stroke="var(--zh-border)" stroke-width="2" stroke-dasharray="4 4" />
              <circle cx="60" cy="65" r="18" stroke="var(--zh-border)" stroke-width="1.5" stroke-dasharray="3 3" />
              <circle cx="140" cy="65" r="22" stroke="var(--zh-border)" stroke-width="1.5" stroke-dasharray="3 3" />
              <circle cx="85" cy="35" r="14" stroke="var(--zh-border)" stroke-width="1.5" stroke-dasharray="3 3" />
              <circle cx="125" cy="38" r="16" stroke="var(--zh-border)" stroke-width="1.5" stroke-dasharray="3 3" />
              <line x1="70" y1="90" x2="130" y2="90" stroke="var(--zh-border)" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <EmptyState title="暂无标签" description="还没有创建任何标签" />
          </div>
        </div>

        <!-- 热门标签列表 -->
        <div v-if="activeTab === 'hot'" class="card hot-card">
          <div v-if="hotLoading" class="p-2 space-y-2">
            <LoadingSkeleton v-for="i in 8" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="hotError" :message="hotError" :on-retry="retryHot" />
          <div v-else-if="hotTags.length > 0" class="tag-list">
            <div
              v-for="(tag, index) in hotTags"
              :key="tag.id"
              class="tag-list-item"
              :style="{ animationDelay: `${index * 0.05}s` }"
              @click="selectTag(tag)"
            >
              <!-- 排名徽章 -->
              <div class="rank-badge" :class="index === 0 ? 'gold' : index === 1 ? 'silver' : index === 2 ? 'bronze' : 'normal'">
                <template v-if="index === 0">
                  <svg class="rank-medal" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10" /><text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">1</text></svg>
                </template>
                <template v-else-if="index === 1">
                  <svg class="rank-medal" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10" /><text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">2</text></svg>
                </template>
                <template v-else-if="index === 2">
                  <svg class="rank-medal" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10" /><text x="12" y="16" text-anchor="middle" fill="white" font-size="10" font-weight="bold">3</text></svg>
                </template>
                <span v-else class="rank-number">{{ index + 1 }}</span>
              </div>
              <!-- 标签信息 -->
              <div class="tag-info">
                <span class="tag-name">{{ tag.name }}</span>
                <span class="tag-meta">{{ tag.articleCount }} 个作品</span>
              </div>
              <!-- 关注按钮 -->
              <button
                class="follow-btn tag-follow-btn"
                :class="{ followed: tag.isFollowed }"
                @click.stop="toggleFollowTag(tag)"
              >
                <span v-if="!tag.isFollowed" class="follow-btn-content">
                  <svg class="follow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
                  关注
                </span>
                <span v-else class="follow-btn-content">
                  <svg class="follow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12" /></svg>
                  已关注
                </span>
              </button>
            </div>
          </div>
          <div v-else class="empty-state-wrapper">
            <svg class="empty-illustration" viewBox="0 0 200 120" fill="none">
              <path d="M80 55c0-5 4-10 10-10h20c6 0 10 5 10 10s-4 10-10 10H90c-6 0-10-5-10-10z" stroke="var(--zh-border)" stroke-width="2" />
              <path d="M70 70l-5 15h70l-5-15" stroke="var(--zh-border)" stroke-width="2" stroke-linecap="round" />
              <circle cx="100" cy="35" r="6" stroke="var(--zh-border)" stroke-width="1.5" />
            </svg>
            <EmptyState title="暂无热门标签" />
          </div>
        </div>

        <!-- 已关注标签 -->
        <div v-if="activeTab === 'followed'" class="card followed-card">
          <div v-if="followedLoading" class="p-2 space-y-2">
            <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="followedError" :message="followedError" :on-retry="retryFollowed" />
          <div v-else-if="followedTags.length > 0" class="tag-list">
            <div
              v-for="(tag, index) in followedTags"
              :key="tag.id"
              class="tag-list-item"
              :style="{ animationDelay: `${index * 0.05}s` }"
              @click="selectTag(tag)"
            >
              <div class="tag-avatar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>
              </div>
              <div class="tag-info">
                <span class="tag-name">{{ tag.name }}</span>
                <span class="tag-meta">{{ tag.articleCount }} 个作品</span>
              </div>
              <button
                class="follow-btn tag-follow-btn followed"
                @click.stop="toggleFollowTag(tag)"
              >
                <span class="follow-btn-content">
                  <svg class="follow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
                  取关
                </span>
              </button>
            </div>
          </div>
          <div v-else class="empty-state-wrapper">
            <svg class="empty-illustration" viewBox="0 0 200 120" fill="none">
              <circle cx="60" cy="50" r="12" stroke="var(--zh-border)" stroke-width="2" />
              <circle cx="100" cy="45" r="15" stroke="var(--zh-border)" stroke-width="2" />
              <circle cx="140" cy="50" r="12" stroke="var(--zh-border)" stroke-width="2" />
              <path d="M55 70l5 15h80l5-15" stroke="var(--zh-border)" stroke-width="2" stroke-linecap="round" />
              <line x1="80" y1="95" x2="120" y2="95" stroke="var(--zh-border)" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <EmptyState title="暂无关注标签" description="去热门标签中关注感兴趣的标签吧" />
          </div>
        </div>

        <!-- 选中标签后的作品列表 -->
        <div v-if="selectedTag" class="selected-section">
          <div class="selected-header">
            <div class="selected-header-left">
              <div class="selected-tag-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>
              </div>
              <h2 class="selected-title">「{{ selectedTag.name }}」相关作品</h2>
            </div>
            <button class="clear-btn" @click="clearSelection">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              清除筛选
            </button>
          </div>
          <ArticleList
            :articles="tagArticles"
            :loading="articlesLoading"
            :has-more="hasMoreArticles"
            @load-more="loadMoreArticles"
          />
        </div>
      </div>

      <!-- 右侧栏（桌面端） -->
      <aside class="sidebar">
        <!-- 当前选中标签信息 -->
        <div v-if="selectedTag" class="sidebar-card selected-info">
          <div class="selected-info-header">
            <div class="selected-info-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><line x1="7" y1="7" x2="7.01" y2="7" /></svg>
            </div>
            <h3 class="selected-info-title">{{ selectedTag.name }}</h3>
          </div>
          <div class="selected-info-body">
            <div class="info-row">
              <span class="info-label">作品数</span>
              <span class="info-value">{{ selectedTag.articleCount }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">状态</span>
              <span class="info-value" :class="{ followed: selectedTag.isFollowed }">
                {{ selectedTag.isFollowed ? '已关注' : '未关注' }}
              </span>
            </div>
            <button
              class="sidebar-follow-btn"
              :class="{ followed: selectedTag.isFollowed }"
              @click="toggleFollowTag(selectedTag)"
            >
              <span v-if="!selectedTag.isFollowed" class="sidebar-follow-content">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
                关注标签
              </span>
              <span v-else class="sidebar-follow-content">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12" /></svg>
                取消关注
              </span>
            </button>
          </div>
        </div>

        <!-- 热门标签快捷入口 -->
        <div class="sidebar-card quick-nav">
          <div class="sidebar-card-header">
            <svg class="sidebar-card-header-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" /></svg>
            <h3 class="sidebar-card-title">热门标签</h3>
          </div>
          <div class="quick-nav-tags">
            <button
              v-for="tag in hotTags.slice(0, 10)"
              :key="tag.id"
              class="quick-nav-tag"
              :class="{ active: selectedTag?.id === tag.id }"
              @click="selectTag(tag)"
            >
              {{ tag.name }}
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 标签聚合页：标签云、热门标签、已关注标签、标签下作品列表 */
import type { Tag, Article, PageResult, ApiResponse } from '@/types'

const { setTitle } = usePageHeaderTitle()
setTitle('标签')

const router = useRouter()
const userStore = useUserStore()

// 请求缓存
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

const tabs = [
  { key: 'cloud', label: '标签云' },
  { key: 'hot', label: '热门标签' },
  { key: 'followed', label: '已关注' },
]

const activeTab = ref('cloud')
const cloudTags = ref<Tag[]>([])
const hotTags = ref<Tag[]>([])
const followedTags = ref<Tag[]>([])
const selectedTag = ref<Tag | null>(null)
const tagArticles = ref<Article[]>([])
const cloudLoading = ref(false)
const hotLoading = ref(false)
const followedLoading = ref(false)
const articlesLoading = ref(false)
const hasMoreArticles = ref(false)
const articlesPage = ref(1)
const cloudError = ref('')
const hotError = ref('')
const followedError = ref('')

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  if (key === 'cloud' && cloudTags.value.length === 0) fetchCloudTags()
  if (key === 'hot' && hotTags.value.length === 0) fetchHotTags()
  if (key === 'followed') fetchFollowedTags()
}

// 获取标签云
const fetchCloudTags = async () => {
  cloudLoading.value = true
  cloudError.value = ''
  try {
    const api = useApi()
    const res = await api.get<Tag[]>('/tags/cloud')
    cloudTags.value = res.data?.data || []
  } catch {
    cloudError.value = '加载标签云失败'
    cloudTags.value = []
  } finally {
    cloudLoading.value = false
  }
}

// 获取热门标签
const fetchHotTags = async () => {
  hotLoading.value = true
  hotError.value = ''
  try {
    const api = useApi()
    const res = await api.get<Tag[]>('/tags/hot', {
      params: { limit: 30 },
    })
    hotTags.value = res.data?.data || []
  } catch {
    hotError.value = '加载热门标签失败'
    hotTags.value = []
  } finally {
    hotLoading.value = false
  }
}

// 获取已关注标签
const fetchFollowedTags = async () => {
  if (!userStore.isLoggedIn) {
    followedTags.value = []
    return
  }
  followedLoading.value = true
  followedError.value = ''
  try {
    const { tagApi } = await import('~/api/tag')
    const res = await tagApi.getFollowedTags()
    followedTags.value = res.data?.data || []
  } catch {
    followedError.value = '加载关注标签失败'
    followedTags.value = []
  } finally {
    followedLoading.value = false
  }
}

// 重试加载
const retryCloud = () => fetchCloudTags()
const retryHot = () => fetchHotTags()
const retryFollowed = () => fetchFollowedTags()

// 选择标签
const selectTag = (tag: Tag) => {
  selectedTag.value = tag
  articlesPage.value = 1
  tagArticles.value = []
  fetchTagArticles()
}

// 清除选择
const clearSelection = () => {
  selectedTag.value = null
  tagArticles.value = []
}

// 获取标签下作品
const fetchTagArticles = async () => {
  if (!selectedTag.value) return
  articlesLoading.value = true
  try {
    const api = useApi()
    const res = await api.get<PageResult<Article>>('/articles', {
      params: { tag_id: selectedTag.value.id, page: articlesPage.value, pageSize: 20 },
    })
    const data = res.data?.data
    const items = data?.list || []
    if (articlesPage.value === 1) {
      tagArticles.value = items
    } else {
      tagArticles.value.push(...items)
    }
    hasMoreArticles.value = items.length >= 20
  } catch {
    hasMoreArticles.value = false
  } finally {
    articlesLoading.value = false
  }
}

// 加载更多作品
const loadMoreArticles = () => {
  articlesPage.value++
  fetchTagArticles()
}

// 关注/取关标签
const toggleFollowTag = async (tag: Tag) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const { tagApi } = await import('~/api/tag')
    if (tag.isFollowed) {
      await tagApi.unfollowTag(tag.id)
      tag.isFollowed = false
    } else {
      await tagApi.followTag(tag.id)
      tag.isFollowed = true
    }
    // 刷新已关注列表
    if (activeTab.value === 'followed') {
      fetchFollowedTags()
    }
  } catch {
    // 错误已在拦截器中处理
  }
}

// 标签云样式：根据作品数计算大小和渐变颜色
const getTagStyle = (tag: Tag) => {
  const maxCount = Math.max(...cloudTags.value.map(t => t.articleCount), 1)
  const ratio = tag.articleCount / maxCount
  const fontSize = 0.875 + ratio * 0.75 // 14px ~ 26px
  // 丰富的渐变色彩组合
  const colorGradients = [
    ['#6366f1', '#8b5cf6'],  // indigo-violet
    ['#06b6d4', '#3b82f6'],  // cyan-blue
    ['#10b981', '#34d399'],  // emerald
    ['#f59e0b', '#f97316'],  // amber-orange
    ['#ef4444', '#ec4899'],  // red-pink
    ['#8b5cf6', '#d946ef'],  // violet-fuchsia
    ['#14b8a6', '#06b6d4'],  // teal-cyan
    ['#f97316', '#ef4444'],  // orange-red
    ['#3b82f6', '#6366f1'],  // blue-indigo
    ['#ec4899', '#8b5cf6'],  // pink-violet
    ['#22c55e', '#10b981'],  // green-emerald
    ['#e11d48', '#f43f5e'],  // rose
  ]
  const colorIndex = Math.abs(tag.id) % colorGradients.length
  const [from, to] = colorGradients[colorIndex]
  const opacity = 0.65 + ratio * 0.35
  return {
    fontSize: `${fontSize}rem`,
    background: `linear-gradient(135deg, ${from}${Math.round(opacity * 100)}%, ${to}${Math.round(opacity * 100)}%)`,
    color: '#fff',
    boxShadow: `0 2px 8px ${from}${Math.round(opacity * 40)}%`,
  }
}

// 排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'bg-yellow-400 text-white'
  if (index === 1) return 'bg-gray-300 text-white'
  if (index === 2) return 'bg-orange-400 text-white'
  return 'bg-[var(--zh-bg-hover)] text-[var(--zh-text-secondary)]'
}

// SSR数据获取
const { data: cloudData } = useAsyncData('tags-cloud', async () => {
  try {
    const api = useApi()
    const res = await api.get<Tag[]>('/tags/cloud')
    return res.data?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: hotData } = useAsyncData('tags-hot', async () => {
  try {
    const api = useApi()
    const res = await api.get<Tag[]>('/tags/hot', {
      params: { limit: 30 },
    })
    return res.data?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

cloudTags.value = cloudData.value
hotTags.value = hotData.value

// 客户端挂载后：如果SSR数据为空，重新获取（处理SSR时token不可用导致401的情况）
onMounted(() => {
  if (cloudTags.value.length === 0) {
    fetchCloudTags()
  }
  if (hotTags.value.length === 0) {
    fetchHotTags()
  }
})

// 页面元信息
useHead({
  title: '标签 - 知讯',
})
</script>

<style scoped>
/* ========== 动画关键帧 ========== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(79, 70, 229, 0); }
}

@keyframes gentle-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  33% { transform: translateY(-2px) rotate(1deg); }
  66% { transform: translateY(1px) rotate(-1deg); }
}

/* ========== 页面容器 ========== */
.zh-tags-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem 1.5rem 2rem;
}

@media (min-width: 1536px) {
  .zh-tags-page {
    max-width: 1400px;
    padding: 1.5rem 2rem 2rem;
  }
}

/* ========== 页面头部 ========== */
.page-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1.5rem 1.5rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08) 0%, rgba(139, 92, 246, 0.06) 40%, rgba(236, 72, 153, 0.04) 100%);
  border: 1px solid var(--zh-border);
  border-radius: 1rem;
  box-shadow: var(--zh-shadow-sm);
  overflow: hidden;
}

.header-bg-decor {
  position: absolute;
  top: -50%;
  right: -10%;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(139, 92, 246, 0.12));
  color: var(--zh-primary);
  flex-shrink: 0;
}

.header-icon svg {
  width: 1.5rem;
  height: 1.5rem;
}

.header-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
  line-height: 1.3;
}

.header-subtitle {
  font-size: 0.875rem;
  color: var(--zh-text-secondary);
  margin: 0.15rem 0 0;
}

/* ========== 统计栏 ========== */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: 0.75rem;
  box-shadow: var(--zh-shadow-sm);
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  justify-content: center;
}

.stats-icon {
  width: 1.1rem;
  height: 1.1rem;
  color: var(--zh-text-tertiary);
  flex-shrink: 0;
}

.stats-icon.hot {
  color: #f59e0b;
}

.stats-icon.followed {
  color: var(--zh-primary);
}

.stats-label {
  font-size: 0.8rem;
  color: var(--zh-text-tertiary);
}

.stats-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--zh-text);
  min-width: 1.5rem;
  text-align: center;
}

.stats-divider {
  width: 1px;
  height: 1.5rem;
  background: var(--zh-border);
}

/* ========== Tab 切换 ========== */
.tabs-wrapper {
  margin-bottom: 1.25rem;
}

.tabs-container {
  position: relative;
  display: flex;
  gap: 0.35rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: 9999px;
  padding: 0.3rem;
  box-shadow: var(--zh-shadow-sm);
}

.tab-btn {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.55rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--zh-text-secondary);
  background: transparent;
  border: none;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
}

.tab-btn:hover {
  color: var(--zh-text);
  background: var(--zh-bg-hover);
}

.tab-btn.active {
  color: #fff;
  background: linear-gradient(135deg, var(--zh-primary), #8b5cf6);
  box-shadow: 0 2px 12px rgba(79, 70, 229, 0.35);
}

.tab-btn-icon {
  display: flex;
  align-items: center;
}

.tab-btn-icon svg {
  width: 1rem;
  height: 1rem;
  transition: transform 0.25s ease;
}

.tab-btn.active .tab-btn-icon svg {
  transform: scale(1.1);
}

/* ========== 内容布局 ========== */
.content-layout {
  display: flex;
  gap: 1.5rem;
}

.main-content {
  flex: 1;
  min-width: 0;
}

/* ========== 通用卡片 ========== */
.card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: 1rem;
  box-shadow: var(--zh-shadow-sm);
  overflow: hidden;
}

/* ========== 标签云 ========== */
.cloud-card {
  padding: 1.5rem;
}

.cloud-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.625rem;
  justify-content: center;
  align-items: center;
  padding: 0.5rem 0;
}

.cloud-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.9rem;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  line-height: 1.4;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              filter 0.3s ease;
  animation: fadeInScale 0.4s ease both;
  position: relative;
  overflow: hidden;
}

.cloud-tag::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.18), transparent);
  border-radius: 9999px;
  pointer-events: none;
}

.cloud-tag:hover {
  transform: translateY(-3px) scale(1.12);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.18);
  filter: brightness(1.1);
  animation: gentle-float 1.5s ease-in-out infinite;
}

.cloud-tag:active {
  transform: scale(0.95);
}

.cloud-tag-name {
  position: relative;
  z-index: 1;
}

.cloud-tag-count {
  font-size: 0.7em;
  opacity: 0.75;
  position: relative;
  z-index: 1;
  font-weight: 500;
}

/* ========== 标签列表 ========== */
.tag-list {
  display: flex;
  flex-direction: column;
}

.tag-list-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.4s ease both;
  border-bottom: 1px solid var(--zh-border);
  border-radius: 0.5rem;
  margin: 0.15rem 0.5rem;
}

.tag-list-item:first-child {
  margin-top: 0.25rem;
}

.tag-list-item:last-child {
  border-bottom: none;
  margin-bottom: 0.25rem;
}

.tag-list-item:hover {
  background: var(--zh-bg-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-bottom-color: transparent;
}

/* ========== 排名徽章 ========== */
.rank-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  flex-shrink: 0;
  border-radius: 0.5rem;
  font-weight: 700;
  font-size: 0.8rem;
}

.rank-badge.gold {
  color: #d97706;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
}

.rank-badge.silver {
  color: #6b7280;
  background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
}

.rank-badge.bronze {
  color: #c2410c;
  background: linear-gradient(135deg, #ffedd5, #fed7aa);
}

.rank-badge.normal {
  color: var(--zh-text-tertiary);
  background: var(--zh-bg-hover);
}

.rank-medal {
  width: 1.5rem;
  height: 1.5rem;
}

.rank-number {
  font-size: 0.85rem;
}

/* ========== 标签信息 ========== */
.tag-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.tag-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--zh-text);
  line-height: 1.3;
}

.tag-meta {
  font-size: 0.75rem;
  color: var(--zh-text-tertiary);
}

/* ========== 标签头像（已关注） ========== */
.tag-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.6rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(139, 92, 246, 0.1));
  color: var(--zh-primary);
  flex-shrink: 0;
}

.tag-avatar svg {
  width: 1.1rem;
  height: 1.1rem;
}

/* ========== 关注按钮 ========== */
.follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.4rem 0.85rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: 9999px;
  border: 1.5px solid var(--zh-primary);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--zh-primary);
  color: #fff;
  flex-shrink: 0;
  white-space: nowrap;
}

.follow-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.follow-btn:active {
  transform: scale(0.96);
}

.follow-btn.followed {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  border-color: var(--zh-border);
}

.follow-btn.followed:hover {
  background: var(--zh-border);
  color: var(--zh-text);
  box-shadow: none;
}

.follow-btn-content {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.follow-icon {
  width: 0.85rem;
  height: 0.85rem;
  transition: transform 0.25s ease;
}

.follow-btn:hover .follow-icon {
  transform: rotate(90deg);
}

.follow-btn.followed:hover .follow-icon {
  transform: rotate(0deg);
}

/* ========== 选中标签作品区域 ========== */
.selected-section {
  margin-top: 1.5rem;
  animation: fadeInUp 0.4s ease both;
}

.selected-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding: 0.85rem 1rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.06), rgba(139, 92, 246, 0.04));
  border: 1px solid rgba(79, 70, 229, 0.12);
  border-radius: 0.75rem;
}

.selected-header-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.selected-tag-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg, var(--zh-primary), rgba(139, 92, 246, 0.8));
  color: #fff;
  flex-shrink: 0;
}

.selected-tag-icon svg {
  width: 1rem;
  height: 1rem;
}

.selected-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.35rem 0.7rem;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--zh-text-secondary);
  background: transparent;
  border: 1px solid var(--zh-border);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-btn:hover {
  color: #ef4444;
  border-color: #fecaca;
  background: #fef2f2;
}

.clear-btn svg {
  width: 0.85rem;
  height: 0.85rem;
}

/* ========== 右侧栏 ========== */
.sidebar {
  display: none;
  width: 20rem;
  flex-shrink: 0;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 1024px) {
  .sidebar {
    display: flex;
  }
}

@media (min-width: 1536px) {
  .sidebar {
    width: 24rem;
  }
}

/* ========== 侧边栏卡片 ========== */
.sidebar-card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: 1rem;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.sidebar-card:hover {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.03);
}

/* ========== 选中标签信息卡片 ========== */
.selected-info-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 1rem 1rem 0.75rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.06), rgba(139, 92, 246, 0.03));
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.selected-info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.6rem;
  background: linear-gradient(135deg, var(--zh-primary), rgba(139, 92, 246, 0.8));
  color: #fff;
  flex-shrink: 0;
}

.selected-info-icon svg {
  width: 1.1rem;
  height: 1.1rem;
}

.selected-info-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
  word-break: break-all;
}

.selected-info-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.85rem;
}

.info-label {
  color: var(--zh-text-secondary);
}

.info-value {
  font-weight: 600;
  color: var(--zh-text);
}

.info-value.followed {
  color: var(--zh-primary);
}

.sidebar-follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 0.6rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: 0.6rem;
  border: none;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--zh-primary);
  color: #fff;
}

.sidebar-follow-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.35);
}

.sidebar-follow-btn:active {
  transform: scale(0.97);
}

.sidebar-follow-btn.followed {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  border: 1px solid var(--zh-border);
}

.sidebar-follow-btn.followed:hover {
  background: var(--zh-border);
  color: var(--zh-text);
  box-shadow: none;
}

.sidebar-follow-content {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.sidebar-follow-content svg {
  width: 0.9rem;
  height: 0.9rem;
  transition: transform 0.25s ease;
}

.sidebar-follow-btn:not(.followed):hover .sidebar-follow-content svg {
  transform: rotate(90deg);
}

/* ========== 快捷导航卡片 ========== */
.sidebar-card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.85rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.sidebar-card-header-icon {
  width: 1rem;
  height: 1rem;
  color: #f59e0b;
  flex-shrink: 0;
}

.sidebar-card-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--zh-text);
  margin: 0;
}

.quick-nav-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.85rem 1rem;
}

.quick-nav-tag {
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 500;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}

.quick-nav-tag:hover {
  background: var(--zh-border);
  color: var(--zh-text);
  transform: translateY(-1px);
}

.quick-nav-tag.active {
  background: var(--zh-primary);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

/* ========== 空状态 ========== */
.empty-state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2.5rem 1.5rem;
  gap: 1rem;
}

.empty-cloud {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;
  gap: 0.75rem;
}

.empty-illustration {
  width: 140px;
  height: 84px;
  opacity: 0.5;
}

/* ========== 响应式 ========== */
@media (max-width: 640px) {
  .zh-tags-page {
    padding: 1rem 0.75rem;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
    padding: 1rem;
  }

  .header-icon {
    width: 2.5rem;
    height: 2.5rem;
  }

  .header-icon svg {
    width: 1.25rem;
    height: 1.25rem;
  }

  .header-title {
    font-size: 1.1rem;
  }

  .stats-bar {
    padding: 0.6rem 0.5rem;
  }

  .stats-item {
    gap: 0.3rem;
  }

  .stats-label {
    font-size: 0.7rem;
  }

  .stats-value {
    font-size: 0.8rem;
  }

  .tab-btn {
    padding: 0.5rem 0.5rem;
    font-size: 0.8rem;
    gap: 0.25rem;
  }

  .tab-btn-icon svg {
    width: 0.85rem;
    height: 0.85rem;
  }

  .cloud-tag {
    padding: 0.3rem 0.65rem;
  }

  .tag-list-item {
    padding: 0.7rem 0.75rem;
    gap: 0.5rem;
  }

  .selected-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.6rem;
  }

  .clear-btn {
    align-self: flex-end;
  }
}

/* ========== 标签页增强样式 ========== */
/* 标签页增强样式 */
.tags-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.25rem;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-lg);
  margin-bottom: 1rem;
}
.tags-tab {
  flex: 1;
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: var(--zh-radius-md);
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary);
  cursor: pointer;
  transition: all 0.25s ease;
}
.tags-tab.active {
  background: var(--zh-bg-elevated);
  color: var(--zh-primary);
  box-shadow: var(--zh-shadow-sm);
}
.tags-tab:hover:not(.active) {
  color: var(--zh-text-secondary);
}

/* 标签云动画 */
.tag-cloud-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.85rem;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-weight: 500;
}
.tag-cloud-item:hover {
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

/* 标签列表项 */
.tag-list-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0.65rem 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: var(--zh-radius-md);
}
.tag-list-item:hover {
  background: var(--zh-bg-hover);
}

.tag-rank-badge {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  font-size: 0.7rem;
  margin-right: 0.5rem;
}

.tag-follow-btn {
  padding: 0.3rem 0.75rem;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: 0.75rem;
}
.tag-follow-btn.following {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}
.tag-follow-btn.not-following {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}
.tag-follow-btn:hover {
  transform: scale(1.05);
}
.tag-follow-btn:active {
  transform: scale(0.95);
}

/* 右侧栏 */
.tag-sidebar {
  width: 20rem;
  flex-shrink: 0;
}
.tag-sidebar-card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
}
.tag-sidebar-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--zh-border);
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--zh-text);
}
.tag-sidebar-body {
  padding: 0.75rem;
}
.tag-quick-btn {
  padding: 0.3rem 0.65rem;
  font-size: 0.75rem;
  border-radius: 9999px;
  border: 1px solid var(--zh-border);
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}
.tag-quick-btn:hover {
  border-color: var(--zh-primary);
  color: var(--zh-primary);
}
.tag-quick-btn.active {
  background: var(--zh-primary);
  color: #fff;
  border-color: var(--zh-primary);
}
</style>