import { get, post, put, del } from './request'
import type { Article, ArticleQuery, PageResult, AuditParams } from '@/types'

/** 获取作品列表 */
export function getArticleList(params: ArticleQuery) {
  return get<PageResult<Article>>('/articles', params as unknown as Record<string, unknown>)
}

/** 获取作品详情 */
export function getArticleDetail(id: number) {
  return get<Article>(`/articles/${id}`)
}

/** 创建作品 */
export function createArticle(data: Partial<Article>) {
  return post<Article>('/articles', data as unknown as Record<string, unknown>)
}

/** 更新作品 */
export function updateArticle(id: number, data: Partial<Article>) {
  return put<Article>(`/articles/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除作品 */
export function deleteArticle(id: number) {
  return del(`/articles/${id}`)
}

/** 审核作品（通过/驳回） */
export function auditArticle(data: AuditParams) {
  return put(`/admin/articles/${data.id}/audit`, {
    status: data.status,
    reason: data.reason,
  } as unknown as Record<string, unknown>)
}

/** 下架作品 */
export function offlineArticle(id: number) {
  return put(`/articles/${id}/status`, { status: 4 } as unknown as Record<string, unknown>)
}

/** 获取待审核作品列表 */
export function getPendingArticles(params: ArticleQuery) {
  return get<PageResult<Article>>('/admin/articles/pending', params as unknown as Record<string, unknown>)
}
