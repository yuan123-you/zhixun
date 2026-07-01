import { get, put, del } from './request'
import type { UserInfo, UserDetail, UserQuery, PageResult, UserStatus } from '@/types'

/** 获取用户列表 */
export function getUserList(params: UserQuery) {
  return get<PageResult<UserInfo>>('/admin/users', params as unknown as Record<string, unknown>)
}

/** 获取用户详情（增强版） */
export function getUserDetail(id: number) {
  return get<UserDetail>(`/admin/users/${id}`)
}

/** 封禁用户 */
export function banUser(id: number) {
  return put(`/admin/users/${id}/status`, { status: 0 } as unknown as Record<string, unknown>)
}

/** 解封用户 */
export function unbanUser(id: number) {
  return put(`/admin/users/${id}/status`, { status: 1 } as unknown as Record<string, unknown>)
}

/** 分配用户角色 */
export function assignRole(id: number, role: string) {
  return put(`/admin/users/${id}/role`, { role } as unknown as Record<string, unknown>)
}

/** 删除用户 */
export function deleteUser(id: number) {
  return del(`/admin/users/${id}`)
}

/** 更新用户状态 */
export function updateUserStatus(id: number, status: UserStatus) {
  return put(`/admin/users/${id}/status`, { status } as unknown as Record<string, unknown>)
}

/** 获取用户登录历史 */
export function getUserLoginHistory(userId: number, page = 1, pageSize = 20) {
  return get<PageResult<any>>(`/admin/users/${userId}/login-history`, { page, pageSize } as any)
}
