<template>
  <Transition name="back-to-top-fade">
    <button
      v-show="isVisible"
      class="back-to-top"
      :aria-label="t('common.backToTop')"
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
/** 返回顶部按钮：滚动超过视口50%时显示，点击平滑滚动至顶部 */

const { t } = useI18n()

const isVisible = ref(false)
let ticking = false

/** 检查滚动位置，决定按钮显隐 */
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    isVisible.value = window.scrollY > window.innerHeight * 0.5
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
  const startScrollY = window.scrollY
  const duration = 800

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easedProgress = easeInOut(progress)

    window.scrollTo(0, startScrollY * (1 - easedProgress))

    if (progress < 1) {
      requestAnimationFrame(step)
    }
  }

  requestAnimationFrame(step)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.back-to-top {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 50;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: var(--color-primary);
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
}

.back-to-top:hover {
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.back-to-top:active {
  transform: scale(0.95);
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

/* 移动端适配：考虑底部导航栏 */
@media (max-width: 767px) {
  .back-to-top {
    bottom: calc(24px + var(--mobile-nav-height, 3.5rem));
  }
}

/* 平板端适配 */
@media (min-width: 768px) and (max-width: 1024px) {
  .back-to-top {
    bottom: 24px;
  }
}

/* 触摸设备优化 */
@media (pointer: coarse) {
  .back-to-top {
    min-height: 44px;
    min-width: 44px;
  }

  .back-to-top:hover {
    filter: none;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

/* 安全区域适配（刘海屏等） */
@supports (padding: env(safe-area-inset-bottom)) {
  .back-to-top {
    bottom: calc(24px + env(safe-area-inset-bottom));
  }

  @media (max-width: 767px) {
    .back-to-top {
      bottom: calc(24px + var(--mobile-nav-height, 3.5rem) + env(safe-area-inset-bottom));
    }
  }
}
</style>
