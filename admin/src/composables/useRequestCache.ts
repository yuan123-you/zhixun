import { ref, shallowRef } from 'vue'
import { get } from '@/api/request'

/** 缓存条目 */
interface CacheEntry<T> {
  data: T
  timestamp: number
  ttl: number
}

/** 请求配置 */
interface RequestCacheOptions {
  /** 缓存过期时间（毫秒），默认 5 分钟 */
  ttl?: number
  /** 是否启用 stale-while-revalidate，默认 true */
  staleWhileRevalidate?: boolean
  /** 错误重试次数，默认 0 */
  retryCount?: number
  /** 重试间隔（毫秒），默认 1000 */
  retryInterval?: number
}

/** 全局缓存存储 */
const cacheMap = new Map<string, CacheEntry<unknown>>()

/** 全局进行中的请求（用于去重） */
const pendingMap = new Map<string, Promise<unknown>>()

/** 生成缓存键 */
function createCacheKey(url: string, params?: Record<string, unknown>): string {
  if (!params || Object.keys(params).length === 0) return url
  const sorted = Object.keys(params)
    .sort()
    .map((k) => `${k}=${JSON.stringify(params[k])}`)
    .join('&')
  return `${url}?${sorted}`
}

/** 清理过期缓存 */
function pruneExpired(): void {
  const now = Date.now()
  for (const [key, entry] of cacheMap) {
    if (now - entry.timestamp >= entry.ttl) {
      cacheMap.delete(key)
    }
  }
}

/**
 * 请求缓存与去重 composable
 *
 * 适用于管理后台 GET 请求场景：
 * - 按 URL + params 缓存响应
 * - 并发相同请求自动去重
 * - 支持 TTL 过期
 * - 支持 stale-while-revalidate
 * - 支持强制刷新与缓存失效
 */
export function useRequestCache<T = unknown>(options: RequestCacheOptions = {}) {
  const {
    ttl = 5 * 60 * 1000,
    staleWhileRevalidate = true,
    retryCount = 0,
    retryInterval = 1000,
  } = options

  const loading = ref(false)
  const error = shallowRef<Error | null>(null)
  const data = shallowRef<T | null>(null)

  /**
   * 执行带缓存和去重的 GET 请求
   */
  async function request(
    url: string,
    params?: Record<string, unknown>,
    overrides?: {
      force?: boolean
      ttl?: number
    },
  ): Promise<T> {
    const cacheKey = createCacheKey(url, params)
    const effectiveTtl = overrides?.ttl ?? ttl
    const force = overrides?.force ?? false

    // 定期清理
    pruneExpired()

    // 1. 非强制刷新时，检查缓存
    if (!force) {
      const cached = cacheMap.get(cacheKey) as CacheEntry<T> | undefined
      if (cached) {
        const age = Date.now() - cached.timestamp
        if (age < effectiveTtl) {
          // 缓存未过期，直接返回
          data.value = cached.data
          return cached.data
        }
        if (staleWhileRevalidate) {
          // 缓存过期但允许 stale-while-revalidate：先返回旧数据，后台刷新
          data.value = cached.data
          backgroundRefresh(cacheKey, url, params, effectiveTtl)
          return cached.data
        }
      }
    }

    // 2. 检查是否有进行中的相同请求（去重）
    const pending = pendingMap.get(cacheKey) as Promise<T> | undefined
    if (pending) {
      const result = await pending
      data.value = result
      return result
    }

    // 3. 发起新请求
    loading.value = true
    error.value = null

    const requestPromise = executeWithRetry<T>(url, params)
    pendingMap.set(cacheKey, requestPromise)

    try {
      const result = await requestPromise
      data.value = result

      // 写入缓存
      cacheMap.set(cacheKey, {
        data: result,
        timestamp: Date.now(),
        ttl: effectiveTtl,
      })

      return result
    } catch (err) {
      error.value = err as Error
      throw err
    } finally {
      pendingMap.delete(cacheKey)
      loading.value = false
    }
  }

  /**
   * 带重试的请求执行
   */
  async function executeWithRetry<R>(
    url: string,
    params?: Record<string, unknown>,
  ): Promise<R> {
    let lastError: Error | null = null
    const maxAttempts = retryCount + 1

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        const res = await get<R>(url, params)
        return res.data
      } catch (err) {
        lastError = err as Error
        if (attempt < maxAttempts) {
          await delay(retryInterval * attempt)
        }
      }
    }

    throw lastError!
  }

  /**
   * 后台刷新（stale-while-revalidate）
   */
  function backgroundRefresh(
    cacheKey: string,
    url: string,
    params: Record<string, unknown> | undefined,
    effectiveTtl: number,
  ) {
    // 如果已有后台刷新进行中，跳过
    if (pendingMap.has(cacheKey)) return

    const refreshPromise = executeWithRetry<T>(url, params)
    pendingMap.set(cacheKey, refreshPromise)

    refreshPromise
      .then((result) => {
        data.value = result
        cacheMap.set(cacheKey, {
          data: result,
          timestamp: Date.now(),
          ttl: effectiveTtl,
        })
      })
      .catch(() => {
        // 后台刷新失败不影响已展示的旧数据
      })
      .finally(() => {
        pendingMap.delete(cacheKey)
      })
  }

  /**
   * 强制刷新指定缓存
   */
  function invalidate(url: string, params?: Record<string, unknown>): void {
    const cacheKey = createCacheKey(url, params)
    cacheMap.delete(cacheKey)
  }

  /**
   * 按 URL 前缀批量失效缓存
   */
  function invalidateByPrefix(urlPrefix: string): void {
    for (const key of cacheMap.keys()) {
      if (key.startsWith(urlPrefix)) {
        cacheMap.delete(key)
      }
    }
  }

  /**
   * 清空所有缓存
   */
  function clearAll(): void {
    cacheMap.clear()
    pendingMap.clear()
  }

  return {
    loading,
    error,
    data,
    request,
    invalidate,
    invalidateByPrefix,
    clearAll,
  }
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
