import type { User, UserSettings, Article, Comment, PaginationParams, PageResult } from '~/types'

/** 用户API */
export const userApi = {
  /** 获取用户资料 */
  getProfile: (userId?: number) => {
    const { get } = useApi()
    return get<User>(userId ? `/users/${userId}` : '/users/me')
  },

  /** 更新用户资料 */
  updateProfile: (data: Partial<User>) => {
    const { put } = useApi()
    return put<User>('/users/me', data)
  },

  /** 获取用户设置 */
  getSettings: () => {
    const { get } = useApi()
    return get<UserSettings>('/users/me/settings')
  },

  /** 更新用户设置 */
  updateSettings: (data: Partial<UserSettings>) => {
    const { put } = useApi()
    return put<UserSettings>('/users/me/settings', data)
  },

  /** 获取浏览历史 */
  getViewHistory: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/users/me/history', params)
  },

  /** 获取用户发布的文章（他人） */
  getUserArticles: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>(`/users/${userId}/articles`, params)
  },

  /** 获取我发布的文章 */
  getMyArticles: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/users/me/articles', params)
  },

  /** 获取我收藏的文章 */
  getMyCollections: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/users/me/collections', params)
  },

  /** 获取我点赞的文章 */
  getMyLikes: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/users/me/likes', params)
  },

  /** 获取我发表的评论 */
  getMyComments: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Comment>>('/users/me/comments', params)
  },
}
