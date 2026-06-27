<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-2 py-2">
      <!-- Header -->
      <div class="mb-2">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">话题广场</h1>
        <p class="text-gray-500 dark:text-gray-400 mt-1">发现热门话题，参与讨论</p>
      </div>

      <!-- Search -->
      <div class="mb-3">
        <div class="relative">
          <input v-model="keyword" @input="onSearch" placeholder="搜索话题..."
            class="w-full px-4 py-3 pl-10 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500" />
          <svg class="absolute left-3 top-3.5 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-4 mb-6 border-b border-gray-200 dark:border-gray-700 pb-2">
        <button @click="orderBy = 'hot'" :class="orderBy === 'hot' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500'"
          class="pb-2 px-1 font-medium transition">热门话题</button>
        <button @click="orderBy = 'new'" :class="orderBy === 'new' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500'"
          class="pb-2 px-1 font-medium transition">最新话题</button>
        <button @click="showCreate = true" v-if="userStore.isLoggedIn"
          class="ml-auto px-4 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 transition">创建话题</button>
      </div>

      <!-- Create Dialog -->
      <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showCreate = false">
        <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md mx-4">
          <h2 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">创建新话题</h2>
          <input v-model="createName" placeholder="话题名称（不超过50字）" maxlength="50"
            class="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 mb-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
          <textarea v-model="createDesc" placeholder="话题描述（选填）" rows="3"
            class="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 mb-4 focus:outline-none focus:ring-2 focus:ring-blue-500"></textarea>
          <div class="flex justify-end gap-3">
            <button @click="showCreate = false" class="px-4 py-2 text-gray-600 dark:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">取消</button>
            <button @click="doCreateTopic" :disabled="!createName.trim()"
              class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition">创建</button>
          </div>
        </div>
      </div>

      <!-- Topic List -->
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 5" :key="i" class="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse"></div>
      </div>
      <div v-else-if="topics.length === 0" class="text-center py-20 text-gray-400">
        <p>暂无话题</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="topic in topics" :key="topic.id"
          class="bg-white dark:bg-gray-800 rounded-xl p-4 flex items-center gap-4 hover:shadow-md transition cursor-pointer"
          @click="navigateTo(`/topics/${topic.id}`)">
          <div v-if="topic.coverImage" class="w-14 h-14 rounded-lg overflow-hidden flex-shrink-0">
            <img :src="topic.coverImage" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-14 h-14 rounded-lg bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white font-bold text-lg flex-shrink-0">
            {{ topic.name.charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-gray-900 dark:text-white">#{{ topic.name }}#</span>
              <span v-if="topic.isOfficial" class="text-xs px-1.5 py-0.5 bg-blue-100 text-blue-600 rounded">官方</span>
            </div>
            <p class="text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5">{{ topic.description || '暂无描述' }}</p>
            <div class="flex gap-4 text-xs text-gray-400 mt-1.5">
              <span>{{ topic.articleCount }} 篇内容</span>
              <span>{{ topic.followCount }} 人关注</span>
            </div>
          </div>
          <button @click.stop="toggleFollow(topic)"
            :class="topic.isFollowed ? 'bg-gray-100 dark:bg-gray-700 text-gray-600' : 'bg-blue-600 text-white'"
            class="px-4 py-1.5 rounded-full text-sm transition flex-shrink-0">
            {{ topic.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center mt-8 gap-2">
        <button v-for="p in totalPages" :key="p" @click="page = p; fetchTopics()"
          :class="page === p ? 'bg-blue-600 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'"
          class="w-10 h-10 rounded-lg transition">{{ p }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { topicApi } from '~/api/topic'
import type { Topic } from '~/api/topic'
import { useUserStore } from '~/stores/user'

const userStore = useUserStore()
const topics = ref<Topic[]>([])
const loading = ref(true)
const keyword = ref('')
const orderBy = ref('hot')
const page = ref(1)
const pageSize = 20
const totalPages = ref(1)
const showCreate = ref(false)
const createName = ref('')
const createDesc = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchTopics() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize, orderBy: orderBy.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await topicApi.getTopics(params)
    topics.value = res.list
    totalPages.value = Math.ceil(res.total / pageSize)
  } catch (e) { /* ignore */ }
  loading.value = false
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 1; fetchTopics() }, 300)
}

async function toggleFollow(topic: Topic) {
  if (!userStore.isLoggedIn) { navigateTo('/login'); return }
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
onMounted(() => { fetchTopics() })
</script>
