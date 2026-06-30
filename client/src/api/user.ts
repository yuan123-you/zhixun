import type { User, UserSettings, Article, Comment, PaginationParams, PageResult } from '@/types'

/** 用户API */
export const userApi = {
  /** 获取用户资料（当前登录用户或指定用户） */
  getProfile: (userId?: number) => {
    const { get } = useApi()
    return get<User>(userId ? `/users/${userId}` : '/user/profile')
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

  /** 获取用户发布的作品（他人） - 使用作品列表接口按userId筛选 */
  getUserArticles: (userId: number, params?: PaginationParams & { sortBy?: string }) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/articles', { ...params, userId, sortBy: params?.sortBy ?? 'latest' })
  },

  /** 获取我发布的作品 */
  getMyArticles: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/articles', params)
  },

  /** 获取我收藏的作品 */
  getMyCollections: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/user/collects', params)
  },

  /** 获取我点赞的作品 */
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

  /** 自动更新IP属地（已登录：通过 Token 调用后端接口入库） */
  updateIpLocation: () => {
    const { post } = useApi()
    return post<string>('/user/ip-location')
  },

  /** 匿名查询当前 IP 属地（不依赖登录态） */
  getIpLocationAnonymous: () => {
    const { get } = useApi()
    return get<string>('/users/ip-location')
  },
}
