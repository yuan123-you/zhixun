<template>
  <!-- 用户卡片组件 -->
  <div class="card p-4 flex items-center space-x-3">
    <!-- 用户头像 -->
    <NuxtLink :to="`/user/${user.id}`">
      <img :src="user.avatar || '/default-avatar.png'" :alt="user.nickname" class="w-12 h-12 rounded-full object-cover" />
    </NuxtLink>

    <!-- 用户信息 -->
    <div class="flex-1 min-w-0">
      <NuxtLink :to="`/user/${user.id}`" class="text-sm font-semibold text-gray-900 dark:text-white hover:text-primary transition-colors">
        {{ user.nickname }}
      </NuxtLink>
      <p v-if="user.bio" class="text-xs text-gray-500 dark:text-gray-400 line-clamp-1 mt-0.5">{{ user.bio }}</p>
      <div class="flex items-center space-x-3 text-xs text-gray-400 dark:text-gray-500 mt-1">
        <span>{{ user.articleCount }} 文章</span>
        <span>{{ user.followerCount }} 粉丝</span>
      </div>
    </div>

    <!-- 关注按钮 -->
    <button
      v-if="showFollowButton"
      class="btn text-sm shrink-0"
      :class="user.isFollowing ? 'btn-secondary' : 'btn-primary'"
      @click="$emit('toggleFollow', user.id)"
    >
      {{ user.isFollowing ? '已关注' : '关注' }}
    </button>
  </div>
</template>

<script setup lang="ts">
/** 用户卡片组件 */
import type { User } from '~/types'

defineProps<{
  user: User
  showFollowButton?: boolean
}>()

defineEmits<{
  toggleFollow: [userId: number]
}>()
</script>
