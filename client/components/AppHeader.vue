<template>
  <!-- 顶部导航栏 -->
  <header class="fixed top-0 left-0 right-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
      <!-- Logo -->
      <NuxtLink to="/" class="flex items-center space-x-2 shrink-0">
        <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
          <span class="text-white font-bold text-sm">知</span>
        </div>
        <span class="text-xl font-bold text-gray-900 dark:text-white hidden sm:block">知讯</span>
      </NuxtLink>

      <!-- 搜索框（桌面端） -->
      <div class="hidden md:flex flex-1 max-w-xl mx-6">
        <SearchBar />
      </div>

      <!-- 导航链接（桌面端） -->
      <nav class="hidden md:flex items-center space-x-1">
        <NuxtLink to="/" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
          首页
        </NuxtLink>
        <NuxtLink to="/rank" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
          排行
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
          写文章
        </NuxtLink>
      </nav>

      <!-- 用户菜单/登录按钮 -->
      <div class="flex items-center space-x-3">
        <!-- 通知图标（已登录） -->
        <NuxtLink v-if="userStore.isLoggedIn" to="/messages" class="relative p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <span v-if="notificationStore.unreadCount > 0" class="absolute -top-0.5 -right-0.5 w-4 h-4 bg-danger text-white text-2xs rounded-full flex items-center justify-center">
            {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
          </span>
        </NuxtLink>

        <!-- 用户头像菜单（已登录） -->
        <div v-if="userStore.isLoggedIn" class="relative group">
          <button class="flex items-center space-x-2 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
            <img
              :src="userStore.userInfo?.avatar || '/default-avatar.png'"
              :alt="userStore.userInfo?.nickname"
              class="w-8 h-8 rounded-full object-cover"
            />
          </button>
          <!-- 下拉菜单 -->
          <div class="absolute right-0 top-full mt-1 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
            <NuxtLink to="/user" class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              个人中心
            </NuxtLink>
            <NuxtLink to="/user/settings" class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              设置
            </NuxtLink>
            <hr class="border-gray-200 dark:border-gray-700" />
            <button class="w-full text-left px-4 py-2 text-sm text-danger hover:bg-gray-100 dark:hover:bg-gray-700" @click="handleLogout">
              退出登录
            </button>
          </div>
        </div>

        <!-- 登录按钮（未登录） -->
        <NuxtLink v-else to="/login" class="btn-primary text-sm">
          登录
        </NuxtLink>

        <!-- 移动端汉堡菜单按钮 -->
        <button class="md:hidden p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg" @click="showMobileMenu = !showMobileMenu">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 移动端搜索框 -->
    <div class="md:hidden px-4 pb-3">
      <SearchBar />
    </div>

    <!-- 移动端汉堡菜单 -->
    <div v-if="showMobileMenu" class="md:hidden border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 animate-slide-down">
      <nav class="py-2">
        <NuxtLink to="/" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          首页
        </NuxtLink>
        <NuxtLink to="/rank" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          排行
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          写文章
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/user" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          个人中心
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/user/settings" class="block px-4 py-3 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700" @click="showMobileMenu = false">
          设置
        </NuxtLink>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
/** 顶部导航栏组件 */

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { logout } = useAuth()

// 移动端菜单显示状态
const showMobileMenu = ref(false)

// 退出登录
const handleLogout = async () => {
  await logout()
}
</script>
