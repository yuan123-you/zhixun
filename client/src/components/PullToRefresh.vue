<template>
  <!-- 下拉刷新容器 -->
  <div class="ptr-wrap" @touchstart.passive="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
    <div class="ptr-indicator" :style="indicatorStyle" :aria-hidden="pullDistance === 0">
      <div class="ptr-indicator-inner">
        <el-icon :class="{ 'is-loading': isRefreshing, rotated: pullDistance >= THRESHOLD && !isRefreshing }" :size="20">
          <Refresh />
        </el-icon>
        <span v-if="isRefreshing">刷新中...</span>
        <span v-else-if="lastRefreshFailed" class="ptr-error">刷新失败，下拉或点击重试</span>
        <span v-else-if="pullDistance >= THRESHOLD">释放刷新</span>
        <span v-else>下拉刷新</span>
      </div>
    </div>
    <div :style="contentStyle" class="ptr-content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  /**
   * 触发刷新时的回调
   * 接受任意返回 Promise 的函数，类型放宽为 unknown 以兼容不同返回类型
   * （useRefresh 返回 Promise<boolean>，fetchFn 返回 Promise<T[]>，直接接收 void 反而会类型报错）
   */
  onRefresh?: () => Promise<unknown>
  /** 外部错误状态，用于显示刷新失败提示 */
  error?: string
  /** 是否启用点击刷新（桌面端备选） */
  clickToRefresh?: boolean
}>(), {
  clickToRefresh: true,
})

const THRESHOLD = 60
const MAX_PULL = THRESHOLD + 20
const startY = ref(0)
const pullDistance = ref(0)
const isRefreshing = ref(false)
const canPull = ref(true)
const lastRefreshFailed = ref(false)
let touchMoved = false

watch(() => props.error, (val) => { if (val) lastRefreshFailed.value = true })

const indicatorStyle = computed(() => ({
  height: pullDistance.value > 0 ? Math.min(pullDistance.value, MAX_PULL) + 'px' : '0px',
  opacity: pullDistance.value > 0 ? Math.min(pullDistance.value / THRESHOLD, 1) : 0,
}))

const contentStyle = computed(() => ({
  transform: pullDistance.value > 0 && !isRefreshing.value ? `translateY(${Math.min(pullDistance.value, MAX_PULL)}px)` : '',
  transition: isRefreshing.value || pullDistance.value === 0 ? 'transform 0.3s ease' : 'none',
}))

/** 跨浏览器获取当前滚动位置 */
const getScrollTop = (): number => {
  return window.scrollY
    || document.documentElement.scrollTop
    || document.body.scrollTop
    || 0
}

const onTouchStart = (e: TouchEvent) => {
  if (isRefreshing.value) {
    canPull.value = false
    return
  }
  // 仅当页面处于顶部时启用下拉刷新
  if (getScrollTop() > 0) {
    canPull.value = false
    return
  }
  canPull.value = true
  startY.value = e.touches[0].clientY
  touchMoved = false
}

const onTouchMove = (e: TouchEvent) => {
  if (!canPull.value || isRefreshing.value) return
  const diff = e.touches[0].clientY - startY.value
  if (diff > 0) {
    // 防止下拉刷新时整页跟随滚动
    if (e.cancelable) e.preventDefault()
    // 阻尼效果：下拉距离超过阈值后增量逐渐减小，提供良好的视觉反馈
    const damped = diff <= THRESHOLD
      ? diff * 0.5
      : THRESHOLD * 0.5 + (diff - THRESHOLD) * 0.25
    pullDistance.value = damped
    touchMoved = true
  }
}

const onTouchEnd = async () => {
  if (isRefreshing.value || !touchMoved) {
    touchMoved = false
    return
  }
  canPull.value = false
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
  touchMoved = false
}

/** 提供给父组件：手动触发刷新（供按钮调用，避免桌面端无下拉手势） */
const triggerRefresh = async (): Promise<void> => {
  if (isRefreshing.value) return
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
}

defineExpose({ triggerRefresh, isRefreshing })
</script>

<style scoped>
.ptr-wrap { position: relative; }
.ptr-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: height 0.2s;
}
.ptr-indicator-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.ptr-error { color: var(--el-color-danger); }
.ptr-content { position: relative; }

/* 加载/旋转图标过渡 */
.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s ease;
}
.is-loading {
  animation: ptr-spin 0.9s linear infinite;
}
@keyframes ptr-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
