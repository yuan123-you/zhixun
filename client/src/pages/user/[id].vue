<template>
  <!-- 用户主页 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-0.5 md:px-1.5 2xl:px-2 py-1.5">

    <!-- 用户资料卡 — 抖音风格居中布局 -->
    <div v-if="userInfo" class="pt-3 pb-2 px-3">
      <div class="flex flex-col items-center">
        <!-- 头像 -->
        <div class="relative shrink-0 mb-2">
          <UserAvatar :src="userInfo.avatar" alt="头像" size="xl" />
          <!-- 在线状态指示灯 -->
          <span
            class="absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-white"
            :class="onlineStatus ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
        </div>

        <!-- 昵称 -->
        <h2 class="text-base md:text-lg font-bold text-[var(--zh-text)] mb-0.5">{{ userInfo.nickname }}</h2>

        <!-- ID 和标签 -->
        <div class="flex flex-wrap items-center justify-center gap-1 mb-1.5">
          <span class="text-[11px] text-[var(--zh-text-tertiary)]">知讯号: {{ userInfo.uid }}</span>
          <span v-if="userInfo.showGenderOnProfile && userInfo.gender" class="text-[11px] text-[var(--zh-text-tertiary)]">
            · {{ userInfo.gender === 1 ? '男' : userInfo.gender === 2 ? '女' : '' }}
          </span>
          <span v-if="userInfo.province" class="text-[11px] text-[var(--zh-text-tertiary)]">· {{ userInfo.province }}</span>
          <span v-if="userInfo.ipLocation" class="text-[11px] text-[var(--zh-text-tertiary)]" :title="`根据 IP 自动定位：${userInfo.ipLocation}`">· IP属地：{{ userInfo.ipLocation }}</span>
        </div>

        <!-- 简介 -->
        <p v-if="userInfo.bio" class="text-[11px] text-[var(--zh-text-secondary)] text-center mb-2 max-w-[280px] line-clamp-2">{{ userInfo.bio }}</p>

        <!-- 统计数据行 -->
        <div class="flex items-center justify-center gap-6 md:gap-8 mb-3">
          <RouterLink :to="`/user/${userId}`" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-[var(--zh-text)]">{{ userInfo.articleCount ?? 0 }}</span>
            <span class="text-[10px] text-[var(--zh-text-tertiary)]">作品</span>
          </RouterLink>
          <div class="flex flex-col items-center">
            <span class="text-base md:text-lg font-bold text-[var(--zh-text)]">{{ userInfo.totalLikeCount ?? 0 }}</span>
            <span class="text-[10px] text-[var(--zh-text-tertiary)]">获赞</span>
          </div>
          <RouterLink :to="`/user/${userId}/following`" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-[var(--zh-text)]">{{ userInfo.followCount ?? 0 }}</span>
            <span class="text-[10px] text-[var(--zh-text-tertiary)]">关注</span>
          </RouterLink>
          <RouterLink :to="`/user/${userId}/followers`" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-[var(--zh-text)]">{{ userInfo.followerCount ?? 0 }}</span>
            <span class="text-[10px] text-[var(--zh-text-tertiary)]">粉丝</span>
          </RouterLink>
        </div>

        <!-- 关注 & 私信按钮（非自己时显示） -->
        <div v-if="!isOwnProfile" class="flex items-center justify-center gap-2">
          <button
            class="text-center py-1.5 px-6 text-[13px] font-medium text-white rounded-md transition-colors"
            :class="userInfo.isFollowing ? 'bg-[var(--zh-border)] text-[var(--zh-text-secondary)] hover:bg-slate-300' : 'bg-[var(--color-primary)] hover:bg-[var(--color-primary-hover)]'"
            @click="toggleFollow"
          >
            {{ userInfo.isFollowing ? '已关注' : '+ 关注' }}
          </button>
          <button
            v-if="userInfo.isMutualFollow"
            class="text-center py-1.5 px-6 text-[13px] font-medium text-white bg-[var(--color-primary)] hover:bg-[var(--color-primary-hover)] rounded-md transition-colors"
            @click="startChat"
          >
            私信
          </button>
        </div>
      </div>
    </div>

    <!-- 用户信息加载失败状态 -->
    <div v-else-if="profileError" class="pt-10 pb-6 px-3 text-center">
      <svg class="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
      </svg>
      <p class="text-[var(--zh-text-secondary)] text-sm">{{ profileError }}</p>
      <button class="btn-primary text-sm mt-3 px-4 py-2" @click="retryProfile">重新加载</button>
    </div>

    <!-- 用户信息加载中骨架屏 -->
    <div v-else class="pt-3 pb-2 px-3 animate-pulse">
      <div class="flex flex-col items-center">
        <div class="w-20 h-20 bg-[var(--zh-border)] rounded-full mb-2"></div>
        <div class="h-5 bg-[var(--zh-border)] rounded w-24 mb-1"></div>
        <div class="h-3 bg-[var(--zh-border)] rounded w-32 mb-2"></div>
        <div class="h-3 bg-[var(--zh-border)] rounded w-48 mb-3"></div>
        <div class="flex items-center gap-8">
          <div v-for="i in 4" :key="i" class="flex flex-col items-center gap-1">
            <div class="h-5 w-8 bg-[var(--zh-border)] rounded"></div>
            <div class="h-2 w-6 bg-[var(--zh-border)] rounded"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Ta的作品列表 -->
    <div v-if="userInfo" class="mt-3">
      <h3 class="text-lg font-semibold text-[var(--zh-text)] mb-2">{{ isOwnProfile ? '我的作品' : '作品' }}</h3>
      <!-- 骨架屏 -->
      <div v-if="loading && articles.length === 0" class="grid grid-cols-3 gap-px">
        <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100" />
      </div>
      <!-- 错误状态 -->
      <ErrorRetry v-else-if="articlesError && !articles.length" :message="articlesError" :on-retry="retryArticles" />
      <!-- 空状态 -->
      <div v-else-if="!loading && articles.length === 0" class="text-center py-10 text-[var(--zh-text-tertiary)]">
        <p class="text-lg">暂无作品</p>
      </div>
      <!-- 三列网格 -->
      <div v-else class="grid grid-cols-3 gap-0">
        <ArticleGridCard v-for="article in articles" :key="article.id" :article="article" />
      </div>
      <!-- 无限滚动哨兵 -->
      <div ref="sentinelRef" class="h-0.5"></div>
      <!-- 加载更多中 -->
      <div v-if="loading && articles.length > 0" class="text-center py-4">
        <span class="text-sm text-[var(--zh-text-tertiary)]">
          <svg class="animate-spin inline w-3.5 h-3.5 mr-1" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          加载中...
        </span>
      </div>
      <div v-else-if="!hasMore && articles.length > 0" class="text-center py-4">
        <span class="text-xs text-[var(--zh-text-tertiary)]">没有更多了</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 用户主页（信息 + 作品列表） */
import type { User, Article } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userId = computed(() => Number(route.params.id))

/** 无效用户ID保护 — 显示友好提示而非直接抛错 */
const isValidUserId = computed(() => !isNaN(userId.value) && userId.value > 0)

if (!isValidUserId.value) {
  throw createError({ statusCode: 404, message: '用户不存在', fatal: false })
}

/** 是否为自己的主页 */
const isOwnProfile = computed(() => {
  return userStore.userInfo?.id === userId.value
})

// Toast 提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
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

// 用户信息
const userInfo = ref<User | null>(null)
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const onlineStatus = ref(false)
const articlesError = ref<string | null>(null)
const profileError = ref<string | null>(null)
// 作品分页
const articlePage = ref(1)
const articlePageSize = 20
const sentinelRef = ref<HTMLElement | null>(null)

const { cachedRequest, invalidate, invalidateByPrefix } = useRequestCache({ ttl: 5 * 60 * 1000 })

/**
 * 重置页面状态（用户切换时清空旧数据，避免显示上一个用户的内容）
 */
const resetState = () => {
  userInfo.value = null
  articles.value = []
  articlesError.value = null
  profileError.value = null
  onlineStatus.value = false
  loading.value = false
  hasMore.value = true
  articlePage.value = 1
}

/**
 * 加载用户信息与作品（封装为函数，便于 onMounted 和 watch 复用）
 */
const fetchUserData = async () => {
  if (!isValidUserId.value) return
  resetState()
  loading.value = true
  try {
    const { userApi } = await import('@/api')
    const profileRes: any = await userApi.getProfile(userId.value)
    userInfo.value = profileRes.data?.data ?? null
    if (!userInfo.value) profileError.value = '用户不存在或已被删除'

    if (userInfo.value) {
      const articlesRes = await userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 })
      const data = articlesRes.data?.data
      articles.value = data?.list || []
      hasMore.value = articles.value.length >= 20
    }
  } catch (error: any) {
    if (!userInfo.value) {
      profileError.value = error?.message || '用户信息加载失败'
    } else {
      articlesError.value = error?.message || '作品加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

// 核心修复：动态路由参数变化时（点击头像切换用户主页）重新加载
// 1. 清空 useRequestCache 中该用户的所有缓存，避免 SWR 返回旧数据
// 2. 调用 fetchUserData 重新拉取
// 3. 重置滚动位置（同路径下 scrollBehavior 返回 false，不会自动滚动到顶）
watch(() => userId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  // 强制清空该用户相关的请求缓存（包括 SWR 中的过期数据）
  invalidateByPrefix(`/user/profile/${oldId}`)
  invalidateByPrefix(`/user/articles/${oldId}`)
  // 切换用户时重置滚动位置，使用 instant 覆盖 Tailwind 的 scroll-behavior:smooth
  if (oldId) window.scrollTo({ top: 0, behavior: 'instant' })
  await fetchUserData()
  if (userStore.isLoggedIn) fetchOnlineStatus()
}, { immediate: false })

// 获取在线状态
const fetchOnlineStatus = async () => {
  try {
    const { socialApi } = await import('@/api/social')
    const response: any = await socialApi.getOnlineStatus(userId.value)
    onlineStatus.value = response.data.data?.[String(userId.value)] ?? false
  } catch {
    onlineStatus.value = false
  }
}

// 发起私信（互关后可用）
const startChat = () => {
  if (!userInfo.value || !userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  navigateTo(`/messages/${userId.value}`)
}

// 关注/取关
const toggleFollow = async () => {
  if (!userInfo.value) return
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const { socialApi } = await import('@/api/social')
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
    showToast('关注失败，请稍后重试', 'error')
  }
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  articlePage.value++
  loading.value = true
  try {
    const { userApi } = await import('@/api')
    const response = await userApi.getUserArticles(userId.value, { page: articlePage.value, pageSize: articlePageSize })
    const data = response.data.data
    const items = data?.list || []
    articles.value.push(...items)
    hasMore.value = items.length >= articlePageSize
  } catch {
    articlePage.value--
  } finally {
    loading.value = false
  }
}

// 无限滚动：IntersectionObserver 监听哨兵元素
let observer: IntersectionObserver | null = null
onMounted(() => {
  // 首次进入页面：加载用户数据 + 在线状态 + 设置滚动哨兵
  fetchUserData()
  if (userStore.isLoggedIn) {
    fetchOnlineStatus()
  }
  nextTick(() => {
    if (sentinelRef.value) {
      observer = new IntersectionObserver((entries) => {
        if (entries[0]?.isIntersecting && hasMore.value && !loading.value) {
          loadMore()
        }
      }, { rootMargin: '200px' })
      observer.observe(sentinelRef.value)
    }
  })
})
onUnmounted(() => observer?.disconnect())

const retryArticles = async () => {
  articlesError.value = null
  loading.value = true
  try {
    const { userApi } = await import('@/api')
    const response: any = await cachedRequest(
      () => userApi.getUserArticles(userId.value, { page: 1, pageSize: 20 }),
      `/user/articles/${userId.value}`,
      { page: 1, pageSize: 20 }
    )
    const data = response.data.data
    articles.value = data?.list || []
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
    const { userApi } = await import('@/api')
    const response: any = await cachedRequest(
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

useHead({
  title: () => userInfo.value ? `${userInfo.value.nickname} - 知讯` : '个人中心 - 知讯',
})
</script>
