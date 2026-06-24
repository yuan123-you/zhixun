import type { User, Message, Conversation, PaginationParams, PageResult } from '~/types'

/** 关注用户信息（含在线状态和互关标识） */
export interface FollowUser extends User {
  isOnline?: boolean
  isMutualFollow?: boolean
}

/** 在线状态映射 */
export interface OnlineStatusMap {
  [userId: string]: boolean
}

/** 社交API */
export const socialApi = {
  /** 切换关注状态 */
  toggleFollow: (userId: number) => {
    const { post } = useApi()
    return post<{ followed: boolean; followCount: number; followerCount: number }>(`/users/${userId}/follow`)
  },

  /** 获取关注列表 */
  getFollowing: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<FollowUser>>(`/follow/following/${userId}`, params)
  },

  /** 获取粉丝列表 */
  getFollowers: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<FollowUser>>(`/follow/followers/${userId}`, params)
  },

  /** 获取用户在线状态 */
  getOnlineStatus: (userId: number) => {
    const { get } = useApi()
    return get<OnlineStatusMap>(`/follow/online-status/${userId}`)
  },

  /** 批量获取用户在线状态 */
  getBatchOnlineStatus: (userIds: number[]) => {
    const { post } = useApi()
    return post<OnlineStatusMap>('/follow/online-status/batch', { userIds })
  },

  /** 发送私信 */
  sendMessage: (conversationId: number, data: { content: string; type?: number }) => {
    const { post } = useApi()
    return post<Message>(`/conversations/${conversationId}/messages`, data)
  },

  /** 获取会话列表 */
  getConversations: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Conversation>>('/conversations', params)
  },

  /** 获取会话消息列表 */
  getMessages: (conversationId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Message>>(`/conversations/${conversationId}/messages`, params)
  },

  /** 标记会话已读 */
  markConversationRead: (conversationId: number) => {
    const { put } = useApi()
    return put(`/conversations/${conversationId}/read`)
  },

  /** 获取未读消息总数 */
  getUnreadCount: () => {
    const { get } = useApi()
    return get<{ count: number }>('/conversations/unread-count')
  },
}
