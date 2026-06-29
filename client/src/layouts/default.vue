<template>
  <!-- 默认布局：顶部导航栏(含汉堡菜单) + 主内容区 + 全端底部Tab导航 -->
  <div class="flex flex-col min-h-dvh-screen bg-[var(--color-bg)] dark:bg-gray-900">
    <!-- 网络状态检测 -->
    <NetworkStatus />

    <!-- 顶部导航栏（仅首页和我的页面显示） -->
    <AppHeader />

    <!-- 主内容区 -->
    <main
      :class="{ 'pt-[52px] md:pt-16': showHeaderInner }"
    >
      <router-view v-slot="{ Component, route: r }">
        <Transition name="page-fade" @before-leave="onPageLeave" @after-enter="onPageEnter">
          <component :is="Component" :key="r.path" />
        </Transition>
      </router-view>
    </main>

    <!-- 返回顶部 -->
    <BackToTop />

    <!-- @提及通知弹窗 -->
    <MentionNotification />

    <!-- 全端底部Tab导航栏 -->
    <MobileNav />

    <!-- 方向锁定提示 -->
    <Transition name="fade">
      <div v-if="showOrientationPrompt" class="orientation-prompt-overlay" @click.self="dismissOrientationPrompt">
        <div class="orientation-prompt-card">
          <div class="orientation-prompt-icon">
            <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-[var(--zh-text)] mb-2">旋转屏幕</h3>
          <p class="text-sm text-[var(--zh-text-secondary)] mb-4">
            {{ desiredOrientation === 'landscape' ? '横屏观看体验更佳' : '竖屏浏览体验更佳' }}
          </p>
          <button class="btn-primary w-full" @click="dismissOrientationPrompt">
            我知道了
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/** 默认布局：顶部导航栏(含汉堡菜单) + 主内容区 + 全端底部Tab导航 */
const { showOrientationPrompt, dismissOrientationPrompt, promptOrientationLock, desiredOrientation } = useOrientation()
const { showHeaderInner } = useHeaderVisibility()

// 页面过渡事件处理
const onPageLeave = (el: Element) => {
  const htmlEl = el as HTMLElement
  htmlEl.style.position = 'absolute'
  htmlEl.style.width = '100%'
}
const onPageEnter = (el: Element) => {
  const htmlEl = el as HTMLElement
  htmlEl.style.position = ''
  htmlEl.style.width = ''
}
</script>
