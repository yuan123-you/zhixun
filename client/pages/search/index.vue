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
    </div>

    <!-- 搜索建议/热门搜索（未搜索时） -->
    <div v-if="!hasSearched">
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="mb-6">
        <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-3">搜索建议</h3>
        <div class="space-y-1">
          <button v-for="item in suggestions" :key="item" class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="selectSuggestion(item)">
            <span v-html="highlightKeyword(item)"></span>
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
          <button v-for="item in searchHistory" :key="item" class="px-3 py-1.5 bg-gray-100 dark:bg-gray-700 text-sm text-gray-700 dark:text-gray-300 rounded-full hover:bg-gray-200 dark:hover:bg-gray-600" @click="selectSuggestion(item)">
            {{ item }}
          </button>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="hotSearches.length > 0">
        <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400 mb-3">热门搜索</h3>
        <div class="space-y-1">
          <button v-for="(item, index) in hotSearches" :key="item" class="w-full text-left flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="selectSuggestion(item)">
            <span class="w-5 text-center text-xs font-bold" :class="index < 3 ? 'text-danger' : 'text-gray-400'">{{ index + 1 }}</span>
            <span class="ml-3">{{ item }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果（已搜索时） -->
    <div v-else>
      <!-- Tab切换 -->
      <div class="flex items-center border-b border-gray-200 dark:border-gray-700 mb-6">
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
        </button>
      </div>

      <!-- 筛选栏 -->
      <div class="flex items-center space-x-4 mb-4 text-sm">
        <select v-model="sortBy" class="input py-1.5 text-sm w-auto">
          <option value="relevance">相关度</option>
          <option value="latest">最新发布</option>
          <option value="popular">最多点赞</option>
        </select>
      </div>

      <!-- 搜索结果列表 -->
      <div v-if="searchResults.length > 0">
        <!-- 文章结果 -->
        <template v-if="activeTab === 'all' || activeTab === 'articles'">
          <ArticleCard v-for="item in searchResults" :key="item.id" :article="item" />
        </template>

        <!-- 用户结果 -->
        <template v-if="activeTab === 'users'">
          <UserCard v-for="user in userResults" :key="user.id" :user="user" :show-follow-button="true" />
        </template>

        <!-- 图片结果 -->
        <template v-if="activeTab === 'images'">
          <ImageGrid :images="imageResults" />
        </template>
      </div>

      <!-- 无结果 -->
      <EmptyState v-else-if="!loading" title="未找到相关结果" description="换个关键词试试吧" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 搜索页：搜索框、搜索建议、搜索历史、热门搜索、关键词高亮 */
import type { Article, User } from '~/types'

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
const hasSearched = ref(false)
const loading = ref(false)
const suggestions = ref<string[]>([])
const hotSearches = ref<string[]>([])
const searchHistory = ref<string[]>([])
const searchResults = ref<Article[]>([])
const userResults = ref<User[]>([])
const imageResults = ref<{ url: string; title: string }[]>([])
const searchInputRef = ref<HTMLInputElement | null>(null)

// 防抖定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 从URL参数初始化搜索
onMounted(() => {
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
})

// 输入处理（防抖300ms）
const handleInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (keyword.value.trim()) {
      try {
        const { searchApi } = await import('~/api')
        const response = await searchApi.getSuggestions(keyword.value.trim())
        suggestions.value = response.data.data
      } catch {
        suggestions.value = []
      }
    } else {
      suggestions.value = []
    }
  }, 300)
}

// 执行搜索
const doSearch = async () => {
  if (!keyword.value.trim()) return
  hasSearched.value = true
  loading.value = true
  saveHistory(keyword.value.trim())

  try {
    const { searchApi } = await import('~/api')
    const response = await searchApi.search(keyword.value.trim(), activeTab.value as any, { page: 1, pageSize: 20 })
    searchResults.value = response.data.data.items as Article[]
  } catch {
    searchResults.value = []
  } finally {
    loading.value = false
  }

  // 更新URL
  router.replace({ query: { keyword: keyword.value.trim() } })
}

// 切换Tab
const switchTab = (key: string) => {
  activeTab.value = key
  if (hasSearched.value) doSearch()
}

// 选择搜索建议
const selectSuggestion = (item: string) => {
  keyword.value = item
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
  const regex = new RegExp(`(${keyword.value.trim()})`, 'gi')
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

// 页面元信息
useHead({
  title: () => keyword.value ? `搜索 "${keyword.value}" - 知讯` : '搜索 - 知讯',
})
</script>
