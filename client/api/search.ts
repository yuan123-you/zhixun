import type { Article, User, PaginationParams, SearchResult } from '~/types'

/** 搜索API */
export const searchApi = {
  /** 综合搜索 */
  search: (keyword: string, type: 'all' | 'articles' | 'users' | 'images', params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Article | User>>('/search', { keyword, type, ...params })
  },

  /** 获取搜索建议 */
  getSuggestions: (keyword: string) => {
    const { get } = useApi()
    return get<string[]>('/search/suggestions', { keyword })
  },

  /** 获取热门搜索 */
  getHotSearches: () => {
    const { get } = useApi()
    return get<string[]>('/search/hot')
  },

  /** 清除搜索历史 */
  clearHistory: () => {
    const { delete: del } = useApi()
    return del('/search/history')
  },
}
