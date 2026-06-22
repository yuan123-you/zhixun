import { get, put, post, del } from './request'
import type { UserInfo, UserQuery, PageResult, UserStatus } from '@/types'

/** 获取用户列表 */
export function getUserList(params: UserQuery) {
  return get<PageResult<UserInfo>>('/users', params as unknown as Record<string, unknown>)
}

/** 获取用户详情 */
export function getUserDetail(id: number) {
  return get<UserInfo>(`/users/${id}`)
}

/** 封禁用户 */
export function banUser(id: number) {
  return put(`/users/${id}/ban`)
}

/** 解封用户 */
export function unbanUser(id: number) {
  return put(`/users/${id}/unban`)
}

/** 分配用户角色 */
export function assignRole(id: number, role: string) {
  return put(`/users/${id}/role`, { role } as unknown as Record<string, unknown>)
}

/** 删除用户 */
export function deleteUser(id: number) {
  return del(`/users/${id}`)
}

/** 更新用户状态 */
export function updateUserStatus(id: number, status: UserStatus) {
  return put(`/users/${id}/status`, { status } as unknown as Record<string, unknown>)
}
