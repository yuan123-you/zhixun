<template>
  <!-- 单条评论项组件（供 CommentSection 内部使用） -->
  <div class="flex space-x-3">
    <!-- 用户头像 -->
    <img :src="comment.user?.avatar || '/default-avatar.png'" :alt="comment.user?.nickname" class="w-8 h-8 rounded-full object-cover shrink-0" />

    <div class="flex-1 min-w-0">
      <!-- 用户名和内容 -->
      <div>
        <span class="text-sm font-medium text-gray-900 dark:text-white">{{ comment.user?.nickname }}</span>
        <p class="text-sm text-gray-700 dark:text-gray-300 mt-1">{{ comment.content }}</p>
      </div>

      <!-- 底部操作栏 -->
      <div class="flex items-center space-x-4 mt-2 text-xs text-gray-400 dark:text-gray-500">
        <span>{{ formatTime(comment.createdAt) }}</span>
        <button class="flex items-center space-x-1 hover:text-primary transition-colors" @click="$emit('like', comment.id)">
          <svg class="w-3.5 h-3.5" :class="comment.isLiked ? 'text-primary fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span>{{ comment.likeCount }}</span>
        </button>
        <button class="hover:text-primary transition-colors" @click="$emit('reply', comment)">
          回复
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 单条评论项组件 */
import type { Comment } from '~/types'

defineProps<{
  comment: Comment
}>()

defineEmits<{
  reply: [comment: Comment]
  like: [commentId: number]
}>()

// 格式化时间
const formatTime = (time: string) => {
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
</script>
