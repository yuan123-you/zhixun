/**
 * 客户端图片缓存服务 (zhixun-image-cache)
 * 解决群组图片、群组头像、用户头像没有本地缓存导致每次页面切换/重新渲染都要重复加载的问题。
 *
 * 架构：
 * 1. 内存级快速缓存（Map<string, string>）：瞬间返回 Blob URL，零延迟零闪烁。
 * 2. 持久化缓存（Browser CacheStorage / IndexedDB）：离线/二次访问命中，避免重复网络请求。
 * 3. 智能回退：CORS 或环境不支持时安全回退至原始 URL。
 */
import { ref, watch, onMounted, type Ref } from 'vue'

const CACHE_NAME = 'zhixun-img-cache-v1'
const memoryCache = new Map<string, string>()
const inFlightRequests = new Map<string, Promise<string>>()

/**
 * 判断当前环境是否支持 CacheStorage
 */
function isCacheStorageAvailable(): boolean {
  return typeof window !== 'undefined' && 'caches' in window
}

/**
 * 获取或加载缓存的图片 URL（返回 Blob URL 或原 URL）
 */
export async function getCachedImageUrl(url: string | null | undefined): Promise<string> {
  if (!url || typeof url !== 'string') return ''
  const trimmed = url.trim()
  if (!trimmed) return ''

  // data URL 或已是 blob URL，无需再次缓存
  if (trimmed.startsWith('data:') || trimmed.startsWith('blob:')) {
    return trimmed
  }

  // 1. 内存缓存命中
  if (memoryCache.has(trimmed)) {
    return memoryCache.get(trimmed)!
  }

  // 2. 避免并发重复请求同一个图片
  if (inFlightRequests.has(trimmed)) {
    return inFlightRequests.get(trimmed)!
  }

  const loadPromise = (async () => {
    try {
      if (isCacheStorageAvailable()) {
        const cache = await caches.open(CACHE_NAME)
        const match = await cache.match(trimmed)
        if (match && match.ok) {
          const blob = await match.blob()
          const blobUrl = URL.createObjectURL(blob)
          memoryCache.set(trimmed, blobUrl)
          return blobUrl
        }

        // 未命中持久化缓存，通过 fetch 请求并存入缓存
        const response = await fetch(trimmed, {
          mode: 'cors',
          credentials: 'omit',
        })
        if (response.ok) {
          // 复制一份写入缓存
          await cache.put(trimmed, response.clone())
          const blob = await response.blob()
          const blobUrl = URL.createObjectURL(blob)
          memoryCache.set(trimmed, blobUrl)
          return blobUrl
        }
      }
    } catch {
      // 跨域或离线等错误，优雅回退到原 URL
    }

    // 默认兜底：直接使用原 URL 并记录到内存
    memoryCache.set(trimmed, trimmed)
    return trimmed
  })().finally(() => {
    inFlightRequests.delete(trimmed)
  })

  inFlightRequests.set(trimmed, loadPromise)
  return loadPromise
}

/**
 * Vue 组合式函数：自动响应式缓存图片 URL
 */
export function useImageCache(srcRef: Ref<string | null | undefined> | (() => string | null | undefined)): Ref<string> {
  const getRaw = () => (typeof srcRef === 'function' ? srcRef() : srcRef.value)
  const cachedUrl = ref<string>(getRaw() || '')

  const resolve = async () => {
    const raw = getRaw()
    if (!raw) {
      cachedUrl.value = ''
      return
    }
    // 如果内存已经命中，同步返回
    if (memoryCache.has(raw)) {
      cachedUrl.value = memoryCache.get(raw)!
      return
    }
    // 先显示原始 URL，避免等待 fetch 导致空白
    cachedUrl.value = raw
    const result = await getCachedImageUrl(raw)
    if (result) {
      cachedUrl.value = result
    }
  }

  onMounted(() => {
    resolve()
  })

  watch(getRaw, () => {
    resolve()
  })

  return cachedUrl
}
