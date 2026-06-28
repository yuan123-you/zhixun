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

  /** 刷新Token - refreshToken 存储在 httpOnly Cookie 中，无需传递参数 */
  refreshToken: (refreshToken?: string) => {
    const { post } = useApi()
    // refreshToken 存储在 httpOnly Cookie 中，后端会自动从 Cookie 读取
    // 不再传递 refreshToken 参数（或传递空对象）
    return post<AuthResponse>('/auth/refresh', refreshToken ? { refreshToken } : {})
  },
}
