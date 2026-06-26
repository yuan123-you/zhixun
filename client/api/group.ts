import type { PaginationParams, PageResult } from '~/types'

export interface GroupInfo {
  id: number
  name: string
  avatar: string
  description: string
  ownerId: number
  ownerName: string
  memberCount: number
  maxMembers: number
  isPublic: number
  myRole: number
  joinedAt: string
}

export interface GroupMessage {
  id: number
  groupId: number
  senderId: number
  senderName: string
  senderAvatar: string
  content: string
  messageType: string
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
  joinGroup: (id: number) => {
    const { post } = useApi()
    return post<void>(`/groups/${id}/join`)
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
  sendMessage: (groupId: number, content: string, messageType = 'text') => {
    const { post } = useApi()
    return post<GroupMessage>('/groups/messages', { groupId, content, messageType })
  },
  searchGroups: (keyword: string, page = 1, pageSize = 20) => {
    const { get } = useApi()
    return get<PageResult<GroupInfo>>('/groups/search', { keyword, page, pageSize })
  },
}