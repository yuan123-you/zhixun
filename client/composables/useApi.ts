import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'

/** Token即将过期的提前量（5分钟，单位毫秒） */
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/** 全局刷新锁：防止多个 useApi() 实例并发刷新 */
let globalIsRefreshing = false
let globalRefreshPromise: Promise<string> | null = null
const globalPendingRequests: Array<(token: string) => void> = []

/** 全局 axios 实例（单例模式，避免重复创建） */
let globalInstance: AxiosInstance | null = null
/** 记录全局实例对应的 baseURL，防止 SSR 和客户端环境实例错配 */
let globalInstanceBaseURL: string | null = null

/** API请求封装组合式函数 */
export const useApi = () => {
  const config = useRuntimeConfig()
  const userStore = useUserStore()

  // 服务端使用内部绝对地址（容器间通信），客户端使用相对路径（经 Nginx 代理）
  const baseURL = import.meta.server
    ? `${config.apiBase}/api/v1`
    : (config.public.apiBase as string)

  // 复用全局实例（仅当 baseURL 一致时，防止 SSR 和客户端环境实例错配）
  if (globalInstance && globalInstanceBaseURL === baseURL) {
    return {
      instance: globalInstance,
      get: <T = any>(url: string, params?: Record<string, any>, options?: AxiosRequestConfig) => {
        return globalInstance!.get<ApiResponse<T>>(url, { params, ...options })
      },
      post: <T = any>(url: string, data?: Record<string, any>, options?: AxiosRequestConfig) => {
        return globalInstance!.post<ApiResponse<T>>(url, data, options)
      },
      put: <T = any>(url: string, data?: Record<string, any>, options?: AxiosRequestConfig) => {
        return globalInstance!.put<ApiResponse<T>>(url, data, options)
      },
      delete: <T = any>(url: string, options?: AxiosRequestConfig) => {
        return globalInstance!.delete<ApiResponse<T>>(url, options)
      },
    }
  }

  // 创建axios实例
  const instance: AxiosInstance = axios.create({
    baseURL,
    timeout: 15000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 判断Token是否即将过期
  const isTokenExpiringSoon = (): boolean => {
    const expiresAt = userStore.tokenExpiresAt
    if (!expiresAt) return false
    return Date.now() >= expiresAt - TOKEN_REFRESH_THRESHOLD
  }

  // 判断Token是否已过期
  const isTokenExpired = (): boolean => {
    const expiresAt = userStore.tokenExpiresAt
    if (!expiresAt) return false
    return Date.now() >= expiresAt
  }

  // 全局Token刷新：使用Promise防止并发刷新
  const refreshAccessToken = async (): Promise<string> => {
    const currentRefreshToken = userStore.refreshToken
    if (!currentRefreshToken) {
      // 用户从未登录过（没有accessToken），静默拒绝，不提示"过期"
      if (userStore.token) {
        if (import.meta.client) showAuthExpiredToast()
        userStore.logout()
        navigateTo('/login')
      }
      throw new Error('请先登录')
    }

    // 如果已经在刷新中，复用同一个Promise
    if (globalIsRefreshing && globalRefreshPromise) {
      return globalRefreshPromise
    }

    globalIsRefreshing = true
    globalRefreshPromise = (async () => {
      try {
        const response = await axios.post(`${baseURL}/auth/refresh`, {
          refreshToken: currentRefreshToken,
        })
        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data
        userStore.setToken(accessToken, newRefreshToken, expiresIn)

        // 重试所有挂起的请求
        const pendingCallbacks = [...globalPendingRequests]
        globalPendingRequests.length = 0
        pendingCallbacks.forEach((callback) => callback(accessToken))

        return accessToken
      } catch {
        // 刷新失败，拒绝所有挂起的请求
        const pendingCallbacks = [...globalPendingRequests]
        globalPendingRequests.length = 0
        pendingCallbacks.forEach((callback) => {
          // 每个 pending 请求的 reject 已在 handleTokenRefresh 中通过 globalRefreshPromise.catch 处理
        })

        if (import.meta.client) showAuthExpiredToast()
        userStore.logout()
        navigateTo('/login')
        throw new Error('登录已过期，请重新登录')
      } finally {
        globalIsRefreshing = false
        globalRefreshPromise = null
      }
    })()

    return globalRefreshPromise
  }

  // 请求拦截器：自动携带Token和CSRF Token，Token即将过期时主动刷新
  instance.interceptors.request.use(
    async (requestConfig) => {
      const requestUrl = requestConfig.url || ''

      // 认证端点（登录/注册/刷新Token/验证码）不携带旧Token，
      // 避免过期Token触发后端403或干扰认证流程
      const isAuthEndpoint = requestUrl.includes('/auth/login')
        || requestUrl.includes('/auth/register')
        || requestUrl.includes('/auth/refresh')
        || requestUrl.includes('/auth/send-code')
        || requestUrl.includes('/auth/graph-captcha')
        || requestUrl.includes('/auth/forgot-password')

      if (!isAuthEndpoint) {
        const token = userStore.token

        // 如果有Token且即将过期（但未完全过期），先刷新Token再发送请求
        if (token && isTokenExpiringSoon() && !isTokenExpired()) {
          try {
            const newToken = await refreshAccessToken()
            requestConfig.headers.Authorization = `Bearer ${newToken}`
          } catch {
            // 刷新失败，不再用旧Token发送请求
            return Promise.reject(new Error('登录信息已过期，请重新登录'))
          }
        } else if (token) {
          requestConfig.headers.Authorization = `Bearer ${token}`
        }
      }

      // SSR 请求标识：服务端渲染时无法携带浏览器 Cookie，需要告知后端跳过 CSRF 校验
      if (import.meta.server) {
        requestConfig.headers['X-SSR-Request'] = 'true'
      }

      // CSRF 防护：从 Cookie 中读取 XSRF-TOKEN 并添加到请求头
      // 仅对状态变更请求（POST/PUT/DELETE/PATCH）添加 CSRF Token
      const method = requestConfig.method?.toUpperCase()
      if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const xsrfToken = getXsrfTokenFromCookie()
        if (xsrfToken) {
          requestConfig.headers['X-XSRF-TOKEN'] = xsrfToken
        }
      }

      return requestConfig
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 403 权限不足弹框提醒（防止重复弹出）
  let forbiddenDialogVisible = false
  const showForbiddenDialog = (message?: string) => {
    if (!import.meta.client || forbiddenDialogVisible) return
    forbiddenDialogVisible = true
    // 安全的文本转义，防止后端错误消息中的 HTML 注入
    const safeMessage = (message || '暂无访问权限')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
    const overlay = document.createElement('div')
    overlay.className = 'fixed inset-0 z-[9999] flex items-center justify-center bg-black/30'
    const dialogDiv = document.createElement('div')
    dialogDiv.className = 'bg-white rounded-xl shadow-[var(--shadow-lg)] px-5 py-3 mx-4 text-center text-sm text-slate-700'
    dialogDiv.textContent = safeMessage
    overlay.appendChild(dialogDiv)
    document.body.appendChild(overlay)
    const close = () => {
      overlay.style.opacity = '0'
      overlay.style.transition = 'opacity 0.15s'
      setTimeout(() => { overlay.remove(); forbiddenDialogVisible = false }, 150)
    }
    overlay.addEventListener('click', (e) => { if (e.target === overlay) close() })
    setTimeout(close, 1500)
  }

  // 响应拦截器：统一错误处理，401时自动刷新Token并重试
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { data } = response

      // 检查数据版本号，版本号变化时清除请求缓存
      const serverVersion = response.headers['x-data-version']
      if (serverVersion && import.meta.client) {
        const storedVersion = storage.get<string>(STORAGE_KEYS.DATA_VERSION)
        if (storedVersion && serverVersion !== storedVersion) {
          // 版本号变化，清除所有请求缓存
          const { clearAll } = useRequestCache()
          clearAll()
        }
        storage.set(STORAGE_KEYS.DATA_VERSION, String(serverVersion))
      }

      // 业务错误码处理
      if (data.code !== 0 && data.code !== 200) {
        // Token过期，尝试刷新
        if (data.code === 401) {
          return handleTokenRefresh(response.config)
        }
        // 其他业务错误
        return Promise.reject(new Error(data.message || '操作失败，请稍后重试'))
      }
      return response
    },
    async (error) => {
      // HTTP状态码错误处理
      if (error.response) {
        const { status } = error.response
        switch (status) {
          case 401:
            // 没有refreshToken时直接拒绝，不再尝试刷新（避免无限循环）
            if (!userStore.refreshToken) {
              // 用户从未登录过（没有accessToken），静默拒绝，不提示"过期"
              if (userStore.token) {
                if (import.meta.client) showAuthExpiredToast()
                userStore.logout()
                navigateTo('/login')
              }
              return Promise.reject(new Error('请先登录'))
            }
            // Token过期，尝试刷新
            return handleTokenRefresh(error.response.config)
          case 403: {
            // 认证相关接口返回403时，可能是CSRF校验、验证码失效等原因，提示更具体的信息
            const requestUrl = error.response.config?.url || ''
            const isAuthEndpoint = requestUrl.includes('/auth/login')
                || requestUrl.includes('/auth/register')
                || requestUrl.includes('/auth/refresh')
                || requestUrl.includes('/auth/send-code')
                || requestUrl.includes('/auth/graph-captcha')
                || requestUrl.includes('/auth/forgot-password')
                || requestUrl.includes('/auth/oauth')
            // 发送验证码接口单独处理
            const isSendCode = requestUrl.includes('/auth/send-code')
            const forbiddenMsg = isSendCode ? '发送验证码失败，请重试'
                : isAuthEndpoint ? '登录失败，请检查账号密码' : '暂无操作权限'
            showForbiddenDialog(isAuthEndpoint && !isSendCode ? '登录失败，请检查账号密码' : undefined)
            return Promise.reject(new Error(forbiddenMsg))
          }
          case 404:
            return Promise.reject(new Error('内容不存在'))
          case 429:
            return Promise.reject(new Error('操作太频繁，稍后再试'))
          case 500:
            return Promise.reject(new Error('服务器繁忙，请稍后重试'))
          default:
            return Promise.reject(new Error(error.response.data?.message || '操作失败，请稍后重试'))
        }
      }
      // 网络错误
      if (error.code === 'ECONNABORTED') {
        return Promise.reject(new Error('请求超时，请稍后重试'))
      }
      return Promise.reject(new Error('网络异常，请检查网络'))
    }
  )

  // 401响应时Token刷新：使用全局刷新锁防止并发
  const handleTokenRefresh = async (requestConfig: AxiosRequestConfig) => {
    const currentRefreshToken = userStore.refreshToken
    if (!currentRefreshToken) {
      // 用户从未登录过（没有accessToken），静默拒绝，不提示"过期"
      if (userStore.token) {
        if (import.meta.client) showAuthExpiredToast()
        userStore.logout()
        navigateTo('/login')
      }
      return Promise.reject(new Error('请先登录'))
    }

    // 如果已经在刷新中，将请求加入等待队列
    if (globalIsRefreshing && globalRefreshPromise) {
      return new Promise((resolve, reject) => {
        globalPendingRequests.push((newToken: string) => {
          if (requestConfig.headers) {
            requestConfig.headers.Authorization = `Bearer ${newToken}`
          }
          resolve(instance(requestConfig))
        })
        globalRefreshPromise!.catch(() => {
          reject(new Error('登录已过期，请重新登录'))
        })
      })
    }

    // 发起刷新
    try {
      const newToken = await refreshAccessToken()
      if (requestConfig.headers) {
        requestConfig.headers.Authorization = `Bearer ${newToken}`
      }
      return instance(requestConfig)
    } catch {
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
  }

  // 封装请求方法
  const get = <T = any>(url: string, params?: Record<string, any>, options?: AxiosRequestConfig) => {
    return instance.get<ApiResponse<T>>(url, { params, ...options })
  }

  const post = <T = any>(url: string, data?: Record<string, any>, options?: AxiosRequestConfig) => {
    return instance.post<ApiResponse<T>>(url, data, options)
  }

  const put = <T = any>(url: string, data?: Record<string, any>, options?: AxiosRequestConfig) => {
    return instance.put<ApiResponse<T>>(url, data, options)
  }

  const del = <T = any>(url: string, options?: AxiosRequestConfig) => {
    return instance.delete<ApiResponse<T>>(url, options)
  }

  // 保存到全局实例
  globalInstance = instance
  globalInstanceBaseURL = baseURL

  return {
    instance,
    get,
    post,
    put,
    delete: del,
  }
}

/**
 * 登录过期提示 Toast（防止重复弹出）
 */
let authExpiredToastVisible = false
function showAuthExpiredToast() {
  if (authExpiredToastVisible) return
  authExpiredToastVisible = true

  const toast = document.createElement('div')
  toast.className = 'fixed top-4 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl shadow-lg text-white text-sm font-medium transition-all duration-300 transform bg-amber-500 flex items-center gap-2'
  // 使用 textContent 替代 innerHTML 防止潜在 XSS
  const span = document.createElement('span')
  span.textContent = '登录已过期，请重新登录'
  toast.innerHTML = `
    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
    </svg>
  `
  toast.appendChild(span)
  document.body.appendChild(toast)

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translate(-50%, -20px)'
    setTimeout(() => {
      toast.remove()
      authExpiredToastVisible = false
    }, 300)
  }, 2000)
}

/**
 * 从浏览器 Cookie 中读取 XSRF-TOKEN
 */
function getXsrfTokenFromCookie(): string | null {
  if (!import.meta.client) return null
  const cookies = document.cookie.split(';')
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=')
    if (name === 'XSRF-TOKEN') {
      return decodeURIComponent(value)
    }
  }
  return null
}
