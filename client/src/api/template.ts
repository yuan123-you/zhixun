import type { PaginationParams, PageResult } from '@/types'

export interface ContentTemplate {
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

export const templateApi = {
  createTemplate: (data: { name: string; description?: string; coverImage?: string; content: string; category?: string; tags?: string }) => {
    const { post } = useApi()
    return post<number>('/templates', data)
  },
  getTemplates: (params?: PaginationParams & { category?: string; keyword?: string }) => {
    const { get } = useApi()
    return get<PageResult<ContentTemplate>>('/templates', params)
  },
  getTemplateDetail: (id: number) => {
    const { get } = useApi()
    return get<ContentTemplate>(`/templates/${id}`)
  },
  useTemplate: (id: number) => {
    const { post } = useApi()
    return post<void>(`/templates/${id}/use`)
  },
  deleteTemplate: (id: number) => {
    const { delete: del } = useApi()
    return del(`/templates/${id}`)
  },
}