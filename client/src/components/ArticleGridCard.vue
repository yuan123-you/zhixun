<template>
  <!-- 网格卡片 - 圆角 + 渐变覆盖 + 悬停缩放 -->
  <RouterLink :to="to || `/articles/${article.id}`" class="grid-card">
    <!-- 有封面图 -->
    <template v-if="article.coverImage">
      <img :src="resolveUrl(article.coverImage) || ''" :alt="article.title" class="grid-card-img" loading="lazy" />
      <div class="grid-card-overlay" />
      <div class="grid-card-info">
        <h3 class="grid-card-title">{{ article.title }}</h3>
        <div class="grid-card-likes">
          <svg class="like-icon" :class="{ liked: article.isLiked }" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
          <span>{{ formatCount(article.likeCount) }}</span>
        </div>
      </div>
    </template>
    <!-- 无封面图：渐变纯色背景 -->
    <template v-else>
      <div class="grid-card-no-img" :style="{ backgroundColor: bgColor }">
        <div class="no-img-gradient" />
        <h3 class="grid-card-no-img-title">{{ article.title }}</h3>
        <div class="grid-card-no-img-likes">
          <svg class="like-icon-lg" :class="{ liked: article.isLiked }" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
          <span>{{ formatCount(article.likeCount) }}</span>
        </div>
      </div>
    </template>
  </RouterLink>
</template>

<script setup lang="ts">
import type { Article } from '@/types'
const props = defineProps<{ article: Article; to?: string }>()
const { resolveUrl } = useResourceUrl()

const palette = ['#4f46e5', '#7c3aed', '#a855f7', '#d946ef', '#ec4899', '#f43f5e', '#f97316', '#eab308', '#10b981', '#14b8a6', '#06b6d4', '#3b82f6', '#2563eb', '#6366f1', '#8b5cf6', '#db2777']
const bgColor = computed(() => palette[(props.article.id * 7) % palette.length])

const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}
</script>

<style scoped>
.grid-card {
  position: relative;
  display: block;
  overflow: hidden;
  border-radius: 0;
  cursor: pointer;
  aspect-ratio: 3 / 4;
  box-shadow: none;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow var(--zh-transition-base);
}
.grid-card:hover { transform: translateY(-4px) scale(1.01); box-shadow: none; }
.grid-card:active { transform: scale(0.97); }

.grid-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.grid-card:hover .grid-card-img { transform: scale(1.08); }

.grid-card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.15) 45%, transparent 100%);
  pointer-events: none;
}

.grid-card-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 10px 10px;
}

.grid-card-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255,255,255,0.92);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.grid-card-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255,255,255,0.65);
}
.like-icon { width: 14px; height: 14px; flex-shrink: 0; }
.like-icon.liked { fill: #f87171; color: #f87171; }

.grid-card-no-img {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px 12px;
  position: relative;
}
.no-img-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.5) 0%, rgba(0,0,0,0.1) 60%, transparent 100%);
}
.grid-card-no-img-title {
  position: relative;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}
.grid-card-no-img-likes {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.75);
}
.like-icon-lg { width: 16px; height: 16px; flex-shrink: 0; }
.like-icon-lg.liked { fill: #f87171; color: #f87171; }
</style>
