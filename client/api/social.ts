import type { User, Message, Conversation, PaginationParams, SearchResult } from '~/types'

/** 社交API */
export const socialApi = {
  /** 切换关注状态 */
  toggleFollow: (userId: number) => {
    const { post } = useApi()
    return post<{ isFollowing: boolean; followerCount: number }>(`/users/${userId}/follow`)
  },

  /** 获取关注列表 */
  getFollowing: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<User>>(`/users/${userId}/following`, params)
  },

  /** 获取粉丝列表 */
  getFollowers: (userId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<User>>(`/users/${userId}/followers`, params)
  },

  /** 发送私信 */
  sendMessage: (conversationId: number, data: { content: string; type?: number }) => {
    const { post } = useApi()
    return post<Message>(`/conversations/${conversationId}/messages`, data)
  },

  /** 获取会话列表 */
  getConversations: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Conversation>>('/conversations', params)
  },

  /** 获取会话消息列表 */
  getMessages: (conversationId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Message>>(`/conversations/${conversationId}/messages`, params)
  },
}
