import { get, del } from './request'
import type { CollaborationInfo, CollaborationQuery, PageResult } from '@/types'

/** 获取协作列表 */
export function getCollaborationList(params: CollaborationQuery) {
  return get<PageResult<CollaborationInfo>>('/admin/collaborations', params as unknown as Record<string, unknown>)
}

/** 删除协作关系 */
export function deleteCollaboration(id: number) {
  return del(`/admin/collaborations/${id}`)
}