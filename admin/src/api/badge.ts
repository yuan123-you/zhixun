import { get, post, del } from './request'
import type { ApiResponse } from '@/types'

export interface BadgeVO {
  id: number
  name: string
  description: string
  icon: string
  category: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getAllBadges(): Promise<ApiResponse<BadgeVO[]>> {
  return get('/incentive/badges' as any)
}

export function createBadge(data: { name: string; description: string; icon?: string; category: string; condition?: string }): Promise<ApiResponse<number>> {
  return post('/admin/badges', data as any)
}

export function deleteBadge(id: number): Promise<ApiResponse<void>> {
  return del(`/admin/badges/${id}`)
}