<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-2 py-2">
      <div v-if="topic" class="bg-white dark:bg-gray-800 rounded-2xl p-4 mb-3 shadow-sm">
        <div class="flex items-start gap-3">
          <div v-if="topic.coverImage" class="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0">
            <img :src="topic.coverImage" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-16 h-16 rounded-xl bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white font-bold text-xl flex-shrink-0">
            {{ topic.name.charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-0.5">
              <h1 class="text-xl font-bold text-gray-900 dark:text-white">#{{ topic.name }}#</h1>
              <span v-if="topic.isOfficial" class="text-xs px-2 py-0.5 bg-blue-100 text-blue-600 rounded-full">官方</span>
            </div>
            <p class="text-gray-500 dark:text-gray-400 text-sm mb-2">{{ topic.description || '暂无描述' }}</p>
            <div class="flex items-center gap-4 text-sm text-gray-400">
              <span>{{ topic.articleCount }} 篇内容</span>
              <span>{{ topic.followCount }} 人关注</span>
            </div>
          </div>
          <button @click="toggleFollow"
            :class="topic.isFollowed ? 'bg-gray-100 dark:bg-gray-700 text-gray-600' : 'bg-blue-600 text-white'"
            class="px-4 py-2 rounded-full text-sm font-medium transition flex-shrink-0">
            {{ topic.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="h-40 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse"></div>
      </div>
      <div v-else-if="articles.length === 0" class="text-center py-16 text-gray-400">
        <p class="text-lg mb-2">该话题下暂无内容</p>
        <button @click="navigateTo('/editor')" class="mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">开始创作</button>
      </div>
      <div v-else class="space-y-3">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { topicApi } from '~/api/topic'
import { articleApi } from '~/api/article'
import type { Topic } from '~/api/topic'
import type { Article } from '~/types'

const route = useRoute()
const topic = ref<Topic | null>(null)
const articles = ref<Article[]>([])
const loading = ref(true)

async function fetchData() {
  const id = Number(route.params.id)
  try {
    const t = await topicApi.getTopicDetail(id)
    topic.value = t
    const res = await articleApi.getArticles({ tagId: id } as any)
    articles.value = res.list || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function toggleFollow() {
  if (!topic.value) return
  try {
    await topicApi.toggleFollow(topic.value.id)
    topic.value.isFollowed = !topic.value.isFollowed
    topic.value.followCount += topic.value.isFollowed ? 1 : -1
  } catch (e) { /* ignore */ }
}

onMounted(fetchData)
</script>
