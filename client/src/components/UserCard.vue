<template>
  <!-- 用户卡片组件（Element Plus 风格） -->
  <el-card shadow="never" class="user-card">
    <div class="user-card-body">
      <RouterLink v-if="user.id" :to="`/user/${user.id}`" class="user-avatar-link">
        <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
      </RouterLink>
      <div v-else class="user-avatar-placeholder">
        <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
      </div>

      <div class="user-info">
        <RouterLink v-if="user.id" :to="`/user/${user.id}`" class="user-name hover">
          {{ user.nickname }}
        </RouterLink>
        <span v-else class="user-name">{{ user.nickname }}</span>
        <p v-if="user.bio" class="user-bio">{{ user.bio }}</p>
        <div class="user-stats">
          <span>{{ user.articleCount }} 作品</span>
          <span>{{ user.followerCount }} 粉丝</span>
        </div>
      </div>

      <el-button
        v-if="showFollowButton"
        :type="user.isFollowing ? 'default' : 'primary'"
        size="small"
        class="follow-btn"
        @click="$emit('toggleFollow', user.id)"
      >
        {{ user.isFollowing ? '已关注' : '关注' }}
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
/** 用户卡片组件，基于 Element Plus el-card */
import type { User } from '@/types'

defineProps<{
  user: User
  showFollowButton?: boolean
}>()

defineEmits<{
  toggleFollow: [userId: number]
}>()
</script>

<style scoped>
.user-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}
.user-card-body {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-avatar-link,
.user-avatar-placeholder {
  flex-shrink: 0;
}
.user-info {
  flex: 1;
  min-width: 0;
}
.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  text-decoration: none;
}
.user-name.hover:hover {
  color: var(--el-color-primary);
}
.user-bio {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.follow-btn {
  flex-shrink: 0;
}
</style>
