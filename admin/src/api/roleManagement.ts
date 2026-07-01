import { get, put } from './request'
import type { PageResult } from '@/types'

/** 管理员信息 */
export interface AdminUser {
  id: number
  uid: string
  username: string
  nickname: string
  avatar: string
  role: string
  status: number
  articleCount: number
  followCount: number
  followerCount: number
  createdAt: string
  updatedAt: string
}

/** 获取管理员列表 */
export function getAdminList(params: { keyword?: string; status?: number; page: number; pageSize: number }) {
  return get<PageResult<AdminUser>>('/admin/admins', params as unknown as Record<string, unknown>)
}

/** 修改管理员角色 */
export function updateAdminRole(id: number, role: string) {
  return put(`/admin/admins/${id}/role`, { role } as unknown as Record<string, unknown>)
}

/** 启用/禁用管理员 */
export function updateAdminStatus(id: number, status: number) {
  return put(`/admin/admins/${id}/status`, { status } as unknown as Record<string, unknown>)
}
