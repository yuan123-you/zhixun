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
  author: User
  likeCount: number
  collectCount: number
  commentCount: number
  viewCount: number
  isLiked: boolean
  isCollected: boolean
  status: ArticleStatus
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

/** 排行项接口 */
export interface RankItem {
  rank: number
  articleId: number
  title: string
  author: string
  heatScore: number
  likeCount: number
  commentCount: number
  viewCount: number
}

/** 搜索结果接口 */
export interface SearchResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
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
}

/** 认证响应 */
export interface AuthResponse {
  token: string
  refreshToken: string
  user: User
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

/** 用户设置接口 */
export interface UserSettings {
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
  // 显示设置
  theme: 'light' | 'dark' | 'system'
  fontSize: 'small' | 'medium' | 'large'
  language: string
}
