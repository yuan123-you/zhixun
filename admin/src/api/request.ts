import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import router from '@/router'

// 创建 Axios 实例
const service = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 请求拦截器
 * - 自动携带 Token
 * - CSRF 防护：状态变更请求自动携带 X-XSRF-TOKEN 请求头
 */
service.interceptors.request.use(
  (config) => {
    const token = storage.get<string>(STORAGE_KEYS.TOKEN)
    if (token) {
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
 * - 统一错误处理
 * - 401 自动跳转登录页
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // 业务错误码处理
    if (res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // Token 过期或无效
      if (res.code === 401) {
        storage.remove(STORAGE_KEYS.TOKEN)
        storage.remove(STORAGE_KEYS.USER_PERMISSIONS)
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res as any
  },
  (error) => {
    // HTTP 状态码错误处理
    const status = error.response?.status
    const messages: Record<number, string> = {
      400: '请求参数错误',
      401: '未授权，请重新登录',
      403: '拒绝访问',
      404: '请求资源不存在',
      500: '服务器内部错误',
    }
    const message = messages[status] || `请求失败: ${error.message}`
    ElMessage.error(message)

    // 401 自动跳转登录页
    if (status === 401) {
      storage.remove(STORAGE_KEYS.TOKEN)
      storage.remove(STORAGE_KEYS.USER_PERMISSIONS)
      router.push('/login')
    }

    return Promise.reject(error)
  }
)

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
