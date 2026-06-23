import type { Article, User, PaginationParams, SearchResultVO } from '~/types'

/** 搜索建议项 */
export interface SuggestionItem {
  type: 'user' | 'article' | 'tag'
  id: number
  text: string
  avatar?: string
}

/** 搜索建议结果 */
export interface SearchSuggestResult {
  completions: SuggestionItem[]
  hotSearches: string[]
}

/** 搜索参数 */
export interface SearchParams extends PaginationParams {
  categoryId?: number
  tagId?: number
  timeRange?: '24h' | '7d' | '30d'
  startDate?: string
  endDate?: string
  sort?: 'relevance' | 'latest' | 'popular'
}

/** 搜索API */
export const searchApi = {
  /** 综合搜索 */
  search: (keyword: string, type: 'all' | 'articles' | 'users' | 'images', params?: SearchParams) => {
    const { get } = useApi()
    return get<SearchResultVO<Article | User>>('/search', { keyword, type, ...params })
  },

  /** 获取搜索建议 */
  getSuggestions: (keyword: string) => {
    const { get } = useApi()
    return get<SearchSuggestResult>('/search/suggest', { keyword })
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
