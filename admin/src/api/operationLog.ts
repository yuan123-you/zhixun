import { get } from './request'
import type { OperationLog, OperationLogQuery, PageResult } from '@/types'

/** 获取操作日志列表 */
export function getOperationLogList(params: OperationLogQuery) {
  return get<PageResult<OperationLog>>('/operation-logs', params as unknown as Record<string, unknown>)
}

/** 获取操作日志详情 */
export function getOperationLogDetail(id: number) {
  return get<OperationLog>(`/operation-logs/${id}`)
}
