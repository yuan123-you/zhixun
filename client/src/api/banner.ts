import type { ApiResponse } from '@/types'

/** 轮播图接口 */
export interface BannerItem {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  linkType: number
  sortOrder: number
  startTime: string
  endTime: string
  status: number
}

/** 公告接口 */
export interface AnnouncementItem {
  id: number
  title: string
  content: string
  type: number
  isTop: number
  startTime: string
  endTime: string
  status: number
}

/** 轮播图API */
export const bannerApi = {
  /** 获取有效轮播图列表 */
  getActiveBanners: () => {
    const { get } = useApi()
    return get<BannerItem[]>('/banners')
  },
}

/** 公告API */
export const announcementApi = {
  /** 获取有效公告列表 */
  getActiveAnnouncements: () => {
    const { get } = useApi()
    return get<AnnouncementItem[]>('/announcements')
  },
}
