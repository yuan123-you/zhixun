<template>
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto py-1.5">
    <PullToRefresh :on-refresh="() => handleRefresh()" :error="error || undefined">
    <div class="flex gap-6">
      <!-- 左侧主内容区 -->
      <div class="flex-1 min-w-0">
        <!-- Tab切换 - 移动端紧凑布局确保单行显示 -->
        <div class="flex items-center border-b border-slate-200 overflow-x-auto no-scrollbar">
          <!-- 汉堡菜单按钮 - 移动端，位于推荐文字左侧 -->
          <button
            class="md:hidden p-1.5 mr-0.5 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg shrink-0"
            @click="toggleMobileMenu"
            aria-label="菜单"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="text-sm font-medium border-b-2 transition-colors whitespace-nowrap no-tap-highlight touch-target px-2 py-2"
            :class="activeTab === tab.key
                ? 'border-primary text-primary'
                : 'border-transparent text-slate-500 hover:text-slate-700'"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>

          <!-- 推荐Tab的"换一批"刷新按钮 - 移动端仅显示图标 -->
          <button
            v-if="activeTab === 'recommend'"
            class="ml-auto flex items-center gap-1 text-sm text-primary hover:text-primary-dark transition-colors rounded-full hover:bg-primary-50/50 no-tap-highlight shrink-0 px-2 py-1"
            :disabled="refreshing"
            @click="handleRefresh"
          >
            <svg
              class="w-4 h-4 transition-transform"
              :class="{ 'animate-spin': refreshing }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>{{ refreshing ? '刷新中' : '换一批' }}</span>
          </button>
        </div>

        <!-- 作品列表 - 平板端双列 -->
        <ArticleList
          :articles="articles"
          :loading="loading"
          :has-more="hasMore"
          :error="error"
          @load-more="loadMore"
          @retry="handleRetry"
        />
      </div>
    </div>
    </PullToRefresh>

    <!-- 汉堡菜单面板（Teleport 到 body 以避免父容器裁剪） -->
    <Teleport to="body">
      <Transition name="mobile-menu">
        <div
          v-if="showMobileMenu"
          class="md:hidden fixed inset-0 z-50"
          @click.self="showMobileMenu = false"
          @touchmove.prevent
        >
          <!-- 遮罩层 -->
          <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="showMobileMenu = false" />
          <!-- 菜单面板 - 阻止事件冒泡以允许内部滚动 -->
          <div
            class="relative bg-white dark:bg-gray-900 border-t border-slate-100 dark:border-gray-700 shadow-xl max-h-screen overflow-y-auto animate-slide-down overscroll-contain"
            @touchmove.stop
            @wheel.stop
          >
            <nav class="py-2">
              <!-- 主导航 -->
              <div class="px-3 py-2">
                <NuxtLink to="/" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                  </svg>
                  <span>首页</span>
                </NuxtLink>
                <NuxtLink to="/discover" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/discover') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                  </svg>
                  <span>发现</span>
                </NuxtLink>
                <NuxtLink to="/rank" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/rank') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                  </svg>
                  <span>排行</span>
                </NuxtLink>
                <NuxtLink to="/search" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/search') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <span>搜索</span>
                </NuxtLink>
                <NuxtLink to="/tags" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/tags') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                  </svg>
                  <span>标签</span>
                </NuxtLink>
                <NuxtLink to="/topics" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/topics') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
                  </svg>
                  <span>话题</span>
                </NuxtLink>
                <NuxtLink to="/groups" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/groups') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  <span>群组</span>
                </NuxtLink>
              </div>

              <!-- 个人功能（已登录） -->
              <ClientOnly>
                <template v-if="userStore.isLoggedIn">
                  <div class="border-t border-slate-100 dark:border-gray-700 mx-3 my-1"></div>
                  <div class="px-3 py-2">
                    <h3 class="px-3 text-xs font-semibold text-slate-400 dark:text-gray-500 uppercase tracking-wider mb-1">个人</h3>
                    <NuxtLink to="/editor" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/editor') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                      <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                      <span>创作</span>
                    </NuxtLink>
                    <NuxtLink to="/notifications" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/notifications') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                      <div class="relative shrink-0">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                        </svg>
                        <span v-if="notificationStore.unreadCount > 0" class="absolute -top-1 -right-1 min-w-[1rem] h-3.5 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-0.5 leading-none">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
                      </div>
                      <span>消息</span>
                    </NuxtLink>
                    <NuxtLink to="/user" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                      <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                      <span>个人中心</span>
                    </NuxtLink>
                    <NuxtLink to="/user/settings" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user/settings') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-800 active:scale-[0.98]'" @click="showMobileMenu = false">
                      <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span>设置</span>
                    </NuxtLink>
                  </div>
                </template>

                <!-- 未登录 -->
                <template v-else>
                  <div class="border-t border-slate-100 dark:border-gray-700 mx-3 my-1"></div>
                  <div class="px-3 py-2">
                    <NuxtLink to="/login" class="flex items-center justify-center gap-2 mx-3 py-3 rounded-xl text-sm font-medium bg-primary text-white transition-all duration-200 min-h-[44px] active:scale-[0.98]" @click="showMobileMenu = false">
                      <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                      </svg>
                      <span>登录 / 注册</span>
                    </NuxtLink>
                  </div>
                </template>
              </ClientOnly>
            </nav>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/** 首页：推荐/热门/最新/关注四个Tab，作品卡片列表 */

import type { Article, PageResult, ApiResponse } from '~/types'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const config = useRuntimeConfig()
const { defaultSort: savedDefaultSort } = useLocalSettings()
const route = useRoute()

// ===== 汉堡菜单状态与滚动锁定 =====
const showMobileMenu = ref(false)

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

// 判断菜单项是否激活
const isMenuActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// 打开菜单时锁定 body 滚动，防止穿透到底层页面
watch(showMobileMenu, (val) => {
  if (import.meta.client) {
    if (val) {
      document.body.style.overflow = 'hidden'
      // 记录当前滚动位置，防止 iOS Safari 的橡皮筋效果
      document.body.style.position = 'fixed'
      document.body.style.width = '100%'
    } else {
      document.body.style.overflow = ''
      document.body.style.position = ''
      document.body.style.width = ''
    }
  }
})

// 路由变化时关闭菜单
watch(() => route.path, () => {
  if (showMobileMenu.value) {
    showMobileMenu.value = false
  }
})

// 组件卸载时恢复 body 样式
onUnmounted(() => {
  if (import.meta.client) {
    document.body.style.overflow = ''
    document.body.style.position = ''
    document.body.style.width = ''
  }
})
// ===== 汉堡菜单结束 =====

// Tab配置
const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'hot', label: '排行' },
  { key: 'latest', label: '最新' },
  { key: 'following', label: '关注' },
]

// 初始Tab：SSR 时固定为 'recommend' 避免 hydration mismatch，
// 客户端 hydration 稳定后再根据 localStorage 中保存的默认排序调整
const activeTab = ref('recommend')

// 客户端挂载后同步用户本地设置的默认排序
// 使用 requestIdleCallback 将 Tab 切换推迟到浏览器空闲时，
// 此时所有 hydration 相关的 DOM 变化、CSS 过渡均已稳定，
// pre-hydration 类也已被移除，避免切换时的内容闪烁和布局错乱。
onMounted(() => {
  const tabMap: Record<string, string> = {
    latest: 'latest',
    hot: 'hot',
  }
  const targetTab = tabMap[savedDefaultSort.value] || 'recommend'
  if (activeTab.value !== targetTab) {
    const switchAndFetch = () => {
      activeTab.value = targetTab
      fetchArticles()
    }
    // 优先使用 requestIdleCallback（浏览器空闲时才执行），
    // 确保 hydration 完成、pre-hydration 已移除后再切换 Tab
    if (typeof requestIdleCallback !== 'undefined') {
      requestIdleCallback(switchAndFetch, { timeout: 800 })
    } else {
      setTimeout(switchAndFetch, 500)
    }
  }
})
const articles = ref<Article[]>([])
const loading = ref(false)
const hasMore = ref(true)
const error = ref<string | null>(null)
const page = ref(1)
const refreshKey = ref('')

// 构建API基础URL：SSR时使用内部地址，客户端时走Nginx代理
const getApiBase = () => {
  return import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)
}

// 通用分页数据获取 — 使用 useApi 以支持 Token 自动刷新和 CSRF 防护
const fetchPage = async (url: string, params: Record<string, any> = {}) => {
  const api = useApi()
  const res = await api.get<PageResult<Article>>(url, { page: params.page || 1, pageSize: params.pageSize || 10, ...params })
  return res.data.data
}

// 切换Tab
const switchTab = (key: string) => {
  if (activeTab.value === key) return
  activeTab.value = key
  articles.value = []
  page.value = 1
  hasMore.value = true
  error.value = null
  // 切换到推荐Tab时重置refreshKey
  if (key === 'recommend') {
    refreshKey.value = ''
  }
  // 切换Tab时失效对应的feed缓存
  const { invalidateByPrefix } = useRequestCache()
  const feedPrefixMap: Record<string, string> = {
    recommend: '/feed/recommend',
    hot: '/feed/hot',
    latest: '/feed/latest',
    following: '/feed/following',
  }
  const prefix = feedPrefixMap[key]
  if (prefix) {
    invalidateByPrefix(prefix)
  }
  fetchArticles()
}

// 下拉/换一批刷新：按当前 Tab 分发，确保推荐走算法换批、其余 Tab 走各自接口重拉
// 修复：此前所有 Tab 下拉刷新都调推荐接口，导致"最新"刷新后内容变成推荐排序、不再按时间排序
const { loading: refreshing, refresh: handleRefresh } = useRefresh({
  onRefresh: async () => {
    error.value = null
    page.value = 1
    hasMore.value = true

    // 推荐 Tab：调用算法推荐接口并 refresh=1 换一批新内容
    if (activeTab.value === 'recommend') {
      const api = useApi()
      const res = await api.get<PageResult<Article>>('/feed/recommend', {
        page: 1,
        pageSize: 10,
        refresh: 1,
      })
      const data = res.data.data
      const items = data?.list || []
      articles.value = items
      refreshKey.value = (data as any)?.refresh_key || ''
      hasMore.value = items.length >= 10
      return
    }

    // 其余 Tab（最新/热门/关注）：重置到第一页，走各自接口重新拉取
    // 最新 Tab 走 /feed/latest，保持按发布时间倒序；不再误调推荐接口
    await fetchArticles()
  },
  debounceMs: 300,
  showError: true,
  errorMessage: undefined, // 使用默认错误消息
  onError: () => {
    hasMore.value = false
  },
})

// 获取作品列表
const fetchArticles = async () => {
  if (loading.value) return

  // 关注tab需要登录
  if (activeTab.value === 'following' && !userStore.isLoggedIn) {
    articles.value = []
    hasMore.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    const pageSize = 10
    let url: string
    let extraParams: Record<string, any> = {}
    switch (activeTab.value) {
      case 'latest':
        url = '/feed/latest'
        break
      case 'following':
        url = '/feed/following'
        break
      case 'hot':
        url = '/feed/hot'
        break
      default:
        url = '/feed/recommend'
        // 推荐Tab翻页时携带refresh_key
        if (refreshKey.value) {
          extraParams.refresh_key = refreshKey.value
        }
    }

    const data = await fetchPage(url, { page: page.value, pageSize, ...extraParams })
    const items = data?.list || []
    if (page.value === 1) {
      articles.value = items
    } else {
      articles.value.push(...items)
    }
    // 首次加载推荐时保存refresh_key
    if (activeTab.value === 'recommend' && page.value === 1) {
      refreshKey.value = (data as any)?.refresh_key || ''
    }
    hasMore.value = items.length >= pageSize
  } catch {
    error.value = '加载失败，请稍后重试'
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = async () => {
  const prevPage = page.value
  page.value++
  try {
    await fetchArticles()
  } catch {
    // 加载失败，回退页码
    page.value = prevPage
  }
}

// 重试上次失败的请求
const handleRetry = () => {
  error.value = null
  fetchArticles()
}

// SSR数据获取 - 使用单个 useAsyncData + Promise.all 并行请求，避免串行等待
const { data: homeData } = await useAsyncData('home-init', async () => {
  const base = getApiBase()
  const headers = import.meta.server ? { 'X-SSR-Request': 'true' } : {}

  const feedRes = await $fetch<ApiResponse<PageResult<Article>>>(`${base}/feed/recommend`, {
    params: { page: 1, pageSize: 10 },
    headers,
  }).catch(() => null)

  return {
    feed: feedRes?.data?.list || [],
    refreshKey: (feedRes?.data as any)?.refresh_key || '',
  }
}, {
  default: () => ({ feed: [] as Article[], refreshKey: '' }),
  // 客户端缓存：60秒内使用缓存，避免导航回首页时重复请求
  // 如果缓存的feed为空（SSR失败场景），跳过缓存让客户端重新请求
  getCachedData(key, nuxtApp) {
    const cached = nuxtApp.payload.data[key] || nuxtApp.static.data[key]
    if (!cached) return undefined
    if (!cached.feed || cached.feed.length === 0) return undefined
    const cacheTimestamp = (nuxtApp.payload as any)._homeInitTimestamp || 0
    const now = Date.now()
    if (now - cacheTimestamp > 60 * 1000) {
      // 缓存过期但仍返回缓存数据以避免水合不匹配
      // 客户端将在 onMounted 中重新请求
      return cached
    }
    return cached
  },
})

// 记录缓存时间戳，供 getCachedData 判断过期
if (import.meta.client) {
  const nuxtApp = useNuxtApp()
  if (!(nuxtApp.payload as any)._homeInitTimestamp) {
    ;(nuxtApp.payload as any)._homeInitTimestamp = Date.now()
  }
}

// 如果 SSR 数据已过期（超过60秒），客户端静默刷新数据
// 先使用缓存数据保证水合一致，再异步请求最新数据
if (import.meta.client) {
  const cacheTimestamp = (useNuxtApp().payload as any)._homeInitTimestamp || 0
  if (Date.now() - cacheTimestamp > 60 * 1000) {
    nextTick(() => { fetchArticles() })
  }
}

articles.value = homeData.value.feed
refreshKey.value = homeData.value.refreshKey

// 页面元信息
useHead({
  title: '知讯 - 优质内容平台',
})
</script>
