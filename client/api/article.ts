import type { Article, PaginationParams, PageResult } from '~/types'

/** 文章API */
export const articleApi = {
  /** 获取文章列表 */
  getArticles: (params?: PaginationParams & { categoryId?: number; tagId?: number }) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/articles', params)
  },

  /** 获取文章详情 */
  getArticleDetail: (id: number) => {
    const { get } = useApi()
    return get<Article>(`/articles/${id}`)
  },

  /** 创建文章 */
  createArticle: (data: Partial<Article>) => {
    const { post } = useApi()
    return post<Article>('/articles', data)
  },

  /** 更新文章 */
  updateArticle: (id: number, data: Partial<Article>) => {
    const { put } = useApi()
    return put<Article>(`/articles/${id}`, data)
  },

  /** 删除文章 */
  deleteArticle: (id: number) => {
    const { delete: del } = useApi()
    return del(`/articles/${id}`)
  },

  /** 修改文章可见性 */
  updateVisibility: (id: number, visibility: number) => {
    const { put } = useApi()
    return put(`/articles/${id}/visibility`, { visibility })
  },

  /** 发布草稿（支持定时发布） */
  publishDraft: (id: number, publishAt?: string) => {
    const { put } = useApi()
    return put(`/articles/${id}/publish`, publishAt ? { publishAt } : {})
  },

  /** 记录分享 */
  recordShare: (id: number, platform?: string) => {
    const { post } = useApi()
    return post(`/articles/${id}/share`, platform ? { platform } : undefined)
  },
}
