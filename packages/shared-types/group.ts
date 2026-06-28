/** 群组信息（客户端与管理端共享） */
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
  /** 群组状态：0=正常，1=禁言 */
  status: number
  createdAt: string
  updatedAt: string
}

/** 群组成员 */
export interface GroupMember {
  id: number
  groupId: number
  userId: number
  username: string
  nickname: string
  avatar: string
  /** 角色：0=成员，1=管理员，2=群主 */
  role: number
  joinedAt: string
}

/** 群组消息 */
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

/** 群组查询参数 */
export interface GroupQuery {
  keyword?: string
  status?: number
  page?: number
  pageSize?: number
}