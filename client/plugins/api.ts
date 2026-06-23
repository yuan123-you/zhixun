import type { ApiResponse } from '~/types'

/** API插件：提供全局 $api 访问，并初始化Token自动刷新拦截器 */
export default defineNuxtPlugin(() => {
  const userStore = useUserStore()
  const { get, post, put, delete: del } = useApi()

  // 客户端初始化：检查Token是否即将过期，如果是则主动刷新
  if (import.meta.client) {
    const checkAndRefreshToken = async () => {
      const { token, refreshToken, tokenExpiresAt } = userStore
      // 有Token和refreshToken，且Token即将在5分钟内过期时，主动刷新
      if (token && refreshToken && tokenExpiresAt) {
        const FIVE_MINUTES = 5 * 60 * 1000
        if (Date.now() >= tokenExpiresAt - FIVE_MINUTES) {
          try {
            const { authApi } = await import('~/api/auth')
            const response = await authApi.refreshToken(refreshToken)
            const authData = response.data.data
            userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
          } catch {
            // 刷新失败，不强制退出，让后续请求的响应拦截器处理
          }
        }
      }
    }

    // 页面可见性变化时检查Token是否需要刷新
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        checkAndRefreshToken()
      }
    })

    // 插件初始化时检查一次
    checkAndRefreshToken()
  }

  // 提供全局 $api 对象
  return {
    provide: {
      api: {
        get,
        post,
        put,
        delete: del,
      } as Record<string, <T = any>(url: string, data?: any, options?: any) => Promise<ApiResponse<T>>>,
    },
  }
})
