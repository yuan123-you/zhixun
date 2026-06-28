/** 会话信息（管理端私信监控） */
export interface ConversationInfo {
  id: number
  user1Id: number
  user1Name: string
  user2Id: number
  user2Name: string
  lastMessage: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

/** 私信消息 */
export interface MessageInfo {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  receiverId: number
  receiverName: string
  content: string
  messageType: string
  createdAt: string
}

/** 私信查询参数 */
export interface MessageQuery {
  keyword?: string
  conversationId?: number
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}