import type { Article, PaginationParams, SearchResult } from '~/types'

/** 信息流API */
export const feedApi = {
  /** 获取推荐信息流 */
  getRecommendFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Article>>('/feed/recommend', params)
  },

  /** 获取最新信息流 */
  getLatestFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Article>>('/feed/latest', params)
  },

  /** 获取关注信息流 */
  getFollowingFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Article>>('/feed/following', params)
  },
}
