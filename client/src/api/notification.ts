import type { Notification, PaginationParams, PageResult } from '@/types'

/** 通知API */
export const notificationApi = {
  /** 获取通知列表 */
  getNotifications: (params?: PaginationParams & { type?: number }) => {
    const { get } = useApi()
    return get<PageResult<Notification>>('/notifications', params)
  },

  /** 标记通知为已读 */
  markAsRead: (id: number) => {
    const { put } = useApi()
    return put(`/notifications/${id}/read`)
  },

  /** 全部标记为已读 */
  markAllAsRead: () => {
    const { put } = useApi()
    return put('/notifications/read-all')
  },

  /** 获取未读通知数量 */
  getUnreadCount: () => {
    const { get } = useApi()
    return get<{ unread_count: number }>('/notifications/unread-count')
  },

  /** 删除通知 */
  deleteNotification: (id: number) => {
    const { delete: del } = useApi()
    return del(`/notifications/${id}`)
  },

  /** 批量标记已读 */
  batchMarkAsRead: (ids: number[]) => {
    const { put } = useApi()
    return put('/notifications/batch-read', ids as any)
  },

  /** 批量删除通知 */
  batchDeleteNotifications: (ids: number[]) => {
    const { delete: del } = useApi()
    return del('/notifications/batch', { data: ids })
  },
}
