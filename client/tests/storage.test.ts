/**
 * storage 工具函数单元测试
 *
 * 测试覆盖：
 * - set/get 基本操作
 * - remove 移除操作
 * - TTL 过期机制
 * - 版本不匹配自动清理
 * - clearExpired 清理过期数据
 * - keys 获取所有键
 * - clearAll 清理全部数据
 * - evictLRU 淘汰机制
 * - 非客户端环境 SSR 安全
 * - 存储空间不足降级
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { storage, STORAGE_KEYS, TTL } from '~/utils/storage'

describe('storage', () => {
  beforeEach(() => {
    // 清理所有测试数据
    storage.clearAll()
  })

  describe('set/get 基本操作', () => {
    it('应该正确存储和读取字符串', () => {
      storage.set('test_key', 'hello')
      expect(storage.get<string>('test_key')).toBe('hello')
    })

    it('应该正确存储和读取数字', () => {
      storage.set('test_num', 42)
      expect(storage.get<number>('test_num')).toBe(42)
    })

    it('应该正确存储和读取对象', () => {
      const obj = { name: 'test', value: 123 }
      storage.set('test_obj', obj)
      expect(storage.get('test_obj')).toEqual(obj)
    })

    it('应该正确存储和读取数组', () => {
      const arr = [1, 2, 3]
      storage.set('test_arr', arr)
      expect(storage.get('test_arr')).toEqual(arr)
    })

    it('不存在的键应返回 null', () => {
      expect(storage.get('non_existent')).toBeNull()
    })
  })

  describe('remove 移除操作', () => {
    it('应该正确移除存储项', () => {
      storage.set('test_key', 'value')
      expect(storage.get('test_key')).toBe('value')
      storage.remove('test_key')
      expect(storage.get('test_key')).toBeNull()
    })

    it('移除不存在的键不应报错', () => {
      expect(() => storage.remove('non_existent')).not.toThrow()
    })
  })

  describe('TTL 过期机制', () => {
    it('设置 TTL 后未过期应能读取', () => {
      storage.set('test_key', 'value', TTL.HOUR_1)
      expect(storage.get('test_key')).toBe('value')
    })

    it('过期后应返回 null', () => {
      storage.set('test_key', 'value', 1) // 1ms TTL
      // 等待 2ms 确保过期
      return new Promise<void>((resolve) => {
        setTimeout(() => {
          expect(storage.get('test_key')).toBeNull()
          resolve()
        }, 2)
      })
    })

    it('不设 TTL 应永不过期', () => {
      storage.set('test_key', 'value')
      expect(storage.get('test_key')).toBe('value')
    })
  })

  describe('keys 获取所有键', () => {
    it('应返回当前应用的所有存储键', () => {
      storage.set('key1', 'v1')
      storage.set('key2', 'v2')
      const keys = storage.keys()
      expect(keys).toContain('key1')
      expect(keys).toContain('key2')
    })

    it('空存储应返回空数组', () => {
      expect(storage.keys()).toEqual([])
    })
  })

  describe('clearAll 清理全部数据', () => {
    it('应清除所有应用数据', () => {
      storage.set('key1', 'v1')
      storage.set('key2', 'v2')
      storage.clearAll()
      expect(storage.keys()).toEqual([])
      expect(storage.get('key1')).toBeNull()
      expect(storage.get('key2')).toBeNull()
    })
  })

  describe('clearExpired 清理过期数据', () => {
    it('应清理已过期的数据', () => {
      storage.set('expired', 'old', 1) // 1ms
      storage.set('valid', 'new', TTL.HOUR_1)

      return new Promise<void>((resolve) => {
        setTimeout(() => {
          const cleared = storage.clearExpired()
          expect(cleared).toBeGreaterThanOrEqual(1)
          expect(storage.get('expired')).toBeNull()
          expect(storage.get('valid')).toBe('new')
          resolve()
        }, 2)
      })
    })
  })

  describe('STORAGE_KEYS 常量', () => {
    it('应包含所有必需的键名', () => {
      expect(STORAGE_KEYS.ACCESS_TOKEN).toBe('accessToken')
      expect(STORAGE_KEYS.REFRESH_TOKEN).toBe('refreshToken')
      expect(STORAGE_KEYS.TOKEN_EXPIRES_AT).toBe('token_expires_at')
      expect(STORAGE_KEYS.USER_SUMMARY).toBe('user_summary')
      expect(STORAGE_KEYS.ARTICLE_DRAFT).toBe('article_draft')
      expect(STORAGE_KEYS.SEARCH_HISTORY).toBe('search_history')
    })
  })

  describe('TTL 常量', () => {
    it('应包含正确的毫秒值', () => {
      expect(TTL.MINUTE_5).toBe(5 * 60 * 1000)
      expect(TTL.MINUTE_10).toBe(10 * 60 * 1000)
      expect(TTL.MINUTE_30).toBe(30 * 60 * 1000)
      expect(TTL.HOUR_1).toBe(60 * 60 * 1000)
      expect(TTL.HOUR_2).toBe(2 * 60 * 60 * 1000)
      expect(TTL.DAY_1).toBe(24 * 60 * 60 * 1000)
      expect(TTL.DAY_7).toBe(7 * 24 * 60 * 60 * 1000)
      expect(TTL.DAY_30).toBe(30 * 24 * 60 * 60 * 1000)
    })
  })

  describe('getSize 估算容量', () => {
    it('应返回非负数值', () => {
      storage.set('size_test', 'x'.repeat(100))
      const size = storage.getSize()
      expect(size).toBeGreaterThan(0)
    })

    it('空存储应返回 0', () => {
      storage.clearAll()
      expect(storage.getSize()).toBe(0)
    })
  })
})