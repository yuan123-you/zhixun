import type { Notification, PaginationParams, SearchResult } from '~/types'

/** 通知API */
export const notificationApi = {
  /** 获取通知列表 */
  getNotifications: (params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Notification>>('/notifications', params)
  },

  /** 标记通知为已读 */
  markAsRead: (id: number) => {
    const { put } = useApi()
    return put(`/notifications/${id}/read`)
  },

  /** 获取未读通知数量 */
  getUnreadCount: () => {
    const { get } = useApi()
    return get<{ count: number }>('/notifications/unread-count')
  },
}
