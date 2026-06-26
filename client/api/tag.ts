import type { Tag, Article, PageResult } from '~/types'

/** 标签API */
export const tagApi = {
  /** 获取标签云 */
  getTagCloud: () => {
    const { get } = useApi()
    return get<Tag[]>('/tags/cloud')
  },

  /** 获取热门标签 */
  getHotTags: (limit: number = 20) => {
    const { get } = useApi()
    return get<Tag[]>('/tags/hot', { limit })
  },

  /** 获取所有标签 */
  getTags: () => {
    const { get } = useApi()
    return get<Tag[]>('/tags')
  },

  /** 搜索标签 */
  searchTags: (keyword: string) => {
    const { get } = useApi()
    return get<Tag[]>('/tags/search', { keyword })
  },

  /** 关注标签 */
  followTag: (id: number) => {
    const { post } = useApi()
    return post(`/tags/${id}/follow`)
  },

  /** 取消关注标签 */
  unfollowTag: (id: number) => {
    const { delete: del } = useApi()
    return del(`/tags/${id}/follow`)
  },

  /** 获取已关注的标签 */
  getFollowedTags: () => {
    const { get } = useApi()
    return get<Tag[]>('/tags/followed')
  },

  /** 获取标签下的作品 */
  getTagArticles: (tagId: number, page: number = 1, pageSize: number = 20) => {
    const { get } = useApi()
    return get<PageResult<Article>>('/articles', { tag_id: tagId, page, pageSize })
  },
}
