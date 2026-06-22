import { get } from './request'
import type { DashboardData } from '@/types'

/** 获取仪表盘数据 */
export function getDashboardData() {
  return get<DashboardData>('/dashboard')
}

/** 获取趋势数据 */
export function getTrendData(params: { startDate: string; endDate: string }) {
  return get('/dashboard/trend', params as unknown as Record<string, unknown>)
}
