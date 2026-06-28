import { get, post } from './request'
import type { CacheConsistencyResult } from '@/types'

/** 检查缓存一致性 */
export function checkCacheConsistency() {
  return get<CacheConsistencyResult>('/admin/cache/consistency')
}

/** 清空所有缓存 */
export function clearAllCaches() {
  return post<{ success: boolean; message: string }>('/admin/cache/clear')
}