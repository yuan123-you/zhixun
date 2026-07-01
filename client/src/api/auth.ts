import type { LoginRequest, RegisterRequest, AuthResponse } from '@/types'

/** 认证API */
export const authApi = {
  /** 用户登录 */
  login: (data: LoginRequest) => {
    const { post } = useApi()
    return post<AuthResponse>('/auth/login', data)
  },

  /** 用户注册 */
  register: (data: RegisterRequest) => {
    const { post } = useApi()
    return post<AuthResponse>('/auth/register', data)
  },

  /** 退出登录 */
  logout: () => {
    const { post } = useApi()
    return post('/auth/logout')
  },

  /** 刷新Token - refreshToken 存储在 sessionStorage 中，通过请求体传递 */
  refreshToken: (refreshToken?: string) => {
    const { post } = useApi()
    // 通过请求体传递 refreshToken，支持多账号同时登录
    return post<AuthResponse>('/auth/refresh', refreshToken ? { refreshToken } : {})
  },
}
