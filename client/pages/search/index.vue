<template>
  <!-- 搜索页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 搜索框（自动聚焦） -->
    <div class="mb-6">
      <div class="flex items-center bg-white dark:bg-gray-800 rounded-full px-4 py-3 shadow-sm border border-gray-200 dark:border-gray-700 focus-within:ring-2 focus-within:ring-primary focus-within:border-transparent transition-all">
        <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="text"
          class="flex-1 bg-transparent border-none outline-none ml-2 text-gray-900 dark:text-white placeholder-gray-400"
          placeholder="搜索文章、用户..."
          @input="handleInput"
          @keydown.enter="doSearch"
        />
        <button v-if="keyword" class="p-1 text-gray-400 hover:text-gray-600" @click="clearKeyword">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      <!-- 最少字符提示 -->
      <p v-if="keyword.trim().length === 1" class="text-xs text-amber-500 mt-1.5 ml-2">请输入至少2个字符进行搜索</p>
    </div>

    <!-- 搜索建议/热门搜索（未搜索时） -->
    <div v-if="!hasSearched">
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="mb-6">
        <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-3">搜索建议</h3>
        <div class="space-y-1">
          <button
            v-for="item in suggestions"
            :key="`${item.type}-${item.id}`"
            class="w-full text-left flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
            @click="selectSuggestion(item)"
          >
            <!-- 类型图标 -->
            <svg v-if="item.type === 'user'" class="w-4 h-4 text-gray-400 mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <svg v-else-if="item.type === 'article'" class="w-4 h-4 text-gray-400 mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <svg v-else class="w-4 h-4 text-gray-400 mr-2 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
            <!-- 用户头像 -->
            <UserAvatar v-if="item.type === 'user'" :src="item.avatar" :alt="item.text" size="xs" class="mr-2" />
            <span v-html="highlightKeyword(item.text)"></span>
            <span class="ml-auto text-xs text-gray-400">{{ item.type === 'user' ? '用户' : item.type === 'article' ? '文章' : '标签' }}</span>
          </button>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0" class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400">搜索历史</h3>
          <button class="text-xs text-gray-400 hover:text-danger" @click="clearHistory">清除</button>
        </div>
        <div class="flex flex-wrap gap-2">
          <button v-for="item in searchHistory" :key="item" class="px-3 py-1.5 bg-gray-100 dark:bg-gray-700 text-sm text-gray-700 dark:text-gray-300 rounded-full hover:bg-gray-200 dark:hover:bg-gray-600" @click="keyword = item; doSearch()">
            {{ item }}
          </button>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="hotSearches.length > 0">
        <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-3">热门搜索</h3>
        <div class="space-y-1">
          <button v-for="(item, index) in hotSearches" :key="item" class="w-full text-left flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="keyword = item; doSearch()">
            <span class="w-5 text-center text-xs font-bold" :class="index < 3 ? 'text-danger' : 'text-gray-400'">{{ index + 1 }}</span>
            <span class="ml-3">{{ item }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果（已搜索时） -->
    <div v-else>
      <!-- Tab切换 -->
      <div class="flex items-center border-b border-gray-200 dark:border-gray-700 mb-4">
        <button
          v-for="tab in searchTabs"
          :key="tab.key"
          class="px-4 py-3 text-sm font-medium border-b-2 transition-colors"
          :class="activeTab === tab.key
            ? 'border-primary text-primary'
            : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span v-if="tabCounts[tab.key]" class="ml-1 text-xs text-gray-400">({{ tabCounts[tab.key] }})</span>
        </button>
      </div>

      <!-- 筛选栏 -->
      <div class="flex items-center flex-wrap gap-3 mb-4 text-sm">
        <!-- 分类筛选 -->
        <select v-model="filterCategoryId" class="input py-1.5 text-sm w-auto min-w-[120px]" @change="doSearch()">
          <option :value="undefined">全部分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>

        <!-- 时间范围筛选 -->
        <select v-model="filterTimeRange" class="input py-1.5 text-sm w-auto min-w-[120px]" @change="doSearch()">
          <option :value="undefined">全部时间</option>
          <option value="24h">最近24小时</option>
          <option value="7d">最近7天</option>
          <option value="30d">最近30天</option>
        </select>

        <!-- 排序 -->
        <select v-model="sortBy" class="input py-1.5 text-sm w-auto min-w-[120px]" @change="doSearch()">
          <option value="relevance">相关度</option>
          <option value="latest">最新发布</option>
          <option value="popular">最多点赞</option>
        </select>
      </div>

      <!-- 搜索中骨架屏 -->
      <div v-if="loading">
        <!-- 综合Tab骨架屏 -->
        <template v-if="activeTab === 'all'">
          <div class="mb-6">
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20 mb-3 animate-pulse"></div>
            <div class="space-y-3">
              <LoadingSkeleton v-for="i in 2" :key="'a'+i" type="article" />
            </div>
          </div>
          <div class="mb-6">
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20 mb-3 animate-pulse"></div>
            <div class="space-y-3">
              <LoadingSkeleton v-for="i in 2" :key="'u'+i" type="user" />
            </div>
          </div>
          <div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20 mb-3 animate-pulse"></div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div v-for="i in 6" :key="'img'+i" class="aspect-square bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
            </div>
          </div>
        </template>
        <!-- 文章Tab骨架屏 -->
        <template v-else-if="activeTab === 'articles'">
          <div class="space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="article" />
          </div>
        </template>
        <!-- 用户Tab骨架屏 -->
        <template v-else-if="activeTab === 'users'">
          <div class="space-y-3">
            <LoadingSkeleton v-for="i in 5" :key="i" type="user" />
          </div>
        </template>
        <!-- 图片Tab骨架屏 -->
        <template v-else-if="activeTab === 'images'">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div v-for="i in 9" :key="i" class="aspect-square bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"></div>
          </div>
        </template>
      </div>

      <!-- 搜索结果列表 -->
      <div v-else-if="hasAnyResult">
        <!-- ===== 综合Tab ===== -->
        <template v-if="activeTab === 'all'">
          <!-- 用户区块 -->
          <div v-if="allUserResults.length > 0" class="mb-6">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400">相关用户</h3>
              <button v-if="tabCounts.users > 3" class="text-xs text-primary hover:underline" @click="switchTab('users')">查看全部 ({{ tabCounts.users }})</button>
            </div>
            <div class="space-y-3">
              <UserCard v-for="user in allUserResults" :key="'u-'+user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
            </div>
          </div>

          <!-- 文章区块 -->
          <div v-if="allArticleResults.length > 0" class="mb-6">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400">相关文章</h3>
              <button v-if="tabCounts.articles > 5" class="text-xs text-primary hover:underline" @click="switchTab('articles')">查看全部 ({{ tabCounts.articles }})</button>
            </div>
            <div class="space-y-3">
              <ArticleCard v-for="item in allArticleResults" :key="'a-'+item.id" :article="item" />
            </div>
          </div>

          <!-- 图片区块 -->
          <div v-if="allImageResults.length > 0" class="mb-6">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400">相关图片</h3>
              <button v-if="tabCounts.images > 6" class="text-xs text-primary hover:underline" @click="switchTab('images')">查看全部 ({{ tabCounts.images }})</button>
            </div>
            <ImageGrid :images="allImageResults" @click="handleImageClick" />
          </div>
        </template>

        <!-- ===== 文章Tab ===== -->
        <template v-if="activeTab === 'articles'">
          <div class="space-y-3">
            <ArticleCard v-for="item in articleResults" :key="item.id" :article="item" />
          </div>
          <!-- 加载更多 -->
          <div ref="articleSentinel" class="h-1"></div>
          <div v-if="loadingMore" class="py-4 text-center text-sm text-gray-400">
            <svg class="animate-spin inline w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreArticles && articleResults.length > 0" class="py-4 text-center text-sm text-gray-400">没有更多了</div>
        </template>

        <!-- ===== 用户Tab ===== -->
        <template v-if="activeTab === 'users'">
          <div class="space-y-3">
            <UserCard v-for="user in userResults" :key="user.id" :user="user" :show-follow-button="true" @toggle-follow="toggleFollow" />
          </div>
          <!-- 加载更多 -->
          <div ref="userSentinel" class="h-1"></div>
          <div v-if="loadingMore" class="py-4 text-center text-sm text-gray-400">
            <svg class="animate-spin inline w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreUsers && userResults.length > 0" class="py-4 text-center text-sm text-gray-400">没有更多了</div>
        </template>

        <!-- ===== 图片Tab ===== -->
        <template v-if="activeTab === 'images'">
          <ImageGrid :images="imageResults" @click="handleImageClick" />
          <!-- 加载更多 -->
          <div ref="imageSentinel" class="h-1"></div>
          <div v-if="loadingMore" class="py-4 text-center text-sm text-gray-400">
            <svg class="animate-spin inline w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            加载中...
          </div>
          <div v-if="!hasMoreImages && imageResults.length > 0" class="py-4 text-center text-sm text-gray-400">没有更多了</div>
        </template>
      </div>

      <!-- 空结果 -->
      <EmptyState v-else title="未找到相关结果" description="换个关键词试试吧" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 搜索页：搜索框、搜索建议、搜索历史、热门搜索、综合搜索结果展示 */
import type { Article, User, Category } from '~/types'
import type { SuggestionItem, SearchParams } from '~/api/search'

const route = useRoute()
const router = useRouter()

// 搜索Tab
const searchTabs = [
  { key: 'all', label: '综合' },
  { key: 'articles', label: '文章' },
  { key: 'users', label: '用户' },
  { key: 'images', label: '图片' },
]

const keyword = ref((route.query.keyword as string) || '')
const activeTab = ref('all')
const sortBy = ref('relevance')
const filterCategoryId = ref<number | undefined>(undefined)
const filterTimeRange = ref<'24h' | '7d' | '30d' | undefined>(undefined)
const hasSearched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
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
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// IntersectionObserver实例
let articleObserver: IntersectionObserver | null = null
let userObserver: IntersectionObserver | null = null
let imageObserver: IntersectionObserver | null = null

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

// 输入处理（防抖300ms）
const handleInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (keyword.value.trim()) {
      try {
        const { searchApi } = await import('~/api')
        const response = await searchApi.getSuggestions(keyword.value.trim())
        const data = response.data.data
        suggestions.value = data?.completions || []
      } catch {
        suggestions.value = []
      }
    } else {
      suggestions.value = []
    }
  }, 300)
}

// 执行搜索
const doSearch = async (loadMore = false) => {
  if (!keyword.value.trim() || keyword.value.trim().length < 2) return
  hasSearched.value = true

  if (!loadMore) {
    loading.value = true
    currentPage.value = 1
    articleResults.value = []
    userResults.value = []
    imageResults.value = []
    tabCounts.value = { all: 0, articles: 0, users: 0, images: 0 }
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

    // 更新计数
    tabCounts.value.articles = (result as any).total || result.articles?.length || 0
    tabCounts.value.users = (result as any).total || result.users?.length || 0
    tabCounts.value.images = (result as any).total || result.images?.length || 0
    tabCounts.value.all = tabCounts.value.articles + tabCounts.value.users + tabCounts.value.images

    if (activeTab.value === 'all') {
      // 综合搜索：所有结果一次性返回
      articleResults.value = (result.articles || []) as Article[]
      userResults.value = (result.users || []) as User[]
      imageResults.value = (result.images || []).map((img: any) => ({
        url: img.coverImage || '',
        title: img.title || '',
        articleTitle: img.title || '',
        author: img.authorName || img.author?.nickname || '',
      }))
      // 综合搜索时，用实际返回数量作为计数
      tabCounts.value.articles = articleResults.value.length
      tabCounts.value.users = userResults.value.length
      tabCounts.value.images = imageResults.value.length
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
        url: img.coverImage || '',
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
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }

  // 更新URL
  router.replace({ query: { keyword: keyword.value.trim() } })
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
}

// 关键词高亮
const highlightKeyword = (text: string) => {
  if (!keyword.value.trim()) return text
  const regex = new RegExp(`(${keyword.value.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<span class="text-primary font-medium">$1</span>')
}

// 保存搜索历史
const saveHistory = (kw: string) => {
  const history = searchHistory.value.filter((h) => h !== kw)
  history.unshift(kw)
  searchHistory.value = history.slice(0, 10)
  if (import.meta.client) {
    localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
  }
}

// 清除搜索历史
const clearHistory = () => {
  searchHistory.value = []
  if (import.meta.client) {
    localStorage.removeItem('searchHistory')
  }
}

// 加载热门搜索
const loadHotSearches = async () => {
  try {
    const { searchApi } = await import('~/api')
    const response = await searchApi.getHotSearches()
    hotSearches.value = response.data.data
  } catch {
    // 忽略加载失败
  }
}

// 加载搜索历史
const loadSearchHistory = () => {
  if (import.meta.client) {
    const saved = localStorage.getItem('searchHistory')
    if (saved) {
      try {
        searchHistory.value = JSON.parse(saved)
      } catch {
        searchHistory.value = []
      }
    }
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const { get } = useApi()
    const response = await get<Category[]>('/categories')
    categories.value = response.data.data || []
  } catch {
    // 忽略加载失败
  }
}

// 关注/取关用户
const toggleFollow = async (userId: number) => {
  try {
    const { socialApi } = await import('~/api')
    await socialApi.toggleFollow(userId)
    // 更新本地状态
    const user = userResults.value.find((u) => u.id === userId)
    if (user) {
      user.isFollowing = !user.isFollowing
      user.followerCount += user.isFollowing ? 1 : -1
    }
  } catch {
    // 忽略
  }
}

// 图片点击
const handleImageClick = (image: any) => {
  // 可根据需要跳转到文章详情
}

// 页面元信息
useHead({
  title: () => keyword.value ? `搜索 "${keyword.value}" - 知讯` : '搜索 - 知讯',
})
</script>
