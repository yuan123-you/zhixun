<template>
  <!-- 顶部导航栏（移动端下移至全局返回顶栏下方） -->
  <header class="fixed top-10 md:top-0 left-0 right-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-lg border-b border-slate-200/60 dark:border-gray-700/60 shadow-[var(--shadow-sm)]">
    <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-6 h-12 md:h-16 flex items-center justify-between gap-2">
      <!-- 左侧：品牌名（返回按钮已由全局 BackButton 组件统一提供） -->
      <div class="flex items-center gap-2 shrink-0">
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
        <NuxtLink to="/topics" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
          话题
        </NuxtLink>
        <NuxtLink to="/groups" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
          群组
        </NuxtLink>
        <ClientOnly>
          <NuxtLink v-if="userStore.isLoggedIn" to="/editor" class="px-3 py-2 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors no-tap-highlight">
            创作
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


  </header>
</template>

<script setup lang="ts">
/** 顶部导航栏组件 */

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { logout } = useAuth()
const route = useRoute()

// 用户下拉菜单显示状态
const showUserMenu = ref(false)

// 退出登录
const handleLogout = async () => {
  await logout()
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
