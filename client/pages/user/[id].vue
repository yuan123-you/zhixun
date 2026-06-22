<template>
  <!-- 用户主页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 用户资料卡 -->
    <div class="card p-6 mb-6">
      <div class="flex items-start space-x-4">
        <!-- 头像 -->
        <img :src="userInfo?.avatar || '/default-avatar.png'" class="w-20 h-20 rounded-full object-cover shrink-0" alt="头像" />

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-bold text-gray-900 dark:text-white">{{ userInfo?.nickname }}</h2>
            <!-- 关注按钮 -->
            <button
              v-if="userStore.userInfo?.id !== userId"
              class="btn text-sm"
              :class="userInfo?.isFollowing ? 'btn-secondary' : 'btn-primary'"
              @click="toggleFollow"
            >
              {{ userInfo?.isFollowing ? '已关注' : '关注' }}
            </button>
          </div>
          <p v-if="userInfo?.bio" class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ userInfo?.bio }}</p>
          <div class="flex items-center space-x-6 mt-3 text-sm text-gray-500 dark:text-gray-400">
            <NuxtLink :to="`/user/${userId}/following`" class="hover:text-primary transition-colors">
              <strong class="text-gray-900 dark:text-white">{{ userInfo?.followCount }}</strong> 关注
            </NuxtLink>
            <NuxtLink :to="`/user/${userId}/followers`" class="hover:text-primary transition-colors">
              <strong class="text-gray-900 dark:text-white">{{ userInfo?.followerCount }}</strong> 粉丝
            </NuxtLink>
            <span><strong class="text-gray-900 dark:text-white">{{ userInfo?.articleCount }}</strong> 文章</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Ta的文章列表 -->
    <div>
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Ta的文章</h3>
      <ArticleList :articles="articles" :loading="loading" :has-more="hasMore" @load-more="loadMore" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 用户主页 */
import type { User, Article } from '~/types'

const route = useRoute()
const userStore = useUserStore()
const userId = computed(() => Number(route.params.id))

// 用户信息
const userInfo = ref<User | null>(null)
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)

// 获取用户信息
const { data: userData } = await useAsyncData(`user-${userId.value}`, async () => {
  const { userApi } = await import('~/api')
  const response = await userApi.getProfile(userId.value)
  return response.data.data
})

userInfo.value = userData.value || null

// 获取用户文章
const { data: articleData } = await useAsyncData(`user-articles-${userId.value}`, async () => {
  const { userApi } = await import('~/api')
  const response = await userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 })
  return response.data.data
})

if (articleData.value) {
  articles.value = articleData.value.items
  hasMore.value = articleData.value.hasMore
}

// 关注/取关
const toggleFollow = async () => {
  if (!userInfo.value) return
  const { socialApi } = await import('~/api')
  const response = await socialApi.toggleFollow(userId.value)
  userInfo.value.isFollowing = response.data.data.isFollowing
  userInfo.value.followerCount = response.data.data.followerCount
}

// 加载更多
const loadMore = () => {
  // 加载更多文章
}

// 页面元信息
useHead({
  title: () => userInfo.value ? `${userInfo.value.nickname}的主页 - 知讯` : '用户主页 - 知讯',
})
</script>
