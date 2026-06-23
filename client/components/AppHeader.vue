<template>
  <!-- 顶部导航栏 -->
  <header class="fixed top-0 left-0 right-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-4 2xl:px-8 h-16 flex items-center justify-between">
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

      <!-- 导航链接（桌面端/平板端） -->
      <nav class="hidden md:flex items-center space-x-1 2xl:space-x-4">
        <NuxtLink to="/" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          首页
        </NuxtLink>
        <NuxtLink to="/rank" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          排行
        </NuxtLink>
        <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="px-3 py-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors no-tap-highlight">
          写文章
        </NuxtLink>
      </nav>

      <!-- 用户菜单/登录按钮 -->
      <div class="flex items-center space-x-3">
        <!-- 语言切换 -->
        <div class="relative" ref="langDropdownRef">
          <button
            class="flex items-center space-x-1 px-2 py-1 rounded-lg text-sm text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            @click="showLangMenu = !showLangMenu"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="hidden sm:inline">{{ currentLangLabel }}</span>
          </button>
          <div
            v-if="showLangMenu"
            class="absolute right-0 top-full mt-1 w-32 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-50"
          >
            <button
              v-for="lang in languages"
              :key="lang.value"
              class="w-full text-left px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              :class="locale === lang.value ? 'text-primary font-medium' : 'text-gray-700 dark:text-gray-300'"
              @click="switchLanguage(lang.value)"
            >
              {{ lang.label }}
            </button>
          </div>
        </div>

        <!-- 通知铃铛（已登录） -->
        <NotificationBell v-if="userStore.isLoggedIn" />

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
const { locale, setLocale } = useI18n()

// 移动端菜单显示状态
const showMobileMenu = ref(false)

// 语言切换菜单
const showLangMenu = ref(false)
const langDropdownRef = ref<HTMLElement | null>(null)

const languages = [
  { label: '中文', value: 'zh-CN' },
  { label: 'English', value: 'en' },
]

const currentLangLabel = computed(() => {
  return languages.find(l => l.value === locale.value)?.label || '中文'
})

// 切换语言
const switchLanguage = async (lang: string) => {
  await setLocale(lang)
  showLangMenu.value = false
}

// 点击外部关闭语言菜单
onClickOutside(langDropdownRef, () => {
  showLangMenu.value = false
})

// 退出登录
const handleLogout = async () => {
  await logout()
}
</script>
