/**
 * 本地存储工具类
 * 提供类型安全的 localStorage 操作，支持 TTL 过期、数据版本管理和容量监控
 * SSR 安全：所有操作均通过 import.meta.client 守卫
 */

/** 存储项结构 */
interface StorageItem<T> {
  /** 存储的值 */
  value: T
  /** 过期时间戳（毫秒），null 表示永不过期 */
  expireAt: number | null
  /** 数据版本号，版本不匹配时自动清理 */
  version: string
}

/** 当前存储 schema 版本 */
const STORAGE_VERSION = '1.0.0'

/** 存储键前缀，避免与其他应用冲突 */
const KEY_PREFIX = 'zhixun_'

/** 默认最大存储条目数（LRU 淘汰用） */
const DEFAULT_MAX_ENTRIES = 200

/**
 * 判断是否在客户端环境
 */
function isClient(): boolean {
  return typeof window !== 'undefined' && !!window.localStorage
}

/**
 * 生成完整的存储键
 */
function fullKey(key: string): string {
  return `${KEY_PREFIX}${key}`
}

/**
 * 本地存储工具类
 */
export const storage = {
  /**
   * 获取存储的值
   * @param key 存储键
   * @returns 值，不存在或已过期返回 null
   */
  get<T>(key: string): T | null {
    if (!isClient()) return null

    try {
      const raw = localStorage.getItem(fullKey(key))
      if (!raw) return null

      const item: StorageItem<T> = JSON.parse(raw)

      // 版本不匹配，清理旧数据
      if (item.version !== STORAGE_VERSION) {
        this.remove(key)
        return null
      }

      // 检查过期
      if (item.expireAt !== null && Date.now() > item.expireAt) {
        this.remove(key)
        return null
      }

      return item.value
    } catch {
      return null
    }
  },

  /**
   * 设置存储值
   * @param key 存储键
   * @param value 值
   * @param ttlMs 过期时间（毫秒），不传表示永不过期
   */
  set<T>(key: string, value: T, ttlMs?: number): void {
    if (!isClient()) return

    try {
      const item: StorageItem<T> = {
        value,
        expireAt: ttlMs ? Date.now() + ttlMs : null,
        version: STORAGE_VERSION,
      }
      localStorage.setItem(fullKey(key), JSON.stringify(item))
    } catch (e) {
      // 存储空间不足时，尝试清理过期数据后重试
      if (e instanceof DOMException && e.name === 'QuotaExceededError') {
        this.clearExpired()
        try {
          const item: StorageItem<T> = {
            value,
            expireAt: ttlMs ? Date.now() + ttlMs : null,
            version: STORAGE_VERSION,
          }
          localStorage.setItem(fullKey(key), JSON.stringify(item))
        } catch {
          // 重试仍失败，静默忽略
        }
      }
    }
  },

  /**
   * 移除存储项
   */
  remove(key: string): void {
    if (!isClient()) return
    localStorage.removeItem(fullKey(key))
  },

  /**
   * 清理所有已过期的存储项
   * @returns 清理的条目数
   */
  clearExpired(): number {
    if (!isClient()) return 0

    let cleared = 0
    const prefix = KEY_PREFIX
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const k = localStorage.key(i)
      if (!k || !k.startsWith(prefix)) continue

      try {
        const raw = localStorage.getItem(k)
        if (!raw) continue
        const item: StorageItem<unknown> = JSON.parse(raw)
        if (item.expireAt !== null && Date.now() > item.expireAt) {
          localStorage.removeItem(k)
          cleared++
        }
      } catch {
        // 无效数据，清理掉
        localStorage.removeItem(k)
        cleared++
      }
    }
    return cleared
  },

  /**
   * 估算已使用的存储容量（字节）
   */
  getSize(): number {
    if (!isClient()) return 0

    let total = 0
    const prefix = KEY_PREFIX
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (!k || !k.startsWith(prefix)) continue
      const v = localStorage.getItem(k)
      if (v) {
        // 每个字符约 2 字节（UTF-16）
        total += (k.length + v.length) * 2
      }
    }
    return total
  },

  /**
   * 获取当前应用的所有存储键（不含前缀）
   */
  keys(): string[] {
    if (!isClient()) return []

    const keys: string[] = []
    const prefix = KEY_PREFIX
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(prefix)) {
        keys.push(k.slice(prefix.length))
      }
    }
    return keys
  },

  /**
   * 清理本应用的所有存储数据
   */
  clearAll(): void {
    if (!isClient()) return

    const keys = this.keys()
    keys.forEach((key) => this.remove(key))
  },

  /**
   * LRU 淘汰：当存储条目超过限制时，移除最旧的条目
   * @param maxEntries 最大条目数
   */
  evictLRU(maxEntries: number = DEFAULT_MAX_ENTRIES): void {
    if (!isClient()) return

    const keys = this.keys()
    if (keys.length <= maxEntries) return

    // 收集所有条目及其过期时间
    const entries: Array<{ key: string; expireAt: number }> = []
    const prefix = KEY_PREFIX

    for (const key of keys) {
      try {
        const raw = localStorage.getItem(fullKey(key))
        if (!raw) continue
        const item: StorageItem<unknown> = JSON.parse(raw)
        entries.push({
          key,
          expireAt: item.expireAt ?? Infinity,
        })
      } catch {
        entries.push({ key, expireAt: 0 })
      }
    }

    // 按过期时间排序，最早过期的先清理
    entries.sort((a, b) => a.expireAt - b.expireAt)

    const toRemove = entries.length - maxEntries
    for (let i = 0; i < toRemove; i++) {
      this.remove(entries[i].key)
    }
  },
}

/** 便捷的 TTL 常量 */
export const TTL = {
  /** 5 分钟 */
  MINUTE_5: 5 * 60 * 1000,
  /** 10 分钟 */
  MINUTE_10: 10 * 60 * 1000,
  /** 30 分钟 */
  MINUTE_30: 30 * 60 * 1000,
  /** 1 小时 */
  HOUR_1: 60 * 60 * 1000,
  /** 2 小时 */
  HOUR_2: 2 * 60 * 60 * 1000,
  /** 1 天 */
  DAY_1: 24 * 60 * 60 * 1000,
  /** 7 天 */
  DAY_7: 7 * 24 * 60 * 60 * 1000,
  /** 30 天 */
  DAY_30: 30 * 24 * 60 * 60 * 1000,
  /** 永不过期（语义化常量，实际传入时不设 TTL） */
  PERMANENT: undefined as unknown as number,
} as const

/** 本地存储键名常量 */
export const STORAGE_KEYS = {
  /** 访问令牌 */
  ACCESS_TOKEN: 'accessToken',
  /** 刷新令牌 */
  REFRESH_TOKEN: 'refreshToken',
  /** Access Token 过期时间戳 */
  TOKEN_EXPIRES_AT: 'token_expires_at',
  /** 分类列表缓存 */
  CATEGORIES_CACHE: 'categories_cache',
  /** 标签列表缓存 */
  TAGS_CACHE: 'tags_cache',
  /** 用户信息摘要缓存 */
  USER_SUMMARY: 'user_summary',
  /** 用户本地设置（主题/字体/语言） - 供 settings.vue 页面使用 */
  SETTINGS_LOCAL: 'settings_local',
  /** 用户偏好设置（主题/语言/通知/读写偏好） - 供 useUserSettings composable 使用 */
  USER_PREFERENCES: 'user_preferences',
  /** 搜索历史 */
  SEARCH_HISTORY: 'search_history',
  /** 作品草稿 */
  ARTICLE_DRAFT: 'article_draft',
  /** 阅读进度前缀 */
  READ_PROGRESS_PREFIX: 'read_progress_',
  /** 浏览历史本地记录 */
  VIEW_HISTORY_LOCAL: 'view_history_local',
  /** 数据版本号 */
  DATA_VERSION: 'data_version',
} as const
