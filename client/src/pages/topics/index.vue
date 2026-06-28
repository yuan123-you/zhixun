<template>
  <div class="min-h-dvh-screen bg-[var(--zh-bg)] dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-2 py-2">

      <!-- Search -->
      <div class="mb-3 flex items-center gap-2">
        <button
          class="w-8 h-8 flex items-center justify-center text-[var(--zh-text-secondary)] hover:text-[var(--zh-primary)] active:scale-95 transition shrink-0 rounded-lg hover:bg-[var(--zh-bg-hover)]"
          aria-label="返回上一页"
          @click="router.back()"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div class="input-field-topic flex-1" :class="{ focused: searchFocused }">
          <svg class="input-prefix-topic" :class="{ 'text-primary': searchFocused }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input v-model="keyword" @input="onSearch" placeholder="搜索话题..."
            class="input-control-topic"
            @focus="searchFocused = true"
            @blur="searchFocused = false" />
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-4 mb-6 border-b border-[var(--zh-border)] dark:border-gray-700 pb-2">
        <button @click="orderBy = 'hot'" :class="orderBy === 'hot' ? 'text-[var(--zh-primary)] border-b-2 border-[var(--zh-primary)]' : 'text-[var(--zh-text-secondary)]'"
          class="pb-2 px-1 font-medium transition">热门话题</button>
        <button @click="orderBy = 'new'" :class="orderBy === 'new' ? 'text-[var(--zh-primary)] border-b-2 border-[var(--zh-primary)]' : 'text-[var(--zh-text-secondary)]'"
          class="pb-2 px-1 font-medium transition">最新话题</button>
        <button @click="showCreate = true" v-if="userStore.isLoggedIn"
          class="ml-auto px-4 py-1.5 bg-[var(--zh-primary)] text-white rounded-lg text-sm hover:bg-[var(--zh-primary-dark)] transition">创建话题</button>
      </div>

      <!-- Create Dialog -->
      <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showCreate = false">
        <div class="bg-[var(--zh-bg-elevated)] dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md mx-4">
          <h2 class="text-lg font-bold mb-4 text-[var(--zh-text)] dark:text-[var(--zh-text)]">创建新话题</h2>
          <div class="input-field-topic mb-3" :class="{ focused: createNameFocused }">
            <input v-model="createName" placeholder="话题名称（不超过50字）" maxlength="50"
              class="input-control-topic"
              @focus="createNameFocused = true"
              @blur="createNameFocused = false" />
          </div>
          <div class="input-field-topic mb-4" :class="{ focused: createDescFocused }">
            <textarea v-model="createDesc" placeholder="话题描述（选填）" rows="3"
              class="input-control-topic !h-auto resize-none"
              @focus="createDescFocused = true"
              @blur="createDescFocused = false"></textarea>
          </div>
          <div class="flex justify-end gap-3">
            <button @click="showCreate = false" class="px-4 py-2 text-[var(--zh-text-secondary)] dark:text-gray-300 rounded-lg hover:bg-[var(--zh-bg-hover)] dark:hover:bg-gray-700">取消</button>
            <button @click="doCreateTopic" :disabled="!createName.trim()"
              class="px-6 py-2 bg-[var(--zh-primary)] text-white rounded-lg hover:bg-[var(--zh-primary-dark)] disabled:opacity-50 transition">创建</button>
          </div>
        </div>
      </div>

      <!-- Topic List -->
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 5" :key="i" class="h-24 bg-[var(--zh-border)] dark:bg-gray-700 rounded-xl animate-pulse"></div>
      </div>
      <div v-else-if="topics.length === 0" class="text-center py-20 text-[var(--zh-text-tertiary)]">
        <p>暂无话题</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="topic in topics" :key="topic.id"
          class="bg-[var(--zh-bg-elevated)] dark:bg-gray-800 rounded-xl p-4 flex items-center gap-4 hover:shadow-md transition cursor-pointer"
          @click="goToTopic(topic.id)">
          <div v-if="topic.coverImage" class="w-14 h-14 rounded-lg overflow-hidden flex-shrink-0">
            <img :src="topic.coverImage" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-14 h-14 rounded-lg bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white font-bold text-lg flex-shrink-0">
            {{ topic.name.charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-[var(--zh-text)] dark:text-[var(--zh-text)]">#{{ topic.name }}#</span>
              <span v-if="topic.isOfficial" class="text-xs px-1.5 py-0.5 bg-blue-100 text-[var(--zh-primary)] rounded">官方</span>
            </div>
            <p class="text-sm text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] truncate mt-0.5">{{ topic.description || '暂无描述' }}</p>
            <div class="flex gap-4 text-xs text-[var(--zh-text-tertiary)] mt-1.5">
              <span>{{ topic.articleCount }} 篇内容</span>
              <span>{{ topic.followCount }} 人关注</span>
            </div>
          </div>
          <button @click.stop="toggleFollow(topic)"
            :class="topic.isFollowed ? 'bg-[var(--zh-bg-hover)] dark:bg-gray-700 text-[var(--zh-text-secondary)]' : 'bg-[var(--zh-primary)] text-white'"
            class="px-4 py-1.5 rounded-full text-sm transition flex-shrink-0">
            {{ topic.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>

      <!-- Infinite scroll loader -->
      <div ref="sentinelRef" class="h-0.5"></div>
      <div v-if="loadingMore" class="flex justify-center py-6">
        <div class="inline-flex items-center gap-2 text-sm text-[var(--zh-text-tertiary)]">
          <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          加载中...
        </div>
      </div>
      <div v-else-if="!hasMore && topics.length > 0" class="text-center py-6">
        <span class="text-xs text-[var(--zh-text-tertiary)]">没有更多了</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { topicApi } from '@/api/topic'
import type { Topic } from '@/api/topic'
import { useUserStore } from '@/stores/user'

const { setTitle } = usePageHeaderTitle()
setTitle('话题广场')

const router = useRouter()
const userStore = useUserStore()
const topics = ref<Topic[]>([])

function goToTopic(id: number | string) {
  router.push(`/topics/${id}`)
}
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const keyword = ref('')
const orderBy = ref('hot')
const page = ref(1)
const pageSize = 20
const sentinelRef = ref<HTMLElement | null>(null)
const showCreate = ref(false)
const createName = ref('')
const createDesc = ref('')
const searchFocused = ref(false)
const createNameFocused = ref(false)
const createDescFocused = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null
let intersectionObserver: IntersectionObserver | null = null

async function fetchTopics(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const params: any = { page: page.value, pageSize, orderBy: orderBy.value }
    if (keyword.value) params.keyword = keyword.value
    const response = await topicApi.getTopics(params)
    const data = response.data?.data
    const list = data?.list || []
    if (isLoadMore) {
      topics.value.push(...list)
    } else {
      topics.value = list
    }
    hasMore.value = list.length >= pageSize
  } catch (e) { /* ignore */ }
  loading.value = false
  loadingMore.value = false
}

function loadMore() {
  if (!hasMore.value || loadingMore.value || loading.value) return
  page.value++
  fetchTopics(true)
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 1; fetchTopics() }, 300)
}

async function toggleFollow(topic: Topic) {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    await topicApi.toggleFollow(topic.id)
    topic.isFollowed = !topic.isFollowed
    topic.followCount += topic.isFollowed ? 1 : -1
  } catch (e) { /* ignore */ }
}

async function doCreateTopic() {
  if (!createName.value.trim()) return
  try {
    await topicApi.createTopic({ name: createName.value.trim(), description: createDesc.value.trim() })
    showCreate.value = false
    createName.value = ''
    createDesc.value = ''
    fetchTopics()
  } catch (e) { /* ignore */ }
}

watch(orderBy, () => { page.value = 1; fetchTopics() })
onMounted(() => {
  fetchTopics()
  // 设置无限滚动观察器
  // 注：onUnmounted 必须在 setup 同步上下文中注册，
  // 不能放在 nextTick / setTimeout 等异步回调里。
  // 因此 observer 提升为模块级变量，并在 onMounted 中创建、在 onUnmounted 中释放。
  if (sentinelRef.value) {
    intersectionObserver = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && hasMore.value && !loading.value && !loadingMore.value) {
        loadMore()
      }
    }, { rootMargin: '200px' })
    intersectionObserver.observe(sentinelRef.value)
  }
})

onUnmounted(() => {
  if (intersectionObserver) {
    intersectionObserver.disconnect()
    intersectionObserver = null
  }
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})
</script>

<style scoped>
/* 统一输入框样式 - 匹配登录页交互 */
.input-field-topic {
  display: flex;
  align-items: center;
  height: 44px;
  border: 1.5px solid var(--zh-border);
  border-radius: 12px;
  background: var(--zh-bg);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}
.input-field-topic.focused {
  border-color: var(--zh-primary);
  box-shadow: 0 0 0 3px rgba(var(--zh-primary-rgb), 0.08);
}

.input-prefix-topic {
  width: 18px;
  height: 18px;
  margin-left: 14px;
  flex-shrink: 0;
  color: var(--zh-text-tertiary);
  transition: color 0.2s ease;
}

.input-control-topic {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 12px;
  font-size: 14px;
  color: var(--zh-text);
  font-family: inherit;
}
.input-control-topic::placeholder {
  color: var(--zh-text-placeholder);
  font-size: 13px;
}
</style>
