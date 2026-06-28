/**
 * useScrollSentinel - 轻量级无限滚动哨兵组合式函数
 *
 * 在列表底部放置一个哨兵元素，当它进入视口时自动触发 loadMore。
 * 用于替代手动"加载更多"按钮，实现滑动自动加载。
 */
import type { Ref } from 'vue'

interface ScrollSentinelOptions {
  /** 加载更多回调 */
  onLoadMore: () => void | Promise<void>
  /** 是否还有更多数据 */
  hasMore: Ref<boolean>
  /** 是否正在加载 */
  loading: Ref<boolean>
  /** 触发加载的提前距离，默认 200px */
  rootMargin?: string
}

export function useScrollSentinel(options: ScrollSentinelOptions) {
  const sentinelRef = ref<HTMLElement | null>(null)
  let observer: IntersectionObserver | null = null

  const setup = () => {
    if (!true) return
    if (observer) observer.disconnect()
    if (!sentinelRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && options.hasMore.value && !options.loading.value) {
          options.onLoadMore()
        }
      },
      { rootMargin: options.rootMargin || '200px' }
    )
    observer.observe(sentinelRef.value)
  }

  onMounted(() => {
    nextTick(setup)
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  // 当 sentinelRef 变化时重新绑定（如 Tab 切换）
  watch(sentinelRef, () => {
    nextTick(setup)
  })

  return { sentinelRef, resetObserver: setup }
}
