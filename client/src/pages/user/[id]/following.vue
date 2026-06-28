<template>
  <!-- 关注列表 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-1.5 2xl:px-2 py-1.5">
    <div class="flex items-center justify-between mb-1.5">
      <p class="text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)]">{{ isOwn ? '你' : userName }}关注的用户</p>
      <span class="text-sm text-[var(--zh-text-secondary)]">共 {{ totalCount }} 人</span>
    </div>
    <ErrorRetry v-if="error && !users.length" :message="error" :on-retry="fetchList" />
    <div v-if="loading && !users.length" class="space-y-2">
      <div v-for="i in 5" :key="i" class="card p-3 animate-pulse">
        <div class="flex items-center gap-3"><div class="w-12 h-12 bg-[var(--zh-border)] rounded-full"/><div class="flex-1 space-y-1.5"><div class="h-4 bg-[var(--zh-border)] rounded w-24"/><div class="h-3 bg-[var(--zh-border)] rounded w-48"/></div></div>
      </div>
    </div>
    <div v-else class="space-y-2">
      <div v-for="user in users" :key="user.id" class="card p-3 flex items-center justify-between hover:shadow-md transition-shadow">
        <RouterLink :to="`/user/${user.id}`" class="flex items-center gap-3 min-w-0 flex-1">
          <UserAvatar :src="user.avatar" :alt="user.nickname" size="lg"/>
          <div class="min-w-0"><p class="font-medium text-[var(--zh-text)] truncate">{{ user.nickname }}</p><p class="text-xs text-[var(--zh-text-tertiary)] truncate">{{ user.bio || '这个人很懒，什么都没写' }}</p></div>
        </RouterLink>
        <button v-if="!isOwn && user.id !== userStore.userInfo?.id" class="btn-secondary text-xs shrink-0 ml-2" :disabled="loadingIds[user.id]" @click.stop="toggleFollowUser(user)">
          {{ loadingIds[user.id] ? '...' : user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
      <EmptyState v-if="!loading && users.length === 0" title="暂无关注" description="Ta还没有关注任何人"/>
    </div>
    <div ref="sentinelRef" class="h-px"></div>
    <div v-if="loading && users.length > 0" class="text-center py-3">
      <span class="text-sm text-[var(--zh-text-tertiary)]">加载中...</span>
    </div>
    <div v-if="!hasMore && users.length > 0" class="text-center py-3 text-sm text-[var(--zh-text-tertiary)]">没有更多了</div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute(); const userStore = useUserStore()
const targetId = computed(() => Number(route.params.id))
const isOwn = computed(() => userStore.userInfo?.id === targetId.value)
const userName = ref('')
const { setTitle } = usePageHeaderTitle()
watchEffect(() => { setTitle(isOwn.value ? '我的关注' : `${userName.value || 'Ta'}的关注`) })

interface FollowItem { id: number; nickname: string; avatar: string; bio?: string; isFollowing?: boolean }
const users = ref<FollowItem[]>([]); const loading = ref(false); const hasMore = ref(true)
const error = ref<string | null>(null); const page = ref(1); const totalCount = ref(0); const loadingIds = ref<Record<number, boolean>>({})

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
useScrollSentinel({ onLoadMore: loadMore, hasMore, loading })

const toggleFollowUser = async (user: FollowItem) => {
  loadingIds.value[user.id] = true
  try {
    const { socialApi } = await import('~/api/social')
    const { data } = await socialApi.toggleFollow(user.id)
    user.isFollowing = data.data.followed
  } finally { loadingIds.value[user.id] = false }
}

onMounted(() => fetchList())

// 核心修复：动态路由 targetId 变化时（切换用户）重置并重新加载
watch(() => targetId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  users.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  totalCount.value = 0
  userName.value = ''
  await fetchList()
})

useHead({ title: () => `${isOwn.value ? '我' : userName.value || 'Ta'}的关注 - 知讯` })
</script>
