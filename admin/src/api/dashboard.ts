import { get } from './request'
import type { EnhancedDashboardData } from '@/types'

/** 获取仪表盘数据（支持时间维度） */
export function getDashboardData(period: 'daily' | 'weekly' | 'monthly' = 'daily') {
  return get<EnhancedDashboardData>('/admin/dashboard/overview', { period } as any)
}

/** @deprecated 使用 getDashboardData 替代 */
export function getEnhancedDashboardData(period: 'daily' | 'weekly' | 'monthly' = 'daily') {
  return getDashboardData(period)
}
