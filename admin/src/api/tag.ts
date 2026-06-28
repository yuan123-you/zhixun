import { get, post, put, del } from './request'
import type { Tag, PageResult, PageParams } from '@/types'

/** 获取标签列表（管理端分页） */
export function getTagList(params?: PageParams & { keyword?: string; sortBy?: string }) {
  return get<PageResult<Tag>>('/admin/tags', params as unknown as Record<string, unknown>)
}

/** 获取所有标签（不分页，用于下拉选择） */
export function getAllTags() {
  return get<Tag[]>('/tags/all')
}

/** 获取标签详情 */
export function getTagDetail(id: number) {
  return get<Tag>(`/tags/${id}`)
}

/** 创建标签 */
export function createTag(data: Partial<Tag>) {
  return post<Tag>('/tags', data as unknown as Record<string, unknown>)
}

/** 更新标签 */
export function updateTag(id: number, data: Partial<Tag>) {
  return put<Tag>(`/tags/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除标签 */
export function deleteTag(id: number) {
  return del(`/tags/${id}`)
}

/** 合并标签 */
export function mergeTag(sourceTagId: number, targetTagId: number) {
  return post<void>('/tags/merge', { sourceTagId, targetTagId } as unknown as Record<string, unknown>)
}

/** 同步标签作品数 */
export function syncArticleCount(tagId?: number) {
  const params = tagId ? { tagId } as unknown as Record<string, unknown> : undefined
  return post<void>('/tags/sync-article-count', params)
}

/** 搜索标签（用于自动补全） */
export function searchTags(keyword: string) {
  return get<Tag[]>('/tags/search', { keyword } as unknown as Record<string, unknown>)
}
