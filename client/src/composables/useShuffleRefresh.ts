/**
 * useShuffleRefresh - "换一批"内容切换组合式函数
 *
 * 功能：
 * - 基于偏移量的分页获取，确保每次获取不同批次的内容
 * - 已展示内容去重：记录已展示的ID集合，避免重复展示
 * - Fisher-Yates 洗牌算法：对获取到的内容进行随机排序，增加差异性
 * - 偏移量自动管理：当数据耗尽时自动回绕到起始位置
 * - 加载状态与错误处理
 * - 防抖保护：防止快速连续点击
 */

export interface ShuffleRefreshOptions<T> {
  /** 数据获取函数，接收 offset 和 limit 参数 */
  fetchFn: (offset: number, limit: number) => Promise<T[]>
  /** 每批获取的数量，默认 5 */
  limit?: number
  /** 数据总量上限（用于判断偏移回绕），0 表示不限制 */
  total?: number
  /** 从数据项中提取唯一标识的函数，默认取 id 字段 */
  getItemId?: (item: T) => string | number
  /** 是否启用洗牌，默认 true */
  shuffle?: boolean
  /** 防抖间隔（毫秒），默认 300ms */
  debounceMs?: number
}

export function useShuffleRefresh<T>(options: ShuffleRefreshOptions<T>) {
  const {
    fetchFn,
    limit = 5,
    total = 0,
    getItemId = (item: any) => item.id,
    shuffle: enableShuffle = true,
    debounceMs = 300,
  } = options

  // 当前展示的数据
  const items = ref<T[]>([]) as Ref<T[]>
  // 加载状态
  const loading = ref(false)
  // 错误信息
  const error = ref('')
  // 当前偏移量
  const offset = ref(0)
  // 已展示过的ID集合（用于去重）
  const shownIds = ref(new Set<string | number>())
  // 上次操作时间戳（防抖）
  let lastActionTime = 0

  /**
   * Fisher-Yates 洗牌算法
   * 时间复杂度 O(n)，空间复杂度 O(1)（原地操作）
   */
  const shuffleArray = (arr: T[]): T[] => {
    if (!enableShuffle || arr.length <= 1) return arr
    const result = [...arr]
    for (let i = result.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[result[i], result[j]] = [result[j], result[i]]
    }
    return result
  }

  /**
   * 过滤已展示过的内容，并记录新展示的ID
   */
  const filterAndRecord = (newItems: T[]): T[] => {
    const filtered = newItems.filter(item => {
      const id = getItemId(item)
      return !shownIds.value.has(id)
    })
    // 记录新展示的ID
    filtered.forEach(item => {
      shownIds.value.add(getItemId(item))
    })
    return filtered
  }

  /**
   * 计算下一个偏移量，支持回绕
   */
  const nextOffset = (currentOffset: number, fetchedCount: number): number => {
    const newOffset = currentOffset + fetchedCount
    // 如果已知总量且偏移超出，回绕到0
    if (total > 0 && newOffset >= total) {
      return 0
    }
    return newOffset
  }

  /**
   * 执行"换一批"操作
   */
  const refresh = async (): Promise<T[]> => {
    // 防抖保护
    const now = Date.now()
    if (now - lastActionTime < debounceMs) {
      return items.value
    }
    lastActionTime = now

    // 防止重复请求
    if (loading.value) {
      return items.value
    }

    loading.value = true
    error.value = ''

    try {
      const newItems = await fetchFn(offset.value, limit)

      if (newItems.length === 0) {
        // 没有获取到数据
        if (offset.value > 0) {
          // 偏移量不为0但无数据，说明已到末尾，回绕重试
          offset.value = 0
          shownIds.value.clear()
          const retryItems = await fetchFn(0, limit)
          const filtered = filterAndRecord(retryItems)
          items.value = shuffleArray(filtered)
          offset.value = nextOffset(0, retryItems.length)
        } else {
          // 从头也没有数据
          items.value = []
        }
      } else {
        // 过滤已展示的内容
        let filtered = filterAndRecord(newItems)

        // 如果过滤后没有新内容（全部重复），清空去重记录重新获取
        if (filtered.length === 0 && newItems.length > 0) {
          shownIds.value.clear()
          filtered = filterAndRecord(newItems)
        }

        // 如果仍然不足，尝试补充获取
        if (filtered.length < limit && offset.value > 0) {
          const supplementCount = limit - filtered.length
          try {
            const supplement = await fetchFn(0, supplementCount)
            const supplementFiltered = filterAndRecord(supplement)
            filtered.push(...supplementFiltered)
          } catch {
            // 补充获取失败，使用已有数据
          }
        }

        items.value = shuffleArray(filtered)
        offset.value = nextOffset(offset.value, newItems.length)
      }

      return items.value
    } catch (err: any) {
      error.value = err.message || '加载失败，请稍后重试'
      // 保留旧数据，不清空
      return items.value
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置状态（回到初始状态）
   */
  const reset = () => {
    items.value = []
    offset.value = 0
    error.value = ''
    shownIds.value.clear()
    lastActionTime = 0
  }

  /**
   * 清除去重记录（允许重新展示之前的内容）
   */
  const clearDedup = () => {
    shownIds.value.clear()
  }

  /**
   * 获取已展示ID数量
   */
  const shownCount = computed(() => shownIds.value.size)

  return {
    items,
    loading,
    error,
    offset,
    shownCount,
    refresh,
    reset,
    clearDedup,
  }
}
