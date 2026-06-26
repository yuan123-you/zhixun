<template>
  <Transition name="back-to-top-fade">
    <button
      v-show="isVisible"
      class="back-to-top bg-white shadow-[var(--shadow-md)] border border-slate-100 text-slate-600 hover:text-primary"
      aria-label="返回顶部"
      @click="scrollToTop"
    >
      <svg
        width="18"
        height="18"
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
  right: 12px;
  bottom: 12px;
  z-index: 50;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-tap-highlight-color: transparent;
  tap-highlight-color: transparent;
  transition: transform 0.1s ease, opacity 0.1s ease, color 0.15s ease;
  opacity: 0.6;
}

.back-to-top:hover {
  opacity: 1;
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

/* 移动端适配：考虑底部导航栏 (与 Tailwind md:768px 断点一致) */
@media (max-width: 767.98px) {
  .back-to-top {
    bottom: calc(12px + var(--mobile-nav-height, 3.5rem));
  }
}

/* 平板端适配 */
@media (min-width: 768px) and (max-width: 1024px) {
  .back-to-top {
    bottom: 12px;
  }
}

/* 触摸设备优化 */
@media (pointer: coarse) {
  .back-to-top {
    min-height: 36px;
    min-width: 36px;
  }

  .back-to-top:hover {
    opacity: 0.6;
  }
}

/* 安全区域适配（刘海屏等） */
@supports (padding: env(safe-area-inset-bottom)) {
  .back-to-top {
    bottom: calc(12px + env(safe-area-inset-bottom));
  }

  @media (max-width: 767.98px) {
    .back-to-top {
      bottom: calc(12px + var(--mobile-nav-height, 3.5rem) + env(safe-area-inset-bottom));
    }
  }
}
</style>
