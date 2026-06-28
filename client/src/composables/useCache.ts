import { storage, STORAGE_KEYS, TTL } from '@/utils/storage'
import type { Category, Tag } from '@/types'

/** 缓存项结构 */
interface CacheItem<T> {
  data: T
  cachedAt: number
}

/**
 * 本地数据缓存组合式函数
 * 采用 stale-while-revalidate 策略：优先返回缓存数据，后台静默刷新
 */
export const useCache = () => {
  const { get } = useApi()

  // SWR 后台刷新去重：防止短时间内多次调用同时发起多个相同请求
  const pendingRefreshes = new Map<string, Promise<any>>()

  /**
   * 获取分类列表（带缓存）
   * 首次加载后缓存 10 分钟，过期后先返回旧数据再后台刷新
   */
  const getCategories = async (): Promise<Category[]> => {
    const cached = storage.get<CacheItem<Category[]>>(STORAGE_KEYS.CATEGORIES_CACHE)

    // 缓存未过期，直接返回
    if (cached && Date.now() - cached.cachedAt < TTL.MINUTE_10) {
      return cached.data
    }

    // 缓存过期或不存在，先返回旧数据（如果有），同时后台刷新
    const staleData = cached?.data || []

    // 后台刷新（去重：同一时刻只发一个刷新请求）
    const refreshKey = 'categories'
    if (!pendingRefreshes.has(refreshKey)) {
      const refreshPromise = get<Category[]>('/categories')
        .then((res) => {
          const data = res.data?.data || res.data || []
          storage.set(STORAGE_KEYS.CATEGORIES_CACHE, {
            data,
            cachedAt: Date.now(),
          })
          return data
        })
        .catch(() => {
          // 刷新失败，保留旧缓存
        })
        .finally(() => {
          pendingRefreshes.delete(refreshKey)
        })
      pendingRefreshes.set(refreshKey, refreshPromise)
    }

    return staleData
  }

  /**
   * 获取标签列表（带缓存）
   */
  const getTags = async (): Promise<Tag[]> => {
    const cached = storage.get<CacheItem<Tag[]>>(STORAGE_KEYS.TAGS_CACHE)

    if (cached && Date.now() - cached.cachedAt < TTL.MINUTE_10) {
      return cached.data
    }

    const staleData = cached?.data || []

    // 后台刷新（去重：同一时刻只发一个刷新请求）
    const refreshKey = 'tags'
    if (!pendingRefreshes.has(refreshKey)) {
      const refreshPromise = get<Tag[]>('/tags')
        .then((res) => {
          const data = res.data?.data || res.data || []
          storage.set(STORAGE_KEYS.TAGS_CACHE, {
            data,
            cachedAt: Date.now(),
          })
          return data
        })
        .catch(() => {
          // 刷新失败，保留旧缓存
        })
        .finally(() => {
          pendingRefreshes.delete(refreshKey)
        })
      pendingRefreshes.set(refreshKey, refreshPromise)
    }

    return staleData
  }

  /**
   * 强制刷新分类缓存
   */
  const refreshCategories = async (): Promise<Category[]> => {
    try {
      const res = await get<Category[]>('/categories')
      const data = res.data?.data || res.data || []
      storage.set(STORAGE_KEYS.CATEGORIES_CACHE, {
        data,
        cachedAt: Date.now(),
      })
      return data
    } catch {
      return storage.get<CacheItem<Category[]>>(STORAGE_KEYS.CATEGORIES_CACHE)?.data || []
    }
  }

  /**
   * 强制刷新标签缓存
   */
  const refreshTags = async (): Promise<Tag[]> => {
    try {
      const res = await get<Tag[]>('/tags')
      const data = res.data?.data || res.data || []
      storage.set(STORAGE_KEYS.TAGS_CACHE, {
        data,
        cachedAt: Date.now(),
      })
      return data
    } catch {
      return storage.get<CacheItem<Tag[]>>(STORAGE_KEYS.TAGS_CACHE)?.data || []
    }
  }

  /**
   * 清除所有数据缓存（分类、标签），不影响用户认证状态
   */
  const clearCache = () => {
    storage.remove(STORAGE_KEYS.CATEGORIES_CACHE)
    storage.remove(STORAGE_KEYS.TAGS_CACHE)
  }

  return {
    getCategories,
    getTags,
    refreshCategories,
    refreshTags,
    clearCache,
  }
}
