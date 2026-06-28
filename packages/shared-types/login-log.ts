/** 登录日志 */
export interface LoginLog {
  id: number
  userId: number
  username: string
  ip: string
  location: string
  /** 用户代理（浏览器信息） */
  userAgent: string
  /** 登录状态：0=失败，1=成功 */
  status: number
  /** 失败原因 */
  failReason: string
  createdAt: string
}

/** 登录日志查询参数 */
export interface LoginLogQuery {
  userId?: number
  keyword?: string
  status?: number
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}