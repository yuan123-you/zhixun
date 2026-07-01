import { get, post, put, del } from './request'
import type { Announcement, PageParams } from '@/types'

/** 获取公告列表（管理端） */
export function getAnnouncementList(params?: PageParams & { keyword?: string }) {
  return get<Announcement[]>('/admin/announcements', params as unknown as Record<string, unknown>)
}

/** 获取有效公告列表（公开） */
export function getActiveAnnouncements() {
  return get<Announcement[]>('/announcements')
}

/** 创建公告 */
export function createAnnouncement(data: Partial<Announcement>) {
  return post<Announcement>('/admin/announcements', data as unknown as Record<string, unknown>)
}

/** 更新公告 */
export function updateAnnouncement(id: number, data: Partial<Announcement>) {
  return put<Announcement>(`/admin/announcements/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除公告 */
export function deleteAnnouncement(id: number) {
  return del(`/admin/announcements/${id}`)
}
