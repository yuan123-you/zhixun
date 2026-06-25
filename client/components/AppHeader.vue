<template>
  <!-- 顶部导航栏 -->
  <header class="fixed top-0 left-0 right-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-6 h-12 md:h-16 flex items-center justify-between gap-2">
      <!-- 左侧：返回按钮/汉堡菜单（移动端）+ 品牌名 -->
      <div class="flex items-center gap-2 shrink-0">
        <!-- 返回按钮（非首页时显示） -->
        <button v-if="showBackButton" class="p-1.5 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="goBack">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <!-- 移动端汉堡菜单按钮（首页时显示） -->
        <button v-if="!showBackButton" class="md:hidden p-1.5 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="showMobileMenu = !showMobileMenu">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>


      </div>

      <!-- 导航链接（桌面端/平板端） -->
      <nav class="hidden md:flex items-center space-x-1 2xl:space-x-4">
        <NuxtLink to="/" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          {{ t('nav.home') }}
        </NuxtLink>
        <NuxtLink to="/discover" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          {{ t('nav.discover') }}
        </NuxtLink>
        <NuxtLink to="/rank" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          {{ t('nav.hot') }}
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          {{ t('nav.write') }}
        </NuxtLink>
      </nav>

      <!-- 右侧：搜索 + 消息 + 用户 -->
      <div class="flex items-center space-x-1 md:space-x-3">
        <!-- 搜索图标 -->
        <NuxtLink to="/search" class="p-1.5 md:p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors" :title="t('common.search')">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </NuxtLink>

        <!-- 消息中心图标（已登录） -->
        <NuxtLink v-if="userStore.isLoggedIn" to="/notifications" class="relative p-1.5 md:p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
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

        <!-- 用户头像菜单（已登录） -->
        <div v-if="userStore.isLoggedIn" class="relative group">
          <button class="flex items-center space-x-2 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
            <UserAvatar
              :src="userStore.userInfo?.avatar"
              :alt="userStore.userInfo?.nickname"
              size="sm"
            />
          </button>
          <!-- 下拉菜单 -->
          <div class="absolute right-0 top-full mt-1 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
            <NuxtLink to="/user" class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ t('nav.profile') }}
            </NuxtLink>
            <NuxtLink to="/user/settings" class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ t('nav.settings') }}
            </NuxtLink>
            <hr class="border-gray-200 dark:border-gray-700" />
            <button class="w-full text-left px-4 py-2 text-sm text-danger hover:bg-gray-100 dark:hover:bg-gray-700" @click="handleLogout">
              {{ t('common.logout') }}
            </button>
          </div>
        </div>

        <!-- 登录按钮（未登录） -->
        <NuxtLink v-else to="/login" class="btn-primary text-sm">
          {{ t('common.login') }}
        </NuxtLink>
      </div>
    </div>

    <!-- 移动端汉堡菜单 -->
    <Transition name="mobile-menu">
      <div v-if="showMobileMenu" class="md:hidden fixed inset-0 top-12 z-50" @click.self="showMobileMenu = false">
        <!-- 遮罩层 -->
        <div class="absolute inset-0 bg-black/30" @click="showMobileMenu = false" />
        <!-- 菜单面板 -->
        <div class="relative bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 shadow-lg max-h-[calc(100vh-3rem)] overflow-y-auto animate-slide-down">
          <nav class="py-2">
            <!-- 主导航 -->
            <div class="px-3 py-2">
              <NuxtLink to="/" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
                <span>{{ t('nav.home') }}</span>
              </NuxtLink>
              <NuxtLink to="/discover" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/discover') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                <span>{{ t('nav.discover') }}</span>
              </NuxtLink>
              <NuxtLink to="/rank" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/rank') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                </svg>
                <span>{{ t('nav.hot') }}</span>
              </NuxtLink>
              <NuxtLink to="/search" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/search') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>{{ t('nav.search') }}</span>
              </NuxtLink>
              <NuxtLink to="/tags" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/tags') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
                <span>{{ t('nav.tags') }}</span>
              </NuxtLink>
            </div>

            <!-- 分类 -->
            <div class="border-t border-gray-100 dark:border-gray-700 mx-3 my-1"></div>
            <div class="px-3 py-2">
              <h3 class="px-3 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider mb-1">{{ t('nav.category') }}</h3>
              <NuxtLink to="/category/tech" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm transition-all duration-200 min-h-[44px]" :class="isMenuActive('/category/tech') ? 'bg-primary/10 text-primary' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <span class="w-2.5 h-2.5 rounded-full bg-blue-500 shrink-0"></span>
                <span>{{ t('nav.tech') }}</span>
              </NuxtLink>
              <NuxtLink to="/category/design" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm transition-all duration-200 min-h-[44px]" :class="isMenuActive('/category/design') ? 'bg-primary/10 text-primary' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <span class="w-2.5 h-2.5 rounded-full bg-purple-500 shrink-0"></span>
                <span>{{ t('nav.design') }}</span>
              </NuxtLink>
              <NuxtLink to="/category/product" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm transition-all duration-200 min-h-[44px]" :class="isMenuActive('/category/product') ? 'bg-primary/10 text-primary' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                <span class="w-2.5 h-2.5 rounded-full bg-green-500 shrink-0"></span>
                <span>{{ t('nav.product') }}</span>
              </NuxtLink>
            </div>

            <!-- 个人功能（已登录） -->
            <template v-if="userStore.isLoggedIn">
              <div class="border-t border-gray-100 dark:border-gray-700 mx-3 my-1"></div>
              <div class="px-3 py-2">
                <h3 class="px-3 text-xs font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-wider mb-1">{{ t('nav.personal') }}</h3>
                <NuxtLink to="/editor" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/editor') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  <span>{{ t('nav.write') }}</span>
                </NuxtLink>
                <NuxtLink to="/notifications" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/notifications') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <div class="relative shrink-0">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    <span v-if="notificationStore.unreadCount > 0" class="absolute -top-1 -right-1 min-w-[1rem] h-3.5 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-0.5 leading-none">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
                  </div>
                  <span>{{ t('nav.messages') }}</span>
                </NuxtLink>
                <NuxtLink to="/user" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span>{{ t('nav.profile') }}</span>
                </NuxtLink>
                <NuxtLink to="/user/settings" class="flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 min-h-[44px]" :class="isMenuActive('/user/settings') ? 'bg-primary/10 text-primary' : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 active:scale-[0.98]'" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span>{{ t('nav.settings') }}</span>
                </NuxtLink>
              </div>
            </template>

            <!-- 未登录 -->
            <template v-else>
              <div class="border-t border-gray-100 dark:border-gray-700 mx-3 my-1"></div>
              <div class="px-3 py-2">
                <NuxtLink to="/login" class="flex items-center justify-center gap-2 mx-3 py-3 rounded-xl text-sm font-medium bg-primary text-white transition-all duration-200 min-h-[44px] active:scale-[0.98]" @click="showMobileMenu = false">
                  <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                  </svg>
                  <span>{{ t('common.login') }} / {{ t('common.register') }}</span>
                </NuxtLink>
              </div>
            </template>
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
const { t } = useI18n()
const { logout } = useAuth()
const route = useRoute()
const router = useRouter()

// 移动端菜单显示状态
const showMobileMenu = ref(false)

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
</script>
