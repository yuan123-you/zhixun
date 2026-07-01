// 共享类型（来自 @zhixun/shared-types 统一类型包）
export {
  ErrorCode,
  ErrorMessage,
  type ApiResponse,
  type PageResult,
  type ArticleVO,
  type CategoryVO,
  type TagVO,
  type UserBrief,
  type ArticleCreateParams,
  type ArticleQueryParams,
  // 群组相关
  type GroupInfo,
  type GroupMember,
  type GroupMessage,
  type GroupQuery,
  // 私信相关
  type ConversationInfo,
  type MessageInfo,
  type MessageQuery,
  // 通知相关
  NotificationType,
  type NotificationBroadcast,
  type NotificationQuery,
  // 登录日志
  type LoginLog,
  type LoginLogQuery,
  // 仪表盘
  type DashboardData,
  type TrendData,
  type RetentionRate,
  type ActivityDistribution,
  type GrowthTrend,
  type CategoryDistribution,
  type HotArticleRank,
  type CreatorRank,
} from '@zhixun/shared-types'

/** 通用分页请求参数 */
export interface PageParams {
  page: number
  pageSize: number
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

/** 作品状态枚举 */
export enum ArticleStatus {
  /** 草稿 */
  Draft = 0,
  /** 待审核 */
  Pending = 1,
  /** 已发布 */
  Published = 2,
  /** 已驳回 */
  Rejected = 3,
  /** 已下架 */
  Offline = 4,
}

/** 作品信息 */
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

/** 作品查询参数 */
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

/** 评论状态（与后端 CommentStatusEnum 对应：0=待审核, 1=正常, 2=已删除） */
export type CommentStatus = 0 | 1 | 2

/** 评论信息 */
export interface Comment {
  id: number
  content: string
  articleId: number
  articleTitle: string
  userId: number
  user?: {
    id: number
    username: string
    nickname: string
    avatar: string
  }
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
  /** 状态值：2=通过(发布)，3=驳回 */
  status: number
  reason?: string
}

// ========== 增强的仪表盘数据（内联定义，避免 @zhixun/shared-types 模块解析问题） ==========

/** 增强的仪表盘统计数据 */
export interface EnhancedDashboardData {
  userTotal: number
  articleTotal: number
  todayDau: number
  todayView: number
  todayLike: number
  todayComment: number
  trend: TrendData
  retentionRates: RetentionRate[]
  activityDistributions: ActivityDistribution[]
  growthTrends: GrowthTrend[]
  categoryDistributions: CategoryDistribution[]
  hotArticleRanks: HotArticleRank[]
  creatorRanks: CreatorRank[]
  pendingArticles?: Article[]
}

// ========== AI 用量监控 ==========

/** AI 使用统计 */
export interface AIUsageStats {
  totalRequests: number
  todayRequests: number
  textGenerationCount: number
  imageGenerationCount: number
  totalTokens: number
  averageResponseTime: number
  userCount: number
  dailyStats: AIUsageDailyStat[]
  topUsers: AIUsageUserStat[]
}

/** AI 日统计 */
export interface AIUsageDailyStat {
  date: string
  requestCount: number
  tokenCount: number
}

/** AI 用户用量 */
export interface AIUsageUserStat {
  userId: number
  username: string
  nickname: string
  requestCount: number
  tokenCount: number
}

// ========== 协作管理 ==========

/** 协作信息 */
export interface CollaborationInfo {
  id: number
  articleId: number
  articleTitle: string
  inviterId: number
  inviterName: string
  inviteeId: number
  inviteeName: string
  permission: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 协作查询参数 */
export interface CollaborationQuery extends PageParams {
  articleId?: number
  status?: number
}

// ========== 安全审计日志 ==========

/** 安全审计日志 */
export interface SecurityAuditLog {
  id: number
  eventType: string
  userId: number
  username: string
  ip: string
  userAgent: string
  requestMethod: string
  requestUri: string
  requestBody: string
  responseStatus: number
  riskLevel: string
  detail: string
  createdAt: string
}

/** 安全审计日志查询参数 */
export interface SecurityAuditLogQuery extends PageParams {
  eventType?: string
  userId?: number
  ip?: string
  riskLevel?: string
  startDate?: string
  endDate?: string
}

/** 安全审计统计 */
export interface SecurityAuditStats {
  total: number
  eventTypeStats: { eventType: string; count: number }[]
  dailyStats: { date: string; count: number }[]
  topIps: { ip: string; count: number }[]
}

// ========== 缓存管理 ==========

/** 缓存状态 */
export interface CacheStatus {
  cacheName: string
  type: string
  size: number
  hitRate: number
  missRate: number
  evictionCount: number
}

/** 缓存一致性检查结果 */
export interface CacheConsistencyResult {
  articleDetail: {
    checkedCount: number
    inconsistentCount: number
    details: { id: number; cacheVersion: number; dbVersion: number }[]
  }
  consistent: boolean
  error?: string
}

// ========== 增强的用户信息 ==========

/** 增强的用户详情（管理端查看） */
export interface UserDetail extends UserInfo {
  uid: string
  followCount: number
  followerCount: number
  articleCount: number
  totalLikeCount: number
  province: string
  ipLocation: string
  gender: number
  birthday: string
  bio: string
  lastLoginAt: string
  lastLoginIp: string
  loginCount: number
  showGenderOnProfile: boolean
  showOnlineStatus: boolean
  messagePermission: number
  settings: UserSettingsAdmin
}

/** 用户设置（管理端查看） */
export interface UserSettingsAdmin {
  notification: {
    notifySystem: number
    notifyInteract: number
    notifyMessage: number
    notifyFollow: number
  }
  privacy: {
    showOnlineStatus: number
    messagePermission: number
    saveViewHistory: number
    contentRecommend: number
    showViewCount: number
    allowSearch: number
  }
  display: {
    fontSize: number
    theme: string
    language: string
  }
}

// ========== 浏览历史统计 ==========

/** 浏览历史统计 */
export interface ViewHistoryStats {
  totalViews: number
  todayViews: number
  uniqueVisitors: number
  avgViewsPerUser: number
  topArticles: { articleId: number; title: string; viewCount: number }[]
  hourlyStats: { hour: number; count: number }[]
}

// ========== 搜索分析 ==========

/** 搜索分析 */
export interface SearchAnalytics {
  totalSearches: number
  todaySearches: number
  avgResultCount: number
  zeroResultRate: number
  topKeywords: { keyword: string; count: number }[]
  topNoResultKeywords: { keyword: string; count: number }[]
}

// ========== 收藏统计 ==========

/** 收藏统计 */
export interface CollectStats {
  totalCollects: number
  todayCollects: number
  topCollectedArticles: { articleId: number; title: string; collectCount: number; authorName: string }[]
}
