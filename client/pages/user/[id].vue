<template>
  <!-- 用户主页 -->
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 返回导航 -->
    <button class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-primary-400 transition-colors mb-4" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      {{ t('common.back') }}
    </button>

    <!-- 用户资料卡 -->
    <div class="card p-6 mb-6">
      <div class="flex items-start space-x-4">
        <!-- 头像 -->
        <div class="relative shrink-0">
          <UserAvatar :src="userInfo?.avatar" alt="头像" size="xl" />
          <!-- 在线状态指示灯 -->
          <span
            class="absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-white dark:border-gray-800"
            :class="onlineStatus ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
        </div>

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
              {{ userInfo?.isFollowing ? t('article.followed') : t('article.followBtn') }}
            </button>
          </div>
          <p v-if="userInfo?.bio" class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ userInfo?.bio }}</p>
          <div class="flex items-center space-x-6 mt-3 text-sm text-gray-500 dark:text-gray-400">
            <button class="hover:text-primary transition-colors" @click="activeTab = 'following'">
              <strong class="text-gray-900 dark:text-white">{{ userInfo?.followCount }}</strong> {{ t('user.following') }}
            </button>
            <button class="hover:text-primary transition-colors" @click="activeTab = 'followers'">
              <strong class="text-gray-900 dark:text-white">{{ userInfo?.followerCount }}</strong> {{ t('user.followers') }}
            </button>
            <span><strong class="text-gray-900 dark:text-white">{{ userInfo?.articleCount }}</strong> {{ t('article.articles') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 关注/粉丝 Tab 切换 -->
    <div class="card overflow-hidden">
      <div class="flex border-b border-gray-200 dark:border-gray-700">
        <button
          class="flex-1 py-3 text-sm font-medium text-center transition-colors relative"
          :class="activeTab === 'following' ? 'text-primary' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
          @click="activeTab = 'following'"
        >
          {{ t('user.following') }}
          <span v-if="activeTab === 'following'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-primary rounded-full"></span>
        </button>
        <button
          class="flex-1 py-3 text-sm font-medium text-center transition-colors relative"
          :class="activeTab === 'followers' ? 'text-primary' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
          @click="activeTab = 'followers'"
        >
          {{ t('user.followers') }}
          <span v-if="activeTab === 'followers'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-primary rounded-full"></span>
        </button>
      </div>

      <!-- 列表内容 -->
      <div class="p-4">
        <!-- 加载状态 -->
        <div v-if="listLoading" class="flex items-center justify-center py-8">
          <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 错误重试 -->
        <ErrorRetry v-else-if="listError" :message="listError" :on-retry="retryList" />

        <!-- 空状态 -->
        <div v-else-if="currentList.length === 0" class="text-center py-8 text-gray-400 dark:text-gray-500">
          <svg class="w-12 h-12 mx-auto mb-2 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p class="text-sm">{{ activeTab === 'following' ? t('user.noFollowing') : t('user.noFollowers') }}</p>
        </div>

        <!-- 用户列表 -->
        <div v-else class="space-y-3">
          <div
            v-for="user in currentList"
            :key="user.id"
            class="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
          >
            <div class="flex items-center space-x-3 min-w-0 flex-1">
              <!-- 头像 + 在线状态 -->
              <div class="relative shrink-0">
                <UserAvatar :src="user.avatar" :alt="user.nickname" size="md" />
                <span
                  class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white dark:border-gray-800"
                  :class="user.isOnline ? 'bg-green-500' : 'bg-gray-400'"
                ></span>
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex items-center space-x-2">
                  <NuxtLink
                    :to="`/user/${user.id}`"
                    class="text-sm font-medium text-gray-900 dark:text-white hover:text-primary transition-colors truncate"
                  >
                    {{ user.nickname }}
                  </NuxtLink>
                  <!-- 互关标识 -->
                  <span
                    v-if="user.isMutualFollow"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-2xs font-medium bg-primary-100 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 shrink-0"
                  >
                    互关
                  </span>
                </div>
                <p v-if="user.bio" class="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{{ user.bio }}</p>
              </div>
            </div>

            <!-- 关注/取关按钮 -->
            <button
              v-if="user.id !== userStore.userInfo?.id"
              class="btn text-xs shrink-0 ml-3"
              :class="user.isFollowing ? 'btn-secondary' : 'btn-primary'"
              :disabled="followLoading[user.id]"
              @click="toggleFollowUser(user)"
            >
              <span v-if="followLoading[user.id]" class="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block mr-1"></span>
              {{ user.isFollowing ? t('article.followed') : t('article.followBtn') }}
            </button>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="currentList.length > 0 && listHasMore" class="text-center py-3">
          <button
            class="text-sm text-primary hover:text-primary-600 transition-colors"
            @click="loadMoreList"
          >
            {{ t('common.loadMore') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Ta的文章列表 -->
    <div class="mt-6">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">{{ t('article.articles') }}</h3>
      <ArticleList :articles="articles" :loading="loading" :has-more="hasMore" :error="articlesError" @load-more="loadMore" @retry="retryArticles" />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 用户主页 */
import type { User, Article } from '~/types'
import type { FollowUser } from '~/api/social'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const userId = computed(() => Number(route.params.id))

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}

// 用户信息
const userInfo = ref<User | null>(null)
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)

// 关注/粉丝 Tab
const activeTab = ref<'following' | 'followers'>('following')

// 关注列表
const followingList = ref<FollowUser[]>([])
const followingPage = ref(1)
const followingHasMore = ref(true)

// 粉丝列表
const followersList = ref<FollowUser[]>([])
const followersPage = ref(1)
const followersHasMore = ref(true)

// 列表加载状态
const listLoading = ref(false)
const followLoading = ref<Record<number, boolean>>({})

// 在线状态
const onlineStatus = ref(false)

// 错误状态
const articlesError = ref<string | null>(null)
const listError = ref<string | null>(null)

// 请求缓存
const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

// 当前显示列表
const currentList = computed(() => activeTab.value === 'following' ? followingList.value : followersList.value)
const listHasMore = computed(() => activeTab.value === 'following' ? followingHasMore.value : followersHasMore.value)

// 获取用户信息
const { data: userData } = await useAsyncData(`user-${userId.value}`, async () => {
  const { userApi } = await import('~/api')
  const response = await cachedRequest(
    () => userApi.getProfile(userId.value),
    `/user/profile/${userId.value}`
  )
  return response.data.data
})

userInfo.value = userData.value || null

// 获取用户文章
const { data: articleData } = await useAsyncData(`user-articles-${userId.value}`, async () => {
  try {
    const { userApi } = await import('~/api')
    const response = await cachedRequest(
      () => userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 }),
      `/user/articles/${userId.value}`,
      { page: 1, pageSize: 20 }
    )
    return response.data.data
  } catch (error: any) {
    articlesError.value = error.message || t('article.loadArticleFailed')
    return null
  }
})

if (articleData.value) {
  articles.value = articleData.value.list || articleData.value.items || []
  hasMore.value = articles.value.length >= 20
}

// 获取在线状态
const fetchOnlineStatus = async () => {
  try {
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.getOnlineStatus(userId.value)
    onlineStatus.value = response.data.data?.[String(userId.value)] ?? false
  } catch {
    onlineStatus.value = false
  }
}

// 获取关注列表
const fetchFollowing = async (page: number = 1) => {
  try {
    listError.value = null
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.getFollowing(userId.value, { page, pageSize: 20 })
    const data = response.data.data
    const items = data?.items || data?.list || []
    if (page === 1) {
      followingList.value = items
    } else {
      followingList.value.push(...items)
    }
    followingHasMore.value = data?.hasMore ?? items.length >= 20
    followingPage.value = page
  } catch (error: any) {
    listError.value = error.message || t('user.loadDataFailed')
  }
}

// 获取粉丝列表
const fetchFollowers = async (page: number = 1) => {
  try {
    listError.value = null
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.getFollowers(userId.value, { page, pageSize: 20 })
    const data = response.data.data
    const items = data?.items || data?.list || []
    if (page === 1) {
      followersList.value = items
    } else {
      followersList.value.push(...items)
    }
    followersHasMore.value = data?.hasMore ?? items.length >= 20
    followersPage.value = page
  } catch (error: any) {
    listError.value = error.message || t('user.loadDataFailed')
  }
}

// 加载列表数据
const loadListData = async () => {
  listLoading.value = true
  try {
    if (activeTab.value === 'following') {
      if (followingList.value.length === 0) {
        await fetchFollowing(1)
      }
    } else {
      if (followersList.value.length === 0) {
        await fetchFollowers(1)
      }
    }
  } finally {
    listLoading.value = false
  }
}

// 加载更多
const loadMoreList = async () => {
  if (activeTab.value === 'following') {
    await fetchFollowing(followingPage.value + 1)
  } else {
    await fetchFollowers(followersPage.value + 1)
  }
}

// 监听Tab切换
watch(activeTab, () => {
  loadListData()
})

// 关注/取关（顶部按钮）
const toggleFollow = async () => {
  if (!userInfo.value) return
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  try {
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.toggleFollow(userId.value)
    const result = response.data.data
    userInfo.value.isFollowing = result.followed
    userInfo.value.followerCount = result.followerCount
    // 同步更新当前用户的关注数
    if (userStore.userInfo) {
      if (result.followed) {
        userStore.userInfo.followCount = (userStore.userInfo.followCount || 0) + 1
      } else {
        userStore.userInfo.followCount = Math.max(0, (userStore.userInfo.followCount || 0) - 1)
      }
    }
    // 刷新粉丝列表
    if (activeTab.value === 'followers') {
      await fetchFollowers(1)
    }
  } catch (error: any) {
    console.error(t('article.followFailed') + ':', error.message)
  }
}

// 列表内关注/取关
const toggleFollowUser = async (user: FollowUser) => {
  if (!userStore.isLoggedIn) {
    navigateTo('/login')
    return
  }
  followLoading.value[user.id] = true
  try {
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.toggleFollow(user.id)
    const result = response.data.data
    const isFollowing = result.followed
    user.isFollowing = isFollowing

    // 更新互关标识
    if (isFollowing && activeTab.value === 'followers') {
      // 关注了粉丝，变成互关
      user.isMutualFollow = true
    } else if (!isFollowing) {
      // 取关后取消互关
      user.isMutualFollow = false
    }

    // 同步更新另一个列表中同一用户的状态
    const otherList = activeTab.value === 'following' ? followersList.value : followingList.value
    const otherUser = otherList.find(u => u.id === user.id)
    if (otherUser) {
      otherUser.isFollowing = isFollowing
      // 如果在关注列表中取关了，互关也消失
      if (!isFollowing) {
        otherUser.isMutualFollow = false
      }
    }

    // 更新用户资料卡中的关注数
    if (userInfo.value) {
      if (isFollowing) {
        userInfo.value.followCount++
      } else {
        userInfo.value.followCount = Math.max(0, userInfo.value.followCount - 1)
      }
    }
  } catch (error: any) {
    console.error(t('article.followFailed') + ':', error.message)
  } finally {
    followLoading.value[user.id] = false
  }
}

// 加载更多文章
const loadMore = () => {
  // 加载更多文章
}

// 重试加载文章
const retryArticles = async () => {
  articlesError.value = null
  loading.value = true
  try {
    const { userApi } = await import('~/api')
    const response = await cachedRequest(
      () => userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 }),
      `/user/articles/${userId.value}`,
      { page: 1, pageSize: 20 }
    )
    const data = response.data.data
    articles.value = data?.list || data?.items || []
    hasMore.value = articles.value.length >= 20
  } catch (error: any) {
    articlesError.value = error.message || t('article.loadArticleFailed')
  } finally {
    loading.value = false
  }
}

// 重试加载关注/粉丝列表
const retryList = () => {
  listError.value = null
  loadListData()
}

// 初始化
onMounted(async () => {
  await Promise.all([
    fetchOnlineStatus(),
    loadListData(),
  ])
})

// 页面元信息
useHead({
  title: () => userInfo.value ? `${userInfo.value.nickname} - 知讯` : t('nav.profile') + ' - 知讯',
})
</script>
