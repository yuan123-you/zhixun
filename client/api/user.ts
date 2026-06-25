import type { User, UserSettings, Article, Comment, PaginationParams, PageResult } from '~/types'

/** 用户API */
export const userApi = {
  /** 获取用户资料（当前登录用户或指定用户） */
  getProfile: (userId?: number) => {
    const { get } = useApi()
    return get<User>(userId ? `/users/${userId}` : '/users/me')
  },

  /** 更新用户资料 */
  updateProfile: (data: Partial<User>) => {
    const { put } = useApi()
    return put<User>('/user/profile', data)
  },

  /** 修改UID */
  updateUid: (uid: string) => {
    const { put } = useApi()
    return put<User>('/user/uid', { uid })
  },

  /** 通过UID查找用户 */
  findByUid: (uid: string) => {
    const { get } = useApi()
    return get<User>('/users/by-uid', { uid })
  },

  /** 获取用户设置 */
  getSettings: () => {
    const { get } = useApi()
    return get<UserSettings>('/user/settings')
  },

  /** 更新用户设置 */
  updateSettings: (data: Partial<UserSettings>) => {
    const { put } = useApi()
    return put<UserSettings>('/user/settings', data)
  },

  /** 获取浏览历史 */
  getViewHistory: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/view-history', params)
  },

  /** 获取用户发布的文章（他人） - 使用文章列表接口按userId筛选 */
  getUserArticles: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/articles', { ...params, userId })
  },

  /** 获取我发布的文章 */
  getMyArticles: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/articles', params)
  },

  /** 获取我收藏的文章 */
  getMyCollections: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/collects', params)
  },

  /** 获取我点赞的文章 */
  getMyLikes: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/likes', params)
  },

  /** 获取我发表的评论 */
  getMyComments: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Comment>>('/user/comments', params)
  },

  /** 获取我的草稿 */
  getMyDrafts: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/articles', { ...params, status: 0 })
  },

  /** 自动更新IP属地 */
  updateIpLocation: () => {
    const { post } = useApi()
    return post<string>('/user/ip-location')
  },
}
