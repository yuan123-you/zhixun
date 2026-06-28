/** 通用 API 响应结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 通用分页结果（与后端 com.zhixun.common.result.PageResult 对齐） */
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
