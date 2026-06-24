import type { ApiResponse } from '~/types'

/** API插件：提供全局 $api 访问，并初始化Token自动刷新拦截器 */
export default defineNuxtPlugin(() => {
  const userStore = useUserStore()
  const { get, post, put, delete: del } = useApi()

  // 客户端初始化：非阻塞方式检查Token状态
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
      // Token为空但refreshToken存在（页面刷新后token过期或丢失），尝试刷新恢复登录态
      else if (!token && refreshToken) {
        try {
          const { authApi } = await import('~/api/auth')
          const response = await authApi.refreshToken(refreshToken)
          const authData = response.data.data
          userStore.setToken(authData.accessToken, authData.refreshToken, authData.expiresIn)
        } catch {
          // 刷新失败，清除无效的refreshToken
          userStore.logout()
        }
      }
    }

    // 页面可见性变化时检查Token是否需要刷新
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        checkAndRefreshToken()
      }
    })

    // 延迟执行Token检查，不阻塞首屏渲染
    requestIdleCallback
      ? requestIdleCallback(() => checkAndRefreshToken())
      : setTimeout(() => checkAndRefreshToken(), 1000)
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
