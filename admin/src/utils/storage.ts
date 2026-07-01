/**
 * 管理端本地存储工具类
 * 提供类型安全的 localStorage 操作，支持 TTL 过期、数据版本管理和容量监控
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

/** 存储键前缀 */
const KEY_PREFIX = 'zhixun_admin_'

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
   */
  get<T>(key: string): T | null {
    try {
      const raw = localStorage.getItem(fullKey(key))
      if (!raw) return null

      const item: StorageItem<T> = JSON.parse(raw)

      if (item.version !== STORAGE_VERSION) {
        this.remove(key)
        return null
      }

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
    try {
      const item: StorageItem<T> = {
        value,
        expireAt: ttlMs ? Date.now() + ttlMs : null,
        version: STORAGE_VERSION,
      }
      localStorage.setItem(fullKey(key), JSON.stringify(item))
    } catch (e) {
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
    localStorage.removeItem(fullKey(key))
  },

  /**
   * 清理所有已过期的存储项
   */
  clearExpired(): number {
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
    let total = 0
    const prefix = KEY_PREFIX
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (!k || !k.startsWith(prefix)) continue
      const v = localStorage.getItem(k)
      if (v) {
        total += (k.length + v.length) * 2
      }
    }
    return total
  },

  /**
   * 获取当前应用的所有存储键（不含前缀）
   */
  keys(): string[] {
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
    const keys = this.keys()
    keys.forEach((key) => this.remove(key))
  },
}

/** 便捷的 TTL 常量 */
export const TTL = {
  MINUTE_5: 5 * 60 * 1000,
  MINUTE_30: 30 * 60 * 1000,
  HOUR_1: 60 * 60 * 1000,
  HOUR_2: 2 * 60 * 60 * 1000,
  DAY_1: 24 * 60 * 60 * 1000,
  DAY_7: 7 * 24 * 60 * 60 * 1000,
  DAY_30: 30 * 24 * 60 * 60 * 1000,
  /** 永不过期（语义化常量，实际传入时不设 TTL） */
  PERMANENT: undefined as unknown as number,
} as const

/** 管理端本地存储键名常量 */
export const STORAGE_KEYS = {
  /** 访问令牌 */
  TOKEN: 'token',
  /** 刷新令牌，用于自动续期 accessToken */
  REFRESH_TOKEN: 'refresh_token',
  /** Token 过期时间戳（毫秒），用于判断是否需要提前刷新 */
  TOKEN_EXPIRES_AT: 'token_expires_at',
  /** 用户信息摘要，用于刷新后快速恢复登录态 */
  USER_INFO: 'user_info',
  /** 用户权限 */
  USER_PERMISSIONS: 'user_permissions',
  /** 侧边栏折叠状态 */
  SIDEBAR_COLLAPSED: 'sidebar_collapsed',
  /** 主题 */
  THEME: 'theme',
  /** 表格列配置前缀 */
  TABLE_CONFIG_PREFIX: 'table_config_',
  /** 分页大小偏好前缀 */
  PAGE_SIZE_PREFIX: 'page_size_',
} as const

/**
 * sessionStorage 存储实例
 * 用于会话级数据（token、refreshToken等），每个浏览器标签页独立存储
 * 支持同一浏览器同时登录多个管理员账号
 */
export const sessionStore = {
  get<T>(key: string): T | null {
    try {
      const raw = sessionStorage.getItem(fullKey(key))
      if (!raw) return null
      const item: StorageItem<T> = JSON.parse(raw)
      if (item.version !== STORAGE_VERSION) {
        this.remove(key)
        return null
      }
      if (item.expireAt !== null && Date.now() > item.expireAt) {
        this.remove(key)
        return null
      }
      return item.value
    } catch {
      return null
    }
  },

  set<T>(key: string, value: T, ttlMs?: number): void {
    try {
      const item: StorageItem<T> = {
        value,
        expireAt: ttlMs ? Date.now() + ttlMs : null,
        version: STORAGE_VERSION,
      }
      sessionStorage.setItem(fullKey(key), JSON.stringify(item))
    } catch {
      // sessionStorage 写入失败静默忽略
    }
  },

  remove(key: string): void {
    sessionStorage.removeItem(fullKey(key))
  },

  clearAll(): void {
    const keysToRemove: string[] = []
    for (let i = 0; i < sessionStorage.length; i++) {
      const k = sessionStorage.key(i)
      if (k && k.startsWith(KEY_PREFIX)) {
        keysToRemove.push(k)
      }
    }
    keysToRemove.forEach((k) => sessionStorage.removeItem(k))
  },
}
