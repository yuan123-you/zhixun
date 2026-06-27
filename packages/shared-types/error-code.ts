/**
 * 错误码枚举 — 前后端统一
 *
 * 分类规则：
 * - 通用：HTTP 标准状态码
 * - 1xxx：认证相关
 * - 2xxx：作品相关
 * - 3xxx：用户相关
 * - 4xxx：文件相关
 * - 5xxx：搜索相关
 */
export enum ErrorCode {
  // 通用
  SUCCESS = 0,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  RATE_LIMIT = 429,
  INTERNAL_ERROR = 500,
  SERVICE_UNAVAILABLE = 503,

  // 认证 (1xxx)
  AUTH_USERNAME_OR_PASSWORD_ERROR = 1001,
  AUTH_TOKEN_EXPIRED = 1002,
  AUTH_TOKEN_INVALID = 1003,
  AUTH_REFRESH_TOKEN_INVALID = 1004,
  AUTH_ACCOUNT_BANNED = 1005,
  AUTH_ACCOUNT_NOT_FOUND = 1006,
  AUTH_VERIFICATION_CODE_ERROR = 1007,

  // 作品 (2xxx)
  ARTICLE_NOT_FOUND = 2001,
  ARTICLE_DELETED = 2002,
  ARTICLE_UNDER_REVIEW = 2003,
  ARTICLE_REJECTED = 2004,
  ARTICLE_FORBIDDEN_WORD = 2005,

  // 用户 (3xxx)
  USER_NOT_FOUND = 3001,
  USER_ALREADY_EXISTS = 3002,
  USER_BANNED = 3003,
  USER_FOLLOW_SELF = 3004,

  // 文件 (4xxx)
  FILE_TOO_LARGE = 4001,
  FILE_TYPE_NOT_ALLOWED = 4002,
  FILE_UPLOAD_FAILED = 4003,

  // 搜索 (5xxx)
  SEARCH_SERVICE_UNAVAILABLE = 5001,
  SEARCH_TOO_MANY_REQUESTS = 5002,
}

/** 错误码对应的中文提示信息 */
export const ErrorMessage: Record<number, string> = {
  [ErrorCode.SUCCESS]: '操作成功',
  [ErrorCode.BAD_REQUEST]: '请求参数错误',
  [ErrorCode.UNAUTHORIZED]: '请先登录',
  [ErrorCode.FORBIDDEN]: '没有权限',
  [ErrorCode.NOT_FOUND]: '内容不存在',
  [ErrorCode.RATE_LIMIT]: '操作太频繁，稍后再试',
  [ErrorCode.INTERNAL_ERROR]: '服务器开小差了',
  [ErrorCode.SERVICE_UNAVAILABLE]: '服务正在维护中',
  [ErrorCode.AUTH_USERNAME_OR_PASSWORD_ERROR]: '登录失败，请检查账号密码',
  [ErrorCode.AUTH_TOKEN_EXPIRED]: '登录信息已过期，请重新登录',
  [ErrorCode.AUTH_TOKEN_INVALID]: '登录信息已过期，请重新登录',
  [ErrorCode.AUTH_REFRESH_TOKEN_INVALID]: '登录信息已过期，请重新登录',
  [ErrorCode.AUTH_ACCOUNT_BANNED]: '账号已被限制使用',
  [ErrorCode.AUTH_ACCOUNT_NOT_FOUND]: '账号不存在',
  [ErrorCode.AUTH_VERIFICATION_CODE_ERROR]: '验证码输入错误',
  [ErrorCode.ARTICLE_NOT_FOUND]: '作品不存在或已被删除',
  [ErrorCode.ARTICLE_DELETED]: '作品已被作者删除',
  [ErrorCode.ARTICLE_UNDER_REVIEW]: '作品正在审核中，请耐心等待',
  [ErrorCode.ARTICLE_REJECTED]: '作品未通过审核',
  [ErrorCode.ARTICLE_FORBIDDEN_WORD]: '作品包含不合规内容，请修改后重试',
  [ErrorCode.USER_NOT_FOUND]: '该用户不存在',
  [ErrorCode.USER_ALREADY_EXISTS]: '该账号已被注册',
  [ErrorCode.USER_BANNED]: '该用户已被限制使用',
  [ErrorCode.USER_FOLLOW_SELF]: '不能关注自己哦',
  [ErrorCode.FILE_TOO_LARGE]: '文件大小超出限制',
  [ErrorCode.FILE_TYPE_NOT_ALLOWED]: '不支持该文件格式',
  [ErrorCode.FILE_UPLOAD_FAILED]: '文件上传失败，请重试',
  [ErrorCode.SEARCH_SERVICE_UNAVAILABLE]: '搜索服务正在维护中',
  [ErrorCode.SEARCH_TOO_MANY_REQUESTS]: '搜索太频繁，请稍后重试',
}
