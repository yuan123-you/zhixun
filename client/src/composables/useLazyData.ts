/**
 * useLazyData - 基于 IntersectionObserver 的懒加载组合式函数
 *
 * 功能：
 * - 当目标元素即将进入视口时才触发数据加载
 * - 支持自定义触发距离（rootMargin）
 * - 加载状态管理（loading / error / data）
 * - 加载完成后自动断开观察（只加载一次）
 * - 错误重试
 * - 兼容 SSR
 */

interface LazyDataOptions<T> {
  /** 数据获取函数 */
  fetchFn: () => Promise<T>
  /** 触发加载的提前距离，默认 200px */
  rootMargin?: string
  /** 是否在挂载时立即加载（跳过懒加载），默认 false */
  immediate?: boolean
  /** 默认数据 */
  defaultData?: T
  /** 加载完成后是否断开观察，默认 true */
  once?: boolean
}

export function useLazyData<T = any>(options: LazyDataOptions<T>) {
  const {
    fetchFn,
    rootMargin = '200px',
    immediate = false,
    defaultData,
    once = true,
  } = options

  // 响应式状态
  const data = ref<T | undefined>(defaultData) as Ref<T | undefined>
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)
  const triggerRef = ref<HTMLElement | null>(null)

  // Observer 实例
  let observer: IntersectionObserver | null = null

  // 加载数据
  const load = async () => {
    if (loading.value || loaded.value) return

    loading.value = true
    error.value = null

    try {
      const result = await fetchFn()
      data.value = result
      loaded.value = true
    } catch (err: any) {
      error.value = err.message || '加载失败，请稍后重试'
    } finally {
      loading.value = false
    }
  }

  // 重试加载
  const retry = () => {
    if (loaded.value) return
    load()
  }

  // 强制重新加载（即使已加载过）
  const forceReload = async () => {
    loaded.value = false
    loading.value = true
    error.value = null

    try {
      const result = await fetchFn()
      data.value = result
      loaded.value = true
    } catch (err: any) {
      error.value = err.message || '加载失败，请稍后重试'
      loaded.value = false
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置为未加载状态（用于动态路由参数变化时清空旧数据）
   * 下次进入视口会重新触发 fetchFn
   */
  const reset = () => {
    loaded.value = false
    loading.value = false
    error.value = null
    data.value = defaultData
    // 断开旧 observer，下一次 setupObserver 时会重建
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  // 设置 IntersectionObserver
  const setupObserver = () => {
    if (!true) return

    // 清除旧 observer
    if (observer) {
      observer.disconnect()
      observer = null
    }

    // 如果已加载或立即加载，跳过
    if (loaded.value || immediate) return

    if (!triggerRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          load()
          // 加载完成后断开观察
          if (once && observer) {
            observer.disconnect()
            observer = null
          }
        }
      },
      { rootMargin }
    )

    observer.observe(triggerRef.value)
  }

  // 生命周期
  onMounted(() => {
    if (immediate) {
      load()
    } else {
      nextTick(() => {
        setupObserver()
      })
    }
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  // 监听 trigger ref 变化
  watch(triggerRef, () => {
    nextTick(() => {
      setupObserver()
    })
  })

  return {
    data,
    loading,
    error,
    loaded,
    triggerRef,
    load,
    retry,
    forceReload,
    reset,
  }
}
