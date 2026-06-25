// 共享类型（来自 @zhixun/shared-types 统一类型包）
// 注：ApiResponse / PageResult 与本地定义冲突，本地版本保持现有字段名（如 list/page_size），不在此处重导出
export {
  ErrorCode,
  ErrorMessage,
  type ArticleVO,
  type CategoryVO,
  type TagVO,
  type UserBrief,
  type UserInfo,
  type ArticleCreateParams,
  type ArticleQueryParams,
} from '@zhixun/shared-types'

// TypeScript 类型定义

/** 文章接口 */
export interface Article {
  id: number
  title: string
  content: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  tags: Tag[]
  /** 作者对象（部分接口返回） */
  author?: User
  /** 作者昵称（后端ArticleVO扁平字段） */
  authorName?: string
  /** 作者头像（后端ArticleVO扁平字段） */
  authorAvatar?: string
  /** 发布设备信息（后端ArticleVO扁平字段） */
  deviceInfo?: string
  /** 发布位置 */
  location?: string
  /** 发布IP属地 */
  ipAddress?: string
  likeCount: number
  collectCount: number
  commentCount: number
  viewCount: number
  shareCount: number
  isLiked: boolean
  isCollected: boolean
  status: ArticleStatus
  isTop?: number
  /** 可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己 */
  visibility?: number
  /** 搜索结果中的正文内容片段（含高亮标记<em>） */
  contentSnippet?: string
  /** 搜索匹配类型：title=标题匹配, content=正文匹配, summary=摘要匹配 */
  matchType?: string
  createdAt: string
  updatedAt: string
}

/** 文章状态 */
export enum ArticleStatus {
  Draft = 0,
  Published = 1,
  Deleted = 2,
}

/** 文章可见性 */
export enum ArticleVisibility {
  Public = 0,
  Followers = 1,
  Mutual = 2,
  Private = 3,
}

/** 用户接口 */
export interface User {
  id: number
  uid: string
  uidUpdatedAt?: string
  username: string
  nickname: string
  avatar: string
  bio: string
  email: string
  phone: string
  gender: Gender
  birthday: string
  role?: string
  province?: string
  followCount: number
  followerCount: number
  articleCount: number
  likeCount: number
  isFollowing: boolean
  createdAt: string
}

/** 性别枚举 */
export enum Gender {
  Unknown = 0,
  Male = 1,
  Female = 2,
}

/** 评论接口 */
export interface Comment {
  id: number
  articleId: number
  userId: number
  user: User
  content: string
  parentId: number | null
  replyUser?: User
  replies: Comment[]
  likeCount: number
  isLiked: boolean
  status: number // 0=待审核, 1=正常, 2=已删除
  createdAt: string
}

/** 分类接口 */
export interface Category {
  id: number
  name: string
  icon: string
  description: string
  articleCount: number
  sortOrder: number
}

/** 标签接口 */
export interface Tag {
  id: number
  name: string
  articleCount: number
  /** 当前用户是否已关注 */
  isFollowed?: boolean
}

/** 通知接口 */
export interface Notification {
  id: number
  type: NotificationType
  title: string
  content: string
  sender: User | null
  isRead: boolean
  createdAt: string
}

/** 通知类型枚举（与后端 NotificationTypeEnum 对齐） */
export enum NotificationType {
  System = 1,
  Audit = 2,
  Interact = 3,
  Follow = 4,
  Message = 5,
  CommentReply = 6,
  Mention = 7,
}

/** 消息接口 */
export interface Message {
  id: number
  conversationId: number
  senderId: number
  sender: User
  content: string
  type: MessageType
  isRead: boolean
  createdAt: string
}

/** 消息类型枚举 */
export enum MessageType {
  Text = 0,
  Image = 1,
  System = 2,
}

/** 会话接口 */
export interface Conversation {
  id: number
  user: User
  lastMessage: Message
  unreadCount: number
  updatedAt: string
}

/** 排行项接口（与后端 HotArticleVO 对齐） */
export interface RankItem {
  /** 文章ID（后端字段名 id） */
  id: number
  /** 标题 */
  title: string
  /** 作者昵称（后端字段名 authorNickname） */
  authorNickname?: string
  /** 热度分数（后端字段名 score） */
  score?: number
  /** 浏览量 */
  viewCount?: number
  /** 点赞数 */
  likeCount?: number
  /** 评论数 */
  commentCount?: number
  /** 创建时间 */
  createdAt?: string
}

/** 分页结果接口（与后端 PageResult 对齐） */
export interface PageResult<T> {
  /** 数据列表（后端字段名 list） */
  list: T[]
  /** 总记录数 */
  total: number
  /** 当前页码 */
  page: number
  /** 每页大小（后端字段名 page_size） */
  page_size: number
}

/** 搜索结果接口（与后端 SearchResultVO 对齐） */
export interface SearchResultVO<T = any> {
  /** 搜索关键词 */
  keyword: string
  /** 总结果数 */
  total: number
  /** 搜索类型 */
  type: string
  /** 文章列表（type=article 或 type=all 时有值） */
  articles: T[]
  /** 文章总数 */
  articleTotal?: number
  /** 用户列表（type=user 或 type=all 时有值） */
  users: T[]
  /** 用户总数 */
  userTotal?: number
  /** 图片列表（type=image 或 type=all 时有值） */
  images: T[]
  /** 图片总数 */
  imageTotal?: number
  /** 搜索耗时（毫秒） */
  took: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email: string
  code: string
}

/** 发送验证码请求 */
export interface SendCodeRequest {
  email: string
  purpose: 'register' | 'login' | 'resetPassword'
  captchaKey: string
  captchaAnswer: string
}

/** 图形验证码响应 */
export interface GraphCaptchaResponse {
  captchaKey: string
  image: string
}

/** 忘记密码请求 */
export interface ForgotPasswordRequest {
  username: string
  email: string
  code: string
  newPassword: string
}

/** 认证响应 */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: {
    id: number
    uid: string
    username: string
    nickname: string
    avatar: string
    role: string
  }
}

/** API响应通用结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PaginationParams {
  page?: number
  pageSize?: number
}

/** 用户设置 - 服务器端通知设置 */
export interface UserSettingsNotification {
  notifySystem: number      // 系统通知：0-关闭，1-开启
  notifyInteract: number    // 互动通知（点赞+评论）：0-关闭，1-开启
  notifyMessage: number     // 私信通知：0-关闭，1-开启
  notifyFollow: number      // 关注通知：0-关闭，1-开启
}

/** 用户设置 - 服务器端隐私设置 */
export interface UserSettingsPrivacy {
  showOnlineStatus: number  // 显示在线状态：0-关闭，1-开启
  messagePermission: number // 私信权限：0-所有人，1-仅关注的人
  saveViewHistory: number   // 保存浏览历史：0-关闭，1-开启
}

/** 用户设置 - 服务器端显示设置 */
export interface UserSettingsDisplay {
  fontSize: number          // 字体大小：0-小，1-标准，2-大
  theme: string             // 主题：light/dark/auto
  language: string          // 语言
}

/** 用户设置 - 服务器端推荐偏好 */
export interface UserSettingsRecommend {
  interestedCategories: number[]
  interestedTags: number[]
  blockedCategories: number[]
  blockedTags: number[]
}

/** 用户设置接口 - 服务器端存储（嵌套结构，匹配后端 UserSettingsVO） */
export interface UserSettingsServer {
  recommend: UserSettingsRecommend
  notification: UserSettingsNotification
  privacy: UserSettingsPrivacy
  display: UserSettingsDisplay
}

/** 用户本地设置接口（仅本地存储，无需同步） */
export interface UserSettingsLocal {
  theme: 'light' | 'dark' | 'system'
  fontSize: 'small' | 'medium' | 'large'
  language: string
  autoLoadImages: boolean
  defaultSort: 'latest' | 'hot'
}

/** 用户设置接口（合并服务器和本地设置） */
export interface UserSettings extends UserSettingsServer, UserSettingsLocal {}
