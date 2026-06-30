import type { User, Message, Conversation, PaginationParams, PageResult } from '@/types'

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
    return get<PageResult<FollowUser>>(`/users/${userId}/following`, params)
  },

  /** 获取粉丝列表 */
  getFollowers: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<FollowUser>>(`/users/${userId}/followers`, params)
  },

  /** 获取用户在线状态 */
  getOnlineStatus: (userId: number) => {
    const { get } = useApi()
    return get<OnlineStatusMap>(`/users/${userId}/online-status`)
  },

  /** 批量获取用户在线状态 */
  getBatchOnlineStatus: (userIds: number[]) => {
    const { get } = useApi()
    return get<OnlineStatusMap>('/user/online-status/batch', { userIds: userIds.join(',') })
  },

  /** 发送私信（userId为对方用户ID） */
  sendMessage: (userId: number, data: { content: string; type?: string }) => {
    const { post } = useApi()
    return post<Message>(`/conversations/${userId}/messages`, {
      content: data.content,
      type: data.type || 'text',
    })
  },

  /** 发送AI助手消息（私信场景，userId为会话目标用户ID） */
  sendAIMessage: (userId: number, question: string) => {
    const { post } = useApi()
    // AI 回复需要调用外部大模型服务，后端超时为 120s + 重试，客户端须留足余量
    return post<Message>(`/conversations/${userId}/ai`, { question }, { timeout: 130_000 })
  },

  /** 获取会话列表 */
  getConversations: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Conversation>>('/conversations', params)
  },

  /** 获取与某用户的私信记录（userId为对方用户ID） */
  getMessages: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<PageResult<Message>>(`/conversations/${userId}/messages`, params)
  },

  /** 标记与某用户的私信已读（userId为对方用户ID） */
  markConversationRead: (userId: number) => {
    const { put } = useApi()
    return put(`/conversations/${userId}/read`)
  },

  /** 获取未读消息总数 */
  getUnreadCount: () => {
    const { get } = useApi()
    return get<{ count: number }>('/conversations/unread-count')
  },
}
