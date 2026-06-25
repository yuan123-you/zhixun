<template>
  <!-- 关注列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <button class="text-slate-500 hover:text-primary transition-colors" @click="goBack">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </button>
        <h1 class="text-xl font-bold text-slate-900">{{ isOwn ? '我' : userName }}的关注</h1>
      </div>
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
          <div class="min-w-0"><p class="font-medium text-slate-900 truncate">{{ user.nickname }}</p><p class="text-xs text-slate-400 truncate">{{ user.bio || '这个人很懒，什么都没写' }}</p></div>
        </NuxtLink>
        <button v-if="!isOwn && user.id !== userStore.userInfo?.id" class="btn-secondary text-xs shrink-0 ml-2" :disabled="loadingIds[user.id]" @click.stop="toggleFollowUser(user)">
          {{ loadingIds[user.id] ? '...' : user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
      <EmptyState v-if="!loading && users.length === 0" title="暂无关注" description="Ta还没有关注任何人"/>
    </div>
    <div v-if="hasMore && !loading" class="text-center mt-3">
      <button class="btn-secondary text-sm" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const targetId = computed(() => Number(route.params.id))
const isOwn = computed(() => userStore.userInfo?.id === targetId.value)
const userName = ref('')

interface FollowItem { id: number; nickname: string; avatar: string; bio?: string; isFollowing?: boolean }
const users = ref<FollowItem[]>([]); const loading = ref(false); const hasMore = ref(true)
const error = ref<string | null>(null); const page = ref(1); const totalCount = ref(0); const loadingIds = ref<Record<number, boolean>>({})

const goBack = () => { if (window.history.length > 1) router.back(); else navigateTo('/') }

const fetchList = async () => {
  if (loading.value) return
  loading.value = true; error.value = null
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.getFollowing(targetId.value, { page: page.value, pageSize: 20 })
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
  } finally { loadingIds.value[user.id] = false }
}

onMounted(() => fetchList())
useHead({ title: () => `${isOwn.value ? '我' : userName.value || 'Ta'}的关注 - 知讯` })
</script>
