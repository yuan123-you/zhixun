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
  likeCount: number
  collectCount: number
  commentCount: number
  viewCount: number
  isLiked: boolean
  isCollected: boolean
  status: ArticleStatus
  isTop?: number
  createdAt: string
  updatedAt: string
}

/** 文章状态 */
export enum ArticleStatus {
  Draft = 0,
  Published = 1,
  Deleted = 2,
}

/** 用户接口 */
export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  bio: string
  email: string
  phone: string
  gender: Gender
  birthday: string
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
  replies: Comment[]
  likeCount: number
  isLiked: boolean
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

/** 通知类型枚举 */
export enum NotificationType {
  System = 0,
  Like = 1,
  Comment = 2,
  Follow = 3,
  Mention = 4,
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

/**
 * @deprecated 使用 PageResult 代替，与后端保持一致
 */
export type SearchResult<T> = PageResult<T>

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
  nickname?: string
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

/** 用户设置接口（服务器端存储，需多设备同步） */
export interface UserSettingsServer {
  // 推荐偏好
  interestedCategories: number[]
  interestedTags: number[]
  blockedCategories: number[]
  blockedTags: number[]
  // 通知设置
  enableLikeNotification: boolean
  enableCommentNotification: boolean
  enableFollowNotification: boolean
  enableSystemNotification: boolean
  // 隐私设置
  showOnlineStatus: boolean
  allowStrangerMessage: boolean
  showViewHistory: boolean
}

/** 用户本地设置接口（仅本地存储，无需同步） */
export interface UserSettingsLocal {
  theme: 'light' | 'dark' | 'system'
  fontSize: 'small' | 'medium' | 'large'
  language: string
}

/** 用户设置接口（合并服务器和本地设置） */
export interface UserSettings extends UserSettingsServer, UserSettingsLocal {}
