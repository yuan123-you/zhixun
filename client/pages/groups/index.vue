<template>
  <div class="max-w-[800px] mx-auto px-3 py-4">
    <!-- 群组广场 -->
    <!-- 顶部标题和创建 -->
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-xl font-bold text-slate-900 dark:text-white">群组广场</h1>
      <ClientOnly>
        <button
          v-if="userStore.isLoggedIn"
          class="flex items-center gap-1 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-600 transition"
          @click="showCreateModal = true"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          创建群组
        </button>
      </ClientOnly>
    </div>

    <!-- 搜索栏 -->
    <div class="relative mb-4">
      <input
        v-model="searchKeyword"
        type="text"
        class="input pl-10"
        placeholder="搜索群组..."
        @keydown.enter="handleSearch"
      />
      <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
    </div>

    <!-- Tab 切换 -->
    <div class="flex items-center gap-4 border-b border-slate-200 dark:border-gray-700 mb-4">
      <button
        class="pb-2 text-sm font-medium border-b-2 transition-colors"
        :class="activeTab === 'my' ? 'border-primary text-primary' : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-gray-400 dark:hover:text-gray-200'"
        @click="switchTab('my')"
      >我的群组</button>
      <button
        class="pb-2 text-sm font-medium border-b-2 transition-colors"
        :class="activeTab === 'search' ? 'border-primary text-primary' : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-gray-400 dark:hover:text-gray-200'"
        @click="switchTab('search')"
      >发现群组</button>
    </div>

    <!-- 我的群组 -->
    <template v-if="activeTab === 'my'">
      <ClientOnly>
        <div v-if="!userStore.isLoggedIn" class="text-center py-16">
          <p class="text-slate-400 mb-3">登录后查看我的群组</p>
          <NuxtLink to="/login" class="btn-primary text-sm">登录</NuxtLink>
        </div>
        <div v-else>
          <div v-if="myGroupsLoading" class="space-y-3">
            <div v-for="i in 3" :key="i" class="card animate-pulse flex items-center gap-3 p-3">
              <div class="w-12 h-12 bg-slate-200 dark:bg-gray-700 rounded-xl shrink-0"></div>
              <div class="flex-1">
                <div class="h-4 bg-slate-200 dark:bg-gray-700 rounded w-32 mb-2"></div>
                <div class="h-3 bg-slate-200 dark:bg-gray-700 rounded w-24"></div>
              </div>
            </div>
          </div>
          <div v-else-if="myGroups.length === 0" class="text-center py-16">
            <p class="text-slate-400 mb-2">还没有加入任何群组</p>
            <button class="text-primary hover:underline text-sm" @click="switchTab('search')">去发现群组</button>
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="group in myGroups"
              :key="group.id"
              class="card flex items-center gap-3 p-3 hover:shadow-[var(--shadow-md)] transition-shadow cursor-pointer"
              @click="navigateTo(`/groups/${group.id}`)"
            >
              <img
                :src="group.avatar || '/avatar-placeholder.png'"
                :alt="group.name"
                class="w-12 h-12 rounded-xl object-cover shrink-0"
              />
              <div class="flex-1 min-w-0">
                <h3 class="text-sm font-semibold text-slate-900 dark:text-white truncate">{{ group.name }}</h3>
                <p class="text-xs text-slate-400 truncate mt-0.5">{{ group.memberCount }} 名成员</p>
              </div>
              <svg class="w-5 h-5 text-slate-300 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>
        </div>
      </ClientOnly>
    </template>

    <!-- 发现群组 -->
    <template v-if="activeTab === 'search'">
      <div v-if="searchResults.length === 0 && searchKeyword" class="text-center py-16">
        <p class="text-slate-400">没有找到匹配的群组</p>
      </div>
      <div v-if="!searchKeyword" class="text-center py-16">
        <p class="text-slate-400">输入关键词搜索群组</p>
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="group in searchResults"
          :key="group.id"
          class="card flex items-center gap-3 p-3 hover:shadow-[var(--shadow-md)] transition-shadow"
        >
          <img
            :src="group.avatar || '/avatar-placeholder.png'"
            :alt="group.name"
            class="w-12 h-12 rounded-xl object-cover shrink-0 cursor-pointer"
            @click="navigateTo(`/groups/${group.id}`)"
          />
          <div class="flex-1 min-w-0 cursor-pointer" @click="navigateTo(`/groups/${group.id}`)">
            <h3 class="text-sm font-semibold text-slate-900 dark:text-white truncate">{{ group.name }}</h3>
            <p class="text-xs text-slate-400 truncate mt-0.5">{{ group.memberCount }} 名成员{{ group.description ? ' · ' + group.description : '' }}</p>
          </div>
          <ClientOnly>
            <button
              v-if="userStore.isLoggedIn && group.myRole === 0"
              class="btn-primary text-xs px-3 py-1.5 rounded-lg shrink-0"
              @click="handleJoin(group.id)"
            >加入</button>
            <span v-else-if="group.myRole && group.myRole > 0" class="text-xs text-slate-400 shrink-0">已加入</span>
          </ClientOnly>
        </div>
      </div>
      <div v-if="searchHasMore" class="text-center py-3">
        <button class="text-sm text-slate-500 hover:text-slate-700" :disabled="searchLoading" @click="loadMoreSearch">
          {{ searchLoading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </template>

    <!-- 创建群组弹窗 -->
    <ClientOnly>
      <Teleport to="body">
        <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showCreateModal = false">
          <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
            <h3 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">创建群组</h3>
            <div class="space-y-3 mb-4">
              <input
                v-model="createForm.name"
                type="text"
                class="input"
                placeholder="群组名称（必填）"
                maxlength="30"
              />
              <textarea
                v-model="createForm.description"
                class="input resize-none"
                rows="2"
                placeholder="群组简介（选填）"
                maxlength="200"
              ></textarea>
            </div>
            <div class="flex justify-end gap-3">
              <button class="btn-ghost text-sm" @click="showCreateModal = false">取消</button>
              <button
                class="btn-primary text-sm"
                :disabled="!createForm.name.trim() || creating"
                @click="handleCreate"
              >{{ creating ? '创建中...' : '创建' }}</button>
            </div>
          </div>
        </div>
      </Teleport>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
/** 群组广场页 */
import { groupApi } from '~/api/group'
import type { GroupInfo } from '~/api/group'

const userStore = useUserStore()

const activeTab = ref<'my' | 'search'>('my')
const searchKeyword = ref('')

// 我的群组
const myGroups = ref<GroupInfo[]>([])
const myGroupsLoading = ref(false)

// 搜索
const searchResults = ref<GroupInfo[]>([])
const searchLoading = ref(false)
const searchPage = ref(1)
const searchHasMore = ref(false)

// 创建弹窗
const showCreateModal = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', description: '' })

const switchTab = (tab: 'my' | 'search') => {
  activeTab.value = tab
  if (tab === 'my') loadMyGroups()
}

const loadMyGroups = async () => {
  myGroupsLoading.value = true
  try {
    const { data } = await groupApi.getMyGroups()
    myGroups.value = data?.data?.list || []
  } catch { myGroups.value = [] }
  finally { myGroupsLoading.value = false }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  searchPage.value = 1
  searchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(searchKeyword.value.trim(), 1, 20)
    searchResults.value = data?.data?.list || []
    searchHasMore.value = (data?.data?.list?.length || 0) >= 20
  } catch { searchResults.value = [] }
  finally { searchLoading.value = false }
}

const loadMoreSearch = async () => {
  searchPage.value++
  searchLoading.value = true
  try {
    const { data } = await groupApi.searchGroups(searchKeyword.value.trim(), searchPage.value, 20)
    const list = data?.data?.list || []
    searchResults.value.push(...list)
    searchHasMore.value = list.length >= 20
  } catch { searchPage.value-- }
  finally { searchLoading.value = false }
}

// 监听搜索输入 - 防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val.trim()) { searchResults.value = []; return }
  searchTimer = setTimeout(handleSearch, 400)
})

const handleJoin = async (groupId: number) => {
  try {
    await groupApi.joinGroup(groupId)
    loadMyGroups()
    const g = searchResults.value.find(g => g.id === groupId)
    if (g) g.myRole = 1
  } catch {
    // 加入失败
  }
}

const handleCreate = async () => {
  if (!createForm.name.trim()) return
  creating.value = true
  try {
    const { data } = await groupApi.createGroup({
      name: createForm.name.trim(),
      description: createForm.description.trim() || undefined,
    })
    showCreateModal.value = false
    createForm.name = ''
    createForm.description = ''
    loadMyGroups()
    navigateTo(`/groups/${data?.data}`)
  } catch { /* ignore */ }
  finally { creating.value = false }
}

onMounted(() => {
  if (userStore.isLoggedIn) loadMyGroups()
})

useHead({ title: '群组广场 - 知讯' })
</script>
