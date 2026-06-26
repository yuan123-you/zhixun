<template>
  <!-- 作品网格卡片：无内边距/间隔的 3 列 grid 布局用 -->
  <NuxtLink
    :to="`/articles/${article.id}`"
    class="relative block overflow-hidden group cursor-pointer"
    style="aspect-ratio: 3/4"
  >
    <!-- 有封面图 -->
    <template v-if="article.coverImage">
      <img
        :src="resolveUrl(article.coverImage) || ''"
        :alt="article.title"
        class="w-full h-full object-cover"
        loading="lazy"
      />
      <!-- 底部渐变遮罩 -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />
      <!-- 底部信息区：标题（半透明）+ 点赞 -->
      <div class="absolute bottom-0 left-0 right-0 p-1.5">
        <h3 class="text-[11px] text-white/70 line-clamp-2 leading-snug">{{ article.title }}</h3>
        <div class="flex items-center gap-1 mt-0.5">
          <svg
            class="w-3 h-3 shrink-0"
            :class="article.isLiked ? 'fill-red-400 text-red-400' : 'text-white/60'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span class="text-[11px] text-white/60">{{ formatCount(article.likeCount) }}</span>
        </div>
      </div>
    </template>

    <!-- 无封面图：纯色背景 + 不透明标题和点赞 -->
    <template v-else>
      <div class="w-full h-full flex flex-col justify-end p-2" :style="{ backgroundColor: bgColor }">
        <h3 class="text-xs text-white font-medium line-clamp-3 leading-snug">{{ article.title }}</h3>
        <div class="flex items-center gap-1 mt-1">
          <svg
            class="w-3.5 h-3.5 shrink-0"
            :class="article.isLiked ? 'fill-red-400 text-red-400' : 'text-white/80'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span class="text-xs text-white/80">{{ formatCount(article.likeCount) }}</span>
        </div>
      </div>
    </template>
  </NuxtLink>
</template>

<script setup lang="ts">
/** 作品网格卡片：封面图/纯色背景 + 标题 + 点赞信息 */
import type { Article } from '~/types'

const props = defineProps<{
  article: Article
}>()

const { resolveUrl } = useResourceUrl()

// 无图时从 ID 取色，保证同一作品颜色稳定
const palette = [
  '#6366f1', '#8b5cf6', '#a855f7', '#d946ef',
  '#ec4899', '#f43f5e', '#f97316', '#eab308',
  '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6',
  '#2563eb', '#4f46e5', '#7c3aed', '#db2777',
]
const bgColor = computed(() => palette[(props.article.id * 7) % palette.length])

// 格式化数量
const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}
</script>
