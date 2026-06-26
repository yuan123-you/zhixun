<template>
  <!-- 默认布局：顶部导航栏 + 主内容区 + 平板侧边栏 + 移动端底部Tab导航 -->
  <div class="min-h-screen bg-[var(--color-bg)] dark:bg-gray-900">
    <!-- 页面切换进度条 -->
    <NuxtLoadingIndicator color="#6366f1" :height="3" :throttle="200" />

    <!-- 网络状态检测 -->
    <NetworkStatus />

    <!-- 全局浮动返回按钮（左上角最边缘，非首页显示） -->
    <BackButton />

    <!-- 顶部导航栏（仅"我的"页面显示） -->
    <AppHeader v-if="showHeader" />

    <!-- 平板端侧边栏（768-1023px） -->
    <ClientOnly>
      <aside
        v-if="isTablet"
        class="tablet-sidebar fixed left-0 bottom-0 w-[260px] z-40 bg-white dark:bg-gray-800 border-r border-slate-200 dark:border-gray-700 overflow-y-auto transition-transform duration-300"
        :class="[isTabletSidebarOpen ? 'translate-x-0' : '-translate-x-full', isBackButtonVisible ? 'top-12' : 'top-0', showHeader ? 'md:top-16' : 'md:top-0']"
      >
        <div class="p-3 space-y-3">
          <!-- 侧边栏导航 -->
          <nav class="space-y-1">
            <NuxtLink to="/" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 hover:bg-slate-50 dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              <span>首页</span>
            </NuxtLink>
            <NuxtLink to="/rank" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/rank') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 hover:bg-slate-50 dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
              </svg>
              <span>排行</span>
            </NuxtLink>
            <NuxtLink to="/topics" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/topics') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 hover:bg-slate-50 dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
              </svg>
              <span>话题</span>
            </NuxtLink>
            <NuxtLink to="/groups" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/groups') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 hover:bg-slate-50 dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <span>群组</span>
            </NuxtLink>
            <ClientOnly>
              <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/editor') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-slate-700 hover:bg-slate-50 dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                <span>创作</span>
              </NuxtLink>
            </ClientOnly>
          </nav>

          <!-- 分类 -->
          <div class="border-t border-slate-200 dark:border-gray-700 pt-3">
            <h3 class="px-3 text-xs font-semibold text-slate-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">分类</h3>
            <div class="space-y-1">
              <NuxtLink to="/category/tech" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-slate-600 dark:text-gray-400 dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
                <span class="w-2 h-2 rounded-full bg-blue-500"></span>
                <span>技术</span>
              </NuxtLink>
              <NuxtLink to="/category/design" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-slate-600 dark:text-gray-400 dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
                <span class="w-2 h-2 rounded-full bg-purple-500"></span>
                <span>设计</span>
              </NuxtLink>
              <NuxtLink to="/category/product" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-slate-600 dark:text-gray-400 dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
                <span class="w-2 h-2 rounded-full bg-green-500"></span>
                <span>产品</span>
              </NuxtLink>
            </div>
          </div>

          <!-- 热门标签 -->
          <div class="border-t border-slate-200 dark:border-gray-700 pt-3">
            <h3 class="px-3 text-xs font-semibold text-slate-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">热门标签</h3>
            <div class="flex flex-wrap gap-1.5 px-3">
              <NuxtLink to="/tags" class="badge-primary cursor-pointer no-tap-highlight">技术</NuxtLink>
              <NuxtLink to="/tags" class="badge-secondary cursor-pointer no-tap-highlight">Vue</NuxtLink>
              <NuxtLink to="/tags" class="badge-accent cursor-pointer no-tap-highlight">AI</NuxtLink>
            </div>
          </div>
        </div>
      </aside>
    </ClientOnly>

    <!-- 平板侧边栏遮罩 -->
    <ClientOnly>
      <Transition name="fade">
        <div
          v-if="isTablet && isTabletSidebarOpen"
          class="fixed inset-0 bg-black/30 z-39"
          :class="[isBackButtonVisible ? 'top-12' : 'top-0', showHeader ? 'md:top-16' : 'md:top-0']"
          @click="closeTabletSidebar"
        />
      </Transition>
    </ClientOnly>

    <!-- 主内容区 -->
    <main
      class="pb-16 md:pb-0"
      :class="mainTopPadding"
      style="transition: padding-left 0.3s ease;"
      :style="isTablet && isTabletSidebarOpen ? 'padding-left: 260px' : ''"
    >
      <slot />
    </main>

    <!-- 平板端侧边栏切换按钮 -->
    <ClientOnly>
      <button
        v-if="isTablet"
        class="fixed left-4 bottom-6 z-50 w-12 h-12 bg-primary text-white rounded-full shadow-lg flex items-center justify-center touch-target no-tap-highlight touch-feedback"
        :class="isTabletSidebarOpen ? 'left-[276px]' : 'left-4'"
        @click="toggleTabletSidebar"
      >
        <svg v-if="!isTabletSidebarOpen" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
        <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </ClientOnly>

    <!-- 返回顶部 -->
    <BackToTop />

    <!-- 移动端底部Tab导航栏 -->
    <MobileNav />

    <!-- 方向锁定提示 -->
    <Transition name="fade">
      <div v-if="showOrientationPrompt" class="orientation-prompt-overlay" @click.self="dismissOrientationPrompt">
        <div class="orientation-prompt-card">
          <div class="orientation-prompt-icon">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-slate-900 mb-2">旋转屏幕</h3>
          <p class="text-sm text-slate-500 mb-4">
            {{ desiredOrientation === 'landscape' ? '横屏观看体验更佳' : '竖屏浏览体验更佳' }}
          </p>
          <button class="btn-primary w-full" @click="dismissOrientationPrompt">
            我知道了
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/** 默认布局：顶部导航栏 + 主内容区 + 平板侧边栏 + 移动端底部Tab导航 */

const route = useRoute()
const userStore = useUserStore()
const { isTablet } = useBreakpoints()
const { showOrientationPrompt, dismissOrientationPrompt, promptOrientationLock, desiredOrientation } = useOrientation()

// 仅"我的"页面显示顶部导航栏
const showHeader = computed(() => route.path.startsWith('/user'))

// 是否首页（首页不显示返回按钮，无需为按钮留顶部空间）
const isHomePage = computed(() => route.path === '/')

// 全局返回按钮是否可见（与 BackButton.vue 中的逻辑保持一致）
const isBackButtonVisible = computed(() => {
  if (route.path === '/') return false
  // 带自己返回按钮的页面：私信详情、群聊、他人关注/粉丝列表
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  if (/^\/user\/\d+\/(followers|following)/.test(route.path)) return false
  return true
})

// 主内容区顶部内边距：
// - /user 页面：移动端 AppHeader(48) + 可选返回顶栏(48)，桌面端 AppHeader(64) 留空间；
// - 其他页面：仅在全局返回按钮可见时为其顶栏留空间，桌面端贴顶
const mainTopPadding = computed(() => {
  if (showHeader.value) {
    // /user 页面：移动端 AppHeader 48px + 可选 BackButton 48px
    return isBackButtonVisible.value ? 'pt-24 md:pt-16' : 'pt-12 md:pt-16'
  }
  // 其他页面：仅在返回按钮可见时为移动端顶栏留 48px 空间
  if (isBackButtonVisible.value) return 'pt-12 md:pt-0'
  return ''
})

// 启动浏览历史自动同步
const { startAutoSync } = useViewHistory()
onMounted(() => {
  startAutoSync()
})

// 平板侧边栏状态
const isTabletSidebarOpen = ref(false)

// 切换平板侧边栏
const toggleTabletSidebar = () => {
  isTabletSidebarOpen.value = !isTabletSidebarOpen.value
}

// 关闭平板侧边栏
const closeTabletSidebar = () => {
  isTabletSidebarOpen.value = false
}

// 判断当前路由是否激活
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// 横屏时自动展开侧边栏
const { isLandscape } = useBreakpoints()
watch(isLandscape, (val) => {
  if (isTablet.value && val) {
    isTabletSidebarOpen.value = true
  }
}, { immediate: true })
</script>
