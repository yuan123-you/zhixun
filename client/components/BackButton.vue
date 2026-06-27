<template>
  <!-- 全局返回按钮：非首页+非tab页显示 -->
  <ClientOnly>
    <template v-if="show">
      <!-- 移动端：迷你顶栏（返回箭头 + 页面标题，垂直居中） -->
      <div
        class="md:hidden fixed top-0 left-0 right-0 z-[60] h-10 flex items-center px-1.5 bg-white/85 dark:bg-gray-800/85 backdrop-blur-md border-b border-slate-200/60 dark:border-gray-700/60"
      >
        <button
          class="w-8 h-8 flex items-center justify-center text-slate-700 dark:text-gray-200 hover:text-primary dark:hover:text-primary-400 active:scale-95 transition no-tap-highlight touch-target shrink-0"
          aria-label="返回上一页"
          @click="goBack"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <span v-if="pageTitle" class="ml-1 text-sm font-medium text-slate-900 dark:text-white truncate">{{ pageTitle }}</span>
      </div>

      <!-- 桌面端：仅箭头，贴左上角边缘 -->
      <Transition name="back-fade">
        <button
          class="back-button-desktop hidden md:flex fixed z-[60] w-9 h-9 items-center justify-center text-slate-500 dark:text-gray-400 hover:text-slate-700 dark:hover:text-gray-200 active:scale-95 transition no-tap-highlight touch-target"
          aria-label="返回上一页"
          @click="goBack"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      </Transition>
    </template>
  </ClientOnly>
</template>

<script setup lang="ts">
/** 全局返回按钮：移动端为迷你顶栏（返回箭头 + 页面标题），桌面端为浮动箭头；tab页隐藏 */
const route = useRoute()
const router = useRouter()
const { title: pageTitle } = usePageHeaderTitle()

// 是否显示（首页 + 5个tab页 + 自带返回导航的页面隐藏）
const show = computed(() => {
  // 首页和5个tabbar页面隐藏
  const tabPages = ['/', '/discover', '/editor', '/notifications', '/user']
  if (tabPages.includes(route.path)) return false
  // 带自己返回按钮的页面：私信详情、群聊、他人关注/粉丝列表
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  if (/^\/user\/\d+\/(followers|following)/.test(route.path)) return false
  return true
})

// 返回上一页；无历史记录则回首页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}
</script>

<style scoped>
/* 桌面端浮动箭头贴左上角边缘 */
.back-button-desktop {
  top: 0.5rem;
  left: 0.5rem;
}

.back-fade-enter-active,
.back-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.back-fade-enter-from,
.back-fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}
</style>
