/**
 * useRequestCache - 请求缓存与去重组合式函数
 *
 * 功能：
 * - 内存级请求缓存，避免短时间内重复请求相同数据
 * - 请求去重：同一时刻对同一接口的并发请求自动合并
 * - TTL 过期策略：缓存数据在指定时间后自动失效
 * - stale-while-revalidate：优先返回缓存数据，后台静默刷新
 * - 缓存手动清除与批量清除
 * - 兼容 SSR（服务端不缓存）
 */

interface CacheEntry<T> {
  data: T
  cachedAt: number
  ttl: number
}

interface RequestCacheOptions {
  /** 缓存有效期（毫秒），默认 5 分钟 */
  ttl?: number
  /** 是否启用 stale-while-revalidate 策略，默认 true */
  staleWhileRevalidate?: boolean
  /** 缓存最大条目数，默认 100 */
  maxEntries?: number
}

// 全局内存缓存（客户端共享）
const memoryCache = new Map<string, CacheEntry<any>>()
// 全局请求去重映射（同一时刻同一 key 的请求共享同一个 Promise）
const pendingRequests = new Map<string, Promise<any>>()

export function useRequestCache(defaultOptions: RequestCacheOptions = {}) {
  const {
    ttl: defaultTtl = 5 * 60 * 1000,
    staleWhileRevalidate: defaultSWR = true,
    maxEntries = 100,
  } = defaultOptions

  // SSR 不缓存
  const isClient = import.meta.client

  /**
   * 生成缓存 key
   */
  const generateKey = (url: string, params?: Record<string, any>): string => {
    const sortedParams = params
      ? JSON.stringify(Object.entries(params).sort(([a], [b]) => a.localeCompare(b)))
      : ''
    return `${url}:${sortedParams}`
  }

  /**
   * 检查缓存是否有效
   */
  const isCacheValid = <T>(key: string): T | null => {
    if (!isClient) return null
    const entry = memoryCache.get(key)
    if (!entry) return null
    if (Date.now() - entry.cachedAt < entry.ttl) {
      return entry.data
    }
    return null
  }

  /**
   * 获取过期但存在的缓存（用于 SWR 策略）
   */
  const getStaleCache = <T>(key: string): T | null => {
    if (!isClient) return null
    const entry = memoryCache.get(key)
    if (!entry) return null
    return entry.data
  }

  /**
   * 设置缓存
   */
  const setCache = <T>(key: string, data: T, ttl?: number): void => {
    if (!isClient) return

    // LRU 淘汰
    if (memoryCache.size >= maxEntries) {
      const oldestKey = memoryCache.keys().next().value
      if (oldestKey) {
        memoryCache.delete(oldestKey)
      }
    }

    memoryCache.set(key, {
      data,
      cachedAt: Date.now(),
      ttl: ttl || defaultTtl,
    })
  }

  /**
   * 带缓存的请求（核心方法）
   *
   * @param requestFn 实际请求函数
   * @param url 请求 URL
   * @param params 请求参数
   * @param options 缓存选项
   */
  const cachedRequest = async <T>(
    requestFn: () => Promise<T>,
    url: string,
    params?: Record<string, any>,
    options?: RequestCacheOptions
  ): Promise<T> => {
    const key = generateKey(url, params)
    const ttl = options?.ttl ?? defaultTtl
    const swr = options?.staleWhileRevalidate ?? defaultSWR

    // 1. 检查有效缓存
    const cached = isCacheValid<T>(key)
    if (cached !== null) {
      return cached
    }

    // 2. SWR 策略：有过期缓存时先返回旧数据，后台刷新
    if (swr) {
      const staleData = getStaleCache<T>(key)
      if (staleData !== null) {
        // 后台静默刷新（不阻塞当前请求）
        requestFn()
          .then((freshData) => {
            setCache(key, freshData, ttl)
          })
          .catch(() => {
            // 后台刷新失败，保留旧缓存
          })
        return staleData
      }
    }

    // 3. 请求去重：同一时刻同一 key 的请求共享同一个 Promise
    const pending = pendingRequests.get(key)
    if (pending) {
      return pending as Promise<T>
    }

    // 4. 发起新请求
    const requestPromise = requestFn()
      .then((data) => {
        setCache(key, data, ttl)
        pendingRequests.delete(key)
        return data
      })
      .catch((err) => {
        pendingRequests.delete(key)
        throw err
      })

    pendingRequests.set(key, requestPromise)
    return requestPromise
  }

  /**
   * 强制刷新（忽略缓存，重新请求）
   */
  const forceRequest = async <T>(
    requestFn: () => Promise<T>,
    url: string,
    params?: Record<string, any>,
    options?: RequestCacheOptions
  ): Promise<T> => {
    const key = generateKey(url, params)
    const ttl = options?.ttl ?? defaultTtl

    // 清除缓存
    memoryCache.delete(key)

    // 清除去重
    pendingRequests.delete(key)

    const data = await requestFn()
    setCache(key, data, ttl)
    return data
  }

  /**
   * 清除指定缓存
   */
  const invalidate = (url: string, params?: Record<string, any>): void => {
    const key = generateKey(url, params)
    memoryCache.delete(key)
    pendingRequests.delete(key)
  }

  /**
   * 清除匹配前缀的所有缓存
   */
  const invalidateByPrefix = (urlPrefix: string): void => {
    for (const key of memoryCache.keys()) {
      if (key.startsWith(urlPrefix)) {
        memoryCache.delete(key)
      }
    }
  }

  /**
   * 清除所有缓存
   */
  const clearAll = (): void => {
    memoryCache.clear()
    pendingRequests.clear()
  }

  /**
   * 获取缓存统计信息
   */
  const getStats = () => ({
    cacheSize: memoryCache.size,
    pendingCount: pendingRequests.size,
  })

  return {
    cachedRequest,
    forceRequest,
    invalidate,
    invalidateByPrefix,
    clearAll,
    getStats,
    generateKey,
  }
}
