<template>
  <!-- 用户主页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 用户资料卡 -->
    <div class="card p-3 mb-3">
      <div class="flex items-start space-x-3">
        <!-- 头像 -->
        <div class="relative shrink-0">
          <UserAvatar :src="userInfo?.avatar" alt="头像" size="lg" />
          <!-- 在线状态指示灯 -->
          <span
            class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white"
            :class="onlineStatus ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-bold text-slate-900">{{ userInfo?.nickname }}</h2>
              <div class="flex flex-wrap items-center gap-1.5 mt-1">
                <span class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
                  ID: {{ userInfo?.uid }}
                </span>
                <span v-if="userInfo?.showGenderOnProfile && userInfo?.gender" class="text-xs text-slate-400 bg-pink-50 px-2 py-0.5 rounded">
                  {{ userInfo.gender === 1 ? '男' : userInfo.gender === 2 ? '女' : '' }}
                </span>
                <span v-if="userInfo?.province" class="text-xs text-slate-400 bg-blue-50 px-2 py-0.5 rounded">
                  {{ userInfo?.province }}
                </span>
                <span v-if="userInfo?.ipLocation" class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
                  IP属地: {{ userInfo?.ipLocation }}
                </span>
              </div>
            </div>
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
          <p v-if="userInfo?.bio" class="text-sm text-slate-500 mt-1">{{ userInfo?.bio }}</p>
          <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
            <button class="hover:text-primary transition-colors" @click="activeTab = 'following'">
              <strong class="text-slate-900">{{ userInfo?.followCount }}</strong> {{ '关注' }}
            </button>
            <button class="hover:text-primary transition-colors" @click="activeTab = 'followers'">
              <strong class="text-slate-900">{{ userInfo?.followerCount }}</strong> {{ '粉丝' }}
            </button>
            <span><strong class="text-slate-900">{{ userInfo?.articleCount }}</strong> {{ '作品' }}</span>
            <!-- 互关标识：仅自己查看自己主页时显示，点击跳转互相关注列表 -->
            <span
              v-if="isOwnProfile"
              class="inline-flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700 transition-colors cursor-pointer"
              @click="activeTab = 'mutual'"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              互关
            </span>
            <!-- 获赞总数 -->
            <span class="inline-flex items-center gap-1">
              <svg class="w-3.5 h-3.5 text-rose-400" fill="currentColor" viewBox="0 0 24 24">
                <path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" />
              </svg>
              <strong class="text-slate-900">{{ formatNumber(userInfo?.totalLikeCount ?? 0) }}</strong> {{ '获赞' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 关注/粉丝/互关 Tab 切换 -->
    <div class="card overflow-hidden">
      <div class="flex border-b border-slate-200">
        <!-- 互相关注 Tab（仅自己可见） -->
        <button
          v-if="isOwnProfile"
          class="flex-1 py-3 text-sm font-medium text-center transition-colors relative"
          :class="activeTab === 'mutual' ? 'text-primary' : 'text-slate-500 hover:text-slate-700'"
          @click="activeTab = 'mutual'"
        >
          {{ '互相关注' }}
          <span v-if="activeTab === 'mutual'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-primary rounded-full"></span>
        </button>
        <button
          class="flex-1 py-3 text-sm font-medium text-center transition-colors relative"
          :class="activeTab === 'following' ? 'text-primary' : 'text-slate-500 hover:text-slate-700'"
          @click="activeTab = 'following'"
        >
          {{ '关注' }}
          <span v-if="activeTab === 'following'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-primary rounded-full"></span>
        </button>
        <button
          class="flex-1 py-3 text-sm font-medium text-center transition-colors relative"
          :class="activeTab === 'followers' ? 'text-primary' : 'text-slate-500 hover:text-slate-700'"
          @click="activeTab = 'followers'"
        >
          {{ '粉丝' }}
          <span v-if="activeTab === 'followers'" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-primary rounded-full"></span>
        </button>
      </div>

      <!-- 列表内容 -->
      <div class="p-2">
        <!-- 加载状态 -->
        <div v-if="listLoading" class="flex items-center justify-center py-8">
          <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 错误重试 -->
        <ErrorRetry v-else-if="listError" :message="listError" :on-retry="retryList" />

        <!-- 空状态 -->
        <div v-else-if="currentList.length === 0" class="text-center py-8 text-slate-400">
          <svg class="w-12 h-12 mx-auto mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p class="text-sm">{{ activeTab === 'following' ? '暂无关注用户' : activeTab === 'mutual' ? '暂无互相关注的用户' : '暂无粉丝' }}</p>
        </div>

        <!-- 用户列表 -->
        <div v-else class="space-y-2">
          <div
            v-for="user in currentList"
            :key="user.id"
            class="flex items-center justify-between w-full p-2 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <div class="flex items-center space-x-2 min-w-0 flex-1">
              <!-- 头像 + 在线状态 -->
              <div class="relative shrink-0">
                <UserAvatar :src="user.avatar" :alt="user.nickname" size="md" />
                <span
                  class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white"
                  :class="user.isOnline ? 'bg-green-500' : 'bg-gray-400'"
                ></span>
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex items-center space-x-2">
                  <NuxtLink
                    :to="`/user/${user.id}`"
                    class="text-sm font-medium text-slate-900 hover:text-primary transition-colors truncate"
                  >
                    {{ user.nickname }}
                  </NuxtLink>
                  <!-- 互关标识 -->
                  <span
                    v-if="user.isMutualFollow"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-2xs font-medium bg-primary-100 text-primary-700 shrink-0"
                  >
                    互关
                  </span>
                </div>
                <p v-if="user.bio" class="text-xs text-slate-500 truncate mt-0.5">{{ user.bio }}</p>
              </div>
            </div>

            <!-- 关注/取关按钮 -->
            <button
              v-if="user.id !== userStore.userInfo?.id"
              class="btn text-xs shrink-0 ml-3"
              :class="activeTab === 'mutual' && user.isFollowing ? 'btn-secondary' : user.isFollowing ? 'btn-secondary' : 'btn-primary'"
              :disabled="followLoading[user.id]"
              @click="activeTab === 'mutual' && user.isFollowing ? openUnfollowConfirm(user) : toggleFollowUser(user)"
            >
              <span v-if="followLoading[user.id]" class="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block mr-1"></span>
              {{ activeTab === 'mutual' && user.isFollowing ? '互相关注' : user.isFollowing ? '已关注' : '关注' }}
            </button>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="currentList.length > 0 && listHasMore" class="text-center py-3">
          <button
            class="text-sm text-primary hover:text-primary-600 transition-colors"
            @click="loadMoreList"
          >
            {{ '加载更多' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Ta的作品列表 -->
    <div>
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

    <!-- 取消关注确认弹框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="unfollowConfirm.show"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
          @click.self="unfollowConfirm.show = false"
        >
          <div class="bg-white rounded-xl shadow-2xl p-6 w-[360px] max-w-[90vw]">
            <h3 class="text-base font-semibold text-slate-900 mb-2">取消关注</h3>
            <p class="text-sm text-slate-600 mb-6">
              确定要取消关注 <span class="font-medium text-slate-900">{{ unfollowConfirm.user?.nickname }}</span> 吗？
            </p>
            <div class="flex justify-end gap-3">
              <button
                class="px-4 py-2 text-sm text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition-colors"
                @click="unfollowConfirm.show = false"
              >
                取消
              </button>
              <button
                class="px-4 py-2 text-sm text-white bg-red-500 rounded-lg hover:bg-red-600 transition-colors"
                :disabled="unfollowConfirm.loading"
                @click="confirmUnfollow"
              >
                <span v-if="unfollowConfirm.loading" class="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin inline-block mr-1"></span>
                确认取消关注
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 他人用户主页（信息 + 作品列表） */
import type { User, Article } from '~/types'
import type { FollowUser } from '~/api/social'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => Number(route.params.id))

/** 无效用户ID保护 */
if (isNaN(userId.value)) {
  throw createError({ statusCode: 404, message: '用户不存在' })
}

/** 是否为自己的主页 */
const isOwnProfile = computed(() => {
  if (!import.meta.client) return false
  return userStore.userInfo?.id === userId.value
})

/** 格式化大数字（如 12345 → 1.2万） */
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  }
  return num.toLocaleString()
}

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

// 关注/粉丝/互关 Tab
const activeTab = ref<'mutual' | 'following' | 'followers'>('following')

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
const currentList = computed(() => {
  if (activeTab.value === 'mutual') {
    return followingList.value.filter(u => u.isMutualFollow)
  }
  return activeTab.value === 'following' ? followingList.value : followersList.value
})
const listHasMore = computed(() => activeTab.value === 'following' ? followingHasMore.value : followersHasMore.value)

// 获取用户信息
const { data: userData, error: userError } = await useAsyncData(`user-${userId.value}`, async () => {
  const { userApi } = await import('~/api')
  const response = await cachedRequest(
    () => userApi.getProfile(userId.value),
    `/user/profile/${userId.value}`
  )
  return response.data.data
})

// 如果用户不存在，抛出 Nuxt 错误页面（404）
if (userError.value || !userData.value) {
  throw createError({ statusCode: 404, message: '用户不存在' })
}

userInfo.value = userData.value

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
    listError.value = error.message || '数据加载失败，请稍后重试'
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
    listError.value = error.message || '数据加载失败，请稍后重试'
  }
}

// 加载列表数据
const loadListData = async () => {
  listLoading.value = true
  try {
    if (activeTab.value === 'mutual' || activeTab.value === 'following') {
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
  if (activeTab.value === 'mutual' || activeTab.value === 'following') {
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
    showToast('关注操作失败', 'error')
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
    showToast('关注操作失败', 'error')
  } finally {
    followLoading.value[user.id] = false
  }
}

// ========== 取消关注确认弹框 ==========
const unfollowConfirm = reactive({
  show: false,
  loading: false,
  user: null as FollowUser | null,
})

/** 打开取消关注确认弹框 */
const openUnfollowConfirm = (user: FollowUser) => {
  unfollowConfirm.user = user
  unfollowConfirm.show = true
}

/** 确认取消关注 */
const confirmUnfollow = async () => {
  const user = unfollowConfirm.user
  if (!user) return
  unfollowConfirm.loading = true
  try {
    const { socialApi } = await import('~/api/social')
    const response = await socialApi.toggleFollow(user.id)
    const result = response.data.data
    user.isFollowing = result.followed
    user.isMutualFollow = false

    // 同步更新粉丝列表中的同一用户
    const otherUser = followersList.value.find(u => u.id === user.id)
    if (otherUser) {
      otherUser.isFollowing = result.followed
      otherUser.isMutualFollow = false
    }

    // 更新用户资料卡中的关注数
    if (userInfo.value) {
      userInfo.value.followCount = Math.max(0, userInfo.value.followCount - 1)
    }

    showToast(`已取消关注 ${user.nickname}`)
  } catch {
    showToast('操作失败，请稍后重试', 'error')
  } finally {
    unfollowConfirm.loading = false
    unfollowConfirm.show = false
    unfollowConfirm.user = null
  }
}

// 文章分页
const articlePage = ref(1)
const articlePageSize = 20

// 加载更多文章
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
    articlesError.value = error.message || '作品加载失败，请稍后重试'
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
  title: () => userInfo.value ? `${userInfo.value.nickname}的主页 - 知讯` : '个人中心' + ' - 知讯',
})
</script>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
