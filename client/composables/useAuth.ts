import type { LoginRequest, RegisterRequest, AuthResponse } from '~/types'

/** 认证组合式函数 */
export const useAuth = () => {
  const userStore = useUserStore()
  const { post } = useApi()

  // 登录
  const login = async (data: LoginRequest) => {
    try {
      const response = await post<AuthResponse>('/auth/login', data)
      const authData = response.data.data
      userStore.setToken(authData.token, authData.refreshToken)
      userStore.setUser(authData.user)
      return authData
    } catch (error: any) {
      throw new Error(error.message || '登录失败')
    }
  }

  // 注册
  const register = async (data: RegisterRequest) => {
    try {
      const response = await post<AuthResponse>('/auth/register', data)
      const authData = response.data.data
      userStore.setToken(authData.token, authData.refreshToken)
      userStore.setUser(authData.user)
      return authData
    } catch (error: any) {
      throw new Error(error.message || '注册失败')
    }
  }

  // 退出登录
  const logout = async () => {
    try {
      await post('/auth/logout')
    } catch {
      // 即使后端退出失败，前端也清除登录状态
    } finally {
      userStore.logout()
      navigateTo('/login')
    }
  }

  // 刷新Token
  const refreshToken = async () => {
    try {
      const response = await post<AuthResponse>('/auth/refresh', {
        refreshToken: userStore.refreshToken,
      })
      const authData = response.data.data
      userStore.setToken(authData.token, authData.refreshToken)
      return authData.token
    } catch {
      userStore.logout()
      navigateTo('/login')
      throw new Error('Token刷新失败')
    }
  }

  // 检查是否已登录
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  // 获取当前用户信息
  const currentUser = computed(() => userStore.userInfo)

  return {
    login,
    register,
    logout,
    refreshToken,
    isLoggedIn,
    currentUser,
  }
}
