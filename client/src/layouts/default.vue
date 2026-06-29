<template>
  <!-- 默认布局：顶部导航栏 + 主内容区 + 平板侧边栏 + 移动端底部Tab导航 -->
  <div class="flex flex-col min-h-dvh-screen bg-[var(--color-bg)] dark:bg-gray-900">
    <!-- 网络状态检测 -->
    <NetworkStatus />

    <!-- 全局浮动返回按钮（左上角最边缘，非首页显示） -->
    <BackButton />

    <!-- 顶部导航栏（仅"我的"页面显示） -->
    <AppHeader v-if="showHeader" />

    <!-- 平板端侧边栏（768-1023px） -->
    <aside
      v-if="isTablet && isMounted"
      class="tablet-sidebar fixed left-0 bottom-0 w-[260px] z-40 bg-[var(--zh-bg-elevated)] dark:bg-gray-800 border-r border-[var(--zh-border)] dark:border-gray-700 overflow-y-auto transition-transform duration-300"
      :class="[isTabletSidebarOpen ? 'translate-x-0' : '-translate-x-full', isBackButtonVisible ? 'top-10' : 'top-0', showHeader ? 'md:top-16' : 'md:top-0']"
    >
      <div class="p-3 space-y-3">
        <!-- 侧边栏导航 -->
        <nav class="space-y-1">
          <RouterLink to="/" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span>首页</span>
          </RouterLink>
          <RouterLink to="/rank" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/rank') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
            </svg>
            <span>排行</span>
          </RouterLink>
          <RouterLink to="/topics" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/topics') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
            </svg>
            <span>话题</span>
          </RouterLink>
          <RouterLink to="/groups" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/groups') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <span>群组</span>
          </RouterLink>
          <template v-if="isMounted">
            <RouterLink v-if="userStore.isLoggedIn" to="/editor" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors no-tap-highlight touch-target" :class="isActive('/editor') ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300' : 'text-[var(--zh-text-secondary)] hover:bg-[var(--zh-bg-hover)] dark:text-gray-300 dark:hover:bg-gray-700'" @click="closeTabletSidebar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              <span>创作</span>
            </RouterLink>
          </template>
        </nav>

        <!-- 分类 -->
        <div class="border-t border-[var(--zh-border)] dark:border-gray-700 pt-3">
          <h3 class="px-3 text-xs font-semibold text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] uppercase tracking-wider mb-1.5">分类</h3>
          <div class="space-y-1">
            <RouterLink to="/category/tech" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
              <span class="w-2 h-2 rounded-full bg-blue-500"></span>
              <span>技术</span>
            </RouterLink>
            <RouterLink to="/category/design" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
              <span class="w-2 h-2 rounded-full bg-purple-500"></span>
              <span>设计</span>
            </RouterLink>
            <RouterLink to="/category/product" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] dark:hover:bg-gray-700 no-tap-highlight touch-target" @click="closeTabletSidebar">
              <span class="w-2 h-2 rounded-full bg-green-500"></span>
              <span>产品</span>
            </RouterLink>
          </div>
        </div>

        <!-- 热门标签 -->
        <div class="border-t border-[var(--zh-border)] dark:border-gray-700 pt-3">
          <h3 class="px-3 text-xs font-semibold text-[var(--zh-text-secondary)] dark:text-[var(--zh-text-tertiary)] uppercase tracking-wider mb-1.5">热门标签</h3>
          <div class="flex flex-wrap gap-1.5 px-3">
            <RouterLink to="/tags" class="badge-primary cursor-pointer no-tap-highlight">技术</RouterLink>
            <RouterLink to="/tags" class="badge-secondary cursor-pointer no-tap-highlight">Vue</RouterLink>
            <RouterLink to="/tags" class="badge-accent cursor-pointer no-tap-highlight">AI</RouterLink>
          </div>
        </div>
      </div>
    </aside>

    <!-- 平板侧边栏遮罩 -->
    <Transition name="fade" v-if="isMounted">
      <div
        v-if="isTablet && isTabletSidebarOpen"
        class="fixed inset-0 bg-black/30 z-39"
        :class="[isBackButtonVisible ? 'top-10' : 'top-0', showHeader ? 'md:top-16' : 'md:top-0']"
        @click="closeTabletSidebar"
      />
    </Transition>

    <!-- 主内容区 -->
    <main
      class="flex-1 pb-[calc(3.125rem+env(safe-area-inset-bottom,0px))] md:pb-0"
      :class="mainTopPadding"
      :style="mainStyle"
    >
      <!--
        使用 <Transition> 包裹 router-view，实现页面切换平滑过渡。
        不使用 mode="out-in" 以避免旧页面离开后、新页面进入前的空白间隙。
        改用 CSS absolute 定位，确保两个页面在过渡期间重叠，避免白屏。

        关键修复：使用 :key="r.path" 而非 :key="r.fullPath"。
        - r.fullPath 包含 query 和 hash，任何 URL 参数变化都会强制重新挂载整个页面，
          导致所有 onMounted 重新执行、组件 state 全部清空、过渡动画重新触发，
          用户感知为"页面莫名其妙地自动刷新"。
        - r.path 仅在路由路径变化时变化；同一页面内 query/hash 变化不会触发重载，
          Vue 会自动响应 props/查询参数变化，与 SPA 单页应用的预期行为一致。
      -->
      <router-view v-slot="{ Component, route: r }">
        <Transition name="page-fade" @before-leave="onPageLeave" @after-enter="onPageEnter">
          <component :is="Component" :key="r.path" />
        </Transition>
      </router-view>
    </main>

    <!-- 平板端侧边栏切换按钮 -->
    <button
      v-if="isTablet && isMounted"
      class="fixed left-4 bottom-6 z-50 w-12 h-12 bg-[var(--color-primary)] text-white rounded-full shadow-lg flex items-center justify-center touch-target no-tap-highlight touch-feedback"
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

    <!-- 返回顶部 -->
    <BackToTop />

    <!-- @提及通知弹窗 -->
    <MentionNotification />

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
          <h3 class="text-lg font-semibold text-[var(--zh-text)] mb-2">旋转屏幕</h3>
          <p class="text-sm text-[var(--zh-text-secondary)] mb-4">
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
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const { isTablet } = useBreakpoints()
const { showOrientationPrompt, dismissOrientationPrompt, promptOrientationLock, desiredOrientation } = useOrientation()

// 客户端挂载标识（替代 ClientOnly）
const isMounted = ref(false)
onMounted(() => { isMounted.value = true })

// 仅"我的"页面显示顶部导航栏
const showHeader = computed(() => route.path.startsWith('/user'))

// 全局返回按钮是否可见
const isBackButtonVisible = computed(() => {
  const tabPages = ['/', '/discover', '/editor', '/notifications', '/user']
  if (tabPages.includes(route.path)) return false
  // 所有 /user/* 路径由 AppHeader 内部提供返回按钮
  if (route.path.startsWith('/user')) return false
  if (/^\/articles\/\d+/.test(route.path)) return false
  if (route.path === '/rank') return false
  if (route.path === '/tags') return false
  if (route.path === '/topics') return false
  if (route.path === '/search') return false
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  return true
})

// 主内容区顶部内边距
const mainTopPadding = computed(() => {
  if (showHeader.value) {
    return 'pt-12 md:pt-16'
  }
  if (isBackButtonVisible.value) return 'pt-10 md:pt-0'
  return ''
})

// 主内容区动态样式（仅平板侧边栏交互时启用过渡，避免 hydration 中间帧）
const mainStyle = computed(() => {
  const base: Record<string, string> = {}
  // 仅在客户端挂载且是平板模式时启用过渡动画
  if (isMounted.value && isTablet.value) {
    base.transition = 'padding-left 0.3s ease'
  }
  if (isTablet.value && isTabletSidebarOpen.value) {
    base.paddingLeft = '260px'
  }
  return base
})

// 平板侧边栏状态
const isTabletSidebarOpen = ref(false)

const toggleTabletSidebar = () => {
  isTabletSidebarOpen.value = !isTabletSidebarOpen.value
}

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

// 页面过渡事件处理
const onPageLeave = (el: Element) => {
  // 离开动画开始时，确保旧页面使用绝对定位，新页面在下方正常渲染
  const htmlEl = el as HTMLElement
  htmlEl.style.position = 'absolute'
  htmlEl.style.width = '100%'
}
const onPageEnter = (el: Element) => {
  // 进入动画完成后，清理绝对定位
  const htmlEl = el as HTMLElement
  htmlEl.style.position = ''
  htmlEl.style.width = ''
}
</script>
