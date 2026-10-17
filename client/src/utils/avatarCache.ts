/**
 * 高性能本地头像缓存服务 (Avatar Cache Service)
 * 解决头像加载缓慢、网络请求过多、页面重载不保留本地缓存等问题
 *
 * 架构：
 * 1. 同步内存级映射（0ms 零延迟）：瞬间返回 Base64 或已解析的 URL，彻底杜绝闪烁
 * 2. 本地持久化缓存（IndexedDB + localStorage 降级）：跨会话持久化存储，离线秒开
 * 3. 失败域名与无效地址快速熔断：拦截已知 404 地址，避免等待网络超时
 * 4. 内联 SVG 默认头像：零网络开销，不需要任何 HTTP 请求
 */

const DB_NAME = 'zhixun_avatar_store'
const DB_VERSION = 1
const STORE_NAME = 'avatars'
const MAX_CACHE_ENTRIES = 500

/** 内存级快速缓存映射（Key: 原始URL或标识 -> Value: Data URI / Blob URL） */
const memoryCache = new Map<string, string>()
/** 进行中的异步读取 Promise，避免同一头像并发重复请求 */
const loadingPromises = new Map<string, Promise<string | null>>()

/** 默认用户头像内联 SVG Data URI（0 网络请求，秒渲染） */
export const DEFAULT_AVATAR_DATA_URI =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" fill="none"%3E%3Crect width="200" height="200" rx="100" fill="%23E2E8F0"/%3E%3Ccircle cx="100" cy="78" r="32" fill="%2394A3B8"/%3E%3Cellipse cx="100" cy="170" rx="52" ry="42" fill="%2394A3B8"/%3E%3C/svg%3E'

let dbPromise: Promise<IDBDatabase | null> | null = null

function getDb(): Promise<IDBDatabase | null> {
  if (typeof window === 'undefined' || !window.indexedDB) {
    return Promise.resolve(null)
  }
  if (!dbPromise) {
    dbPromise = new Promise((resolve) => {
      try {
        const req = indexedDB.open(DB_NAME, DB_VERSION)
        req.onupgradeneeded = () => {
          const db = req.result
          if (!db.objectStoreNames.contains(STORE_NAME)) {
            db.createObjectStore(STORE_NAME, { keyPath: 'url' })
          }
        }
        req.onsuccess = () => {
          const db = req.result
          db.onclose = () => { dbPromise = null }
          resolve(db)
        }
        req.onerror = () => {
          resolve(null)
        }
      } catch {
        resolve(null)
      }
    })
  }
  return dbPromise
}

/** 简单哈希函数 */
function quickHash(str: string): string {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return String(Math.abs(h))
}

/**
 * 判断是否为已知失效的远程占位地址
 */
export function isBrokenAvatarUrl(url?: string | null): boolean {
  if (!url) return true
  const lower = url.toLowerCase().trim()
  if (!lower) return true
  // 过滤已经下线或失效的生成图接口地址
  if (lower.includes('trae-api-cn.mchost.guru')) return true
  if (lower.includes('avatar-placeholder')) return true
  return false
}

/**
 * 同步检查内存中是否已有缓存
 */
export function getMemoryCachedAvatar(url?: string | null): string | null {
  if (!url) return null
  const trimmed = url.trim()
  if (!trimmed) return null
  if (trimmed.startsWith('data:') || trimmed.startsWith('blob:')) return trimmed
  return memoryCache.get(trimmed) || null
}

/**
 * 从本地持久化存储（IndexedDB / localStorage）读取已缓存的头像
 */
export async function getLocalCachedAvatar(url?: string | null): Promise<string | null> {
  if (!url || isBrokenAvatarUrl(url)) return null
  const trimmed = url.trim()

  // 1. 内存命中
  if (memoryCache.has(trimmed)) {
    return memoryCache.get(trimmed)!
  }

  // 2. localStorage 极速命中（针对当前用户等核心高频头像）
  try {
    const lsKey = `zhixun_av_${quickHash(trimmed)}`
    const lsVal = localStorage.getItem(lsKey)
    if (lsVal) {
      memoryCache.set(trimmed, lsVal)
      return lsVal
    }
  } catch { /* ignore */ }

  // 3. IndexedDB 持久化查询
  const db = await getDb()
  if (!db) return null

  return new Promise<string | null>((resolve) => {
    try {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const req = store.get(trimmed)
      req.onsuccess = () => {
        const item = req.result
        if (item && item.data) {
          memoryCache.set(trimmed, item.data)
          resolve(item.data)
        } else {
          resolve(null)
        }
      }
      req.onerror = () => resolve(null)
    } catch {
      resolve(null)
    }
  })
}

/**
 * 将头像数据（Base64 或 Blob URL）持久化存储到本地
 */
export async function saveLocalAvatar(url: string, dataUrl: string): Promise<void> {
  if (!url || !dataUrl || isBrokenAvatarUrl(url)) return
  const trimmed = url.trim()
  memoryCache.set(trimmed, dataUrl)

  // 存储至 IndexedDB
  try {
    const db = await getDb()
    if (db) {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      store.put({
        url: trimmed,
        data: dataUrl,
        updatedAt: Date.now(),
      })
    }
  } catch { /* ignore */ }

  // 核心用户头像同步存入 localStorage 保证刷新极速读出
  try {
    const lsKey = `zhixun_av_${quickHash(trimmed)}`
    localStorage.setItem(lsKey, dataUrl)
  } catch { /* ignore */ }
}

/**
 * 预加载并持久化缓存头像
 */
export async function preloadAndCacheAvatar(url?: string | null): Promise<string | null> {
  if (!url || isBrokenAvatarUrl(url)) return null
  const trimmed = url.trim()

  // 内存命中直接返回
  if (memoryCache.has(trimmed)) {
    return memoryCache.get(trimmed)!
  }

  // 本地已存
  const local = await getLocalCachedAvatar(trimmed)
  if (local) return local

  // 并发去重
  if (loadingPromises.has(trimmed)) {
    return loadingPromises.get(trimmed)!
  }

  const task = (async () => {
    try {
      // 通过图片元素载入并转为 Base64 存入本地
      return await new Promise<string | null>((resolve) => {
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.onload = () => {
          try {
            const canvas = document.createElement('canvas')
            const size = Math.min(img.naturalWidth || 128, 256)
            canvas.width = size
            canvas.height = size
            const ctx = canvas.getContext('2d')
            if (ctx) {
              ctx.drawImage(img, 0, 0, size, size)
              const dataUrl = canvas.toDataURL('image/jpeg', 0.85)
              saveLocalAvatar(trimmed, dataUrl)
              resolve(dataUrl)
              return
            }
          } catch {
            // 跨域受阻时，直接记录原地址
          }
          memoryCache.set(trimmed, trimmed)
          resolve(trimmed)
        }
        img.onerror = () => {
          resolve(null)
        }
        img.src = trimmed
      })
    } catch {
      return null
    } finally {
      loadingPromises.delete(trimmed)
    }
  })()

  loadingPromises.set(trimmed, task)
  return task
}
