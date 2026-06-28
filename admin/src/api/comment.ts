import { get, del, post, put } from './request'
import type { Comment, CommentQuery, PageResult } from '@/types'

/** 获取评论列表 */
export function getCommentList(params: CommentQuery) {
  return get<PageResult<Comment>>('/admin/comments', params as unknown as Record<string, unknown>)
}

/** 获取评论详情 */
export function getCommentDetail(id: number) {
  return get<Comment>(`/admin/comments/${id}`)
}

/** 删除评论 */
export function deleteComment(id: number) {
  return del(`/admin/comments/${id}`)
}

/** 审核评论（通过） */
export function approveComment(id: number) {
  return put(`/comments/${id}/audit`, { action: 'approve' } as unknown as Record<string, unknown>)
}

/** 批量删除评论 */
export function batchDeleteComments(ids: number[]) {
  return post('/admin/comments/batch-delete', { ids } as unknown as Record<string, unknown>)
}
