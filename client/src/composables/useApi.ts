import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types'
import { navigateTo } from '@/composables/navigateTo'
import { friendlyMessage } from '@/utils/friendlyError'

/** Token即将过期的提前量（5分钟，单位毫秒） */
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/** 全局刷新锁 */
let globalIsRefreshing = false
let globalRefreshPromise: Promise<string> | null = null
const globalPendingRequests: Array<(token: string) => void> = []

/** Token 同步回调：由外部注入，用于在 token 刷新后将新 token 同步到 Pinia store */
let onTokenRefreshed: ((token: string, refreshToken: string, expiresIn?: number) => void) | null = null

/** 注册 token 刷新后的同步回调 */
export function setOnTokenRefreshed(cb: (token: string, refreshToken: string, expiresIn?: number) => void) {
  onTokenRefreshed = cb
}

/** 全局 axios 实例 */
let globalInstance: AxiosInstance | null = null

const BASE_URL = '/api/v1'

/**
 * 从 sessionStorage 获取 store 状态（客户端环境）
 */
function getTokenFromStore(): { token: string; refreshToken: string; expiresAt: number } {
  if (typeof window === 'undefined') return { token: '', refreshToken: '', expiresAt: 0 }
  try {
    const raw = sessionStorage.getItem('zhixun_accessToken')
    const token = raw ? JSON.parse(raw).value || '' : ''
    const rawRt = sessionStorage.getItem('zhixun_refreshToken')
    const refreshToken = rawRt ? JSON.parse(rawRt).value || '' : ''
    const rawExp = sessionStorage.getItem('zhixun_token_expires_at')
    const expiresAt = rawExp ? JSON.parse(rawExp).value || 0 : 0
    return { token, refreshToken, expiresAt }
  } catch { return { token: '', refreshToken: '', expiresAt: 0 } }
}

function setTokenToStore(token: string, refreshToken: string, expiresIn?: number) {
  if (typeof window === 'undefined') return
  const setItem = (key: string, value: any) => {
    sessionStorage.setItem(key, JSON.stringify({ value, version: '1.0.0', expireAt: null }))
  }
  setItem('zhixun_accessToken', token)
  setItem('zhixun_refreshToken', refreshToken)
  if (expiresIn !== undefined) {
    setItem('zhixun_token_expires_at', Date.now() + expiresIn * 1000)
  }
}

function clearTokenFromStore() {
  if (typeof window === 'undefined') return
  ['zhixun_accessToken', 'zhixun_refreshToken', 'zhixun_token_expires_at', 'zhixun_user_summary'].forEach(k => sessionStorage.removeItem(k))
}

/** API请求封装组合式函数 */
export const useApi = () => {
  // 复用全局实例
  if (globalInstance) {
    return createApiMethods(globalInstance)
  }

  const instance: AxiosInstance = axios.create({
    baseURL: BASE_URL,
    timeout: 15000,
    withCredentials: true, // 必须开启：让浏览器在跨域请求中携带 Cookie（refreshToken 等 httpOnly Cookie）
    headers: {
      'X-Client-Type': 'client', // 区分客户端/管理员端，后端据此设置不同的 Cookie 名称
    },
    // 不设置默认 Content-Type：axios transformRequest 会自动为 plain object 设置 application/json，
    // 而 FormData 上传时由浏览器自动设置 multipart/form-data 及 boundary，
    // 避免默认 application/json 覆盖 FormData 的 Content-Type 导致后端 MultipartException
  })

  // 全局Token刷新
  const refreshAccessToken = async (): Promise<string> => {
    // refreshToken 存储在 sessionStorage 中，每个标签页独立
    // 通过请求体传递 refreshToken，避免 httpOnly Cookie 在多账号场景下的冲突

    if (globalIsRefreshing && globalRefreshPromise) {
      return globalRefreshPromise
    }

    globalIsRefreshing = true
    globalRefreshPromise = (async () => {
      try {
        // 从 sessionStorage 读取 refreshToken 并通过请求体传递
        const { refreshToken: currentRefreshToken } = getTokenFromStore()
        const response = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken: currentRefreshToken || undefined,
        }, {
          withCredentials: true, // 必须开启：让浏览器携带 httpOnly Cookie 中的 refreshToken
          headers: { 'X-Client-Type': 'client' },
        })
        const { accessToken, refreshToken: newRt, expiresIn } = response.data.data
        setTokenToStore(accessToken, newRt || '', expiresIn)
        onTokenRefreshed?.(accessToken, newRt || '', expiresIn)
        const cb = [...globalPendingRequests]
        globalPendingRequests.length = 0
        cb.forEach((c) => c(accessToken))
        return accessToken
      } catch {
        globalPendingRequests.length = 0
        showAuthExpiredToast()
        clearTokenFromStore()
        // 修复：不再使用 location.href（会触发整页刷新，体感为"页面莫名其妙地自动刷新"）。
        // 改用 Vue Router 客户端跳转，保留 SPA 单页应用体验。
        // 兜底：若 router 尚未初始化（极端情况），才退化为 location.href。
        try {
          await navigateTo({ path: '/login', query: { redirect: location.pathname + location.search } })
        } catch {
          location.href = '/login'
        }
        throw new Error('登录已过期')
      } finally {
        globalIsRefreshing = false
        globalRefreshPromise = null
      }
    })()
    return globalRefreshPromise
  }

  // 请求拦截器
  instance.interceptors.request.use(
    async (config) => {
      const requestUrl = config.url || ''
      const isAuth = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register')
        || requestUrl.includes('/auth/refresh') || requestUrl.includes('/auth/send-code')
        || requestUrl.includes('/auth/graph-captcha') || requestUrl.includes('/auth/forgot-password')

      if (!isAuth) {
        const { token, expiresAt } = getTokenFromStore()
        if (token) {
          const isExpiringSoon = expiresAt > 0 && Date.now() >= expiresAt - TOKEN_REFRESH_THRESHOLD
          const isExpired = expiresAt > 0 && Date.now() >= expiresAt
          if (isExpiringSoon && !isExpired) {
            try {
              const newToken = await refreshAccessToken()
              config.headers.Authorization = `Bearer ${newToken}`
            } catch { return Promise.reject(new Error('登录信息已过期')) }
          } else if (!isExpired) {
            config.headers.Authorization = `Bearer ${token}`
          }
        }
      }

      const method = config.method?.toUpperCase()
      if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const xsrf = getXsrfTokenFromCookie()
        if (xsrf) config.headers['X-XSRF-TOKEN'] = xsrf
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  // 响应拦截器
  let forbiddenVisible = false
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { data, config } = response
      // 排除 /auth/logout 接口：退出登录时即便返回 401 也应正常处理，不应触发 token 刷新
      const isLogoutRequest = config?.url?.includes('/auth/logout')
      if (data.code !== 0 && data.code !== 200) {
        if (data.code === 401 && !isLogoutRequest) {
          return handleTokenRefresh(response.config) as any
        }
        return Promise.reject(new Error(friendlyMessage({ code: data.code, message: data.message }))) as any
      }
      return response
    },
    async (error) => {
      if (error.response) {
        const { status, config, data } = error.response
        // 关键修复：如果是 /auth/logout 接口的 401/403，不应触发 token 刷新或重定向
        // 退出登录时 token 已经失效属于正常情况
        const isLogoutRequest = config?.url?.includes('/auth/logout')
        switch (status) {
          case 401: {
            if (isLogoutRequest) {
              // 退出登录时返回 401 直接当作成功处理
              return Promise.reject(new Error('已退出登录'))
            }
            // refreshToken 存储在 httpOnly Cookie 中，前端无法读取
            // 直接尝试刷新，后端会从 Cookie 读取 refreshToken
            return handleTokenRefresh(error.response.config)
          }
          case 403:
            if (isLogoutRequest) {
              return Promise.reject(new Error('已退出登录'))
            }
            if (!forbiddenVisible) {
              forbiddenVisible = true
              // 使用更显眼、更适配移动端、且语义明确的提示
              const tip = friendlyMessage({ status, code: data?.code, message: data?.message }, '您没有权限执行此操作')
              showMobileAlert(tip)
              setTimeout(() => { forbiddenVisible = false }, 1800)
            }
            return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message }, '您没有权限执行此操作')))
          case 404: return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message }, '内容不存在或已被删除')))
          case 408:
          case 504: return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message }, '请求超时，请稍后重试')))
          case 429: return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message }, '操作太频繁，请稍后再试')))
          case 500:
          case 502:
          case 503: return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message }, '服务繁忙，请稍后重试')))
          default: return Promise.reject(new Error(friendlyMessage({ status, code: data?.code, message: data?.message || error.message })))
        }
      }
      // 网络错误（如后端关闭、连接拒绝等）—— 不应被当作业务错误抛出
      const code = error?.code || ''
      if (/ERR_CANCELED|aborted/i.test(code)) {
        return Promise.reject(new Error('请求已取消'))
      }
      return Promise.reject(new Error(friendlyMessage(error, '网络连接失败，请检查网络后重试')))
    }
  )

  const handleTokenRefresh = async (requestConfig: AxiosRequestConfig) => {
    // refreshToken 存储在 httpOnly Cookie 中，前端无法读取
    // 直接尝试刷新，后端会从 Cookie 读取 refreshToken
    if (globalIsRefreshing && globalRefreshPromise) {
      return new Promise((resolve, reject) => {
        globalPendingRequests.push((newToken: string) => {
          if (requestConfig.headers) requestConfig.headers.Authorization = `Bearer ${newToken}`
          resolve(instance(requestConfig))
        })
        globalRefreshPromise!.catch(() => reject(new Error('登录已过期')))
      })
    }
    try {
      const newToken = await refreshAccessToken()
      if (requestConfig.headers) requestConfig.headers.Authorization = `Bearer ${newToken}`
      return instance(requestConfig)
    } catch {
      return Promise.reject(new Error('登录已过期'))
    }
  }

  globalInstance = instance
  return createApiMethods(instance)
}

function createApiMethods(instance: AxiosInstance) {
  const get = <T = any>(url: string, params?: Record<string, any>, options?: AxiosRequestConfig) =>
    instance.get<ApiResponse<T>>(url, { params, ...options })
  const post = <T = any>(url: string, data?: any, options?: AxiosRequestConfig) =>
    instance.post<ApiResponse<T>>(url, data, options)
  const put = <T = any>(url: string, data?: any, options?: AxiosRequestConfig) =>
    instance.put<ApiResponse<T>>(url, data, options)
  const del = <T = any>(url: string, options?: AxiosRequestConfig) =>
    instance.delete<ApiResponse<T>>(url, options)

  // 上传专用方法：支持 FormData 和上传进度回调
  // 注意：不手动设置 Content-Type，让 axios 自动添加带 boundary 的 multipart/form-data 头
  const upload = <T = any>(url: string, formData: FormData, onProgress?: (percent: number) => void): Promise<AxiosResponse<ApiResponse<T>>> => {
    return instance.post<ApiResponse<T>>(url, formData, {
      timeout: 60000, // 上传超时60秒
      headers: { 'Content-Type': undefined }, // 显式清除，让浏览器自动设置 multipart/form-data + boundary
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      },
    })
  }

  return { instance, get, post, put, delete: del, upload }
}

let toastVisible = false
function showAuthExpiredToast() {
  if (toastVisible) return
  toastVisible = true
  const t = document.createElement('div')
  t.className = 'fixed top-4 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px] z-[9999] px-4 py-3 rounded-2xl shadow-2xl text-white text-sm font-medium bg-amber-500 transition-all duration-300 flex items-start gap-2 whitespace-normal break-words opacity-0 -translate-y-2'
  t.innerHTML = `
    <svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>
    </svg>
    <span class="flex-1 leading-relaxed">登录已过期，请重新登录</span>
  `
  document.body.appendChild(t)
  requestAnimationFrame(() => { t.style.opacity = '1'; t.style.transform = 'translateY(0)' })
  setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateY(-8px)'; setTimeout(() => { t.remove(); toastVisible = false }, 300) }, 2200)
}

/**
 * 移动端友好的全屏弹层 —— 用于必须中断用户操作的强提示（如 403）。
 *  - 居中悬浮，左右各 12px 边距，移动端不会溢出
 *  - 文本支持换行，不会被截掉
 *  - 1.8s 后自动消失
 */
function showMobileAlert(message: string) {
  const safe = String(message || '').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const el = document.createElement('div')
  el.className = 'fixed inset-0 z-[9999] flex items-center justify-center bg-black/30 p-3'
  el.innerHTML = `
    <div class="bg-white rounded-2xl shadow-2xl px-5 py-4 max-w-[calc(100vw-1.5rem)] sm:max-w-[420px] w-full text-center">
      <div class="flex items-center justify-center w-10 h-10 mx-auto mb-2 rounded-full bg-amber-50">
        <svg class="w-6 h-6 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>
        </svg>
      </div>
      <p class="text-sm text-slate-700 leading-relaxed break-words">${safe}</p>
    </div>
  `
  document.body.appendChild(el)
  setTimeout(() => {
    el.style.transition = 'opacity 0.2s'
    el.style.opacity = '0'
    setTimeout(() => el.remove(), 220)
  }, 1600)
}

function getXsrfTokenFromCookie(): string | null {
  const cookies = document.cookie.split(';')
  for (const c of cookies) {
    const [n, v] = c.trim().split('=')
    if (n === 'XSRF-TOKEN') return decodeURIComponent(v)
  }
  return null
}
