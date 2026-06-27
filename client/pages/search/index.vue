<template>
  <!-- 搜索页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1 2xl:px-2 py-1">
    <!-- 搜索框（自动聚焦 + 自动搜索） -->
    <div class="mb-2 md:mb-3">
      <div class="flex items-center bg-white rounded-full px-2 md:px-3 py-1.5 md:py-2 shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-primary focus-within:border-transparent transition-all">
        <!-- 加载指示器 -->
        <svg v-if="autoSearching" class="w-4 h-4 md:w-5 md:h-5 text-primary animate-spin shrink-0" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="w-4 h-4 md:w-5 md:h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="text"
          class="flex-1 bg-transparent border-none outline-none ml-1.5 md:ml-2 text-sm md:text-base text-slate-900 placeholder-gray-400"
          placeholder="搜索作品、用户..."
          @input="handleInput"
          @keydown.enter="doSearch"
        />
        <button v-show="keyword" class="p-0.5 md:p-1 text-gray-400 hover:text-gray-600 shrink-0" @click="clearKeyword">
          <svg class="w-3.5 h-3.5 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <!-- 清除按钮占位（保持搜索按钮位置固定） -->
        <span v-show="!keyword" class="w-5 md:w-6 h-3.5 md:h-4 shrink-0"></span>
        <!-- 搜索按钮 -->
        <button class="ml-1.5 md:ml-2 px-2.5 md:px-3 py-1 md:py-1.5 bg-primary text-white text-xs md:text-sm rounded-full hover:bg-primary-600 transition-colors shrink-0" @click="doSearch">
          搜索
        </button>
      </div>

    </div>

    <!-- 搜索建议/热门搜索（未搜索时） -->
    <div v-if="!hasSearched" class="text-xs md:text-sm">
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="mb-3 md:mb-6">
        <h3 class="text-xs md:text-sm font-medium text-slate-500 mb-2 md:mb-3">搜索建议</h3>
        <div class="space-y-0.5 md:space-y-1">
          <button
            v-for="item in suggestions"
            :key="`${item.type}-${item.id}`"
            class="w-full text-left flex items-center px-2.5 md:px-4 py-1.5 md:py-2 text-xs md:text-sm text-slate-700 hover:bg-slate-50 rounded-lg"
            @click="selectSuggestion(item)"
          >
            <!-- 类型图标 -->
            <svg v-if="item.type === 'user'" class="w-3.5 h-3.5 md:w-4 md:h-4 text-gray-400 mr-1.5 md:mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <svg v-else-if="item.type === 'article'" class="w-3.5 h-3.5 md:w-4 md:h-4 text-gray-400 mr-1.5 md:mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <svg v-else class="w-3.5 h-3.5 md:w-4 md:h-4 text-gray-400 mr-1.5 md:mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
            <!-- 用户头像 -->
            <UserAvatar v-if="item.type === 'user'" :src="item.avatar" :alt="item.text" size="xs" class="mr-1.5 md:mr-2" />
            <span v-html="highlightKeyword(item.text)"></span>
            <span class="ml-auto text-[10px] md:text-xs text-gray-400">{{ item.type === 'user' ? '用户' : item.type === 'article' ? '作品' : '标签' }}</span>
          </button>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0" class="mb-3 md:mb-6">
        <div class="flex items-center justify-between mb-2 md:mb-3">
          <h3 class="text-xs md:text-sm font-medium text-slate-500">搜索历史</h3>
          <button class="text-[10px] md:text-xs text-gray-400 hover:text-danger" @click="clearHistory">清除</button>
        </div>
        <div class="flex flex-wrap gap-1.5 md:gap-2">
          <button v-for="item in searchHistory" :key="item" class="px-2 md:px-3 py-1 md:py-1.5 bg-slate-50 text-xs md:text-sm text-slate-700 rounded-full hover:bg-slate-200" @click="keyword = item; doSearch()">
            {{ item }}
          </button>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="hotSearches.length > 0">
        <h3 class="text-xs md:text-sm font-medium text-slate-500 mb-1.5 md:mb-2">热门搜索</h3>
        <div class="space-y-0.5 md:space-y-1">
          <button v-for="(item, index) in hotSearches" :key="item" class="w-full text-left flex items-center px-2.5 md:px-4 py-1.5 md:py-2 text-xs md:text-sm text-slate-700 hover:bg-slate-50 rounded-lg" @click="keyword = item; doSearch()">
            <span class="w-4 md:w-5 text-center text-[10px] md:text-xs font-bold" :class="index < 3 ? 'text-danger' : 'text-gray-400'">{{ index + 1 }}</span>
            <span class="ml-2 md:ml-3">{{ item }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果（已搜索时） -->
    <div v-else class="text-xs md:text-sm">
      <!-- Tab切换 -->
      <div class="flex items-center border-b border-slate-200 mb-2 md:mb-4">
        <button
          v-for="tab in searchTabs"
          :key="tab.key"
          class="px-2.5 md:px-4 py-2 md:py-3 text-xs md:text-sm font-medium border-b-2 transition-colors"
          :class="activeTab === tab.key
            ? 'border-primary text-primary'
            : 'border-transparent text-slate-500 hover:text-slate-700'"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span v-if="tabCounts[tab.key]" class="ml-0.5 md:ml-1 text-[10px] md:text-xs text-gray-400">({{ tabCounts[tab.key] }})</span>
        </button>
      </div>

      <!-- 筛选栏（紧凑型） -->
      <div class="flex items-center flex-wrap gap-1 mb-1 md:mb-1.5">
        <!-- 分类筛选 -->
        <select v-model="filterCategoryId" class="input text-[10px] w-auto min-w-[60px] max-w-[75px] rounded" @change="doSearch()">
          <option :value="undefined">全部分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>

        <!-- 时间范围筛选 -->
        <select v-model="filterTimeRange" class="input text-[10px] w-auto min-w-[60px] max-w-[75px] rounded" @change="doSearch()">
          <option :value="undefined">全部时间</option>
          <option value="24h">24小时</option>
          <option value="7d">7天</option>
          <option value="30d">30天</option>
        </select>

        <!-- 排序 -->
        <select v-model="sortBy" class="input text-[10px] w-auto min-w-[54px] max-w-[68px] rounded" @change="doSearch()">
          <option value="relevance">相关度</option>
          <option value="latest">最新</option>
          <option value="popular">最热</option>
        </select>
      </div>

      <!-- 搜索中骨架屏 -->
      <div v-if="loading">
        <!-- 综合Tab骨架屏 -->
        <template v-if="activeTab === 'all'">
          <div class="mb-2 md:mb-3">
            <div class="h-3 md:h-4 bg-slate-200 rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="space-y-1.5 md:space-y-2">
              <LoadingSkeleton v-for="i in 2" :key="'a'+i" type="article" />
            </div>
          </div>
          <div class="mb-2 md:mb-3">
            <div class="h-3 md:h-4 bg-slate-200 rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="space-y-1.5 md:space-y-2">
              <LoadingSkeleton v-for="i in 2" :key="'u'+i" type="user" />
            </div>
          </div>
          <div>
            <div class="h-3 md:h-4 bg-slate-200 rounded w-16 md:w-20 mb-2 md:mb-3 animate-pulse"></div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 md:gap-3">
              <div v-for="i in 6" :key="'img'+i" class="aspect-square bg-slate-200 rounded-lg animate-pulse"></div>
            </div>
          </div>
        </template>
        <!-- 作品Tab骨架屏 -->
        <template v-else-if="activeTab === 'articles'">
          <div class="space-y-2 md:space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
          </div>
        </template>
        <!-- 用户Tab骨架屏 -->
        <template v-else-if="activeTab === 'users'">
          <div class="space-y-2 md:space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="user" />
          </div>
        </template>
        <!-- 图片Tab骨架屏 -->
        <template v-else-if="activeTab === 'images'">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 md:gap-3">
            <div v-for="i in 9" :key="i" class="aspect-square bg-slate-200 rounded-lg animate-pulse"></div>
          </div>
        </template>
      </div>

      <!-- 搜索失败 -->
      <ErrorRetry v-else-if="searchError" :message="searchError" :on-retry="retrySearch" />

      <!-- 搜索结果列表 -->
      <div v-else-if="hasAnyResult">
        <!-- ===== 综合Tab ===== -->
        <template v-if="activeTab === 'all'">
          <!-- 用户区块 -->
          <div v-if="allUserResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-slate-500">相关用户</h3>
              <button v-if="tabCounts.users > 3" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('users')">查看全部 ({{ tabCounts.users }})</button>
            </div>
            <div class="space-y-1.5 md:space-y-2">
              <UserCard v-for="user in allUserResults" :key="'u-'+user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
            </div>
          </div>

          <!-- 作品区块 -->
          <div v-if="allArticleResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-slate-500">相关作品</h3>
              <button v-if="tabCounts.articles > 5" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('articles')">查看全部 ({{ tabCounts.articles }})</button>
            </div>
            <div class="space-y-1.5 md:space-y-2">
              <ArticleCard v-for="item in allArticleResults" :key="'a-'+item.id" :article="item" />
            </div>
          </div>

          <!-- 图片区块 -->
          <div v-if="allImageResults.length > 0" class="mb-2 md:mb-3">
            <div class="flex items-center justify-between mb-1.5 md:mb-2">
              <h3 class="text-xs md:text-sm font-medium text-slate-500">相关图片</h3>
              <button v-if="tabCounts.images > 6" class="text-[10px] md:text-xs text-primary hover:underline" @click="switchTab('images')">查看全部 ({{ tabCounts.images }})</button>
            </div>
            <ImageGrid :images="allImageResults" @click="handleImageClick" />
          </div>
        </template>

        <!-- ===== 作品Tab ===== -->
        <template v-if="activeTab === 'articles'">
          <div class="space-y-1.5 md:space-y-2">
            <ArticleCard v-for="item in articleResults" :key="item.id" :article="item" />
          </div>
          <!-- 加载更多 -->
          <div ref="articleSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreArticles && articleResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">没有更多了</div>
        </template>

        <!-- ===== 用户Tab ===== -->
        <template v-if="activeTab === 'users'">
          <div class="space-y-1.5 md:space-y-2">
            <UserCard v-for="user in userResults" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
          <!-- 加载更多 -->
          <div ref="userSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreUsers && userResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">没有更多了</div>
        </template>

        <!-- ===== 图片Tab ===== -->
        <template v-if="activeTab === 'images'">
          <ImageGrid :images="imageResults" @click="handleImageClick" />
          <!-- 加载更多 -->
          <div ref="imageSentinel" class="h-0.5 md:h-1"></div>
          <div v-if="loadingMore" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">
            <svg class="animate-spin inline w-3.5 h-3.5 md:w-4 md:h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreImages && imageResults.length > 0" class="py-3 md:py-4 text-center text-xs md:text-sm text-gray-400">没有更多了</div>
        </template>
      </div>

      <!-- 空结果 -->
      <EmptyState v-else :title="'未找到相关结果'" :description="'换个关键词试试吧'" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 搜索页：搜索框、搜索建议、搜索历史、热门搜索、综合搜索结果展示 */
import type { Article, User, Category } from '~/types'
import type { SuggestionItem, SearchParams } from '~/api/search'

const route = useRoute()
const router = useRouter()
const { resolveUrl } = useResourceUrl()
const userStore = useUserStore()

// 请求缓存：搜索建议用短TTL，热门搜索和分类用长TTL
const { cachedRequest: cachedRequestShort } = useRequestCache({ ttl: 2 * 60 * 1000 })
const { cachedRequest: cachedRequestLong } = useRequestCache({ ttl: 10 * 60 * 1000 })

// 搜索Tab
const searchTabs = [
  { key: 'all', label: computed(() => '综合') },
  { key: 'articles', label: computed(() => '作品') },
  { key: 'users', label: computed(() => '用户') },
  { key: 'images', label: computed(() => '图片') },
]

const keyword = ref((route.query.keyword as string) || '')
const activeTab = ref('all')
const sortBy = ref('relevance')
const filterCategoryId = ref<number | undefined>(undefined)
const filterTimeRange = ref<'24h' | '7d' | '30d' | undefined>(undefined)
const hasSearched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const searchError = ref('')
const suggestions = ref<SuggestionItem[]>([])
const hotSearches = ref<string[]>([])
const searchHistory = ref<string[]>([])
const categories = ref<Category[]>([])

// 搜索结果
const articleResults = ref<Article[]>([])
const userResults = ref<User[]>([])
const imageResults = ref<{ url: string; title: string; articleTitle?: string; author?: string }[]>([])

// 综合Tab截取的展示数据
const allArticleResults = computed(() => articleResults.value.slice(0, 5))
const allUserResults = computed(() => userResults.value.slice(0, 3))
const allImageResults = computed(() => imageResults.value.slice(0, 6))

// 各Tab计数（综合搜索时从后端返回的total取）
const tabCounts = ref<Record<string, number>>({ all: 0, articles: 0, users: 0, images: 0 })

// 分页
const currentPage = ref(1)
const pageSize = 20
const totalResults = ref(0)

// 无限滚动sentinel
const articleSentinel = ref<HTMLElement | null>(null)
const userSentinel = ref<HTMLElement | null>(null)
const imageSentinel = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)

// 是否有更多数据
const hasMoreArticles = computed(() => articleResults.value.length < tabCounts.value.articles)
const hasMoreUsers = computed(() => userResults.value.length < tabCounts.value.users)
const hasMoreImages = computed(() => imageResults.value.length < tabCounts.value.images)

// 是否有任何结果
const hasAnyResult = computed(() => {
  if (activeTab.value === 'all') {
    return articleResults.value.length > 0 || userResults.value.length > 0 || imageResults.value.length > 0
  }
  if (activeTab.value === 'articles') return articleResults.value.length > 0
  if (activeTab.value === 'users') return userResults.value.length > 0
  if (activeTab.value === 'images') return imageResults.value.length > 0
  return false
})

// 防抖定时器
let suggestTimer: ReturnType<typeof setTimeout> | null = null
let autoSearchTimer: ReturnType<typeof setTimeout> | null = null
// AbortController 取消进行中的请求
let abortController: AbortController | null = null

// IntersectionObserver实例
let articleObserver: IntersectionObserver | null = null
let userObserver: IntersectionObserver | null = null
let imageObserver: IntersectionObserver | null = null

// 自动搜索中指示
const autoSearching = ref(false)

// 从URL参数初始化搜索
onMounted(async () => {
  if (keyword.value) {
    doSearch()
  }
  // 自动聚焦
  nextTick(() => {
    searchInputRef.value?.focus()
  })

  // 加载热门搜索
  loadHotSearches()
  // 加载搜索历史
  loadSearchHistory()
  // 加载分类列表
  loadCategories()
})

onUnmounted(() => {
  articleObserver?.disconnect()
  userObserver?.disconnect()
  imageObserver?.disconnect()
})

// 输入处理：建议快速展示200ms，自动搜索防抖500ms
const handleInput = () => {
  const trimmed = keyword.value.trim()

  // 清除旧的定时器
  if (suggestTimer) clearTimeout(suggestTimer)
  if (autoSearchTimer) clearTimeout(autoSearchTimer)

  // 取消进行中的搜索请求
  if (abortController) {
    abortController.abort()
    abortController = null
  }

  if (!trimmed) {
    suggestions.value = []
    autoSearching.value = false
    return
  }

  // 搜索建议（短延迟，快速展示）
  suggestTimer = setTimeout(async () => {
    try {
      const { searchApi } = await import('~/api')
      const response = await cachedRequestShort(
        () => searchApi.getSuggestions(trimmed),
        '/search/suggestions',
        { keyword: trimmed }
      )
      const data = response.data.data
      suggestions.value = data?.completions || []
    } catch {
      suggestions.value = []
    }
  }, 200)

  // 自动搜索（较长延迟，不为空即触发）
  autoSearchTimer = setTimeout(() => {
    autoSearching.value = true
    doAutoSearch()
  }, 500)
}

// 自动搜索（带 AbortController 取消机制）
const doAutoSearch = async () => {
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    autoSearching.value = false
    return
  }

  // 创建新的 AbortController
  abortController = new AbortController()
  const signal = abortController.signal

  try {
    const { searchApi } = await import('~/api')
    const params: SearchParams = {
      page: 1,
      pageSize,
      sort: sortBy.value as SearchParams['sort'],
      categoryId: filterCategoryId.value,
      timeRange: filterTimeRange.value,
    }

    // 检查是否已被取消
    if (signal.aborted) return

    hasSearched.value = true
    loading.value = true
    searchError.value = ''
    currentPage.value = 1
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    tabCounts.value = { all: 0, articles: 0, users: 0, images: 0 }

    saveHistory(trimmed)

    const response = await searchApi.search(trimmed, activeTab.value as any, params)

    // 请求完成后检查是否已被取消
    if (signal.aborted) return

    const result = response.data.data

    if (activeTab.value === 'all') {
      articleResults.value = (result.articles || []) as Article[]
      userResults.value = (result.users || []) as User[]
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.articles = result.articleTotal ?? articleResults.value.length
      tabCounts.value.users = result.userTotal ?? userResults.value.length
      tabCounts.value.images = result.imageTotal ?? imageResults.value.length
      tabCounts.value.all = tabCounts.value.articles + tabCounts.value.users + tabCounts.value.images
    } else if (activeTab.value === 'articles') {
      articleResults.value = (result.articles || []) as Article[]
      tabCounts.value.articles = result.total || articleResults.value.length
    } else if (activeTab.value === 'users') {
      userResults.value = (result.users || []) as User[]
      tabCounts.value.users = result.total || userResults.value.length
    } else if (activeTab.value === 'images') {
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.images = result.total || imageResults.value.length
    }

    nextTick(() => setupInfiniteScroll())
    router.replace({ query: { keyword: trimmed } })
  } catch (err: any) {
    if (signal.aborted) return
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    // 如果是自动搜索触发的，不显示错误提示（用户可能还在输入）
  } finally {
    if (!signal.aborted) {
      loading.value = false
      autoSearching.value = false
    }
  }
}

// 执行搜索
const doSearch = async (loadMore = false) => {
  if (!keyword.value.trim()) return
  hasSearched.value = true

  // 取消进行中的自动搜索
  if (!loadMore && abortController) {
    abortController.abort()
    abortController = null
  }
  if (!loadMore && autoSearchTimer) {
    clearTimeout(autoSearchTimer)
    autoSearching.value = false
  }
  if (!loadMore && suggestTimer) {
    clearTimeout(suggestTimer)
  }

  if (!loadMore) {
    loading.value = true
    currentPage.value = 1
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    tabCounts.value = { all: 0, articles: 0, users: 0, images: 0 }
    searchError.value = ''
  } else {
    loadingMore.value = true
    currentPage.value++
  }

  saveHistory(keyword.value.trim())

  try {
    const { searchApi } = await import('~/api')
    const params: SearchParams = {
      page: currentPage.value,
      pageSize,
      sort: sortBy.value as SearchParams['sort'],
      categoryId: filterCategoryId.value,
      timeRange: filterTimeRange.value,
    }

    const response = await searchApi.search(keyword.value.trim(), activeTab.value as any, params)
    const result = response.data.data

    // 更新计数 & 填充结果
    if (activeTab.value === 'all') {
      // 综合搜索：所有结果一次性返回，使用后端返回的各分类总数
      articleResults.value = (result.articles || []) as Article[]
      userResults.value = (result.users || []) as User[]
      imageResults.value = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      tabCounts.value.articles = result.articleTotal ?? articleResults.value.length
      tabCounts.value.users = result.userTotal ?? userResults.value.length
      tabCounts.value.images = result.imageTotal ?? imageResults.value.length
      tabCounts.value.all = tabCounts.value.articles + tabCounts.value.users + tabCounts.value.images
    } else if (activeTab.value === 'articles') {
      const newArticles = (result.articles || []) as Article[]
      if (loadMore) {
        articleResults.value = [...articleResults.value, ...newArticles]
      } else {
        articleResults.value = newArticles
      }
      tabCounts.value.articles = result.total || articleResults.value.length
    } else if (activeTab.value === 'users') {
      const newUsers = (result.users || []) as User[]
      if (loadMore) {
        userResults.value = [...userResults.value, ...newUsers]
      } else {
        userResults.value = newUsers
      }
      tabCounts.value.users = result.total || userResults.value.length
    } else if (activeTab.value === 'images') {
      const newImages = (result.images || []).map((img: any) => ({
        url: resolveUrl(img.coverImage) || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      if (loadMore) {
        imageResults.value = [...imageResults.value, ...newImages]
      } else {
        imageResults.value = newImages
      }
      tabCounts.value.images = result.total || imageResults.value.length
    }

    // 设置无限滚动观察器
    nextTick(() => {
      setupInfiniteScroll()
    })
  } catch {
    if (!loadMore) {
      articleResults.value = []
      userResults.value = []
      imageResults.value = []
      searchError.value = '搜索失败，请稍后重试'
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }

  // 更新URL
  router.replace({ query: { keyword: keyword.value.trim() } })
}

// 重试搜索
const retrySearch = async () => {
  searchError.value = ''
  await doSearch()
}

// 设置无限滚动
const setupInfiniteScroll = () => {
  // 断开旧观察器
  articleObserver?.disconnect()
  userObserver?.disconnect()
  imageObserver?.disconnect()

  const observerCallback = (entries: IntersectionObserverEntry[]) => {
    if (entries[0]?.isIntersecting && !loadingMore.value) {
      doSearch(true)
    }
  }

  if (activeTab.value === 'articles' && articleSentinel.value && hasMoreArticles.value) {
    articleObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    articleObserver.observe(articleSentinel.value)
  }
  if (activeTab.value === 'users' && userSentinel.value && hasMoreUsers.value) {
    userObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    userObserver.observe(userSentinel.value)
  }
  if (activeTab.value === 'images' && imageSentinel.value && hasMoreImages.value) {
    imageObserver = new IntersectionObserver(observerCallback, { rootMargin: '200px' })
    imageObserver.observe(imageSentinel.value)
  }
}

// 切换Tab
const switchTab = (key: string) => {
  activeTab.value = key
  doSearch()
}

// 选择搜索建议
const selectSuggestion = (item: SuggestionItem) => {
  keyword.value = item.text
  doSearch()
}

// 清除关键词
const clearKeyword = () => {
  keyword.value = ''
  suggestions.value = []
  hasSearched.value = false
  autoSearching.value = false
  if (suggestTimer) clearTimeout(suggestTimer)
  if (autoSearchTimer) clearTimeout(autoSearchTimer)
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

// 关键词高亮
const highlightKeyword = (text: string) => {
  if (!keyword.value.trim()) return escapeHtml(text)
  const escaped = escapeHtml(text)
  const escapedKeyword = escapeHtml(keyword.value.trim())
  const regex = new RegExp(`(${escapedKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return escaped.replace(regex, '<span class="text-primary font-medium">$1</span>')
}

// HTML转义，防止XSS
const escapeHtml = (str: string) => {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/`/g, '&#96;')
}

// 搜索历史管理（使用统一 composable）
const { getHistory, addHistory, clearHistory: clearSearchHistory } = useSearchHistory()

// 保存搜索历史
const saveHistory = (kw: string) => {
  addHistory(kw)
  // 同步更新本地显示列表
  searchHistory.value = getHistory().map((e) => e.keyword)
}

// 清除搜索历史
const clearHistory = () => {
  clearSearchHistory()
  searchHistory.value = []
}

// 加载热门搜索
const loadHotSearches = async () => {
  try {
    const { searchApi } = await import('~/api')
    const response = await cachedRequestLong(
      () => searchApi.getHotSearches(),
      '/search/hot'
    )
    hotSearches.value = response.data.data
  } catch {
    // 忽略加载失败
  }
}

// 加载搜索历史（从统一 composable 读取）
const loadSearchHistory = () => {
  searchHistory.value = getHistory().map((e) => e.keyword)
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const { get } = useApi()
    const response = await cachedRequestLong(
      () => get<Category[]>('/categories'),
      '/categories'
    )
    categories.value = response.data.data || []
  } catch {
    // 忽略加载失败
  }
}

// 关注/取关用户
const toggleFollow = async (userId: number) => {
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api')
    const response = await socialApi.toggleFollow(userId)
    const result = response.data.data
    // 更新本地状态
    const user = userResults.value.find((u) => u.id === userId)
    if (user) {
      user.isFollowing = result.followed
      user.followerCount = result.followerCount
    }
  } catch (error: any) {
    console.error('关注操作失败:', error.message)
  }
}

// 图片点击
const handleImageClick = (image: any) => {
  // 可根据需要跳转到作品详情
}

// 页面元信息
useHead({
  title: () => keyword.value ? `${'搜索'} "${keyword.value}" - 知讯` : `${'搜索'} - 知讯`,
})
</script>
