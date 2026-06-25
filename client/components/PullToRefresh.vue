<template>
  <!-- 下拉刷新容器 -->
  <div
    class="pull-to-refresh"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- 下拉指示器 -->
    <div
      class="flex items-center justify-center transition-transform duration-200 overflow-hidden"
      :style="{
        height: pullDistance > 0 ? Math.min(pullDistance, THRESHOLD + 20) + 'px' : '0px',
        opacity: pullDistance > 0 ? Math.min(pullDistance / THRESHOLD, 1) : 0,
      }"
    >
      <div class="flex items-center gap-2 text-sm text-slate-500">
        <svg
          class="w-5 h-5 transition-transform"
          :class="{ 'animate-spin': isRefreshing, 'rotate-180': pullDistance >= THRESHOLD && !isRefreshing }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span v-if="isRefreshing">刷新中...</span>
        <span v-else-if="lastRefreshFailed" class="text-red-500">刷新失败</span>
        <span v-else-if="pullDistance >= THRESHOLD">释放刷新</span>
        <span v-else>下拉刷新</span>
      </div>
    </div>

    <!-- 内容区 -->
    <div
      :style="{
        transform: pullDistance > 0 && !isRefreshing ? `translateY(${Math.min(pullDistance, THRESHOLD + 20)}px)` : '',
        transition: isRefreshing || pullDistance === 0 ? 'transform 0.3s ease' : 'none',
      }"
    >
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 下拉刷新组件：触摸下拉触发刷新回调，支持错误状态反馈 */

const props = withDefaults(defineProps<{
  /** 刷新回调 */
  onRefresh?: () => Promise<void>
  /** 外部错误状态（供父组件传入，用于显示刷新失败提示） */
  error?: string
}>(), {})

const THRESHOLD = 60

const startY = ref(0)
const pullDistance = ref(0)
const isRefreshing = ref(false)
const canPull = ref(true)
const lastRefreshFailed = ref(false)

// 监听外部错误状态
watch(() => props.error, (val) => {
  if (val) {
    lastRefreshFailed.value = true
  }
})

const onTouchStart = (e: TouchEvent) => {
  // 只在页面滚动到顶部时才允许下拉
  if (window.scrollY > 0) {
    canPull.value = false
    return
  }
  canPull.value = true
  startY.value = e.touches[0].clientY
}

const onTouchMove = (e: TouchEvent) => {
  if (!canPull.value || isRefreshing.value) return

  const currentY = e.touches[0].clientY
  const diff = currentY - startY.value

  if (diff > 0 && e.cancelable) {
    // 阻止默认滚动行为
    e.preventDefault()
    // 使用阻尼效果，下拉越远阻力越大
    pullDistance.value = diff * 0.5
  }
}

const onTouchEnd = async () => {
  if (isRefreshing.value) return

  if (pullDistance.value >= THRESHOLD) {
    isRefreshing.value = true
    pullDistance.value = THRESHOLD

    try {
      await props.onRefresh?.()
      lastRefreshFailed.value = false
    } catch {
      lastRefreshFailed.value = true
    } finally {
      isRefreshing.value = false
      pullDistance.value = 0
    }
  } else {
    pullDistance.value = 0
  }
}
</script>
