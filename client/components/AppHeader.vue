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
    <div v-if="showMobileMenu" class="md:hidden border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 animate-slide-down">
      <nav class="py-2">
        <NuxtLink to="/" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.home') }}
        </NuxtLink>
        <NuxtLink to="/discover" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.discover') }}
        </NuxtLink>
        <NuxtLink to="/rank" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.hot') }}
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.write') }}
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/user" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.profile') }}
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/user/settings" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          {{ t('nav.settings') }}
        </NuxtLink>
      </nav>
    </div>
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
</script>
