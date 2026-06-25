/** 通用 API 响应结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 通用分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  pages?: number
  refresh_key?: string
}
