<template>
  <!-- 个人中心 - 抖音风格 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-0 md:px-1.5">
    <!-- 个人资料卡 - 抖音紧凑居中布局 -->
    <div class="pt-3 pb-2 px-3">
      <div class="flex flex-col items-center">
        <!-- 头像（可点击编辑） -->
        <div class="relative group cursor-pointer shrink-0 mb-1" @click="triggerAvatarUpload">
          <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" :size="isMobile ? 'lg' : 'xl'" />
          <div class="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />

        <!-- 昵称 -->
        <h2 class="text-base md:text-lg font-bold text-slate-900 mb-0">{{ userStore.userInfo?.nickname }}</h2>

        <!-- ID 和标签 -->
        <div class="flex flex-wrap items-center justify-center gap-1 mb-1">
          <span class="text-[11px] text-slate-400">
            知讯号: {{ userStore.userInfo?.uid }}
          </span>
          <span v-if="userStore.userInfo?.showGenderOnProfile && userStore.userInfo?.gender" class="text-[11px] text-slate-400">
            · {{ userStore.userInfo.gender === 1 ? '男' : userStore.userInfo.gender === 2 ? '女' : '' }}
          </span>
          <span v-if="userStore.userInfo?.province" class="text-[11px] text-slate-400">
            · {{ userStore.userInfo?.province }}
          </span>
          <span v-if="userStore.userInfo?.ipLocation" class="text-[11px] text-slate-400">
            · IP: {{ userStore.userInfo?.ipLocation }}
          </span>
        </div>

        <!-- 简介 -->
        <p v-if="userStore.userInfo?.bio" class="text-[11px] text-slate-500 text-center mb-1 max-w-[280px] line-clamp-2">{{ userStore.userInfo?.bio }}</p>

        <!-- 统计数据行 -->
        <div class="flex items-center justify-center gap-6 md:gap-8 mb-2">
          <NuxtLink to="/user/articles" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-slate-900">{{ userStore.userInfo?.articleCount ?? 0 }}</span>
            <span class="text-[10px] text-slate-400">作品</span>
          </NuxtLink>
          <div class="flex flex-col items-center">
            <span class="text-base md:text-lg font-bold text-slate-900">{{ userStore.userInfo?.totalLikeCount ?? 0 }}</span>
            <span class="text-[10px] text-slate-400">获赞</span>
          </div>
          <NuxtLink to="/user/following" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-slate-900">{{ userStore.userInfo?.followCount ?? 0 }}</span>
            <span class="text-[10px] text-slate-400">关注</span>
          </NuxtLink>
          <NuxtLink to="/user/followers" class="flex flex-col items-center cursor-pointer hover:text-primary transition-colors">
            <span class="text-base md:text-lg font-bold text-slate-900">{{ userStore.userInfo?.followerCount ?? 0 }}</span>
            <span class="text-[10px] text-slate-400">粉丝</span>
          </NuxtLink>
        </div>

        <!-- 编辑资料按钮 -->
        <NuxtLink to="/user/edit" class="block w-[200px] mx-auto text-center py-1.5 text-[13px] font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-md transition-colors">
          编辑资料
        </NuxtLink>
      </div>

    </div>

    <!-- Tab切换 - 紧凑样式 -->
    <div class="flex items-center border-b border-slate-100 overflow-x-auto no-scrollbar mt-2">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="flex-1 md:flex-none px-2 md:px-4 py-2 text-[13px] md:text-sm font-medium border-b-2 whitespace-nowrap transition-colors"
        :class="activeTab === tab.key
          ? 'border-slate-900 text-slate-900'
          : 'border-transparent text-slate-400 hover:text-slate-600'"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab内容 -->
    <div>
      <!-- 作品 -->
      <ErrorRetry v-if="activeTab === 'published' && tabError && !publishedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'published' && !tabError">
        <!-- 骨架屏 -->
        <div v-if="loading && publishedArticles.length === 0" class="grid grid-cols-3 gap-0">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && publishedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm mb-2">还没有发布作品</p>
          <NuxtLink to="/editor" class="text-primary hover:underline text-sm">去创作</NuxtLink>
        </div>
        <div v-else class="grid grid-cols-3 gap-0">
          <div v-for="article in publishedArticles" :key="article.id" class="relative">
            <ArticleGridCard :article="article" :to="`/user/preview/${article.id}`" />
          </div>
        </div>
        <div v-if="hasMore" class="text-center py-3">
          <button class="text-[13px] text-slate-400 hover:text-slate-600" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 草稿 -->
      <ErrorRetry v-if="activeTab === 'drafts' && tabError && !draftArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'drafts' && !tabError">
        <!-- 骨架屏 -->
        <div v-if="loading && draftArticles.length === 0" class="grid grid-cols-3 gap-0">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && draftArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm mb-2">草稿箱为空</p>
          <p class="text-xs">草稿保存30天后将自动清理</p>
          <NuxtLink to="/editor" class="text-primary hover:underline text-sm mt-2 inline-block">去创作</NuxtLink>
        </div>
        <div v-else class="grid grid-cols-3 gap-0">
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
        <div v-if="hasMore" class="text-center py-3">
          <button class="text-[13px] text-slate-400 hover:text-slate-600" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 收藏 -->
      <ErrorRetry v-if="activeTab === 'collected' && tabError && !collectedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'collected' && !tabError">
        <div v-if="loading && collectedArticles.length === 0" class="grid grid-cols-3 gap-0">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && collectedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm">暂无收藏</p>
        </div>
        <div v-else class="grid grid-cols-3 gap-0">
          <ArticleGridCard v-for="article in collectedArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-3">
          <button class="text-[13px] text-slate-400 hover:text-slate-600" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 点赞 -->
      <ErrorRetry v-if="activeTab === 'liked' && tabError && !likedArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'liked' && !tabError">
        <div v-if="loading && likedArticles.length === 0" class="grid grid-cols-3 gap-0">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && likedArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm">暂无点赞</p>
        </div>
        <div v-else class="grid grid-cols-3 gap-0">
          <ArticleGridCard v-for="article in likedArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-3">
          <button class="text-[13px] text-slate-400 hover:text-slate-600" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>

      <!-- 评论 -->
      <ErrorRetry v-if="activeTab === 'comments' && tabError && !myComments.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'comments' && !tabError" class="px-3">
        <div v-for="comment in myComments" :key="comment.id" class="py-2.5 border-b border-slate-100">
          <p class="text-[13px] text-slate-700 leading-snug">{{ comment.content }}</p>
          <div class="flex items-center justify-between mt-1.5">
            <span class="text-[11px] text-slate-400">{{ formatDate(comment.createdAt) }}</span>
            <NuxtLink :to="`/articles/${comment.articleId}`" class="text-[11px] text-primary hover:text-primary-600">{{ '查看作品' }}</NuxtLink>
          </div>
        </div>
        <div v-if="!loading && myComments.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm">暂无评论</p>
        </div>
      </div>

      <!-- 历史 -->
      <ErrorRetry v-if="activeTab === 'history' && tabError && !historyArticles.length" :message="tabError" :on-retry="retryTabData" />
      <div v-if="activeTab === 'history' && !tabError">
        <div v-if="loading && historyArticles.length === 0" class="grid grid-cols-3 gap-0">
          <div v-for="i in 6" :key="i" class="aspect-[3/4] bg-slate-100 animate-pulse" />
        </div>
        <div v-else-if="!loading && historyArticles.length === 0" class="text-center py-10 text-slate-400">
          <p class="text-sm">暂无浏览历史</p>
        </div>
        <div v-else class="grid grid-cols-3 gap-0">
          <ArticleGridCard v-for="article in historyArticles" :key="article.id" :article="article" />
        </div>
        <div v-if="hasMore" class="text-center py-3">
          <button class="text-[13px] text-slate-400 hover:text-slate-600" :disabled="loading" @click="loadMore">
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
          <p class="text-sm text-slate-600 mb-3">作品「{{ scheduleTarget?.title || '无标题' }}」将在指定时间自动发布</p>
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
          <p class="text-sm text-slate-600 mb-4">确定要删除作品「{{ deleteTarget?.title || '无标题' }}」吗？此操作不可撤销。</p>
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
const { isMobile } = useBreakpoints()

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
    // 不手动设置 Content-Type，让 axios 自动设置带 boundary 的 multipart/form-data
    const uploadRes = await apiPost<any>('/files/upload/image', formData)
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
  { key: 'published', label: '作品' },
  { key: 'drafts', label: '草稿' },
  { key: 'collected', label: '收藏' },
  { key: 'liked', label: '点赞' },
  { key: 'comments', label: '评论' },
  { key: 'history', label: '历史' },
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

// 发布草稿（立即）
const publishDraftNow = async (article: Article) => {
  publishingId.value = article.id
  try {
    await articleApi.publishDraft(article.id)
    showToast('作品已发布')
    // 从草稿列表移除
    draftArticles.value = draftArticles.value.filter(a => a.id !== article.id)
    // 如果当前在作品Tab，刷新作品列表
    if (activeTab.value === 'published') {
      currentPage.value = 1
      await loadTabData()
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || '发布失败，请稍后重试', 'error')
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
    showToast(err?.response?.data?.message || '操作失败，请稍后重试', 'error')
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
    showToast('作品已删除')
    const id = deleteTarget.value.id
    publishedArticles.value = publishedArticles.value.filter(a => a.id !== id)
    draftArticles.value = draftArticles.value.filter(a => a.id !== id)
    showDeleteModal.value = false
    deleteTarget.value = null
  } catch (err: any) {
    showToast(err?.response?.data?.message || '删除失败，请稍后重试', 'error')
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
  // 获取完整个人资料（含统计数字）
  try {
    const { data: profileData } = await userApi.getProfile()
    const profile = profileData?.data
    if (profile) {
      userStore.updateProfile(profile)
    }
  } catch {
    // 静默失败，不影响主流程
  }
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
