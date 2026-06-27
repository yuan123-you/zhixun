/**
 * useInfiniteScroll - 通用无限滚动/滑动加载组合式函数
 *
 * 功能：
 * - 基于 IntersectionObserver 实现滚动到底部自动加载
 * - 支持自定义触发距离（rootMargin）
 * - 防重复加载锁
 * - 加载状态管理（loading / loadingMore / hasMore / error）
 * - 错误重试
 * - 兼容 SSR
 */

interface InfiniteScrollOptions<T> {
  /** 数据获取函数，接收 page 参数，返回数据列表 */
  fetchFn: (page: number) => Promise<{ list: T[]; total?: number; hasMore?: boolean }>
  /** 每页条数，用于判断是否还有更多数据 */
  pageSize?: number
  /** 触发加载的提前距离，默认 200px */
  rootMargin?: string
  /** 是否在挂载时立即加载第一页，默认 true */
  immediate?: boolean
  /** 初始页码，默认 1 */
  initialPage?: number
}

export function useInfiniteScroll<T = any>(options: InfiniteScrollOptions<T>) {
  const {
    fetchFn,
    pageSize = 20,
    rootMargin = '200px',
    immediate = true,
    initialPage = 1,
  } = options

  // 响应式状态
  const items = ref<T[]>([]) as Ref<T[]>
  const page = ref(initialPage)
  const loading = ref(false)
  const loadingMore = ref(false)
  const hasMore = ref(true)
  const error = ref<string | null>(null)
  const sentinelRef = ref<HTMLElement | null>(null)

  // Observer 实例
  let observer: IntersectionObserver | null = null

  // 是否为首次加载
  const isFirstPage = computed(() => page.value === initialPage)

  // 加载数据
  const load = async (isLoadMore = false) => {
    // 防重复加载
    if (loading.value || loadingMore.value) return
    if (isLoadMore && !hasMore.value) return

    // 清除之前的错误
    error.value = null

    if (isLoadMore) {
      loadingMore.value = true
      page.value++
    } else {
      loading.value = true
    }

    try {
      const result = await fetchFn(page.value)
      const list = result.list || []

      if (isLoadMore) {
        items.value = [...items.value, ...list]
      } else {
        items.value = list
      }

      // 判断是否还有更多数据
      if (result.hasMore !== undefined) {
        hasMore.value = result.hasMore
      } else {
        hasMore.value = list.length >= pageSize
      }
    } catch (err: any) {
      // 加载失败时回退页码
      if (isLoadMore) {
        page.value--
      }
      error.value = err.message || '内容加载失败，请稍后重试'
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  // 加载更多
  const loadMore = () => {
    load(true)
  }

  // 重新加载（重置到第一页）
  const reload = async () => {
    page.value = initialPage
    hasMore.value = true
    items.value = []
    await load(false)
  }

  // 重试当前页
  const retry = () => {
    load(page.value > initialPage)
  }

  // 设置 IntersectionObserver
  const setupObserver = () => {
    if (!import.meta.client) return

    // 清除旧 observer
    if (observer) {
      observer.disconnect()
      observer = null
    }

    if (!sentinelRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !loading.value && !loadingMore.value && hasMore.value && !error.value) {
          loadMore()
        }
      },
      { rootMargin }
    )

    observer.observe(sentinelRef.value)
  }

  // 重新设置 observer（当 sentinel 元素变化时）
  const resetObserver = () => {
    nextTick(() => {
      setupObserver()
    })
  }

  // 生命周期
  onMounted(() => {
    if (immediate) {
      load(false).then(() => {
        resetObserver()
      })
    } else {
      resetObserver()
    }
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  // 监听 sentinel ref 变化
  watch(sentinelRef, () => {
    resetObserver()
  })

  return {
    items,
    page,
    loading,
    loadingMore,
    hasMore,
    error,
    sentinelRef,
    isFirstPage,
    loadMore,
    reload,
    retry,
    resetObserver,
  }
}
