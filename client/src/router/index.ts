import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { storage, STORAGE_KEYS } from '@/utils/storage'

// 布局组件（懒加载）
const DefaultLayout = () => import('@/layouts/default.vue')
const BlankLayout = () => import('@/layouts/blank.vue')

// 认证中间件：需要登录
const authGuard = (to: any, _from: any, next: Function) => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    // 显示登录提示
    showAuthToast()
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
}

// 游客中间件：已登录用户跳过登录/注册页
const guestGuard = (_to: any, _from: any, next: Function) => {
  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
}

// 登录过期 Toast
let authToastVisible = false
function showAuthToast() {
  if (authToastVisible) return
  authToastVisible = true
  const toast = document.createElement('div')
  toast.className = 'fixed top-4 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl shadow-lg text-white text-sm font-medium bg-amber-500 flex items-center gap-2'
  toast.innerHTML = '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>'
  const span = document.createElement('span')
  span.textContent = '请先登录后再访问'
  toast.appendChild(span)
  document.body.appendChild(toast)
  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translate(-50%, -20px)'
    toast.style.transition = 'all 0.3s'
    setTimeout(() => { toast.remove(); authToastVisible = false }, 300)
  }, 2000)
}

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      // 首页
      {
        path: '',
        name: 'Home',
        component: () => import('@/pages/index.vue'),
        meta: { title: '知讯' },
      },
      // 排行
      {
        path: 'rank',
        name: 'Rank',
        component: () => import('@/pages/rank/index.vue'),
        meta: { title: '排行榜' },
      },
      // 发现页
      {
        path: 'discover',
        name: 'Discover',
        component: () => import('@/pages/discover/index.vue'),
        meta: { title: '发现' },
      },
      // 标签页
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/pages/tags/index.vue'),
        meta: { title: '标签' },
      },
      // 分类页
      {
        path: 'category/:slug',
        name: 'Category',
        component: () => import('@/pages/category/[slug].vue'),
        meta: { title: '分类' },
      },
      // 搜索页
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/pages/search/index.vue'),
        meta: { title: '搜索' },
      },
      // 作品详情
      {
        path: 'articles/:id',
        name: 'ArticleDetail',
        component: () => import('@/pages/articles/[id].vue'),
        meta: { title: '作品详情' },
      },
      // 话题广场
      {
        path: 'topics',
        name: 'TopicsSquare',
        component: () => import('@/pages/topics/index.vue'),
        meta: { title: '话题广场' },
      },
      // 话题详情
      {
        path: 'topics/:id',
        name: 'TopicDetail',
        component: () => import('@/pages/topics/[id]/index.vue'),
        meta: { title: '话题详情' },
      },
      // 群组广场
      {
        path: 'groups',
        name: 'GroupsSquare',
        component: () => import('@/pages/groups/index.vue'),
        meta: { title: '群组广场' },
      },
      // 群组聊天
      {
        path: 'groups/:id',
        name: 'GroupChat',
        component: () => import('@/pages/groups/[id].vue'),
        meta: { title: '群聊', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 编辑器（需要登录）
      {
        path: 'editor',
        name: 'Editor',
        component: () => import('@/pages/editor/index.vue'),
        meta: { title: '创作', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 编辑器预览
      {
        path: 'editor/preview',
        name: 'EditorPreview',
        component: () => import('@/pages/editor/preview.vue'),
        meta: { title: '预览', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 通知中心
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/pages/notifications/index.vue'),
        meta: { title: '消息中心', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 私信首页
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/pages/messages/index.vue'),
        meta: { title: '私信', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 私信聊天
      {
        path: 'messages/:id',
        name: 'MessageChat',
        component: () => import('@/pages/messages/[id].vue'),
        meta: { title: '聊天', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 用户个人中心（需要登录）
      {
        path: 'user',
        name: 'UserProfile',
        component: () => import('@/pages/user/index.vue'),
        meta: { title: '个人中心', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 我的作品
      {
        path: 'user/articles',
        name: 'UserArticles',
        component: () => import('@/pages/user/articles.vue'),
        meta: { title: '我的作品', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 编辑资料
      {
        path: 'user/edit',
        name: 'UserEdit',
        component: () => import('@/pages/user/edit.vue'),
        meta: { title: '编辑资料', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 我的粉丝
      {
        path: 'user/followers',
        name: 'UserFollowers',
        component: () => import('@/pages/user/followers.vue'),
        meta: { title: '粉丝', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 我的关注
      {
        path: 'user/following',
        name: 'UserFollowing',
        component: () => import('@/pages/user/following.vue'),
        meta: { title: '关注', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 设置
      {
        path: 'user/settings',
        name: 'UserSettings',
        component: () => import('@/pages/user/settings.vue'),
        meta: { title: '设置', requiresAuth: true },
        beforeEnter: authGuard,
      },
      // 用户主页（动态参数）
      {
        path: 'user/:id',
        name: 'UserHome',
        component: () => import('@/pages/user/[id].vue'),
        meta: { title: '用户主页' },
      },
      // 用户预览作品
      {
        path: 'user/preview/:id',
        name: 'UserPreview',
        component: () => import('@/pages/user/preview/[id].vue'),
        meta: { title: '作品预览' },
      },
      // 用户粉丝列表
      {
        path: 'user/:id/followers',
        name: 'UserFollowersList',
        component: () => import('@/pages/user/[id]/followers.vue'),
        meta: { title: '粉丝列表' },
      },
      // 用户关注列表
      {
        path: 'user/:id/following',
        name: 'UserFollowingList',
        component: () => import('@/pages/user/[id]/following.vue'),
        meta: { title: '关注列表' },
      },
    ],
  },
  // 登录（空白布局，已登录用户自动跳转）
  {
    path: '/login',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/pages/login.vue'),
        meta: { title: '登录' },
        beforeEnter: guestGuard,
      },
    ],
  },
  // 注册
  {
    path: '/register',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Register',
        component: () => import('@/pages/register.vue'),
        meta: { title: '注册' },
        beforeEnter: guestGuard,
      },
    ],
  },
  // 忘记密码
  {
    path: '/forgot-password',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'ForgotPassword',
        component: () => import('@/pages/forgot-password.vue'),
        meta: { title: '找回密码' },
        beforeEnter: guestGuard,
      },
    ],
  },
  // 管理后台重定向
  {
    path: '/admin/:pathMatch(.*)*',
    redirect: () => {
      // 开发环境重定向到 admin 开发服务器，生产环境由 Nginx 代理
      if (import.meta.env.DEV) {
        window.location.href = 'http://localhost:3001/admin/' + (window.location.hash || '')
        return { path: '/' }
      }
      return { path: '/' }
    },
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory('/'),
  routes,
  // 滚动行为：合理处理页面切换时的滚动位置，避免出现"硬跳"或"白屏"
  // - 同 path + 不同 hash：滚动到锚点
  // - 切换到带 hash 的新页面：先尝试滚到锚点，失败则滚到顶
  // - 其他情况：保持原有滚动位置（避免每次切换都强制滚到顶造成视觉跳动）
  // 注意：返回 top:0 会导致页面在过渡动画期间瞬间跳到顶部，与 transition 动画叠加产生"白屏"。
  // 因此默认保留滚动位置，由 <Transition> 包裹的页面入场动画平滑承接。
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    // 同一路由仅 query/hash 变化：保留当前滚动位置，避免无谓跳动
    if (_from.path === to.path) {
      return false
    }
    // 显式指定 behavior:'instant'，覆盖 Tailwind CSS 3.4 preflight 的 scroll-behavior:smooth。
    // 否则 window.scrollTo 会触发 300~500ms 的平滑滚动，而页面过渡仅 180ms，
    // 导致旧页面在淡出过程中缓慢上移，视觉上"残留原内容"。
    // 'instant' 是浏览器实际支持的值，但 TS 的 ScrollBehavior 类型未包含，故用 as any。
    return { left: 0, top: 0, behavior: 'instant' as any }
  },
})

// 全局前置守卫：统一处理认证逻辑
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 确保 store 从 localStorage 恢复（首次路由导航时可能尚未调用 init）
  if (!userStore.token) {
    const savedAccessToken = storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN)
    if (savedAccessToken) {
      userStore.init()
    }
  }

  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录 → 跳转登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录用户访问登录/注册页 → 跳转首页
  const isGuestRoute = ['/login', '/register', '/forgot-password'].includes(to.path)
  if (isGuestRoute && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

// 全局路由守卫：设置页面标题
router.afterEach((to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = title + ' - 知讯'
  } else {
    document.title = '知讯 - 优质内容平台'
  }
})

export default router
