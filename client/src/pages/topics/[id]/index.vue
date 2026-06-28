<template>
  <div class="min-h-dvh-screen bg-[var(--zh-bg)] dark:bg-gray-900">
    <div class="max-w-4xl mx-auto px-2 py-2">
      <div v-if="topic" class="bg-[var(--zh-bg-elevated)] dark:bg-gray-800 rounded-2xl p-4 mb-3 shadow-sm">
        <div class="flex items-start gap-3">
          <div v-if="topic.coverImage" class="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0">
            <img :src="topic.coverImage" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-16 h-16 rounded-xl bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white font-bold text-xl flex-shrink-0">
            {{ topic.name.charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-0.5">
              <h1 class="text-xl font-bold text-[var(--zh-text)] dark:text-[var(--zh-text)]">#{{ topic.name }}#</h1>
              <span v-if="topic.isOfficial" class="text-xs px-2 py-0.5 bg-blue-100 text-[var(--zh-primary)] rounded-full">官方</span>
            </div>
            <p class="text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] text-sm mb-2">{{ topic.description || '暂无描述' }}</p>
            <div class="flex items-center gap-4 text-sm text-[var(--zh-text-tertiary)]">
              <span>{{ topic.articleCount }} 篇内容</span>
              <span>{{ topic.followCount }} 人关注</span>
            </div>
          </div>
          <button @click="toggleFollow"
            :class="topic.isFollowed ? 'bg-[var(--zh-bg-hover)] dark:bg-gray-700 text-[var(--zh-text-secondary)]' : 'bg-[var(--zh-primary)] text-white'"
            class="px-4 py-2 rounded-full text-sm font-medium transition flex-shrink-0">
            {{ topic.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="h-40 bg-[var(--zh-border)] dark:bg-gray-700 rounded-xl animate-pulse"></div>
      </div>
      <div v-else-if="articles.length === 0" class="text-center py-16 text-[var(--zh-text-tertiary)]">
        <p class="text-lg mb-2">该话题下暂无内容</p>
        <button @click="router.push('/editor')" class="mt-4 px-6 py-2 bg-[var(--zh-primary)] text-white rounded-lg hover:bg-[var(--zh-primary-dark)] transition">开始创作</button>
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
import { topicApi } from '@/api/topic'
import { articleApi } from '@/api/article'
import type { Topic } from '@/api/topic'
import type { Article } from '@/types'

const router = useRouter()
const route = useRoute()
const { setTitle } = usePageHeaderTitle()
const topic = ref<Topic | null>(null)
const articles = ref<Article[]>([])
const loading = ref(true)

async function fetchData() {
  const id = Number(route.params.id)
  try {
    const t = await topicApi.getTopicDetail(id)
    topic.value = (t as any)?.data?.data ?? t
    if (t) setTitle(`#${(t as any)?.data?.data?.name ?? (t as any)?.name}`)
    const res = await articleApi.getArticles({ tagId: id } as any)
    articles.value = (res as any)?.list ?? (res as any)?.data?.data?.list ?? []
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

// 核心修复：动态路由 topicId 变化时（切换话题）重置状态并重新加载
watch(() => route.params.id, async (newId, oldId) => {
  if (!newId || newId === oldId) return
  topic.value = null
  articles.value = []
  loading.value = true
  window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
  await fetchData()
})
</script>
