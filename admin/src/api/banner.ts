import { get, post, put, del } from './request'
import type { Banner, PageResult, PageParams } from '@/types'

/** 获取轮播图列表（管理端） */
export function getBannerList(params?: PageParams & { keyword?: string }) {
  return get<Banner[]>('/admin/banners', params as unknown as Record<string, unknown>)
}

/** 获取有效轮播图列表（公开） */
export function getActiveBanners() {
  return get<Banner[]>('/banners')
}

/** 创建轮播图 */
export function createBanner(data: Partial<Banner>) {
  return post<Banner>('/admin/banners', data as unknown as Record<string, unknown>)
}

/** 更新轮播图 */
export function updateBanner(id: number, data: Partial<Banner>) {
  return put<Banner>(`/admin/banners/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除轮播图 */
export function deleteBanner(id: number) {
  return del(`/admin/banners/${id}`)
}
