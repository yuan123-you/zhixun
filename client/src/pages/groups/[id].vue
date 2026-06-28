<template>
  <!-- 群组聊天页 -->
  <div class="max-w-[900px] mx-auto h-[calc(100dvh-3.75rem)] md:h-[calc(100dvh-4rem)]">
    <!-- 加载 -->
    <div v-if="loading" class="flex items-center justify-center h-full">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
    </div>

    <!-- 聊天主界面 -->
    <ClientOnly v-else-if="group">
      <GroupChatWindow
        :group="group"
        :current-user-id="userStore.userInfo?.id || 0"
        @close="goBack"
      />
    </ClientOnly>

    <!-- 空状态 -->
    <div v-else class="flex items-center justify-center h-full">
      <div class="text-center">
        <p class="text-[var(--zh-text-tertiary)] mb-3">群组不存在或已解散</p>
        <RouterLink to="/groups" class="text-primary hover:underline text-sm">返回群组广场</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { groupApi } from '@/api/group'
import type { GroupInfo } from '@/api/group'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const groupId = computed(() => Number(route.params.id))
const group = ref<GroupInfo | null>(null)
const loading = ref(true)

const loadGroup = async () => {
  loading.value = true
  try {
    const { data } = await groupApi.getGroupDetail(groupId.value)
    group.value = data?.data || null
  } catch { group.value = null }
  finally { loading.value = false }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/groups')
}

onMounted(() => loadGroup())

// 核心修复：动态路由 groupId 变化时（切换群组）重置状态并重新加载
watch(() => groupId.value, async (newId, oldId) => {
  if (newId === oldId || !newId || isNaN(newId)) return
  group.value = null
  loading.value = true
  window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
  await loadGroup()
})

useHead({
  title: () => group.value ? `${group.value.name} - 知讯` : '群组 - 知讯',
})
</script>
