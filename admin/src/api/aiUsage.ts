import { get } from './request'
import type { AIUsageStats } from '@/types'

/** 获取 AI 使用统计 */
export function getAIUsageStats(period: 'daily' | 'weekly' | 'monthly' = 'daily') {
  return get<AIUsageStats>('/admin/ai/stats', { period } as any)
}