<template>
  <!-- 顶部导航栏 -->
  <header class="fixed top-0 left-0 right-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-lg border-b border-slate-200/60 dark:border-gray-700/60 shadow-[var(--shadow-sm)]">
    <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-6 h-12 md:h-16 flex items-center justify-between gap-2">
      <!-- 左侧：返回按钮/汉堡菜单（移动端）+ 品牌名 -->
      <div class="flex items-center gap-2 shrink-0">
        <!-- 返回按钮（非首页时显示） -->
        <button v-if="showBackButton" class="p-1.5 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg" @click="goBack">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <!-- 移动端汉堡菜单按钮（首页时显示） -->
        <button v-if="!showBackButton" class="md:hidden p-1.5 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg" @click="showMobileMenu = !showMobileMenu">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>


      </div>

      <!-- 导航链接（桌面端/平板端） -->
      <nav class="hidden md:flex items-center space-x-1 2xl:space-x-4">
        <NuxtLink to="/" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
          首页
        </NuxtLink>
        <NuxtLink to="/discover" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
          发现
        </NuxtLink>
        <NuxtLink to="/rank" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
          排行
        </NuxtLink>
        <ClientOnly>
          <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
            写文章
          </NuxtLink>
        </ClientOnly>
      </nav>

      <!-- 右侧：搜索 + 消息 + 用户 -->
      <div class="flex items-center space-x-1 md:space-x-3">
        <!-- 搜索图标 -->
        <NuxtLink to="/search" class="p-1.5 md:p-2 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg transition-colors" title="搜索">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </NuxtLink>

        <!-- 消息中心图标（已登录） -->
        <ClientOnly>
          <NuxtLink v-if="userStore.isLoggedIn" to="/notifications" class="relative p-1.5 md:p-2 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <span
              v-if="notificationStore.unreadCount > 0"
              class="absolute -top-0.5 -right-0.5 min-w-[1rem] h-4 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-1"
            >
              {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
            </span>
          </NuxtLink>

          <!-- 设置图标（已登录） -->
          <NuxtLink v-if="userStore.isLoggedIn" to="/user/settings" class="p-1.5 md:p-2 text-slate-600 dark:text-gray-300 hover:bg-slate-50 dark:hover:bg-gray-700 rounded-lg transition-colors" title="设置">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </NuxtLink>
        </ClientOnly>

        <!-- 用户头像菜单（已登录） / 登录按钮（未登录） -->
        <ClientOnly>
          <div v-if="userStore.isLoggedIn" class="relative">
            <div class="flex items-center">
              <NuxtLink to="/user" class="p-1 rounded-lg hover:bg-slate-50 transition-colors no-tap-highlight" title="个人中心">
                <UserAvatar
                  :src="userStore.userInfo?.avatar"
                  :alt="userStore.userInfo?.nickname"
                  size="sm"
                />
              </NuxtLink>
              <button class="p-0.5 text-slate-400 hover:text-slate-600 transition-colors" @click.stop="showUserMenu = !showUserMenu" title="更多">
                <svg class="w-3.5 h-3.5 transition-transform" :class="{ 'rotate-180': showUserMenu }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
            </div>
            <!-- 下拉菜单 - 点击切换，避免触摸设备 hover 粘滞 -->
            <Transition name="user-menu">
              <div v-if="showUserMenu" class="absolute right-0 top-full mt-1 w-48 bg-white rounded-xl shadow-[var(--shadow-lg)] border border-slate-100 z-50">
                <NuxtLink to="/user" class="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50" @click="showUserMenu = false">
                  个人中心
                </NuxtLink>
                <NuxtLink to="/user/settings" class="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50" @click="showUserMenu = false">
                  设置
                </NuxtLink>
                <hr class="border-slate-100" />
                <button class="w-full text-left px-4 py-2 text-sm text-danger hover:bg-slate-50" @click="showUserMenu = false; handleLogout()">
                  退出登录
                </button>
              </div>
            </Transition>
          </div>
          <NuxtLink v-else to="/login" class="btn-primary text-sm">
            登录
          </NuxtLink>
        </ClientOnly>
      </div>
    </div>

    <!-- 移动端汉堡菜单 -->
    <Transition name="mobile-menu">
      <div v-if="showMobileMenu" class="md:hidden fixed inset-0 top-12 z-50" @click.self="showMobileMenu = false">
        <!-- 遮罩层 -->
        <div class="absolute inset-0 bg-black/20 backdrop-blur-sm" @click="showMobileMenu = false" />
        <!-- 菜单面板 -->
        <div class="relative bg-white border-t border-slate-100 shadow-lg max-h-[calc(100vh-3rem)] overflow-y-auto animate-slide-down">
          <nav class="py-2">
            <!-- 主导航 -->
            <div class="px-3 py-2">
              <NuxtLink to="/" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
                <span>首页</span>
              </NuxtLink>
              <NuxtLink to="/discover" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/discover') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                <span>发现</span>
              </NuxtLink>
              <NuxtLink to="/rank" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/rank') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                </svg>
                <span>排行</span>
              </NuxtLink>
              <NuxtLink to="/search" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/search') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>搜索</span>
              </NuxtLink>
              <NuxtLink to="/tags" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/tags') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
                <span>标签</span>
              </NuxtLink>
            </div>

            <!-- 个人功能（已登录） -->
            <ClientOnly>
              <template v-if="userStore.isLoggedIn">
                <div class="border-t border-slate-100 mx-3 my-1"></div>
                <div class="px-3 py-2">
                  <h3 class="px-3 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">个人</h3>
                  <NuxtLink to="/editor" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/editor') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                    <span>写文章</span>
                  </NuxtLink>
                  <NuxtLink to="/notifications" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/notifications') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                    <div class="relative shrink-0">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                      </svg>
                      <span v-if="notificationStore.unreadCount > 0" class="absolute -top-1 -right-1 min-w-[1rem] h-3.5 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-0.5 leading-none">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
                    </div>
                    <span>消息</span>
                  </NuxtLink>
                  <NuxtLink to="/user" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
                    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span>个人中心</span>
                  </NuxtLink>
                  <NuxtLink to="/user/settings" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user/settings') ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50 active:scale-[0.98]'" @click="showMobileMenu = false">
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
                <div class="border-t border-slate-100 mx-3 my-1"></div>
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
  </header>
</template>

<script setup lang="ts">
/** 顶部导航栏组件 */

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { logout } = useAuth()
const route = useRoute()
const router = useRouter()

// 移动端菜单显示状态
const showMobileMenu = ref(false)

// 用户下拉菜单显示状态
const showUserMenu = ref(false)

// 是否显示返回按钮（非首页时显示）
const showBackButton = computed(() => route.path !== '/')

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}

// 退出登录
const handleLogout = async () => {
  await logout()
}

// 判断菜单项是否激活
const isMenuActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// 点击外部关闭用户菜单
onMounted(() => {
  const closeOnOutsideClick = (e: MouseEvent) => {
    if (showUserMenu.value) showUserMenu.value = false
  }
  document.addEventListener('click', closeOnOutsideClick)
  onUnmounted(() => document.removeEventListener('click', closeOnOutsideClick))
})

// 路由变化时关闭菜单
watch(() => route.path, () => {
  showMobileMenu.value = false
  showUserMenu.value = false
})
</script>

<style scoped>
/* 用户菜单过渡动画 */
.user-menu-enter-active,
.user-menu-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.user-menu-enter-from,
.user-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
