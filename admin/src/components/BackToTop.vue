<template>
  <Transition name="back-to-top-fade">
    <button
      v-show="isVisible"
      class="back-to-top"
      aria-label="返回顶部"
      @click="scrollToTop"
    >
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M18 15l-6-6-6 6" />
      </svg>
    </button>
  </Transition>
</template>

<script setup lang="ts">
/** 返回顶部按钮：滚动超过容器视口50%时显示，点击平滑滚动至顶部 */

import { ref, onMounted, onBeforeUnmount } from 'vue'

const isVisible = ref(false)
let ticking = false
let scrollContainer: HTMLElement | Window = window

/** 检查滚动位置，决定按钮显隐 */
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const scrollY = scrollContainer instanceof Window
      ? scrollContainer.scrollY
      : scrollContainer.scrollTop
    const viewportHeight = scrollContainer instanceof Window
      ? scrollContainer.innerHeight
      : scrollContainer.clientHeight
    isVisible.value = scrollY > viewportHeight * 0.5
    ticking = false
  })
}

/** 缓动函数 ease-in-out */
function easeInOut(t: number): number {
  return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t
}

/** 平滑滚动至顶部，800ms 内完成 */
function scrollToTop() {
  const startTime = performance.now()
  const startScrollY = scrollContainer instanceof Window
    ? scrollContainer.scrollY
    : scrollContainer.scrollTop
  const duration = 800

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easedProgress = easeInOut(progress)

    const newScrollY = startScrollY * (1 - easedProgress)
    if (scrollContainer instanceof Window) {
      scrollContainer.scrollTo(0, newScrollY)
    } else {
      scrollContainer.scrollTop = newScrollY
    }

    if (progress < 1) {
      requestAnimationFrame(step)
    }
  }

  requestAnimationFrame(step)
}

onMounted(() => {
  // 管理后台的滚动容器是 .main-content
  const mainContent = document.querySelector('.main-content') as HTMLElement
  if (mainContent) {
    scrollContainer = mainContent
    mainContent.addEventListener('scroll', onScroll, { passive: true })
  } else {
    scrollContainer = window
    window.addEventListener('scroll', onScroll, { passive: true })
  }
  onScroll()
})

onBeforeUnmount(() => {
  if (scrollContainer instanceof Window) {
    scrollContainer.removeEventListener('scroll', onScroll)
  } else {
    scrollContainer.removeEventListener('scroll', onScroll)
  }
})
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.back-to-top {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 50;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: $primary-color;
  color: #ffffff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  -webkit-tap-highlight-color: transparent;
  tap-highlight-color: transparent;
  transition: transform 0.1s ease, opacity 0.1s ease;

  &:hover {
    filter: brightness(1.1);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  }

  &:active {
    transform: scale(0.95);
  }
}

/* 显示/隐藏过渡动画 300ms ease */
.back-to-top-fade-enter-active,
.back-to-top-fade-leave-active {
  transition: opacity 300ms ease, transform 300ms ease;
}

.back-to-top-fade-enter-from,
.back-to-top-fade-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

.back-to-top-fade-enter-to,
.back-to-top-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

/* 移动端适配 */
@media (max-width: 767px) {
  .back-to-top {
    width: 48px;
    height: 48px;
    right: 16px;
    bottom: 16px;

    svg {
      width: 20px;
      height: 20px;
    }
  }
}

/* 平板端适配 */
@media (min-width: 768px) and (max-width: 1024px) {
  .back-to-top {
    right: 20px;
    bottom: 20px;
  }
}

/* 触摸设备优化 */
@media (pointer: coarse) {
  .back-to-top {
    min-height: 44px;
    min-width: 44px;

    &:hover {
      filter: none;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }
}

/* 安全区域适配 */
@supports (padding: env(safe-area-inset-bottom)) {
  .back-to-top {
    bottom: calc(24px + env(safe-area-inset-bottom));
  }

  @media (max-width: 767px) {
    .back-to-top {
      bottom: calc(16px + env(safe-area-inset-bottom));
    }
  }
}
</style>
