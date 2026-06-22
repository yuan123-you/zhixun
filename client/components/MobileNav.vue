<template>
  <!-- 移动端底部Tab导航 -->
  <nav class="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 safe-bottom">
    <div class="flex items-center justify-around h-14">
      <!-- 首页 -->
      <NuxtLink to="/" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <span class="text-2xs mt-0.5">首页</span>
      </NuxtLink>

      <!-- 热门 -->
      <NuxtLink to="/rank" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/rank') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
        </svg>
        <span class="text-2xs mt-0.5">热门</span>
      </NuxtLink>

      <!-- 发布 -->
      <NuxtLink to="/editor" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/editor') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center -mt-4 shadow-lg">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </div>
        <span class="text-2xs mt-0.5">发布</span>
      </NuxtLink>

      <!-- 消息 -->
      <NuxtLink to="/messages" class="flex flex-col items-center justify-center flex-1 py-1 relative" :class="isActive('/messages') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span class="text-2xs mt-0.5">消息</span>
        <!-- 未读消息红点 -->
        <span v-if="notificationStore.unreadCount > 0" class="absolute top-0 right-1/4 w-2 h-2 bg-danger rounded-full"></span>
      </NuxtLink>

      <!-- 我的 -->
      <NuxtLink to="/user" class="flex flex-col items-center justify-center flex-1 py-1" :class="isActive('/user') ? 'text-primary' : 'text-gray-500 dark:text-gray-400'">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span class="text-2xs mt-0.5">我的</span>
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
