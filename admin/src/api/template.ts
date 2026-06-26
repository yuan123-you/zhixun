import { get, post, del } from './request'
import type { ApiResponse } from '@/types'

export interface TemplateVO {
  id: number
  name: string
  description: string
  coverImage: string
  category: string
  content: string
  tags: string
  useCount: number
  creatorName: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getTemplates(page = 1, pageSize = 20): Promise<ApiResponse<PageResult<TemplateVO>>> {
  return get('/templates', { page, pageSize } as any)
}

export function createOfficialTemplate(data: { name: string; description?: string; content: string; category?: string }): Promise<ApiResponse<number>> {
  return post('/templates', data as any)
}

export function deleteTemplate(id: number): Promise<ApiResponse<void>> {
  return del(`/templates/${id}`)
}