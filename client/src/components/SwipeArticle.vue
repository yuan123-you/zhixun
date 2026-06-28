<template>
  <!-- 作品滑动切换容器 -->
  <div class="swipe-article" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
    <Transition name="swipe-hint">
      <div v-if="swipingDirection === 'left' && swipeDistance >= THRESHOLD" class="swipe-next-hint">
        <el-icon :size="32"><ArrowRight /></el-icon>
        <span>下一篇</span>
      </div>
    </Transition>
    <Transition name="swipe-hint">
      <div v-if="swipingDirection === 'right' && swipeDistance >= THRESHOLD" class="swipe-prev-hint">
        <el-icon :size="32"><ArrowLeft /></el-icon>
        <span>上一篇</span>
      </div>
    </Transition>

    <div :style="contentStyle">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const THRESHOLD = 80

const props = defineProps<{ prevId?: number | null; nextId?: number | null }>()
const router = useRouter()

const startX = ref(0); const startY = ref(0); const swipeDistance = ref(0)
const swipingDirection = ref<'left' | 'right' | null>(null); const isSwiping = ref(false)
const isHorizontalSwipe = ref<boolean | null>(null)

const contentStyle = computed(() => ({
  transform: swipeDistance.value > 0
    ? `translateX(${swipingDirection.value === 'left' ? -swipeDistance.value : swipeDistance.value}px)`
    : '',
  opacity: swipeDistance.value > 0 ? 1 - swipeDistance.value / (THRESHOLD * 3) : 1,
  transition: isSwiping.value ? 'none' : 'transform 0.3s ease, opacity 0.3s ease',
}))

const onTouchStart = (e: TouchEvent) => {
  startX.value = e.touches[0].clientX; startY.value = e.touches[0].clientY
  isHorizontalSwipe.value = null; isSwiping.value = true
}

const onTouchMove = (e: TouchEvent) => {
  const currentX = e.touches[0].clientX; const currentY = e.touches[0].clientY
  const diffX = currentX - startX.value; const diffY = currentY - startY.value
  if (isHorizontalSwipe.value === null) {
    if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) isHorizontalSwipe.value = Math.abs(diffX) > Math.abs(diffY)
    return
  }
  if (!isHorizontalSwipe.value) return
  if (e.cancelable) e.preventDefault()
  if (diffX < 0) { swipingDirection.value = 'left'; swipeDistance.value = Math.min(Math.abs(diffX) * 0.6, THRESHOLD * 2) }
  else if (diffX > 0) { swipingDirection.value = 'right'; swipeDistance.value = Math.min(diffX * 0.6, THRESHOLD * 2) }
}

const onTouchEnd = () => {
  isSwiping.value = false
  if (swipeDistance.value >= THRESHOLD) {
    if (swipingDirection.value === 'left' && props.nextId) navigateTo(`/articles/${props.nextId}`)
    else if (swipingDirection.value === 'right' && props.prevId) navigateTo(`/articles/${props.prevId}`)
  }
  swipeDistance.value = 0; swipingDirection.value = null; isHorizontalSwipe.value = null
}
</script>

<style scoped>
.swipe-article { position: relative; overflow: hidden; }
.swipe-next-hint,
.swipe-prev-hint {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--el-text-color-placeholder);
  pointer-events: none;
  font-size: 12px;
}
.swipe-next-hint { right: 16px; }
.swipe-prev-hint { left: 16px; }
.swipe-hint-enter-active,
.swipe-hint-leave-active { transition: opacity 0.2s ease; }
.swipe-hint-enter-from,
.swipe-hint-leave-to { opacity: 0; }
</style>
