import { get } from './request'
import type { SecurityAuditLog, SecurityAuditLogQuery, SecurityAuditStats, PageResult } from '@/types'

/** 获取安全审计日志 */
export function getSecurityAuditLogs(params: SecurityAuditLogQuery) {
  return get<PageResult<SecurityAuditLog>>('/admin/security-audit-logs', params as unknown as Record<string, unknown>)
}

/** 获取安全审计统计 */
export function getSecurityAuditStats(startDate?: string, endDate?: string) {
  return get<SecurityAuditStats>('/admin/security-audit-logs/stats', { startDate, endDate } as any)
}