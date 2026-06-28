import { get } from './request'
import type { LoginLog, LoginLogQuery, PageResult } from '@/types'

/** 获取登录日志列表 */
export function getLoginLogList(params: LoginLogQuery) {
  return get<PageResult<LoginLog>>('/admin/login-logs', params as unknown as Record<string, unknown>)
}