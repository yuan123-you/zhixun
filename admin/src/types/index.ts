// 共享类型（来自 @zhixun/shared-types 统一类型包）
// 注：ApiResponse / PageResult / UserInfo 与本地定义有差异，本地版本保留现有字段名，不在此处重导出
export {
  ErrorCode,
  ErrorMessage,
  type ArticleVO,
  type CategoryVO,
  type TagVO,
  type UserBrief,
  type ArticleCreateParams,
  type ArticleQueryParams,
} from '@zhixun/shared-types'

/** 通用分页请求参数 */
export interface PageParams {
  page: number
  pageSize: number
}

/** 通用分页响应 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** 统一API响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录响应数据 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: LoginUserInfo
}

/** 登录用户信息（登录接口返回） */
export interface LoginUserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  permissions: string[]
}

/** 用户信息 */
export interface UserInfo {
  id: number
  uid: string
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  role: string
  permissions: string[]
  status: UserStatus
  createdAt: string
  updatedAt: string
}

/** 用户状态枚举 */
export enum UserStatus {
  /** 正常 */
  Active = 'active',
  /** 封禁 */
  Banned = 'banned',
}

/** 文章状态枚举 */
export enum ArticleStatus {
  /** 草稿 */
  Draft = 'draft',
  /** 待审核 */
  Pending = 'pending',
  /** 已发布 */
  Published = 'published',
  /** 已下架 */
  Offline = 'offline',
  /** 已驳回 */
  Rejected = 'rejected',
}

/** 文章信息 */
export interface Article {
  id: number
  title: string
  summary: string
  content: string
  coverImage: string
  categoryId: number
  categoryName: string
  tags: Tag[]
  authorId: number
  authorName: string
  authorAvatar: string
  status: ArticleStatus
  viewCount: number
  likeCount: number
  commentCount: number
  location?: string
  ipAddress?: string
  rejectReason?: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

/** 文章查询参数 */
export interface ArticleQuery extends PageParams {
  keyword?: string
  status?: ArticleStatus
  categoryId?: number
  tagId?: number
  authorId?: number
  startDate?: string
  endDate?: string
}

/** 分类信息 */
export interface Category {
  id: number
  name: string
  description: string
  icon: string
  sort: number
  parentId: number
  children?: Category[]
  articleCount: number
  createdAt: string
  updatedAt: string
}

/** 标签信息 */
export interface Tag {
  id: number
  name: string
  color: string
  articleCount: number
  createdAt: string
  updatedAt: string
}

/** 评论状态枚举 */
export enum CommentStatus {
  /** 正常 */
  Active = 'active',
  /** 已删除 */
  Deleted = 'deleted',
  /** 待审核 */
  Pending = 'pending',
}

/** 评论信息 */
export interface Comment {
  id: number
  content: string
  articleId: number
  articleTitle: string
  userId: number
  username: string
  userAvatar: string
  parentId: number
  replyCount: number
  likeCount: number
  status: CommentStatus
  createdAt: string
  updatedAt: string
}

/** 评论查询参数 */
export interface CommentQuery extends PageParams {
  keyword?: string
  articleId?: number
  status?: CommentStatus
  startDate?: string
  endDate?: string
}

/** 用户查询参数 */
export interface UserQuery extends PageParams {
  keyword?: string
  status?: UserStatus
  role?: string
  startDate?: string
  endDate?: string
}

/** 敏感词级别枚举 */
export enum SensitiveWordLevel {
  /** 替换 */
  Replace = 'replace',
  /** 审核 */
  Review = 'review',
  /** 禁止 */
  Block = 'block',
}

/** 敏感词信息 */
export interface SensitiveWord {
  id: number
  word: string
  level: SensitiveWordLevel
  createdAt: string
  updatedAt: string
}

/** 操作日志信息 */
export interface OperationLog {
  id: number
  userId: number
  username: string
  module: string
  action: string
  target: string
  ip: string
  detail: string
  createdAt: string
}

/** 操作日志查询参数 */
export interface OperationLogQuery extends PageParams {
  keyword?: string
  module?: string
  userId?: number
  startDate?: string
  endDate?: string
}

/** 仪表盘统计数据 */
export interface DashboardData {
  userCount: number
  articleCount: number
  dailyActive: number
  viewCount: number
  interactionCount: number
  trendData: TrendData[]
  hotArticles: HotArticle[]
  pendingArticles: Article[]
}

/** 趋势数据 */
export interface TrendData {
  date: string
  userCount: number
  articleCount: number
  viewCount: number
}

/** 热门文章 */
export interface HotArticle {
  id: number
  title: string
  viewCount: number
  likeCount: number
  commentCount: number
}

/** 系统设置 */
export interface SystemSettings {
  siteName: string
  siteDescription: string
  logo: string
  banners: Banner[]
  announcements: Announcement[]
}

/** 轮播图 */
export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  linkType: number
  sortOrder: number
  startTime: string
  endTime: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 公告 */
export interface Announcement {
  id: number
  title: string
  content: string
  type: number
  isTop: number
  startTime: string
  endTime: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 文件上传响应 */
export interface UploadResult {
  url: string
  filename: string
  size: number
}

/** 审核操作参数 */
export interface AuditParams {
  id: number
  action: 'approve' | 'reject'
  reason?: string
}
