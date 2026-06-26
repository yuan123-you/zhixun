/** 作品视图对象 */
export interface ArticleVO {
  id: number
  title: string
  summary: string
  content?: string
  coverImage: string
  status: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  isTop: boolean
  isRecommended: boolean
  categoryId?: number
  categoryName?: string
  tags?: TagVO[]
  author?: UserBrief
  deviceInfo?: string
  location?: string
  ipAddress?: string
  createdAt: string
  updatedAt: string
}

/** 用户简要信息 */
export interface UserBrief {
  id: number
  username: string
  nickname: string
  avatar: string
}

/** 标签视图对象 */
export interface TagVO {
  id: number
  name: string
  articleCount?: number
}

/** 作品创建参数 */
export interface ArticleCreateParams {
  title: string
  content: string
  summary?: string
  coverImage?: string
  categoryId?: number
  tagIds?: number[]
  status?: 'published' | 'draft'
}

/** 作品查询参数 */
export interface ArticleQueryParams {
  keyword?: string
  categoryId?: number
  tagId?: number
  status?: string
  sort?: string
  page?: number
  pageSize?: number
}
