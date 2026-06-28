import { get, del, put } from './request'
import type { GroupInfo, GroupMember, GroupMessage, GroupQuery, PageResult, ApiResponse } from '@/types'

/** 获取群组列表 */
export function getGroupList(params: GroupQuery) {
  return get<PageResult<GroupInfo>>('/admin/groups', params as unknown as Record<string, unknown>)
}

/** 获取群组详情 */
export function getGroupDetail(id: number) {
  return get<GroupInfo>(`/admin/groups/${id}`)
}

/** 获取群组成员列表 */
export function getGroupMembers(groupId: number, page = 1, pageSize = 20) {
  return get<PageResult<GroupMember>>(`/admin/groups/${groupId}/members`, { page, pageSize } as any)
}

/** 获取群组消息列表 */
export function getGroupMessages(groupId: number, page = 1, pageSize = 20) {
  return get<PageResult<GroupMessage>>(`/admin/groups/${groupId}/messages`, { page, pageSize } as any)
}

/** 解散群组 */
export function disbandGroup(id: number) {
  return del(`/admin/groups/${id}`)
}

/** 禁言/恢复群组 */
export function toggleGroupStatus(id: number, status: number) {
  return put(`/admin/groups/${id}/status`, { status } as unknown as Record<string, unknown>)
}