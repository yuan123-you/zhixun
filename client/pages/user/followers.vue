<template>
  <!-- 我的粉丝列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <div class="flex items-center justify-between mb-3">
      <h1 class="text-xl font-bold text-slate-900">我的粉丝</h1>
      <span class="text-sm text-slate-500">共 {{ totalCount }} 人</span>
    </div>
    <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchFollowers" />
    <!-- 加载骨架屏 -->
    <div v-if="loading && !users.length" class="space-y-2">
      <div v-for="i in 5" :key="i" class="card p-3 animate-pulse">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-slate-200 rounded-full" />
          <div class="flex-1 space-y-1.5">
            <div class="h-4 bg-slate-200 rounded w-24" />
            <div class="h-3 bg-slate-200 rounded w-48" />
          </div>
        </div>
      </div>
    </div>
    <!-- 粉丝列表 -->
    <div v-else class="space-y-2">
      <div
        v-for="user in users"
        :key="user.id"
        class="card p-3 flex items-center justify-between cursor-pointer hover:shadow-md transition-shadow"
        @click="navigateTo(`/user/${user.id}`)"
      >
        <div class="flex items-center gap-3 min-w-0">
          <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
          <div class="min-w-0">
            <p class="font-medium text-slate-900 truncate">
              {{ user.nickname }}
              <span v-if="user.isFollowing" class="text-xs text-primary ml-1">互相关注</span>
            </p>
            <p class="text-xs text-slate-400 truncate">{{ user.bio || '这个人很懒，什么都没写' }}</p>
          </div>
        </div>
        <button
          class="text-xs btn-primary shrink-0 ml-2"
          :class="{ 'btn-secondary': user.isFollowing }"
          @click.stop="handleToggleFollow(user)"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
      <EmptyState v-if="!loading && users.length === 0" title="暂无粉丝" description="发布优质内容可以吸引更多粉丝" />
    </div>
    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="text-center mt-3">
      <button class="btn-secondary text-sm" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 粉丝列表 */
import type { User } from '~/types'

definePageMeta({
  middleware: 'auth',
})

const userStore = useUserStore()

interface FollowUser extends User {
  id: number
  nickname: string
  avatar: string
  bio: string
  isFollowing?: boolean
}

const users = ref<FollowUser[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const totalCount = ref(0)
const pageSize = 20

const fetchFollowers = async () => {
  const userId = userStore.userInfo?.id
  if (!userId) {
    error.value = '请先登录'
    return
  }
  if (loading.value) return
  loading.value = true
  error.value = null
  try {
    const { data } = await useApi().get<any>(`/users/${userStore.userInfo!.id}/followers`, {
      page: page.value,
      pageSize,
    })
    // 兼容不同返回格式
    const list = data.data?.list || data.data || []
    const total = data.data?.total || list.length
    if (page.value === 1) {
      users.value = list
    } else {
      users.value.push(...list)
    }
    totalCount.value = total
    hasMore.value = users.value.length < total
  } catch {
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  page.value++
  await fetchFollowers()
  if (error.value) page.value--
}

const handleToggleFollow = async (targetUser: FollowUser) => {
  try {
    await useApi().post(`/users/${targetUser.id}/follow`)
    targetUser.isFollowing = !targetUser.isFollowing
    userStore.updateProfile({
      followerCount: targetUser.isFollowing
        ? (userStore.userInfo?.followerCount || 0) + 1
        : Math.max(0, (userStore.userInfo?.followerCount || 0) - 1),
    })
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  fetchFollowers()
})

useHead({
  title: () => '我的粉丝 - 知讯',
})
</script>
