<template>
  <!-- 文章滑动切换容器 -->
  <div
    class="swipe-article relative overflow-hidden"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- 左滑提示（下一篇） -->
    <Transition name="swipe-hint">
      <div
        v-if="swipingDirection === 'left' && swipeDistance >= THRESHOLD"
        class="absolute right-4 top-1/2 -translate-y-1/2 z-10 flex flex-col items-center text-gray-400 dark:text-gray-500 pointer-events-none"
      >
        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
        <span class="text-xs mt-1">下一篇</span>
      </div>
    </Transition>

    <!-- 右滑提示（上一篇） -->
    <Transition name="swipe-hint">
      <div
        v-if="swipingDirection === 'right' && swipeDistance >= THRESHOLD"
        class="absolute left-4 top-1/2 -translate-y-1/2 z-10 flex flex-col items-center text-gray-400 dark:text-gray-500 pointer-events-none"
      >
        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        <span class="text-xs mt-1">上一篇</span>
      </div>
    </Transition>

    <!-- 内容区 -->
    <div
      :style="{
        transform: swipeDistance > 0 ? `translateX(${translateX}px)` : '',
        opacity: swipeDistance > 0 ? 1 - swipeDistance / (THRESHOLD * 3) : 1,
        transition: isSwiping ? 'none' : 'transform 0.3s ease, opacity 0.3s ease',
      }"
    >
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 文章滑动切换组件：左右滑动切换上/下篇 */

const THRESHOLD = 80

const props = defineProps<{
  /** 上一篇文章ID */
  prevId?: number | null
  /** 下一篇文章ID */
  nextId?: number | null
}>()

const router = useRouter()

const startX = ref(0)
const startY = ref(0)
const swipeDistance = ref(0)
const swipingDirection = ref<'left' | 'right' | null>(null)
const isSwiping = ref(false)
const isHorizontalSwipe = ref<boolean | null>(null)

const translateX = computed(() => {
  if (swipingDirection.value === 'left') return -swipeDistance.value
  if (swipingDirection.value === 'right') return swipeDistance.value
  return 0
})

const onTouchStart = (e: TouchEvent) => {
  startX.value = e.touches[0].clientX
  startY.value = e.touches[0].clientY
  isHorizontalSwipe.value = null
  isSwiping.value = true
}

const onTouchMove = (e: TouchEvent) => {
  const currentX = e.touches[0].clientX
  const currentY = e.touches[0].clientY
  const diffX = currentX - startX.value
  const diffY = currentY - startY.value

  // 判断滑动方向（首次移动时确定）
  if (isHorizontalSwipe.value === null) {
    if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) {
      isHorizontalSwipe.value = Math.abs(diffX) > Math.abs(diffY)
    }
    return
  }

  // 非水平滑动，不处理
  if (!isHorizontalSwipe.value) return

  // 阻止默认滚动
  if (e.cancelable) e.preventDefault()

  if (diffX < 0) {
    // 左滑 → 下一篇
    swipingDirection.value = 'left'
    swipeDistance.value = Math.min(Math.abs(diffX) * 0.6, THRESHOLD * 2)
  } else if (diffX > 0) {
    // 右滑 → 上一篇
    swipingDirection.value = 'right'
    swipeDistance.value = Math.min(diffX * 0.6, THRESHOLD * 2)
  }
}

const onTouchEnd = () => {
  isSwiping.value = false

  if (swipeDistance.value >= THRESHOLD) {
    if (swipingDirection.value === 'left' && props.nextId) {
      navigateTo(`/articles/${props.nextId}`)
    } else if (swipingDirection.value === 'right' && props.prevId) {
      navigateTo(`/articles/${props.prevId}`)
    }
  }

  // 重置状态
  swipeDistance.value = 0
  swipingDirection.value = null
  isHorizontalSwipe.value = null
}
</script>

<style scoped>
.swipe-hint-enter-active,
.swipe-hint-leave-active {
  transition: opacity 0.2s ease;
}
.swipe-hint-enter-from,
.swipe-hint-leave-to {
  opacity: 0;
}
</style>
