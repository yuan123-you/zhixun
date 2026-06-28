/** 通知类型枚举（与后端 NotificationTypeEnum 对齐） */
export enum NotificationType {
  System = 1,
  Audit = 2,
  Interact = 3,
  Follow = 4,
  Message = 5,
  CommentReply = 6,
  Mention = 7,
}

/** 通知视图（客户端用） */
export interface NotificationItem {
  id: number
  type: NotificationType
  title: string
  content: string
  sender: { id: number; nickname: string; avatar: string } | null
  isRead: boolean
  createdAt: string
}

/** 通知广播参数（管理端用） */
export interface NotificationBroadcast {
  type: NotificationType
  title: string
  content: string
  targetUserIds?: number[]
  targetAll?: boolean
}

/** 通知查询参数 */
export interface NotificationQuery {
  type?: NotificationType
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}