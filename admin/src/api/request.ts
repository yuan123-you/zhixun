import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiResponse } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import { friendlyMessage, toFriendlyError } from '@/utils/friendlyError'
import router from '@/router'

// 扩展 Axios 类型，支持重试计数
declare module 'axios' {
  export interface AxiosRequestConfig {
    __retryCount?: number
  }
}

// ========== 请求重试配置 ==========
/** 最大重试次数 */
const MAX_RETRIES = 2
/** 基础重试间隔（ms），每次重试延迟 = RETRY_DELAY * retryCount（指数退避） */
const RETRY_DELAY = 1000

// 创建 Axios 实例
const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ========== Token 自动刷新机制 ==========
/** Token 即将过期的提前量（5分钟，单位毫秒） */
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/** 全局刷新锁：防止多个请求并发刷新 Token */
let isRefreshing = false
let refreshPromise: Promise<string> | null = null
const pendingRequests: Array<(token: string) => void> = []

/**
 * 判断 Token 是否即将过期（过期前5分钟内）
 * 使用 storage 直接读取，避免引入 Pinia store 造成循环依赖
 */
function isTokenExpiringSoon(): boolean {
  const expiresAt = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
  if (!expiresAt) return false
  return Date.now() >= expiresAt - TOKEN_REFRESH_THRESHOLD
}

/**
 * 判断 Token 是否已过期
 */
function isTokenExpired(): boolean {
  const expiresAt = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
  if (!expiresAt) return false
  return Date.now() >= expiresAt
}

/**
 * 使用 refreshToken 刷新 accessToken
 * 使用全局刷新锁防止并发刷新
 */
async function refreshAccessToken(): Promise<string> {
  const currentRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
  if (!currentRefreshToken) {
    throw new Error('登录信息已过期，请重新登录')
  }

  // 如果已经在刷新中，复用同一个 Promise
  if (isRefreshing && refreshPromise) {
    return refreshPromise
  }

  isRefreshing = true
  refreshPromise = (async () => {
    try {
      const response = await axios.post('/api/v1/auth/refresh', {
        refreshToken: currentRefreshToken,
      })
      const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data

      // 持久化新 Token
      storage.set(STORAGE_KEYS.TOKEN, accessToken)
      storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken)
      if (expiresIn !== undefined) {
        const expiresAt = Date.now() + expiresIn * 1000
        storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
      }

      // 重试所有挂起的请求
      pendingRequests.forEach((callback) => callback(accessToken))
      pendingRequests.length = 0

      return accessToken
    } catch {
      // 刷新失败，清除所有认证数据并跳转登录页
      storage.remove(STORAGE_KEYS.TOKEN)
      storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
      storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
      storage.remove(STORAGE_KEYS.USER_PERMISSIONS)
      router.push('/login')
      throw new Error('登录已过期，请重新登录')
    } finally {
      isRefreshing = false
      refreshPromise = null
    }
  })()

  return refreshPromise
}

/**
 * 请求拦截器
 * - 自动携带 Token
 * - Token 即将过期时主动刷新（防止过期前瞬间发起的请求返回 401）
 * - CSRF 防护：状态变更请求自动携带 X-XSRF-TOKEN 请求头
 */
service.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = storage.get<string>(STORAGE_KEYS.TOKEN)

    // 如果有 Token 且即将过期（但未完全过期），先刷新再发送
    if (token && isTokenExpiringSoon() && !isTokenExpired()) {
      try {
        const newToken = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${newToken}`
      } catch {
        // 刷新失败，仍使用旧 Token 发送，由响应拦截器处理 401
        config.headers.Authorization = `Bearer ${token}`
      }
    } else if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // CSRF 防护：对 POST/PUT/DELETE/PATCH 请求添加 X-XSRF-TOKEN 请求头
    const method = config.method?.toUpperCase()
    if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      const xsrfToken = getXsrfTokenFromCookie()
      if (xsrfToken) {
        config.headers['X-XSRF-TOKEN'] = xsrfToken
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * - 统一错误处理（自动翻译为友好提示）
 * - 401 时自动尝试用 refreshToken 刷新并重试请求
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // 业务错误码处理
    if (res.code !== 0 && res.code !== 200) {
      // 业务错误：用友好提示弹窗；不显示技术细节
      showFriendlyError({ code: res.code, message: res.message })
      // Token 过期或无效 — 尝试刷新
      if (res.code === 401) {
        return handleTokenRefresh(response.config)
      }
      return Promise.reject(new Error(friendlyMessage({ code: res.code, message: res.message })))
    }
    return res as any
  },
  async (error) => {
    // HTTP 状态码错误处理
    const status = error.response?.status
    const data = error.response?.data

    // 401 — 尝试刷新 Token 并重试
    if (status === 401) {
      const hasRefreshToken = !!storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
      if (hasRefreshToken) {
        return handleTokenRefresh(error.response.config)
      }
      // 无 refreshToken，直接跳转登录
      storage.remove(STORAGE_KEYS.TOKEN)
      storage.remove(STORAGE_KEYS.USER_PERMISSIONS)
      router.push('/login')
      return Promise.reject(error)
    }

    // 5xx / 网络错误 — 自动重试（指数退避）
    const isServerError = status !== undefined && status >= 500
    const isNetworkError = error.code === 'ECONNABORTED' || error.code === 'ERR_NETWORK'
    if (isServerError || isNetworkError) {
      const config = error.config as AxiosRequestConfig
      config.__retryCount = config.__retryCount || 0
      if (config.__retryCount < MAX_RETRIES) {
        config.__retryCount++
        // 指数退避：第1次等1s，第2次等2s
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY * (config.__retryCount || 0)))
        return service(config)
      }
    }

    // 友好化错误处理
    showFriendlyError({ status, message: data?.message, raw: error })

    return Promise.reject(error)
  }
)

/**
 * 友好错误提示：移动端适配、不溢出屏幕、大白话
 *  - 业务码 → 业务码语义表
 *  - HTTP 状态码 → 通用提示
 *  - 403 单独处理为更显眼的弹框
 *  - 其他错误 → ElMessage 顶部居中
 */
function showFriendlyError(opts: { code?: number | string; status?: number; message?: string; raw?: any }) {
  const fe = toFriendlyError(opts)
  // 403 用更显眼的弹框（强提示）
  if (fe.status === 403 || fe.code === 403 || fe.code === '403') {
    ElMessageBox.alert(
      '抱歉，您暂时没有权限执行此操作。如有疑问请联系系统管理员。',
      '暂无操作权限',
      { confirmButtonText: '我知道了', type: 'warning' }
    ).catch(() => {})
    return
  }
  // 业务码 0/200 不弹窗
  if (fe.code !== undefined && [0, 200, '0', '200'].includes(fe.code as any)) return
  // 默认走顶部轻提示；Element Plus 默认就是顶部居中，配置 offset 后很显眼
  ElMessage({
    message: fe.title,
    type: 'error',
    // 长消息自动延长展示
    duration: fe.title.length > 20 ? 4000 : 2200,
    // 移动端也会换行 + 居中，不溢出
    customClass: 'zhixun-friendly-message',
    // Element Plus 的 grouping 让相同消息合并，但这里不去重
    grouping: true,
  })
}

/**
 * 处理 Token 刷新并重试原请求
 * 使用全局刷新锁，多个并发 401 只触发一次刷新
 */
async function handleTokenRefresh(requestConfig: AxiosRequestConfig) {
  const currentRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
  if (!currentRefreshToken) {
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.USER_PERMISSIONS)
    router.push('/login')
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }

  // 如果已经在刷新中，将当前请求加入等待队列
  if (isRefreshing && refreshPromise) {
    return new Promise((resolve, reject) => {
      pendingRequests.push((newToken: string) => {
        if (requestConfig.headers) {
          requestConfig.headers.Authorization = `Bearer ${newToken}`
        }
        resolve(service(requestConfig))
      })
      refreshPromise!.catch(() => {
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
    return service(requestConfig)
  } catch {
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }
}

/**
 * 通用请求方法封装
 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service(config) as Promise<ApiResponse<T>>
}

/** GET 请求 */
export function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ method: 'GET', url, params })
}

/** POST 请求 */
export function post<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ method: 'POST', url, data })
}

/** PUT 请求 */
export function put<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ method: 'PUT', url, data })
}

/** DELETE 请求 */
export function del<T = unknown>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ method: 'DELETE', url, params })
}

/**
 * 从浏览器 Cookie 中读取 XSRF-TOKEN
 */
function getXsrfTokenFromCookie(): string | null {
  const cookies = document.cookie.split(';')
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=')
    if (name === 'XSRF-TOKEN') {
      return decodeURIComponent(value)
    }
  }
  return null
}

export default service
