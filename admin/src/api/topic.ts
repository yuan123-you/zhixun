import { get, post, put, del } from './request'
import type { ApiResponse } from '@/types'

export interface TopicVO {
  id: number
  name: string
  description: string
  coverImage: string
  articleCount: number
  followCount: number
  hotScore: number
  isOfficial: boolean
  isFollowed: boolean
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getTopics(page = 1, pageSize = 20): Promise<ApiResponse<PageResult<TopicVO>>> {
  return get('/topics', { page, pageSize, orderBy: 'hot' } as any)
}

export function createOfficialTopic(name: string, description?: string): Promise<ApiResponse<number>> {
  return post('/topics', { name, description, isOfficial: true } as any)
}

export function deleteTopic(id: number): Promise<ApiResponse<void>> {
  return del(`/topics/${id}`)
}

export function toggleTopicStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return put(`/topics/${id}/status`, { status } as any)
}