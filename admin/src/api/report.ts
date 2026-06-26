import { get, put } from './request'
import type { ApiResponse } from '@/types'

export interface ReportVO {
  id: number
  type: string
  targetId: number
  targetTitle: string
  reporterId: number
  reporterName: string
  reason: string
  description: string
  status: number
  handledBy?: number
  handlerName?: string
  handledAt?: string
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function getPendingReports(type: string, page = 1, pageSize = 20): Promise<ApiResponse<PageResult<ReportVO>>> {
  return get('/reports/pending', { type, page, pageSize } as any)
}

export function handleReport(id: number, status: number, type = 'article'): Promise<ApiResponse<void>> {
  return put(`/reports/${id}/handle`, { status, type } as any)
}