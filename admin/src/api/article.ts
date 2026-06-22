import { get, post, put, del } from './request'
import type { Article, ArticleQuery, PageResult, AuditParams } from '@/types'

/** 获取文章列表 */
export function getArticleList(params: ArticleQuery) {
  return get<PageResult<Article>>('/articles', params as unknown as Record<string, unknown>)
}

/** 获取文章详情 */
export function getArticleDetail(id: number) {
  return get<Article>(`/articles/${id}`)
}

/** 创建文章 */
export function createArticle(data: Partial<Article>) {
  return post<Article>('/articles', data as unknown as Record<string, unknown>)
}

/** 更新文章 */
export function updateArticle(id: number, data: Partial<Article>) {
  return put<Article>(`/articles/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除文章 */
export function deleteArticle(id: number) {
  return del(`/articles/${id}`)
}

/** 审核文章（通过/驳回） */
export function auditArticle(data: AuditParams) {
  return post(`/articles/${data.id}/audit`, {
    action: data.action,
    reason: data.reason,
  })
}

/** 下架文章 */
export function offlineArticle(id: number) {
  return put(`/articles/${id}/offline`)
}

/** 获取待审核文章列表 */
export function getPendingArticles(params: ArticleQuery) {
  return get<PageResult<Article>>('/articles/pending', params as unknown as Record<string, unknown>)
}
