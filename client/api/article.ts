import type { Article, PaginationParams, SearchResult } from '~/types'

/** 文章API */
export const articleApi = {
  /** 获取文章列表 */
  getArticles: (params?: PaginationParams & { categoryId?: number; tagId?: number }) => {
    const { get } = useApi()
    return get<SearchResult<Article>>('/articles', params)
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
}
