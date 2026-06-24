<template>
  <!-- 搜索框组件 -->
  <div class="relative" ref="searchRef">
    <div class="flex items-center bg-gray-100 dark:bg-gray-700 rounded-full px-4 py-2 focus-within:ring-2 focus-within:ring-primary focus-within:bg-white dark:focus-within:bg-gray-600 transition-all">
      <!-- 搜索图标 -->
      <svg class="w-5 h-5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>

      <!-- 输入框 -->
      <input
        ref="inputRef"
        v-model="keyword"
        type="text"
        class="flex-1 bg-transparent border-none outline-none ml-2 text-sm text-gray-900 dark:text-white placeholder-gray-400"
        :placeholder="t('search.placeholder')"
        @input="handleInput"
        @focus="showSuggestions = true"
        @keydown.enter="handleSearch"
      />

      <!-- 清除按钮 -->
      <button v-if="keyword" class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" @click="clearKeyword">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- 搜索建议下拉 -->
    <div v-if="showSuggestions && (suggestions.length > 0 || hotSearches.length > 0 || searchHistory.length > 0)" class="absolute top-full left-0 right-0 mt-2 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 overflow-hidden z-50">
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0 && keyword" class="py-2">
        <div class="px-4 py-1.5 text-xs text-gray-400">{{ t('search.suggestions') }}</div>
        <button v-for="item in suggestions" :key="item" class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="selectSuggestion(item)">
          {{ item }}
        </button>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0 && !keyword" class="py-2">
        <div class="flex items-center justify-between px-4 py-1.5">
          <span class="text-xs text-gray-400">{{ t('search.history') }}</span>
          <button class="text-xs text-gray-400 hover:text-danger" @click="clearHistory">{{ t('common.clear') }}</button>
        </div>
        <button v-for="item in searchHistory" :key="item" class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="selectSuggestion(item)">
          {{ item }}
        </button>
      </div>

      <!-- 热门搜索 -->
      <div v-if="hotSearches.length > 0 && !keyword" class="py-2 border-t border-gray-100 dark:border-gray-700">
        <div class="px-4 py-1.5 text-xs text-gray-400">{{ t('search.hotSearch') }}</div>
        <button v-for="(item, index) in hotSearches" :key="item" class="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center" @click="selectSuggestion(item)">
          <span class="w-5 text-center text-xs" :class="index < 3 ? 'text-danger font-bold' : 'text-gray-400'">{{ index + 1 }}</span>
          <span class="ml-2">{{ item }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 搜索框组件：输入防抖、搜索建议、热门搜索 */

const router = useRouter()
const { searchApi } = useApi()
const { t } = useI18n()

const keyword = ref('')
const suggestions = ref<string[]>([])
const hotSearches = ref<string[]>([])
const searchHistory = ref<string[]>([])
const showSuggestions = ref(false)
const searchRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

// 防抖定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 输入处理（防抖300ms）
const handleInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (keyword.value.trim()) {
      try {
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
const handleSearch = () => {
  if (!keyword.value.trim()) return
  // 保存搜索历史
  saveHistory(keyword.value.trim())
  showSuggestions.value = false
  router.push(`/search?keyword=${encodeURIComponent(keyword.value.trim())}`)
}

// 选择搜索建议
const selectSuggestion = (item: string) => {
  keyword.value = item
  handleSearch()
}

// 清除关键词
const clearKeyword = () => {
  keyword.value = ''
  suggestions.value = []
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
const clearHistory = async () => {
  searchHistory.value = []
  if (import.meta.client) {
    localStorage.removeItem('searchHistory')
  }
  try {
    await searchApi.clearHistory()
  } catch {
    // 忽略清除失败
  }
}

// 点击外部关闭建议
const handleClickOutside = (event: MouseEvent) => {
  if (searchRef.value && !searchRef.value.contains(event.target as Node)) {
    showSuggestions.value = false
  }
}

// 加载热门搜索和搜索历史
onMounted(async () => {
  // 从 localStorage 恢复搜索历史
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

  // 获取热门搜索
  try {
    const response = await searchApi.getHotSearches()
    hotSearches.value = response.data.data
  } catch {
    // 忽略加载失败
  }

  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>
