<template>
  <!-- 作品列表组件：支持无限滚动、骨架屏、平板双列布局、错误重试 -->
  <div>
    <!-- 骨架屏加载状态 -->
    <template v-if="loading && articles.length === 0">
      <div>
        <template v-for="i in 6" :key="i">
          <hr v-if="i > 1" class="border-slate-200 dark:border-gray-700" />
          <LoadingSkeleton type="article" />
        </template>
      </div>
    </template>

    <!-- 加载失败（首次加载） -->
    <template v-else-if="error && articles.length === 0">
      <ErrorRetry :message="error" :on-retry="handleRetry" />
    </template>

    <!-- 作品列表 -->
    <template v-else>
      <!-- 单列全宽，使用 <hr> 分隔 -->
      <div>
        <template v-for="(article, index) in articles" :key="article.id">
          <hr v-if="index > 0" class="border-slate-200 dark:border-gray-700" />
          <ArticleCard :article="article" />
        </template>
      </div>

      <!-- 加载更多骨架屏 -->
      <div v-if="loading && articles.length > 0">
        <hr class="border-slate-200 dark:border-gray-700" />
        <LoadingSkeleton v-for="i in 2" :key="'more-' + i" type="article" />
      </div>

      <!-- 加载更多失败 -->
      <div v-if="error && articles.length > 0" class="mt-2.5">
        <ErrorRetry :message="error" :on-retry="handleRetry" />
      </div>

      <!-- 没有更多数据 -->
      <div v-if="!hasMore && !error && articles.length > 0" class="text-center text-sm text-slate-400 py-2">
        没有更多了
      </div>

      <!-- 空状态 -->
      <EmptyState v-if="!loading && !error && articles.length === 0" title="暂无作品" description="快去发现精彩内容吧" />
    </template>

    <!-- 无限滚动触发器 -->
    <div ref="sentinelRef" class="h-1"></div>
  </div>
</template>

<script setup lang="ts">
/** 作品列表组件：支持无限滚动、骨架屏、平板双列布局、错误重试 */
import type { Article } from '~/types'

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

// 无限滚动触发元素
const sentinelRef = ref<HTMLElement | null>(null)

// 重试处理
const handleRetry = () => {
  emit('retry')
}

// 使用 IntersectionObserver 实现无限滚动
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

  onUnmounted(() => {
    observer.disconnect()
  })
})
</script>
