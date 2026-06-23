import type { Article, PaginationParams, PageResult } from '~/types'

/** 推荐信息流请求参数 */
export interface RecommendParams extends PaginationParams {
  /** 是否刷新获取新一批推荐（1=刷新） */
  refresh?: number
  /** 刷新密钥，用于翻页时保持同一批推荐内容 */
  refresh_key?: string
}

/** 信息流API */
export const feedApi = {
  /** 获取推荐信息流 */
  getRecommendFeed: (params?: RecommendParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/feed/recommend', params)
  },

  /** 获取热门信息流 */
  getHotFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/feed/hot', params)
  },

  /** 获取最新信息流 */
  getLatestFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/feed/latest', params)
  },

  /** 获取关注信息流 */
  getFollowingFeed: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/feed/following', params)
  },
}
