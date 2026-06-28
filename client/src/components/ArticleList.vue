<template>
  <!-- 作品列表组件：支持无限滚动、骨架屏、错误重试 -->
  <div>
    <template v-if="loading && articles.length === 0">
      <div>
        <template v-for="i in 6" :key="i">
          <LoadingSkeleton type="article" />
        </template>
      </div>
    </template>

    <template v-else-if="error && articles.length === 0">
      <ErrorRetry :message="error" :on-retry="handleRetry" />
    </template>

    <template v-else>
      <div>
        <template v-for="(article, index) in articles" :key="article.id">
          <ArticleCard :article="article" />
        </template>
      </div>

      <div v-if="loading && articles.length > 0">
        <LoadingSkeleton v-for="i in 2" :key="'more-' + i" type="article" />
      </div>

      <div v-if="error && articles.length > 0" style="margin-top: 10px;">
        <ErrorRetry :message="error" :on-retry="handleRetry" />
      </div>

      <div v-if="!hasMore && !error && articles.length > 0" style="text-align: center; font-size: 14px; color: var(--el-text-color-placeholder); padding: 8px 0;">
        没有更多了
      </div>

      <EmptyState v-if="!loading && !error && articles.length === 0" title="暂无作品" description="快去发现精彩内容吧" />
    </template>

    <div ref="sentinelRef" style="height: 1px;"></div>
  </div>
</template>

<script setup lang="ts">
/** 作品列表组件 */
import type { Article } from '@/types'

const props = withDefaults(defineProps<{
  articles: Article[]
  loading: boolean
  hasMore: boolean
  error?: string | null
}>(), {
  error: null,
})

const emit = defineEmits<{
  loadMore: []
  retry: []
}>()

const sentinelRef = ref<HTMLElement | null>(null)
const handleRetry = () => emit('retry')

onMounted(() => {
  if (!sentinelRef.value) return
  const observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !props.loading && props.hasMore && !props.error) {
        emit('loadMore')
      }
    },
    { rootMargin: '200px' }
  )
  observer.observe(sentinelRef.value)
  onUnmounted(() => observer.disconnect())
})
</script>
