import type { ApiResponse } from '@/types'
import { setOnTokenRefreshed } from '@/composables/useApi'

/** API 插件：提供全局 $api 访问，并初始化 Token 自动刷新拦截器（纯 SPA 版） */
export function initApiPlugin() {
  const userStore = useUserStore()
  const { get, post, put, delete: del } = useApi()

  // 从 sessionStorage 恢复用户状态
  userStore.init()

  // 注册 token 刷新同步回调：确保 axios 拦截器刷新 token 后同步更新 Pinia store
  setOnTokenRefreshed((token: string, refreshToken: string, expiresIn?: number) => {
    userStore.setToken(token, refreshToken, expiresIn)
  })

  const checkAndRefreshToken = async () => {
    const { token, tokenExpiresAt } = userStore
    // refreshToken 存储在 sessionStorage 中，通过请求体传递
    if (token && tokenExpiresAt) {
      const FIVE_MINUTES = 5 * 60 * 1000
      if (Date.now() >= tokenExpiresAt - FIVE_MINUTES) {
        try {
          const { authApi } = await import('~/api/auth')
          // 通过请求体传递 refreshToken（从 sessionStorage 读取）
          const response = await authApi.refreshToken(userStore.refreshToken)
          const authData = response.data.data
          userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
        } catch {
          // 刷新失败，不强制退出
        }
      }
    } else if (!token) {
      // 无 accessToken，尝试刷新
      try {
        const { authApi } = await import('~/api/auth')
        const response = await authApi.refreshToken(userStore.refreshToken)
        const authData = response.data.data
        userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
        userStore.setUser(authData.userInfo as unknown as User)
      } catch {
        userStore.logout()
      }
    }
  }

  const fetchLatestUserInfo = async () => {
    if (userStore.token && userStore.userInfo?.id) {
      try {
        const { get: apiGet } = useApi()
        const response = await apiGet<any>(`/users/${userStore.userInfo.id}`)
        if (response.data?.data) {
          userStore.setUser(response.data.data)
        }
      } catch (e: any) {
        if (e?.message?.includes('用户不存在')) {
          userStore.logout()
        }
      }

      try {
        const { useUserSettings } = await import('~/composables/useUserSettings')
        const { syncFromServer } = useUserSettings()
        await syncFromServer()
      } catch {
        // 设置同步失败不影响主流程
      }
    }
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      checkAndRefreshToken()
    }
  })

  requestIdleCallback
    ? requestIdleCallback(() => { checkAndRefreshToken(); fetchLatestUserInfo() })
    : setTimeout(() => { checkAndRefreshToken(); fetchLatestUserInfo() }, 1000)

  return {
    get,
    post,
    put,
    delete: del,
  } as any
}
