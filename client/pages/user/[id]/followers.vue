<template>
  <!-- 粉丝列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ isOwn ? '我' : userName }}的粉丝</h1>
    <div class="flex items-center justify-between mt-1 mb-1.5">
      <p class="text-gray-500 dark:text-gray-400">{{ isOwn ? '你' : userName }}的粉丝</p>
      <span class="text-sm text-slate-500">共 {{ totalCount }} 人</span>
    </div>
    <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchList" />
    <div v-if="loading && !users.length" class="space-y-2">
      <div v-for="i in 5" :key="i" class="card p-3 animate-pulse">
        <div class="flex items-center gap-3"><div class="w-12 h-12 bg-slate-200 rounded-full"/><div class="flex-1 space-y-1.5"><div class="h-4 bg-slate-200 rounded w-24"/><div class="h-3 bg-slate-200 rounded w-48"/></div></div>
      </div>
    </div>
    <div v-else class="space-y-2">
      <div v-for="user in users" :key="user.id" class="card p-3 flex items-center justify-between hover:shadow-md transition-shadow">
        <NuxtLink :to="`/user/${user.id}`" class="flex items-center gap-3 min-w-0 flex-1">
          <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg"/>
          <div class="min-w-0">
            <p class="font-medium text-slate-900 truncate">{{ user.nickname }}<span v-if="user.isMutualFollow" class="text-xs text-primary ml-1">互关</span></p>
            <p class="text-xs text-slate-400 truncate">{{ user.bio || '这个人很懒，什么都没写' }}</p>
          </div>
        </NuxtLink>
        <button v-if="!isOwn && user.id !== userStore.userInfo?.id" class="text-xs btn-primary shrink-0 ml-2" :class="{ 'btn-secondary': user.isFollowing }" :disabled="loadingIds[user.id]" @click.stop="toggleFollowUser(user)">
          {{ loadingIds[user.id] ? '...' : user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
      <EmptyState v-if="!loading && users.length === 0" title="暂无粉丝" description="Ta还没有粉丝"/>
    </div>
    <div v-if="hasMore && !loading" class="text-center mt-3">
      <button class="btn-secondary text-sm" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute(); const userStore = useUserStore()
const targetId = computed(() => Number(route.params.id))
const isOwn = computed(() => userStore.userInfo?.id === targetId.value)
const userName = ref('')

interface FollowItem { id: number; nickname: string; avatar: string; bio?: string; isFollowing?: boolean; isMutualFollow?: boolean }
const users = ref<FollowItem[]>([]); const loading = ref(false); const hasMore = ref(true)
const error = ref<string | null>(null); const page = ref(1); const totalCount = ref(0); const loadingIds = ref<Record<number, boolean>>({})

const fetchList = async () => {
  if (loading.value) return
  loading.value = true; error.value = null
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.getFollowers(targetId.value, { page: page.value, pageSize: 20 })
    const list = data.data?.list || data.data?.items || []
    if (page.value === 1) users.value = list; else users.value.push(...list)
    totalCount.value = data.data?.total || list.length
    hasMore.value = users.value.length < totalCount.value
    if (list.length > 0) userName.value = list[0]?.nickname || ''
  } catch { error.value = '加载失败，请稍后重试' }
  finally { loading.value = false }
}

const loadMore = async () => { if (loading.value || !hasMore.value) return; page.value++; await fetchList(); if (error.value) page.value-- }

const toggleFollowUser = async (user: FollowItem) => {
  loadingIds.value[user.id] = true
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.toggleFollow(user.id)
    user.isFollowing = data.data.followed
    user.isMutualFollow = data.data.followed && user.isMutualFollow
  } finally { loadingIds.value[user.id] = false }
}

onMounted(() => fetchList())
useHead({ title: () => `${isOwn.value ? '我' : userName.value || 'Ta'}的粉丝 - 知讯` })
</script>
