import type { LoginRequest, RegisterRequest, SendCodeRequest, ForgotPasswordRequest, AuthResponse, GraphCaptchaResponse } from '@/types'

/** 认证组合式函数 */
export const useAuth = () => {
  const userStore = useUserStore()
  const router = useRouter()
  const { post, put, get } = useApi()

  // 登录
  const login = async (data: LoginRequest) => {
    try {
      const response = await post<AuthResponse>('/auth/login', data)
      const authData = response.data.data
      userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
      userStore.setUser(authData.userInfo as unknown as User)
      return authData
    } catch (error: any) {
      throw new Error(error.message || '登录失败，请检查账号密码')
    }
  }

  // 注册
  const register = async (data: RegisterRequest) => {
    try {
      const response = await post<AuthResponse>('/auth/register', {
        username: data.username,
        password: data.password,
        email: data.email,
        code: data.code,
      })
      const authData = response.data.data
      userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
      userStore.setUser(authData.userInfo as unknown as User)
      return authData
    } catch (error: any) {
      throw new Error(error.message || '注册失败，请稍后重试')
    }
  }

  // 获取图形验证码
  const getGraphCaptcha = async () => {
    try {
      const response = await get<GraphCaptchaResponse>('/auth/graph-captcha')
      return response.data.data
    } catch (error: any) {
      throw new Error(error.message || '验证码获取失败，请稍后重试')
    }
  }

  // 发送验证码
  const sendCode = async (data: SendCodeRequest) => {
    try {
      await post('/auth/send-code', data)
    } catch (error: any) {
      throw new Error(error.message || '验证码发送失败，请稍后重试')
    }
  }

  // 忘记密码
  const forgotPassword = async (data: ForgotPasswordRequest) => {
    try {
      await put('/auth/forgot-password', data)
    } catch (error: any) {
      throw new Error(error.message || '重置失败，请稍后重试')
    }
  }

  // 退出登录
  const logout = async () => {
    try {
      // 设置超时，避免后端无响应时长时间等待
      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('退出超时')), 2000)
      )
      await Promise.race([post('/auth/logout'), timeoutPromise])
    } catch {
      // 即使后端退出失败，前端也清除登录状态
    } finally {
      userStore.logout()
      // 关键修复：使用 replace 而非 push，避免用户从浏览器返回键回到"我的"页面
      // 同时强制刷新一次 router，让 /user 等需要登录的页面守卫立即重定向到 /login
      try {
        await router.replace('/login')
      } catch {
        // 兜底：路由跳转失败时强制重定向
        window.location.href = '/login'
      }
    }
  }

  // 刷新Token
  const refreshToken = async () => {
    try {
      const response = await post<AuthResponse>('/auth/refresh', {
        refreshToken: userStore.refreshToken,
      })
      const authData = response.data.data
      userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
      return authData.accessToken
    } catch {
      userStore.logout()
      router.push('/login')
      throw new Error('登录信息已过期，请重新登录')
    }
  }

  // 检查是否已登录
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  // 获取当前用户信息
  const currentUser = computed(() => userStore.userInfo)

  return {
    login,
    register,
    getGraphCaptcha,
    sendCode,
    forgotPassword,
    logout,
    refreshToken,
    isLoggedIn,
    currentUser,
  }
}
