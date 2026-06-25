<template>
  <!-- 个人中心 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 个人资料卡 -->
    <div class="card p-3 mb-3">
      <div class="flex items-start space-x-3">
        <!-- 头像（可点击编辑） -->
        <div class="relative group cursor-pointer shrink-0" @click="triggerAvatarUpload">
          <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" size="xl" />
          <div class="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-bold text-slate-900">{{ userStore.userInfo?.nickname }}</h2>
            <NuxtLink to="/user/edit" class="btn-secondary text-sm">{{ '编辑资料' }}</NuxtLink>
          </div>
          <div class="flex items-center gap-2 mt-1">
            <span class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
              ID: {{ userStore.userInfo?.uid }}
            </span>
            <span v-if="userStore.userInfo?.showGenderOnProfile && userStore.userInfo?.gender" class="text-xs text-slate-400 bg-pink-50 px-2 py-0.5 rounded">
              {{ userStore.userInfo.gender === 1 ? '男' : userStore.userInfo.gender === 2 ? '女' : '' }}
            </span>
            <span v-if="userStore.userInfo?.province" class="text-xs text-slate-400 bg-blue-50 px-2 py-0.5 rounded">
              {{ userStore.userInfo?.province }}
            </span>
            <span v-if="userStore.userInfo?.ipLocation" class="text-xs text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
              IP属地: {{ userStore.userInfo?.ipLocation }}
            </span>
          </div>
          <p v-if="userStore.userInfo?.bio" class="text-sm text-slate-500 mt-1">{{ userStore.userInfo?.bio }}</p>
          <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
            <NuxtLink to="/user/articles" class="cursor-pointer hover:text-primary transition-colors">
              <strong class="text-slate-900">{{ userStore.userInfo?.articleCount }}</strong> {{ '文章' }}
            </NuxtLink>
            <NuxtLink to="/user/following" class="cursor-pointer hover:text-primary transition-colors">
              <strong class="text-slate-900">{{ userStore.userInfo?.followCount }}</strong> {{ '关注' }}
            </NuxtLink>
            <NuxtLink to="/user/followers" class="cursor-pointer hover:text-primary transition-colors">
              <strong class="text-slate-900">{{ userStore.userInfo?.followerCount }}</strong> {{ '粉丝' }}
            </NuxtLink>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab切换 -->
    <div class="flex items-center border-b border-slate-200 mb-3 overflow-x-auto no-scrollbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-3 py-2 text-sm font-medium border-b-2 whitespace-nowrap transition-colors shrink-0"
        :class="activeTab === tab.key
          ? 'border-primary text-primary'
          : 'border-transparent text-slate-500 hover:text-slate-700'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab内容 -->
    <div>
      <!-- 文章 -->
      <ErrorRetry v-if="activeTab === 'published' && tabError && !publishedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'published' && !tabError">
        <!-- 骨架屏 -->
        <div v-if="loading && publishedArticles.length === 0" class="grid grid-cols-3">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && publishedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-lg mb-2">还没有发布文章</p>
          <NuxtLink to="/editor" class="text-primary hover:underline text-sm">去写文章</NuxtLink>
        </div>
        <div v-else class="grid grid-cols-3">
          <div v-for="article in publishedArticles" :key="article.id" class="relative group">
            <ArticleGridCard :article="article" />
            <!-- 悬浮操作层 -->
            <div class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity bg-black/40 flex flex-col">
              <div class="flex items-start justify-end p-1 gap-1">
                <select
                  :value="article.visibility ?? 0"
                  class="text-[10px] border-0 rounded bg-white/90 text-slate-700 px-1 py-0.5 focus:outline-none cursor-pointer"
                  @click.stop
                  @change="handleVisibilityChange(article, Number(($event.target as HTMLSelectElement).value))"
                >
                  <option :value="0">公开</option>
                  <option :value="1">粉丝</option>
                  <option :value="2">互关</option>
                  <option :value="3">私密</option>
                </select>
                <button
                  class="p-0.5 rounded bg-white/90 text-slate-500 hover:text-red-500 transition-colors"
                  @click.stop="handleDeleteArticle(article)"
                  title="删除"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="hasMore" class="text-center py-4">
          <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 草稿 -->
      <ErrorRetry v-if="activeTab === 'drafts' && tabError && !draftArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'drafts' && !tabError">
        <!-- 骨架屏 -->
        <div v-if="loading && draftArticles.length === 0" class="grid grid-cols-3">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && draftArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-lg mb-2">草稿箱为空</p>
          <p class="text-xs">草稿保存30天后将自动清理</p>
          <NuxtLink to="/editor" class="text-primary hover:underline text-sm mt-2 inline-block">去写文章</NuxtLink>
        </div>
        <div v-else class="grid grid-cols-3">
          <div v-for="article in draftArticles" :key="article.id" class="relative group">
            <ArticleGridCard :article="article" />
            <!-- 悬浮操作层 -->
            <div class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity bg-black/40 flex flex-col justify-end p-1.5 gap-1">
              <div class="flex items-center gap-1 flex-wrap">
                <button class="text-[10px] bg-white/90 text-slate-700 hover:text-primary px-1.5 py-0.5 rounded transition-colors" @click.stop="editDraft(article.id)">编辑</button>
                <button class="text-[10px] bg-primary/90 text-white hover:bg-primary px-1.5 py-0.5 rounded transition-colors" :disabled="publishingId === article.id" @click.stop="publishDraftNow(article)">{{ publishingId === article.id ? '...' : '发布' }}</button>
                <button class="text-[10px] bg-white/90 text-slate-600 hover:text-primary px-1.5 py-0.5 rounded transition-colors" @click.stop="openScheduleModal(article)">定时</button>
                <button class="text-[10px] bg-white/90 text-slate-500 hover:text-red-500 px-1 py-0.5 rounded transition-colors" @click.stop="handleDeleteArticle(article)" title="删除">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
              <div v-if="getDraftExpiry(article.createdAt)" class="text-[10px] text-white/60">{{ getDraftExpiry(article.createdAt) }}</div>
            </div>
          </div>
        </div>
        <div v-if="hasMore" class="text-center py-4">
          <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 收藏 -->
      <ErrorRetry v-if="activeTab === 'collected' && tabError && !collectedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'collected' && !tabError">
        <div v-if="loading && collectedArticles.length === 0" class="grid grid-cols-3">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && collectedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-lg">暂无收藏</p>
        </div>
        <div v-else class="grid grid-cols-3">
          <ArticleGridCard v-for="article in collectedArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-4">
          <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 点赞 -->
      <ErrorRetry v-if="activeTab === 'liked' && tabError && !likedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'liked' && !tabError">
        <div v-if="loading && likedArticles.length === 0" class="grid grid-cols-3">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && likedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-lg">暂无点赞</p>
        </div>
        <div v-else class="grid grid-cols-3">
          <ArticleGridCard v-for="article in likedArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-4">
          <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 评论 -->
      <ErrorRetry v-if="activeTab === 'comments' && tabError && !myComments.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'comments' && !tabError" class="space-y-4">
        <div v-for="comment in myComments" :key="comment.id" class="card p-2">
          <p class="text-sm text-slate-700">{{ comment.content }}</p>
          <div class="flex items-center justify-between mt-2">
            <span class="text-xs text-gray-400">{{ formatDate(comment.createdAt) }}</span>
            <NuxtLink :to="`/articles/${comment.articleId}`" class="text-xs text-primary hover:text-primary-600">{{ '查看文章' }}</NuxtLink>
          </div>
        </div>
        <EmptyState v-if="!loading && myComments.length === 0" title="暂无评论" />
      </div>

      <!-- 历史 -->
      <ErrorRetry v-if="activeTab === 'history' && tabError && !historyArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'history' && !tabError">
        <div v-if="loading && historyArticles.length === 0" class="grid grid-cols-3">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && historyArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-lg">暂无浏览历史</p>
        </div>
        <div v-else class="grid grid-cols-3">
          <ArticleGridCard v-for="article in historyArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-4">
          <button class="text-sm text-primary hover:underline" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 定时发布弹窗 -->
    <Teleport to="body">
      <div v-if="showScheduleModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showScheduleModal = false">
        <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md mx-4">
          <h3 class="text-lg font-bold text-slate-900 mb-4">定时发布</h3>
          <p class="text-sm text-slate-600 mb-3">文章「{{ scheduleTarget?.title || '无标题' }}」将在指定时间自动发布</p>
          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">选择发布时间</label>
            <input
              v-model="scheduleTime"
              type="datetime-local"
              class="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:border-primary focus:outline-none"
              :min="minScheduleTime"
            />
          </div>
          <div class="flex justify-end gap-3">
            <button class="btn-ghost text-sm" @click="showScheduleModal = false">取消</button>
            <button class="btn-primary text-sm" :disabled="!scheduleTime || schedulingId !== null" @click="confirmSchedulePublish">
              {{ schedulingId !== null ? '设置中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="showDeleteModal = false">
        <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md mx-4">
          <h3 class="text-lg font-bold text-slate-900 mb-4">确认删除</h3>
          <p class="text-sm text-slate-600 mb-4">确定要删除文章「{{ deleteTarget?.title || '无标题' }}」吗？此操作不可撤销。</p>
          <div class="flex justify-end gap-3">
            <button class="btn-ghost text-sm" @click="showDeleteModal = false">取消</button>
            <button class="bg-red-500 text-white px-4 py-2 rounded-lg text-sm hover:bg-red-600 transition-colors" :disabled="deletingId !== null" @click="confirmDelete">
              {{ deletingId !== null ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 个人中心页 */
import type { Article, Comment } from '~/types'
import { userApi, articleApi } from '~/api'

definePageMeta({
  middleware: 'auth',
})

const userStore = useUserStore()
const router = useRouter()
const { put: apiPut } = useApi()

const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)

// 触发头像上传
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

// 处理头像变更
const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过 5MB')
    return
  }

  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { post: apiPost } = useApi()
    const uploadRes = await apiPost<any>('/files/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const avatarUrl = uploadRes.data?.data
    if (avatarUrl) {
      await apiPut<any>('/user/profile', { avatar: avatarUrl })
      userStore.updateProfile({ avatar: avatarUrl })
    }
  } catch (err: any) {
    alert(err?.response?.data?.message || '头像上传失败，请稍后重试')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

const tabs = [
  { key: 'published', label: computed(() => '文章') },
  { key: 'drafts', label: computed(() => '草稿') },
  { key: 'collected', label: computed(() => '收藏') },
  { key: 'liked', label: computed(() => '点赞') },
  { key: 'comments', label: computed(() => '评论') },
  { key: 'history', label: computed(() => '历史') },
]

const activeTab = ref('published')
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 10
const tabError = ref<string | null>(null)

const { cachedRequest } = useRequestCache({ ttl: 5 * 60 * 1000 })

const publishedArticles = ref<Article[]>([])
const draftArticles = ref<Article[]>([])
const collectedArticles = ref<Article[]>([])
const likedArticles = ref<Article[]>([])
const myComments = ref<Comment[]>([])
const historyArticles = ref<Article[]>([])

// 发布中状态
const publishingId = ref<number | null>(null)
const schedulingId = ref<number | null>(null)

// 定时发布弹窗
const showScheduleModal = ref(false)
const scheduleTarget = ref<Article | null>(null)
const scheduleTime = ref('')

// 删除确认弹窗
const showDeleteModal = ref(false)
const deleteTarget = ref<Article | null>(null)
const deletingId = ref<number | null>(null)

// 最小可选时间
const minScheduleTime = computed(() => {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  const local = new Date(now.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
})

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  hasMore.value = true
  currentPage.value = 1
  tabError.value = null
  loadTabData()
}

// 重试加载Tab数据
const retryTabData = () => {
  tabError.value = null
  loadTabData()
}

// 加载Tab数据
const loadTabData = async () => {
  loading.value = true
  tabError.value = null
  try {
    const params = { page: currentPage.value, pageSize }
    let list: any[] = []
    let total = 0
    switch (activeTab.value) {
      case 'published': {
        const { data } = await userApi.getMyArticles(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        publishedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'drafts': {
        const { data } = await userApi.getMyDrafts(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        draftArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'collected': {
        const { data } = await userApi.getMyCollections(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        collectedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'liked': {
        const { data } = await userApi.getMyLikes(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        likedArticles.value = list
        hasMore.value = list.length < total
        break
      }
      case 'comments': {
        const { data } = await userApi.getMyComments(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        myComments.value = list
        hasMore.value = list.length < total
        break
      }
      case 'history': {
        const { data } = await userApi.getViewHistory(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        historyArticles.value = list
        hasMore.value = list.length < total
        break
      }
    }
  } catch {
    tabError.value = '数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  loading.value = true
  try {
    const params = { page: currentPage.value, pageSize }
    let list: any[] = []
    let total = 0
    switch (activeTab.value) {
      case 'published': {
        const { data } = await userApi.getMyArticles(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        publishedArticles.value.push(...list)
        hasMore.value = publishedArticles.value.length < total
        break
      }
      case 'drafts': {
        const { data } = await userApi.getMyDrafts(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        draftArticles.value.push(...list)
        hasMore.value = draftArticles.value.length < total
        break
      }
      case 'collected': {
        const { data } = await userApi.getMyCollections(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        collectedArticles.value.push(...list)
        hasMore.value = collectedArticles.value.length < total
        break
      }
      case 'liked': {
        const { data } = await userApi.getMyLikes(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        likedArticles.value.push(...list)
        hasMore.value = likedArticles.value.length < total
        break
      }
      case 'comments': {
        const { data } = await userApi.getMyComments(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        myComments.value.push(...list)
        hasMore.value = myComments.value.length < total
        break
      }
      case 'history': {
        const { data } = await userApi.getViewHistory(params)
        list = data?.data?.list || data?.data?.items || []
        total = data?.data?.total || list.length
        historyArticles.value.push(...list)
        hasMore.value = historyArticles.value.length < total
        break
      }
    }
  } catch {
    currentPage.value--
    tabError.value = '加载更多失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 修改文章可见性
const handleVisibilityChange = async (article: Article, visibility: number) => {
  try {
    await articleApi.updateVisibility(article.id, visibility)
    article.visibility = visibility
    showToast('可见性已更新')
  } catch (err: any) {
    showToast(err?.response?.data?.message || '操作失败', 'error')
  }
}

// 发布草稿（立即）
const publishDraftNow = async (article: Article) => {
  publishingId.value = article.id
  try {
    await articleApi.publishDraft(article.id)
    showToast('文章已发布')
    // 从草稿列表移除
    draftArticles.value = draftArticles.value.filter(a => a.id !== article.id)
    // 如果当前在文章Tab，刷新文章列表
    if (activeTab.value === 'published') {
      currentPage.value = 1
      await loadTabData()
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || '发布失败', 'error')
  } finally {
    publishingId.value = null
  }
}

// 打开定时发布弹窗
const openScheduleModal = (article: Article) => {
  scheduleTarget.value = article
  scheduleTime.value = ''
  showScheduleModal.value = true
}

// 确认定时发布
const confirmSchedulePublish = async () => {
  if (!scheduleTarget.value || !scheduleTime.value) return
  schedulingId.value = scheduleTarget.value.id
  try {
    const publishAt = new Date(scheduleTime.value).toISOString()
    await articleApi.publishDraft(scheduleTarget.value.id, publishAt)
    showToast('已设置为定时发布')
    draftArticles.value = draftArticles.value.filter(a => a.id !== scheduleTarget.value!.id)
    showScheduleModal.value = false
    scheduleTarget.value = null
  } catch (err: any) {
    showToast(err?.response?.data?.message || '操作失败', 'error')
  } finally {
    schedulingId.value = null
  }
}

// 编辑草稿
const editDraft = (articleId: number) => {
  router.push(`/editor?id=${articleId}`)
}

// 删除确认
const handleDeleteArticle = (article: Article) => {
  deleteTarget.value = article
  showDeleteModal.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value) return
  deletingId.value = deleteTarget.value.id
  try {
    await articleApi.deleteArticle(deleteTarget.value.id)
    showToast('文章已删除')
    const id = deleteTarget.value.id
    publishedArticles.value = publishedArticles.value.filter(a => a.id !== id)
    draftArticles.value = draftArticles.value.filter(a => a.id !== id)
    showDeleteModal.value = false
    deleteTarget.value = null
  } catch (err: any) {
    showToast(err?.response?.data?.message || '删除失败', 'error')
  } finally {
    deletingId.value = null
  }
}

// 计算草稿剩余天数
const getDraftExpiry = (createdAt: string) => {
  const created = new Date(createdAt)
  const expiry = new Date(created.getTime() + 30 * 24 * 60 * 60 * 1000)
  const now = new Date()
  const daysLeft = Math.ceil((expiry.getTime() - now.getTime()) / (24 * 60 * 60 * 1000))
  if (daysLeft <= 0) return '即将清理'
  if (daysLeft <= 7) return `${daysLeft}天后清理`
  return ''
}

// Toast提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  if (!import.meta.client) return
  const toast = document.createElement('div')
  toast.className = `fixed top-4 right-4 z-[60] px-4 py-3 rounded-lg shadow-lg text-white text-sm transition-all duration-300 ${
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

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面加载时获取默认Tab数据
onMounted(async () => {
  loadTabData()
  // 自动更新IP属地
  try {
    const { userApi } = await import('~/api')
    const res = await userApi.updateIpLocation()
    const ipLocation = res.data?.data
    if (ipLocation && userStore.userInfo) {
      userStore.updateProfile({ ipLocation })
    }
  } catch {
    // 静默失败，不影响主流程
  }
})

// 页面元信息
useHead({
  title: () => '个人中心' + ' - 知讯',
})
</script>
