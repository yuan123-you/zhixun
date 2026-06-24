<template>
  <!-- 移动端底部Tab导航 -->
  <nav class="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 safe-bottom no-tap-highlight">
    <div class="flex items-center justify-around h-14">
      <!-- 首页 -->
      <NuxtLink to="/" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <span class="text-2xs mt-0.5">{{ $t('nav.home') }}</span>
      </NuxtLink>

      <!-- 发现 -->
      <NuxtLink to="/discover" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/discover') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
        </svg>
        <span class="text-2xs mt-0.5">{{ $t('nav.discover') }}</span>
      </NuxtLink>

      <!-- 发布 -->
      <NuxtLink to="/editor" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/editor') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center -mt-4 shadow-lg">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </div>
        <span class="text-2xs mt-0.5">{{ $t('nav.publish') }}</span>
      </NuxtLink>

      <!-- 消息（整合私信+通知） -->
      <NuxtLink to="/notifications" class="flex flex-col items-center justify-center flex-1 py-1 relative" :class="isActive('/notifications') || isActive('/messages') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <span class="text-2xs mt-0.5">{{ $t('nav.messages') }}</span>
        <!-- 未读消息/通知红点 -->
        <span v-if="notificationStore.unreadCount > 0" class="absolute top-0 right-1/4 min-w-[1rem] h-4 bg-danger text-white text-2xs rounded-full flex items-center justify-center px-1">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
      </NuxtLink>

      <!-- 我的 -->
      <NuxtLink to="/user" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/user') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span class="text-2xs mt-0.5">{{ $t('nav.mine') }}</span>
      </NuxtLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
/** 移动端底部Tab导航组件 */

const notificationStore = useNotificationStore()
const route = useRoute()

// 判断当前路由是否激活
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>
