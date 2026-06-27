<template>
  <!-- 我的关注列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">我的关注</h1>
    <div class="flex items-center justify-between mt-1 mb-1.5">
      <p class="text-gray-500 dark:text-gray-400">你关注的用户</p>
      <span class="text-sm text-slate-500">共 {{ totalCount }} 人</span>
    </div>
    <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchFollowing" />
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
    <!-- 关注列表 -->
    <div v-else class="space-y-2">
      <div
        v-for="user in users"
        :key="user.id"
        class="card p-2.5 flex items-center justify-between cursor-pointer hover:shadow-md transition-shadow"
        @click="navigateTo(`/user/${user.id}`)"
      >
        <div class="flex items-center gap-3 min-w-0">
          <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg" />
          <div class="min-w-0">
            <p class="font-medium text-slate-900 truncate">{{ user.nickname }}</p>
            <p class="text-xs text-slate-400 truncate">{{ user.bio || '这个人很懒，什么都没写' }}</p>
          </div>
        </div>
        <button
          class="btn-secondary text-xs shrink-0 ml-2"
          @click.stop="handleToggleFollow(user)"
        >
          {{ '已关注' }}
        </button>
      </div>
      <EmptyState v-if="!loading && users.length === 0" title="暂无关注" description="去发现更多有趣的人吧" />
    </div>
    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="text-center mt-3">
      <button class="btn-secondary text-sm" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 关注列表 */
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

const fetchFollowing = async () => {
  const userId = userStore.userInfo?.id
  if (!userId) {
    error.value = '请先登录'
    return
  }
  if (loading.value) return
  loading.value = true
  error.value = null
  try {
    const { data } = await useApi().get<any>(`/users/${userStore.userInfo!.id}/following`, {
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
  await fetchFollowing()
  if (error.value) page.value--
}

const handleToggleFollow = async (targetUser: FollowUser) => {
  try {
    await useApi().post(`/users/${targetUser.id}/follow`)
    users.value = users.value.filter(u => u.id !== targetUser.id)
    totalCount.value = Math.max(0, totalCount.value - 1)
    userStore.updateProfile({ followCount: Math.max(0, (userStore.userInfo?.followCount || 0) - 1) })
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  fetchFollowing()
})

useHead({
  title: () => '我的关注 - 知讯',
})
</script>
