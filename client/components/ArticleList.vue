<template>
  <!-- 文章列表组件：支持无限滚动、骨架屏、平板双列布局 -->
  <div>
    <!-- 骨架屏加载状态 -->
    <template v-if="loading && articles.length === 0">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <LoadingSkeleton v-for="i in 6" :key="i" type="article" />
      </div>
    </template>

    <!-- 文章列表 -->
    <template v-else>
      <!-- 平板端双列网格，移动端单列 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>

      <!-- 加载更多骨架屏 -->
      <div v-if="loading && articles.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
        <LoadingSkeleton v-for="i in 2" :key="'more-' + i" type="article" />
      </div>

      <!-- 没有更多数据 -->
      <div v-if="!hasMore && articles.length > 0" class="text-center text-sm text-gray-400 dark:text-gray-500 py-4">
        没有更多了
      </div>

      <!-- 空状态 -->
      <EmptyState v-if="!loading && articles.length === 0" title="暂无文章" description="快去发现精彩内容吧" />
    </template>

    <!-- 无限滚动触发器 -->
    <div ref="sentinelRef" class="h-1"></div>
  </div>
</template>

<script setup lang="ts">
/** 文章列表组件：支持无限滚动、骨架屏、平板双列布局 */
import type { Article } from '~/types'

const props = defineProps<{
  articles: Article[]
  loading: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  loadMore: []
}>()

// 无限滚动触发元素
const sentinelRef = ref<HTMLElement | null>(null)

// 使用 IntersectionObserver 实现无限滚动
onMounted(() => {
  if (!sentinelRef.value) return

  const observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !props.loading && props.hasMore) {
        emit('loadMore')
      }
    },
    { rootMargin: '200px' }
  )

  observer.observe(sentinelRef.value)

  onUnmounted(() => {
    observer.disconnect()
  })
})
</script>
