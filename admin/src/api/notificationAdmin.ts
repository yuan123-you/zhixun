import { get, post } from './request'
import type { NotificationBroadcast, NotificationQuery, PageResult } from '@/types'

/** 获取通知列表 */
export function getNotificationList(params: NotificationQuery) {
  return get<PageResult<any>>('/admin/notifications', params as unknown as Record<string, unknown>)
}

/** 发送系统通知 */
export function sendNotification(data: NotificationBroadcast) {
  return post('/admin/notifications/send', data as unknown as Record<string, unknown>)
}