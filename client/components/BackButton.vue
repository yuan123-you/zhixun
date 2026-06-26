<template>
  <!-- 全局返回按钮：非首页、非自带导航的页面显示 -->
  <ClientOnly>
    <template v-if="show">
      <!-- 移动端：迷你顶栏（返回箭头 + 页面标题），填充右侧空间不浪费 -->
      <div
        class="md:hidden fixed top-0 left-0 right-0 z-[60] h-12 flex items-center gap-2 px-2 bg-white/85 dark:bg-gray-800/85 backdrop-blur-md border-b border-slate-200/60 dark:border-gray-700/60"
      >
        <button
          class="w-9 h-9 flex items-center justify-center rounded-lg text-slate-700 dark:text-gray-200 hover:bg-slate-100 dark:hover:bg-gray-700 active:scale-95 transition no-tap-highlight touch-target shrink-0"
          aria-label="返回上一页"
          @click="goBack"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <!-- 页面标题：利用按钮右侧空间 -->
        <h1 class="text-base font-semibold text-slate-900 dark:text-gray-100 truncate flex-1 min-w-0">
          {{ pageTitle }}
        </h1>
      </div>

      <!-- 桌面端：浮动小圆按钮，贴左上角边缘 -->
      <Transition name="back-fade">
        <button
          class="back-button-desktop hidden md:flex fixed z-[60] w-10 h-10 items-center justify-center rounded-full bg-white/80 dark:bg-gray-800/80 backdrop-blur-md text-slate-700 dark:text-gray-200 shadow-md border border-slate-200/60 dark:border-gray-700/60 hover:bg-white dark:hover:bg-gray-700 active:scale-95 transition no-tap-highlight touch-target"
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
/** 全局返回按钮：移动端为迷你顶栏（返回箭头+标题），桌面端为浮动小圆按钮；首页隐藏 */
const route = useRoute()
const router = useRouter()

// 是否显示（首页、自带返回导航的页面隐藏，避免遮挡/双重按钮）
const show = computed(() => {
  if (route.path === '/') return false
  // 带自己返回按钮的页面：私信详情、群聊、他人关注/粉丝列表
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  if (/^\/user\/\d+\/(followers|following)/.test(route.path)) return false
  return true
})

// 页面标题：优先取页面设置的 document.title，否则按路由推断兜底
const fallbackTitle = computed(() => {
  const path = route.path
  // 精确匹配常见页面
  const exact: Record<string, string> = {
    '/discover': '发现',
    '/rank': '热榜',
    '/topics': '话题广场',
    '/groups': '群组广场',
    '/search': '搜索',
    '/tags': '标签',
    '/editor': '创作',
    '/notifications': '消息',
    '/messages': '消息',
    '/user': '我的主页',
    '/user/settings': '设置',
    '/user/edit': '编辑资料',
    '/user/articles': '我的作品',
    '/user/following': '关注',
    '/user/followers': '粉丝',
  }
  if (exact[path]) return exact[path]
  if (path.startsWith('/articles/')) return '作品详情'
  if (path.startsWith('/topics/')) return '话题'
  if (path.startsWith('/groups/')) return '群组'
  if (path.startsWith('/category/')) return '分类'
  if (path.startsWith('/user/')) return '个人主页'
  return '返回'
})

const pageTitle = ref('')

const updateTitle = () => {
  if (import.meta.client) {
    // document.title 形如 "发现 - 知讯"，去掉站点后缀
    const t = (document.title || '').replace(/\s*[-–—]\s*知讯.*$/, '').trim()
    pageTitle.value = t || fallbackTitle.value
  } else {
    pageTitle.value = fallbackTitle.value
  }
}

watch(() => route.path, updateTitle, { immediate: true })
onMounted(() => {
  updateTitle()
  // title 可能在路由完成后异步更新，延迟再同步一次
  const stop = watch(() => (import.meta.client ? document.title : ''), () => updateTitle())
  onUnmounted(stop)
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
/* 桌面端浮动按钮贴左上角边缘 */
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
