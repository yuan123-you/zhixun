<template>
  <!-- 文章卡片组件 - 支持平板双列网格布局 -->
  <article class="card p-4 md:p-5 hover:shadow-md transition-shadow cursor-pointer no-tap-highlight touch-feedback" @click="navigateToDetail">
    <div class="flex gap-4">
      <!-- 文章内容区 -->
      <div class="flex-1 min-w-0">
        <!-- 标题 -->
        <h3 class="text-base md:text-lg font-semibold text-gray-900 dark:text-white line-clamp-2 mb-2">
          {{ article.title }}
        </h3>

        <!-- 摘要 -->
        <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">
          {{ article.summary }}
        </p>

        <!-- 底部信息 -->
        <div class="flex items-center text-xs text-gray-400 dark:text-gray-500 space-x-3 flex-nowrap overflow-hidden">
          <!-- 作者 -->
          <div class="flex items-center space-x-1 shrink-0 max-w-[120px]">
            <UserAvatar :src="article.authorAvatar || article.author?.avatar" :alt="article.authorName || article.author?.nickname" size="xs" />
            <span class="truncate">{{ article.authorName || article.author?.nickname }}</span>
          </div>

          <!-- 发布时间 -->
          <span class="shrink-0">{{ formatTime(article.createdAt) }}</span>

          <!-- 互动数据 -->
          <div class="flex items-center space-x-2 shrink-0">
            <span class="flex items-center space-x-0.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              <span>{{ formatCount(article.likeCount) }}</span>
            </span>
            <span class="flex items-center space-x-0.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span>{{ formatCount(article.commentCount) }}</span>
            </span>
            <span class="flex items-center space-x-0.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <span>{{ formatCount(article.viewCount) }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 封面图（右侧，sm以上显示） -->
      <div v-if="article.coverImage" class="hidden sm:block w-28 md:w-32 h-24 shrink-0">
        <img :src="article.coverImage" :alt="article.title" class="w-full h-full object-cover rounded-lg" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
/** 文章卡片组件 - 支持平板双列网格布局 */
import type { Article } from '~/types'

const props = defineProps<{
  article: Article
}>()

// 跳转到文章详情
const navigateToDetail = () => {
  navigateTo(`/articles/${props.article.id}`)
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 格式化数量
const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}
</script>
