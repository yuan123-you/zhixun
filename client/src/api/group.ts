import type { PaginationParams, PageResult } from '@/types'

export interface GroupInfo {
  id: number
  name: string
  groupNumber: string
  avatar: string
  description: string
  ownerId: number
  ownerName: string
  memberCount: number
  maxMembers: number
  isPublic: number
  status?: number
  myRole?: number | null
  joinedAt?: string | null
}

export interface GroupMessage {
  id: number
  groupId: number
  senderId: number
  senderName: string
  senderAvatar: string
  content: string
  messageType: string
  mentionedUserIds: number[]
  createdAt: string
}

export interface GroupMember {
  id: number
  groupId: number
  userId: number
  userName: string
  userAvatar: string
  nickname: string
  role: number
  joinedAt: string
}

export interface GroupJoinRequestInfo {
  id: number
  groupId: number
  groupName: string
  userId: number
  userName: string
  userAvatar: string
  message: string
  status: number
  createdAt: string
}

export const groupApi = {
  createGroup: (data: { name: string; description?: string; avatar?: string; isPublic?: number }) => {
    const { post } = useApi()
    return post<number>('/groups', data)
  },
  getGroupDetail: (id: number) => {
    const { get } = useApi()
    return get<GroupInfo>(`/groups/${id}`)
  },
  getMyGroups: (page = 1, pageSize = 20) => {
    const { get } = useApi()
    return get<PageResult<GroupInfo>>('/groups/my', { page, pageSize })
  },
  leaveGroup: (id: number) => {
    const { post } = useApi()
    return post<void>(`/groups/${id}/leave`)
  },
  dismissGroup: (id: number) => {
    const { delete: del } = useApi()
    return del(`/groups/${id}`)
  },
  inviteMembers: (groupId: number, userIds: number[]) => {
    const { post } = useApi()
    return post<void>('/groups/invite', { groupId, userIds })
  },
  kickMember: (groupId: number, userId: number) => {
    const { post } = useApi()
    return post<void>(`/groups/${groupId}/kick/${userId}`)
  },
  setAdmin: (groupId: number, userId: number, isAdmin: boolean) => {
    const { post } = useApi()
    return post<void>(`/groups/${groupId}/admin/${userId}?isAdmin=${isAdmin}`)
  },
  getMessages: (groupId: number, offset = 0, limit = 50) => {
    const { get } = useApi()
    return get<GroupMessage[]>(`/groups/${groupId}/messages`, { offset, limit })
  },
  sendMessage: (groupId: number, content: string, messageType = 'text', mentionedUserIds?: number[]) => {
    const { post } = useApi()
    return post<GroupMessage>('/groups/messages', { groupId, content, messageType, mentionedUserIds })
  },
  searchMessages: (groupId: number, params: { keyword?: string; messageType?: string; startDate?: string; endDate?: string; senderId?: number; offset?: number; limit?: number }) => {
    const { get } = useApi()
    return get<GroupMessage[]>(`/groups/${groupId}/messages/search`, params)
  },
  sendAIMessage: (groupId: number, question: string) => {
    const { post } = useApi()
    return post<GroupMessage>('/groups/messages/ai', { groupId, question })
  },
  searchGroups: (keyword: string, page = 1, pageSize = 20) => {
    const { get } = useApi()
    return get<PageResult<GroupInfo>>('/groups/search', { keyword, page, pageSize })
  },
  getMembers: (groupId: number) => {
    const { get } = useApi()
    return get<GroupMember[]>(`/groups/${groupId}/members`)
  },
  requestJoin: (groupId: number, message?: string) => {
    const { post } = useApi()
    return post<void>(`/groups/${groupId}/request-join`, null, { params: { message } })
  },
  getPendingRequests: (groupId: number) => {
    const { get } = useApi()
    return get<GroupJoinRequestInfo[]>(`/groups/${groupId}/requests`)
  },
  approveJoinRequest: (requestId: number) => {
    const { post } = useApi()
    return post<void>(`/groups/requests/${requestId}/approve`)
  },
  rejectJoinRequest: (requestId: number) => {
    const { post } = useApi()
    return post<void>(`/groups/requests/${requestId}/reject`)
  },
}
