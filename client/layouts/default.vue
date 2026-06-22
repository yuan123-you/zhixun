<template>
  <!-- 默认布局：顶部导航栏 + 主内容区 + 移动端底部Tab导航 -->
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 顶部导航栏 -->
    <AppHeader />

    <!-- 主内容区 -->
    <main class="pt-16 pb-16 md:pb-0">
      <slot />
    </main>

    <!-- 移动端底部Tab导航栏 -->
    <MobileNav />
  </div>
</template>
