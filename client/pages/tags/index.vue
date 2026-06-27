<template>
  <!-- 标签聚合页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">

    <!-- Tab切换：标签云 / 热门标签 / 已关注 -->
    <div class="flex items-center space-x-2 mb-2">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-3 py-1.5 text-sm font-medium rounded-lg transition-colors"
        :class="activeTab === tab.key
          ? 'bg-primary text-white'
          : 'bg-slate-50 text-slate-700 hover:bg-slate-200'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- 标签云 -->
        <div v-if="activeTab === 'cloud'" class="card p-6">
          <div v-if="cloudLoading" class="flex items-center justify-center py-12">
            <LoadingSkeleton v-for="i in 12" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="cloudError" :message="cloudError" :on-retry="retryCloud" />
          <div v-else-if="cloudTags.length > 0" class="flex flex-wrap gap-2">
            <button
              v-for="tag in cloudTags"
              :key="tag.id"
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-all hover:shadow-[var(--shadow-md)] cursor-pointer"
              :style="getTagStyle(tag)"
              @click="selectTag(tag)"
            >
              <span>{{ tag.name }}</span>
              <span class="text-xs opacity-70">{{ tag.articleCount }}</span>
            </button>
          </div>
          <EmptyState v-else title="暂无标签" description="还没有创建任何标签" />
        </div>

        <!-- 热门标签列表 -->
        <div v-if="activeTab === 'hot'" class="card">
          <div v-if="hotLoading" class="p-2 space-y-2">
            <LoadingSkeleton v-for="i in 8" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="hotError" :message="hotError" :on-retry="retryHot" />
          <div v-else-if="hotTags.length > 0" class="divide-y divide-slate-100">
            <div
              v-for="(tag, index) in hotTags"
              :key="tag.id"
              class="flex items-center w-full p-2 hover:bg-slate-50 transition-colors text-left cursor-pointer"
              @click="selectTag(tag)"
            >
              <!-- 排名 -->
              <span class="w-7 h-7 rounded-full flex items-center justify-center shrink-0 font-bold text-sm mr-2" :class="getRankClass(index)">
                {{ index + 1 }}
              </span>
              <!-- 标签名 -->
              <div class="flex-1 min-w-0">
                <span class="text-sm font-medium text-slate-900">{{ tag.name }}</span>
              </div>
              <!-- 作品数 -->
              <span class="text-sm text-slate-500">{{ tag.articleCount }} 个作品</span>
              <!-- 关注按钮 -->
              <button
                class="ml-3 px-3 py-1 text-xs font-medium rounded-full transition-colors"
                :class="tag.isFollowed
                  ? 'bg-slate-50 text-slate-600'
                  : 'bg-primary-50 text-primary-700 hover:bg-primary/20'"
                @click.stop="toggleFollowTag(tag)"
              >
                {{ tag.isFollowed ? '已关注' : '关注' }}
              </button>
            </div>
          </div>
          <EmptyState v-else title="暂无热门标签" />
        </div>

        <!-- 已关注标签 -->
        <div v-if="activeTab === 'followed'" class="card">
          <div v-if="followedLoading" class="p-2 space-y-2">
            <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
          </div>
          <ErrorRetry v-else-if="followedError" :message="followedError" :on-retry="retryFollowed" />
          <div v-else-if="followedTags.length > 0" class="divide-y divide-slate-100">
            <div
              v-for="tag in followedTags"
              :key="tag.id"
              class="flex items-center w-full p-2 hover:bg-slate-50 transition-colors text-left cursor-pointer"
              @click="selectTag(tag)"
            >
              <div class="flex-1 min-w-0">
                <span class="text-sm font-medium text-slate-900">{{ tag.name }}</span>
              </div>
              <span class="text-sm text-slate-500">{{ tag.articleCount }} 个作品</span>
              <button
                class="ml-3 px-3 py-1 text-xs font-medium rounded-full bg-slate-50 text-slate-600 transition-colors"
                @click.stop="toggleFollowTag(tag)"
              >
                取关
              </button>
            </div>
          </div>
          <EmptyState v-else title="暂无关注标签" description="去热门标签中关注感兴趣的标签吧" />
        </div>

        <!-- 选中标签后的作品列表 -->
        <div v-if="selectedTag" class="mt-3">
          <div class="flex items-center justify-between mb-2">
            <h2 class="text-lg font-semibold text-slate-900">
              「{{ selectedTag.name }}」相关作品
            </h2>
            <button class="text-sm text-slate-500 hover:text-slate-700" @click="clearSelection">
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
      <aside class="hidden lg:block w-80 2xl:w-96 shrink-0 space-y-2">
        <!-- 当前选中标签信息 -->
        <div v-if="selectedTag" class="card">
          <div class="p-1.5 border-b border-slate-200">
            <h3 class="font-semibold text-slate-900">{{ selectedTag.name }}</h3>
          </div>
          <div class="p-1.5 space-y-1.5">
            <div class="flex items-center justify-between text-sm">
              <span class="text-slate-500">作品数</span>
              <span class="font-medium text-slate-900">{{ selectedTag.articleCount }}</span>
            </div>
            <button
              class="w-full px-3 py-1.5 text-sm font-medium rounded-lg transition-colors"
              :class="selectedTag.isFollowed
                ? 'bg-slate-50 text-slate-700 hover:bg-slate-200'
                : 'bg-primary text-white hover:bg-primary-dark'"
              @click="toggleFollowTag(selectedTag)"
            >
              {{ selectedTag.isFollowed ? '取消关注' : '关注标签' }}
            </button>
          </div>
        </div>

        <!-- 热门标签快捷入口 -->
        <div class="card">
          <div class="p-2 border-b border-slate-200">
            <h3 class="font-semibold text-slate-900">热门标签</h3>
          </div>
          <div class="p-2 flex flex-wrap gap-1.5">
            <button
              v-for="tag in hotTags.slice(0, 10)"
              :key="tag.id"
              class="px-2 py-0.5 text-sm rounded-full transition-colors"
              :class="selectedTag?.id === tag.id
                ? 'bg-primary text-white'
                : 'bg-slate-50 text-slate-700 hover:bg-slate-200'"
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
import type { Tag, Article, PageResult, ApiResponse } from '~/types'

const { setTitle } = usePageHeaderTitle()
setTitle('标签')

const userStore = useUserStore()
const config = useRuntimeConfig()

// 构建API基础URL：SSR时使用内部地址，客户端时走Nginx代理
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

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
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/cloud`, {
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    cloudTags.value = res?.data || []
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
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/hot`, {
      params: { limit: 30 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    hotTags.value = res?.data || []
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
    const base = getApiBase()
    const res = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/articles`, {
      params: { tag_id: selectedTag.value.id, page: articlesPage.value, pageSize: 20 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    const data = res?.data
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
    navigateTo('/login')
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

// 标签云样式：根据作品数计算大小和颜色
const getTagStyle = (tag: Tag) => {
  const maxCount = Math.max(...cloudTags.value.map(t => t.articleCount), 1)
  const ratio = tag.articleCount / maxCount
  const fontSize = 0.875 + ratio * 0.75 // 14px ~ 26px
  const colors = [
    { bg: 'rgb(59, 130, 246)', text: '#fff' },    // blue
    { bg: 'rgb(16, 185, 129)', text: '#fff' },     // emerald
    { bg: 'rgb(245, 158, 11)', text: '#fff' },     // amber
    { bg: 'rgb(239, 68, 68)', text: '#fff' },      // red
    { bg: 'rgb(139, 92, 246)', text: '#fff' },     // violet
    { bg: 'rgb(236, 72, 153)', text: '#fff' },     // pink
    { bg: 'rgb(20, 184, 166)', text: '#fff' },     // teal
    { bg: 'rgb(249, 115, 22)', text: '#fff' },     // orange
  ]
  const colorIndex = tag.id % colors.length
  const opacity = 0.6 + ratio * 0.4
  const color = colors[colorIndex]
  return {
    fontSize: `${fontSize}rem`,
    backgroundColor: color.bg.replace('rgb', 'rgba').replace(')', `, ${opacity})`),
    color: color.text,
  }
}

// 排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'bg-yellow-400 text-white'
  if (index === 1) return 'bg-gray-300 text-white'
  if (index === 2) return 'bg-orange-400 text-white'
  return 'bg-slate-50 text-slate-500'
}

// SSR数据获取
const { data: cloudData } = await useAsyncData('tags-cloud', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/cloud`, {
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
  } catch {
    return []
  }
}, { default: () => [] })

const { data: hotData } = await useAsyncData('tags-hot', async () => {
  try {
    const base = getApiBase()
    const res = await $fetch<ApiResponse<Tag[]>>(`${base}/tags/hot`, {
      params: { limit: 30 },
      headers: import.meta.server ? { 'X-SSR-Request': 'true' } : {},
    })
    return res?.data || []
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
