<template>
  <!-- 搜索框组件（Element Plus 风格） -->
  <div ref="searchRef" class="search-bar">
    <el-input
      ref="inputRef"
      v-model="keyword"
      placeholder="搜索作品、用户..."
      :prefix-icon="SearchIcon"
      clearable
      size="large"
      @input="handleInput"
      @focus="showSuggestions = true"
      @keydown.enter="handleSearch"
    />

    <div v-if="showSuggestions && (suggestions.length > 0 || hotSearches.length > 0 || searchHistory.length > 0)" class="search-dropdown">
      <div v-if="suggestions.length > 0 && keyword" class="search-section">
        <div class="search-section-title">搜索建议</div>
        <button v-for="item in suggestions" :key="item" class="search-suggestion" @click="selectSuggestion(item)">
          {{ item }}
        </button>
      </div>

      <div v-if="searchHistory.length > 0 && !keyword" class="search-section">
        <div class="search-section-header">
          <span>搜索历史</span>
          <el-button text size="small" type="danger" @click="clearHistory">清除</el-button>
        </div>
        <button v-for="item in searchHistory" :key="item" class="search-suggestion" @click="selectSuggestion(item)">
          {{ item }}
        </button>
      </div>

      <div v-if="hotSearches.length > 0 && !keyword" class="search-section">
        <div class="search-section-title">热门搜索</div>
        <button v-for="(item, index) in hotSearches" :key="item" class="search-suggestion" @click="selectSuggestion(item)">
          <span class="hot-rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
          <span>{{ item }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search as SearchIcon } from '@element-plus/icons-vue'

const router = useRouter()
const { searchApi } = useApi()
const { getHistory, addHistory, clearHistory: clearSearchHistory } = useSearchHistory()

const keyword = ref('')
const suggestions = ref<string[]>([])
const hotSearches = ref<string[]>([])
const showSuggestions = ref(false)
const searchRef = ref<HTMLElement | null>(null)
const inputRef = ref<any>(null)

const searchHistory = computed(() => getHistory().map((e) => e.keyword))

const fetchSuggestions = async (query: string) => {
  if (query.trim()) {
    try { const response = await searchApi.getSuggestions(query.trim()); suggestions.value = response.data.data }
    catch { suggestions.value = [] }
  } else { suggestions.value = [] }
}

const debouncedFetchSuggestions = useDebounceFn(fetchSuggestions, 300)

const handleInput = () => debouncedFetchSuggestions(keyword.value)

const handleSearch = () => {
  if (!keyword.value.trim()) return
  addHistory(keyword.value.trim())
  showSuggestions.value = false
  router.push(`/search?keyword=${encodeURIComponent(keyword.value.trim())}`)
}

const selectSuggestion = (item: string) => { keyword.value = item; handleSearch() }

const clearHistory = async () => {
  clearSearchHistory()
  try { await searchApi.clearHistory() } catch { /* ignore */ }
}

const handleClickOutside = (event: MouseEvent) => {
  if (searchRef.value && !searchRef.value.contains(event.target as Node)) showSuggestions.value = false
}

onMounted(async () => {
  try { const response = await searchApi.getHotSearches(); hotSearches.value = response.data.data }
  catch { /* ignore */ }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  debouncedFetchSuggestions.cancel()
})
</script>

<style scoped>
.search-bar { position: relative; }
.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 8px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-dark);
  border: 1px solid var(--el-border-color-lighter);
  overflow: hidden;
  z-index: 50;
}
.search-section { padding: 8px 0; }
.search-section + .search-section { border-top: 1px solid var(--el-border-color-lighter); }
.search-section-title { padding: 6px 16px; font-size: 12px; color: var(--el-text-color-placeholder); }
.search-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 16px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.search-suggestion {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.15s;
}
.search-suggestion:hover { background: var(--el-fill-color-lighter); }
.hot-rank { width: 20px; text-align: center; font-size: 12px; color: var(--el-text-color-placeholder); }
.hot-rank.top3 { color: var(--el-color-danger); font-weight: 700; }
</style>
