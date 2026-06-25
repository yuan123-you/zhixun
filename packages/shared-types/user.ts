/** 用户信息（共享类型） */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  permissions: string[]
  status: 'active' | 'banned' | 'deleted'
  createdAt: string
  updatedAt: string
}
