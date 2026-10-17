<template>
  <!-- 发现页：高品质最佳实践排版与重构 -->
  <div class="discover-page max-w-[1240px] 2xl:max-w-[1400px] mx-auto px-3 sm:px-4 py-2 sm:py-3 animate-fade-in-up">
    <!-- ===== 顶部横幅 ===== -->
    <section class="discover-hero mb-4 sm:mb-5">
      <div class="discover-hero-bg"></div>
      <div class="discover-hero-glow"></div>
      <div class="discover-hero-content">
        <div class="flex items-center gap-3">
          <div class="discover-hero-icon-wrap">
            <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" fill="currentColor" fill-opacity="0.3" />
            </svg>
          </div>
          <div>
            <h1 class="discover-hero-title">探索与发现</h1>
            <p class="discover-hero-subtitle">发掘实时热榜、前沿话题与优质创作者</p>
          </div>
        </div>

        <!-- 统计摘要 -->
        <div class="discover-quick-stats">
          <div class="quick-stat-item">
            <span class="quick-stat-num">{{ hotRankItems.length }}</span>
            <span class="quick-stat-lbl">热榜文章</span>
          </div>
          <div class="quick-stat-div"></div>
          <div class="quick-stat-item">
            <span class="quick-stat-num">{{ hotTags.length }}</span>
            <span class="quick-stat-lbl">热门话题</span>
          </div>
          <div class="quick-stat-div"></div>
          <div class="quick-stat-item">
            <span class="quick-stat-num">{{ recommendUsers.length }}</span>
            <span class="quick-stat-lbl">推荐作者</span>
          </div>
        </div>
      </div>
    </section>

    <PullToRefresh :on-refresh="refreshAll">
      <!-- ===== 主网格双栏布局 (桌面端 8:4 / 移动端单栏) ===== -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-5 sm:gap-6">
        <!-- 左侧主栏：热榜 (8列) -->
        <div class="lg:col-span-8 space-y-4">
          <div class="discover-card p-4 sm:p-5">
            <!-- 头部：标题与日/周/月榜切换 -->
            <div class="flex flex-wrap items-center justify-between gap-3 pb-3.5 mb-3.5 border-b border-[var(--zh-border)] dark:border-gray-800">
              <div class="flex items-center gap-2.5">
                <div class="section-icon-badge bg-amber-500/10 text-amber-500">
                  <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.316.492-.633.984-.95 1.477a17.577 17.577 0 01-1.025 1.411c-.39.497-.78 1.012-1.127 1.57A8.136 8.136 0 005.5 11.5a6.5 6.5 0 1013 0c0-1.285-.373-2.483-1.018-3.488a10.026 10.026 0 00-1.782-2.18 16.517 16.517 0 00-1.638-1.442 16.92 16.92 0 00-1.667-1.837zM10 7.828c.277-.384.56-.76.843-1.134.137-.182.274-.364.41-.546.224.207.45.413.67.625.334.321.673.65 1.002.99.414.428.81.875 1.173 1.353.498.654.802 1.444.802 2.384a4.5 4.5 0 11-9 0c0-.85.253-1.57.65-2.19.245-.383.54-.747.83-1.116.208-.266.42-.533.62-.806z" clip-rule="evenodd" />
                  </svg>
                </div>
                <div>
                  <h2 class="text-base sm:text-lg font-bold text-[var(--zh-text)]">热点内容榜</h2>
                  <p class="text-[11px] text-[var(--zh-text-tertiary)]">依据综合热度算法实时更新</p>
                </div>
              </div>

              <!-- 榜单周期切换 -->
              <div class="flex items-center gap-1 bg-[var(--zh-bg-hover)] dark:bg-gray-800/80 p-1 rounded-lg">
                <button
                  v-for="p in periods"
                  :key="p.key"
                  class="px-2.5 py-1 text-xs font-semibold rounded-md transition-all duration-200"
                  :class="activePeriod === p.key ? 'bg-primary text-white shadow-xs' : 'text-[var(--zh-text-secondary)] hover:text-[var(--zh-text)]'"
                  @click="switchPeriod(p.key)"
                >
                  {{ p.label }}
                </button>
              </div>
            </div>

            <!-- 加载中骨架屏 -->
            <div v-if="rankLoading" class="space-y-3">
              <div v-for="i in 6" :key="i" class="flex items-center gap-3.5 p-3 rounded-xl bg-[var(--zh-bg)] animate-pulse">
                <div class="w-8 h-8 rounded-lg bg-[var(--zh-border)] shrink-0"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-4 bg-[var(--zh-border)] rounded w-3/4"></div>
                  <div class="h-3 bg-[var(--zh-border)] rounded w-1/3"></div>
                </div>
                <div class="w-16 h-12 bg-[var(--zh-border)] rounded-lg shrink-0"></div>
              </div>
            </div>

            <!-- 错误重试 -->
            <ErrorRetry v-else-if="rankError" :message="rankError" :on-retry="fetchHotRank" />

            <!-- 热榜列表 -->
            <div v-else-if="hotRankItems.length > 0" class="space-y-2.5">
              <div
                v-for="(item, index) in hotRankItems"
                :key="item.id"
                class="rank-row-card group"
                :class="getRankRowClass(index)"
                @click="goToArticle(item.id)"
              >
                <!-- 排名标记 -->
                <div class="rank-pos-badge" :class="getRankPosClass(index)">
                  <span v-if="index === 0" class="rank-medal">🥇</span>
                  <span v-else-if="index === 1" class="rank-medal">🥈</span>
                  <span v-else-if="index === 2" class="rank-medal">🥉</span>
                  <span v-else class="rank-num">{{ index + 1 }}</span>
                </div>

                <!-- 封面缩略图 -->
                <div v-if="(item as any).coverImage" class="rank-thumb-wrap shrink-0">
                  <img
                    :src="resolveUrl((item as any).coverImage) || ''"
                    :alt="item.title"
                    class="rank-thumb"
                    loading="lazy"
                    @error="(e: Event) => (e.target as HTMLElement).parentElement?.classList.add('hidden')"
                  />
                </div>

                <!-- 标题与作者元信息 -->
                <div class="flex-1 min-w-0 pr-2">
                  <h3 class="rank-title group-hover:text-primary transition-colors">
                    {{ item.title }}
                  </h3>
                  <div class="flex items-center gap-3 mt-1.5 text-xs text-[var(--zh-text-tertiary)]">
                    <span v-if="item.authorNickname" class="truncate max-w-[120px] font-medium text-[var(--zh-text-secondary)]">
                      {{ item.authorNickname }}
                    </span>
                    <span class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      {{ formatCount(item.viewCount) }}
                    </span>
                    <span class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5 text-rose-500" fill="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      {{ formatCount(item.likeCount) }}
                    </span>
                  </div>
                </div>

                <!-- 热度指标 -->
                <div class="flex flex-col items-end shrink-0 pl-1">
                  <div class="flex items-center gap-1 text-amber-500 font-bold text-xs sm:text-sm">
                    <svg class="w-3.5 h-3.5 fill-current animate-pulse" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.316.492-.633.984-.95 1.477a17.577 17.577 0 01-1.025 1.411c-.39.497-.78 1.012-1.127 1.57A8.136 8.136 0 005.5 11.5a6.5 6.5 0 1013 0c0-1.285-.373-2.483-1.018-3.488a10.026 10.026 0 00-1.782-2.18 16.517 16.517 0 00-1.638-1.442 16.92 16.92 0 00-1.667-1.837z" clip-rule="evenodd" />
                    </svg>
                    <span>{{ formatHeat(item.score ?? 0) }}</span>
                  </div>
                  <span class="text-[10px] text-[var(--zh-text-tertiary)] mt-0.5">热度</span>
                </div>
              </div>
            </div>

            <!-- 空态 -->
            <EmptyState v-else title="暂无热榜数据" description="稍后再来探索最新热门文章" />
          </div>
        </div>

        <!-- 右侧边栏：热门标签 + 创作者推荐 (4列) -->
        <div class="lg:col-span-4 space-y-5">
          <!-- 热门话题 / 标签 -->
          <div class="discover-card p-4 sm:p-5">
            <div class="flex items-center justify-between pb-3 mb-3 border-b border-[var(--zh-border)] dark:border-gray-800">
              <div class="flex items-center gap-2">
                <div class="section-icon-badge bg-blue-500/10 text-blue-500">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
                  </svg>
                </div>
                <h2 class="text-sm sm:text-base font-bold text-[var(--zh-text)]">热门话题</h2>
              </div>
              <RouterLink to="/tags" class="text-xs text-primary hover:text-primary-dark font-medium flex items-center gap-0.5">
                全部
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
              </RouterLink>
            </div>

            <!-- 骨架屏 -->
            <div v-if="tagsLoading" class="flex flex-wrap gap-2">
              <div v-for="i in 8" :key="i" class="w-16 h-7 bg-[var(--zh-bg)] rounded-full animate-pulse"></div>
            </div>

            <!-- 标签列表 -->
            <div v-else-if="hotTags.length > 0" class="flex flex-wrap gap-2">
              <RouterLink
                v-for="tag in hotTags"
                :key="tag.id"
                :to="`/search?keyword=${encodeURIComponent(tag.name)}`"
                class="tag-pill"
              >
                <span class="text-primary font-bold mr-0.5">#</span>
                <span>{{ tag.name }}</span>
                <span v-if="tag.articleCount" class="text-[10px] text-[var(--zh-text-tertiary)] ml-1">
                  {{ tag.articleCount }}
                </span>
              </RouterLink>
            </div>

            <EmptyState v-else title="暂无热门标签" />
          </div>

          <!-- 推荐关注创作者 -->
          <div class="discover-card p-4 sm:p-5">
            <div class="flex items-center justify-between pb-3 mb-3 border-b border-[var(--zh-border)] dark:border-gray-800">
              <div class="flex items-center gap-2">
                <div class="section-icon-badge bg-emerald-500/10 text-emerald-500">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                </div>
                <h2 class="text-sm sm:text-base font-bold text-[var(--zh-text)]">创作者推荐</h2>
              </div>

              <!-- 换一批按钮 -->
              <button
                class="shuffle-btn"
                :disabled="usersLoading"
                @click="shuffleUsers"
                title="换一批推荐创作者"
              >
                <svg
                  class="w-3.5 h-3.5"
                  :class="{ 'animate-spin': usersLoading }"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>换一批</span>
              </button>
            </div>

            <!-- 用户骨架屏 -->
            <div v-if="usersLoading && recommendUsers.length === 0" class="space-y-3">
              <div v-for="i in 4" :key="i" class="flex items-center gap-3 p-2 rounded-xl bg-[var(--zh-bg)] animate-pulse">
                <div class="w-10 h-10 rounded-full bg-[var(--zh-border)] shrink-0"></div>
                <div class="flex-1 space-y-1.5">
                  <div class="h-3.5 bg-[var(--zh-border)] rounded w-24"></div>
                  <div class="h-2.5 bg-[var(--zh-border)] rounded w-36"></div>
                </div>
                <div class="w-14 h-6 bg-[var(--zh-border)] rounded-full shrink-0"></div>
              </div>
            </div>

            <!-- 用户列表 -->
            <div v-else-if="recommendUsers.length > 0" class="space-y-3">
              <div
                v-for="user in recommendUsers"
                :key="user.id"
                class="flex items-center justify-between gap-3 p-2 rounded-xl hover:bg-[var(--zh-bg-hover)] dark:hover:bg-gray-800/60 transition-colors"
              >
                <div class="flex items-center gap-2.5 min-w-0 cursor-pointer" @click="goToUser(user.id)">
                  <UserAvatar :src="user.avatar" :alt="user.nickname" size="md" />
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-[var(--zh-text)] truncate">{{ user.nickname }}</p>
                    <p class="text-[11px] text-[var(--zh-text-tertiary)] truncate mt-0.5">
                      {{ user.bio || `${user.followerCount || 0} 粉丝` }}
                    </p>
                  </div>
                </div>

                <button
                  class="follow-btn shrink-0"
                  :class="user.isFollowing ? 'follow-btn--following' : 'follow-btn--not-following'"
                  @click="handleToggleFollow(user)"
                >
                  {{ user.isFollowing ? '已关注' : '+ 关注' }}
                </button>
              </div>
            </div>

            <EmptyState v-else title="暂无推荐创作者" />
          </div>
        </div>
      </div>
    </PullToRefresh>
  </div>
</template>

<script setup lang="ts">
/**
 * 发现页 (Discover) - 重构优化最佳实践
 * - 纯 Vue 3 响应式生命周期，移除脆弱的伪 SSR 调用
 * - 周期切换 (日/周/月榜) 实时刷新
 * - 创作者换一批与实时关注/取关绑定
 * - 缓存机制防止重复无效请求
 */
import type { RankItem, User, Tag } from '@/types'
import { useResourceUrl } from '@/composables/useResourceUrl'
import { showToast } from '@/composables/useToast'

const router = useRouter()
const userStore = useUserStore()
const { resolveUrl } = useResourceUrl()
const { cachedRequest, invalidate } = useRequestCache({ ttl: 5 * 60 * 1000 })

// 路由导航
const goToArticle = (id: number | string) => router.push(`/articles/${id}`)
const goToUser = (id: number | string) => router.push(`/user/${id}`)

// 榜单周期
const periods: Array<{ key: 'daily' | 'weekly' | 'monthly'; label: string }> = [
  { key: 'daily', label: '今日热榜' },
  { key: 'weekly', label: '本周热榜' },
  { key: 'monthly', label: '本月热榜' },
]
const activePeriod = ref<'daily' | 'weekly' | 'monthly'>('daily')

// 状态
const hotRankItems = ref<RankItem[]>([])
const rankLoading = ref(false)
const rankError = ref('')

const hotTags = ref<Tag[]>([])
const tagsLoading = ref(false)

const recommendUsers = ref<User[]>([])
const usersLoading = ref(false)
const userOffset = ref(0)

// 切换周期
function switchPeriod(p: 'daily' | 'weekly' | 'monthly') {
  if (activePeriod.value === p) return
  activePeriod.value = p
  fetchHotRank()
}

// 格式化数字
function formatCount(num?: number): string {
  if (num == null) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

function formatHeat(score: number): string {
  if (score == null || score < 1) return '1'
  if (score >= 10000) return `${(score / 10000).toFixed(1)}万`
  if (score >= 100) return Math.round(score).toString()
  return score.toFixed(1)
}

// 样式映射
function getRankRowClass(index: number) {
  if (index === 0) return 'rank-row--gold'
  if (index === 1) return 'rank-row--silver'
  if (index === 2) return 'rank-row--bronze'
  return ''
}

function getRankPosClass(index: number) {
  if (index === 0) return 'rank-pos--gold'
  if (index === 1) return 'rank-pos--silver'
  if (index === 2) return 'rank-pos--bronze'
  return 'rank-pos--normal'
}

// 获取热榜
async function fetchHotRank() {
  rankLoading.value = true
  rankError.value = ''
  try {
    const url = '/rank/hot'
    const params = { period: activePeriod.value, limit: 15 }
    const api = useApi()
    const res = await cachedRequest<any>(
      () => api.get<RankItem[]>(url, { params: { ...params, _t: Date.now() } }).then(r => r.data),
      url,
      params,
    )
    const list = res?.data || res || []
    hotRankItems.value = Array.isArray(list) ? list : []
  } catch {
    rankError.value = '热榜加载失败，请检查网络后重试'
    hotRankItems.value = []
  } finally {
    rankLoading.value = false
  }
}

// 获取热门标签
async function fetchHotTags() {
  tagsLoading.value = true
  try {
    const url = '/tags/hot'
    const params = { limit: 16 }
    const api = useApi()
    const res = await cachedRequest<any>(
      () => api.get<Tag[]>(url, { params: { ...params, _t: Date.now() } }).then(r => r.data),
      url,
      params,
    )
    const list = res?.data || res || []
    hotTags.value = Array.isArray(list) ? list : []
  } catch {
    hotTags.value = []
  } finally {
    tagsLoading.value = false
  }
}

// 获取推荐创作者
async function shuffleUsers() {
  if (usersLoading.value) return
  usersLoading.value = true
  try {
    const api = useApi()
    const res = await api.get<any>('/users/recommend', {
      params: { limit: 5, offset: userOffset.value, _t: Date.now() },
    })
    const list = res?.data?.data || res?.data || []
    if (Array.isArray(list) && list.length > 0) {
      recommendUsers.value = list
      userOffset.value += list.length
    } else if (userOffset.value > 0) {
      // 循环回绕
      userOffset.value = 0
      const retryRes = await api.get<any>('/users/recommend', {
        params: { limit: 5, offset: 0, _t: Date.now() },
      })
      const retryList = retryRes?.data?.data || retryRes?.data || []
      recommendUsers.value = Array.isArray(retryList) ? retryList : []
    }
  } catch {
    // 忽略加载失败，保留旧数据
  } finally {
    usersLoading.value = false
  }
}

// 关注 / 取关
async function handleToggleFollow(user: User) {
  if (!userStore.isLoggedIn) {
    showToast('请先登录', 'warning')
    router.push('/login')
    return
  }
  try {
    const { socialApi } = await import('@/api')
    await socialApi.toggleFollow(user.id)
    user.isFollowing = !user.isFollowing
    user.followerCount = (user.followerCount || 0) + (user.isFollowing ? 1 : -1)
    showToast(user.isFollowing ? '已关注' : '已取消关注', 'success')
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error')
  }
}

// 全量下拉刷新
async function refreshAll() {
  invalidate('/rank/hot')
  invalidate('/tags/hot')
  userOffset.value = 0
  await Promise.allSettled([fetchHotRank(), fetchHotTags(), shuffleUsers()])
}

onMounted(() => {
  fetchHotRank()
  fetchHotTags()
  shuffleUsers()
})

useHead({
  title: '发现 - 知讯',
})
</script>

<style scoped>
/* ==================== 发现页样式 ==================== */
.discover-page {
  min-height: calc(100vh - 120px);
}

/* ===== 英雄横幅 ===== */
.discover-hero {
  position: relative;
  border-radius: var(--zh-radius-lg, 1rem);
  overflow: hidden;
  padding: 1.25rem 1.5rem;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.discover-hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08) 0%, rgba(147, 51, 234, 0.06) 50%, rgba(245, 158, 11, 0.04) 100%);
  pointer-events: none;
}

.discover-hero-glow {
  position: absolute;
  top: -40%;
  right: -10%;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.discover-hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 640px) {
  .discover-hero-content {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.discover-hero-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--zh-radius-md, 0.75rem);
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.discover-hero-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--zh-text);
  line-height: 1.25;
}

.discover-hero-subtitle {
  font-size: 0.8125rem;
  color: var(--zh-text-tertiary);
  margin-top: 0.15rem;
}

/* 快速统计 */
.discover-quick-stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: var(--zh-bg-hover);
  border-radius: 0.75rem;
  padding: 0.5rem 0.875rem;
  align-self: flex-start;
}

@media (min-width: 640px) {
  .discover-quick-stats {
    align-self: auto;
  }
}

.quick-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 0.375rem;
}

.quick-stat-num {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--zh-primary);
  line-height: 1.2;
}

.quick-stat-lbl {
  font-size: 0.6875rem;
  color: var(--zh-text-tertiary);
}

.quick-stat-div {
  width: 1px;
  height: 20px;
  background: var(--zh-border);
}

/* ===== 卡片容器 ===== */
.discover-card {
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border);
  border-radius: var(--zh-radius-lg, 1rem);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: border-color 0.2s ease;
}

.section-icon-badge {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ===== 热榜行卡片 ===== */
.rank-row-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.875rem;
  border-radius: var(--zh-radius-md, 0.75rem);
  background: var(--zh-bg);
  border: 1px solid var(--zh-border-light);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.rank-row-card:hover {
  background: var(--zh-bg-hover);
  border-color: var(--zh-primary-light, #a5b4fc);
  transform: translateX(3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.rank-row--gold {
  border-left: 3px solid #f59e0b;
}

.rank-row--silver {
  border-left: 3px solid #94a3b8;
}

.rank-row--bronze {
  border-left: 3px solid #d97706;
}

.rank-pos-badge {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 0.8125rem;
  font-weight: 700;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}

.rank-pos--gold {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
}

.rank-pos--silver {
  background: linear-gradient(135deg, #e2e8f0, #94a3b8);
  color: #fff;
}

.rank-pos--bronze {
  background: linear-gradient(135deg, #fed7aa, #d97706);
  color: #fff;
}

.rank-medal {
  font-size: 1rem;
}

.rank-num {
  font-size: 0.8125rem;
  font-family: inherit;
}

.rank-thumb-wrap {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--zh-border);
}

.rank-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.rank-row-card:hover .rank-thumb {
  transform: scale(1.08);
}

.rank-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--zh-text);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ===== 标签芯片 ===== */
.tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.85rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--zh-text-secondary);
  background: var(--zh-bg);
  border: 1px solid var(--zh-border-light);
  border-radius: 9999px;
  transition: all 0.2s ease;
  text-decoration: none;
}

.tag-pill:hover {
  background: var(--zh-primary-bg, #eef2ff);
  color: var(--zh-primary);
  border-color: rgba(var(--zh-primary-rgb), 0.3);
  transform: translateY(-1px);
}

/* ===== 创作者换一批与关注按钮 ===== */
.shuffle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--zh-text-tertiary);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.shuffle-btn:hover:not(:disabled) {
  color: var(--zh-primary);
  background: var(--zh-bg-hover);
}

.follow-btn {
  padding: 0.3rem 0.875rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.follow-btn--not-following {
  background: var(--zh-primary);
  color: #fff;
  box-shadow: 0 1px 4px rgba(var(--zh-primary-rgb), 0.2);
}

.follow-btn--not-following:hover {
  background: var(--zh-primary-dark, #4338ca);
}

.follow-btn--following {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  border: 1px solid var(--zh-border);
}

.follow-btn--following:hover {
  background: var(--zh-border);
}
</style>

