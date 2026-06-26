<template>
  <!-- 用户主页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 返回导航 -->
    <button class="flex items-center gap-1 text-sm text-slate-500 hover:text-primary-600 transition-colors mb-2" @click="goBack">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      {{ '返回' }}
    </button>

    <!-- 用户资料卡 -->
    <div v-if="userInfo" class="card p-3 mb-3">
      <div class="flex items-start space-x-3">
        <!-- 头像 -->
        <div class="relative shrink-0">
          <UserAvatar :src="userInfo.avatar" alt="头像" size="xl" />
          <!-- 在线状态指示灯 -->
          <span
            class="absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-white"
            :class="onlineStatus ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-xl font-bold text-slate-900">{{ userInfo.nickname }}</h2>
              <div class="flex flex-wrap items-center gap-1.5 mt-1">
                <span class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
                  ID: {{ userInfo.uid }}
                </span>
                <span v-if="userInfo.showGenderOnProfile && userInfo.gender" class="text-xs text-slate-400 bg-pink-50 px-2 py-0.5 rounded">
                  {{ userInfo.gender === 1 ? '男' : userInfo.gender === 2 ? '女' : '' }}
                </span>
                <span v-if="userInfo.province" class="text-xs text-slate-400 bg-blue-50 px-2 py-0.5 rounded">
                  {{ userInfo.province }}
                </span>
                <span v-if="userInfo.ipLocation" class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
                  IP属地: {{ userInfo.ipLocation }}
                </span>
              </div>
            </div>
            <!-- 关注按钮（非自己时显示） -->
            <button
              v-if="!isOwnProfile"
              class="btn text-sm"
              :class="userInfo.isFollowing ? 'btn-secondary' : 'btn-primary'"
              @click="toggleFollow"
            >
              {{ userInfo.isFollowing ? '已关注' : '关注' }}
            </button>
          </div>
          <p v-if="userInfo.bio" class="text-sm text-slate-500 mt-1">{{ userInfo.bio }}</p>
          <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
            <NuxtLink :to="`/user/${userId}/following`" class="hover:text-primary transition-colors">
              <strong class="text-slate-900">{{ userInfo.followCount }}</strong> {{ '关注' }}
            </NuxtLink>
            <NuxtLink :to="`/user/${userId}/followers`" class="hover:text-primary transition-colors">
              <strong class="text-slate-900">{{ userInfo.followerCount }}</strong> {{ '粉丝' }}
            </NuxtLink>
            <span><strong class="text-slate-900">{{ userInfo.articleCount }}</strong> {{ '作品' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户信息加载失败状态 -->
    <div v-else-if="profileError" class="card p-6 mb-3 text-center">
      <svg class="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
      </svg>
      <p class="text-slate-500 text-sm">{{ profileError }}</p>
      <button class="btn-primary text-sm mt-3 px-4 py-2" @click="retryProfile">重新加载</button>
    </div>

    <!-- 用户信息加载中骨架屏 -->
    <div v-else class="card p-3 mb-3 animate-pulse">
      <div class="flex items-start space-x-3">
        <div class="w-16 h-16 bg-slate-200 rounded-full shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="h-5 bg-slate-200 rounded w-1/3"></div>
          <div class="h-3 bg-slate-200 rounded w-1/4"></div>
          <div class="h-3 bg-slate-200 rounded w-2/3"></div>
        </div>
      </div>
    </div>

    <!-- Ta的作品列表 -->
    <div v-if="userInfo" class="mt-3">
      <h3 class="text-lg font-semibold text-slate-900 mb-2">{{ isOwnProfile ? '我的作品' : '作品' }}</h3>
      <!-- 骨架屏 -->
      <div v-if="loading && articles.length === 0" class="grid grid-cols-3">
        <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
      </div>
      <!-- 错误状态 -->
      <ErrorRetry v-else-if="articlesError && !articles.length" :message="articlesError" :on-retry="retryArticles" />
      <!-- 空状态 -->
      <div v-else-if="!loading && articles.length === 0" class="text-center py-10 text-slate-400">
        <p class="text-lg">暂无作品</p>
      </div>
      <!-- 三列网格 -->
      <div v-else class="grid grid-cols-3">
        <ArticleGridCard v-for="article in articles" :key="article.id" :article="article" />
      </div>
      <!-- 加载更多 -->
      <div v-if="hasMore" class="text-center py-4">
        <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 用户主页（信息 + 作品列表） */
import type { User, Article } from '~/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => Number(route.params.id))

/** 无效用户ID保护 — 显示友好提示而非直接抛错 */
const isValidUserId = computed(() => !isNaN(userId.value) && userId.value > 0)

if (!isValidUserId.value) {
  throw createError({ statusCode: 404, message: '用户不存在', fatal: false })
}

/** 是否为自己的主页 */
const isOwnProfile = computed(() => {
  if (!import.meta.client) return false
  return userStore.userInfo?.id === userId.value
})

// Toast 提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  if (!import.meta.client) return
  const toast = document.createElement('div')
  toast.className = `fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-white text-sm transition-all duration-300 transform translate-x-0 opacity-100 ${
    type === 'success' ? 'bg-green-500' : 'bg-red-500'
  }`
  toast.textContent = message
  document.body.appendChild(toast)
  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateX(100%)'
    setTimeout(() => toast.remove(), 300)
  }, 2000)
}

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
const onlineStatus = ref(false)
const articlesError = ref<string | null>(null)
const profileError = ref<string | null>(null)

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

// 获取用户信息
const { data: userData, error: userError } = await useAsyncData(`user-info-${userId.value}`, async () => {
  const { userApi } = await import('~/api')
  const response = await cachedRequest(
    () => userApi.getProfile(userId.value),
    `/user/profile/${userId.value}`
  )
  return response.data.data
})

if (userError.value) {
  profileError.value = userError.value.message || '用户信息加载失败'
} else if (!userData.value) {
  profileError.value = '用户不存在或已被删除'
}

if (userData.value) {
  userInfo.value = userData.value
}

// 获取用户作品（仅当用户信息加载成功后）
const { data: articleData } = await useAsyncData(`user-works-${userId.value}`, async () => {
  if (!userData.value) return null
  try {
    const { userApi } = await import('~/api')
    const response = await cachedRequest(
      () => userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 }),
      `/user/articles/${userId.value}`,
      { page: 1, pageSize: 20 }
    )
    return response.data.data
  } catch (error: any) {
    articlesError.value = error.message || '作品加载失败，请稍后重试'
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

// 关注/取关
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
    if (userStore.userInfo) {
      userStore.userInfo.followCount = result.followed
        ? (userStore.userInfo.followCount || 0) + 1
        : Math.max(0, (userStore.userInfo.followCount || 0) - 1)
    }
  } catch {
    showToast('关注操作失败', 'error')
  }
}

// 作品分页
const articlePage = ref(1)
const articlePageSize = 20

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  articlePage.value++
  loading.value = true
  try {
    const { userApi } = await import('~/api')
    const response = await userApi.getUserArticles(userId.value, { page: articlePage.value, pageSize: articlePageSize })
    const data = response.data.data
    const items = data?.list || data?.items || []
    articles.value.push(...items)
    hasMore.value = items.length >= articlePageSize
  } catch {
    articlePage.value--
  } finally {
    loading.value = false
  }
}

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
    articlesError.value = error.message || '作品加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 重新加载用户信息 */
const retryProfile = async () => {
  profileError.value = null
  try {
    const { userApi } = await import('~/api')
    const response = await cachedRequest(
      () => userApi.getProfile(userId.value),
      `/user/profile/${userId.value}`
    )
    if (response.data.data) {
      userInfo.value = response.data.data
      profileError.value = null
    } else {
      profileError.value = '用户不存在或已被删除'
    }
  } catch (error: any) {
    profileError.value = error.message || '用户信息加载失败'
  }
}

onMounted(() => {
  fetchOnlineStatus()
})

useHead({
  title: () => userInfo.value ? `${userInfo.value.nickname} - 知讯` : '个人中心 - 知讯',
})
</script>
