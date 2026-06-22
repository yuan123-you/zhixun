import type { User, UserSettings, Article, PaginationParams, SearchResult } from '~/types'

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
    return get<SearchResult<Article>>('/users/me/history', params)
  },

  /** 获取用户发布的文章 */
  getUserArticles: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Article>>(`/users/${userId}/articles`, params)
  },
}
