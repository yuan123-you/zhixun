/**
 * $fetch - 基于 axios 的 HTTP 请求函数
 * 自动应用全局拦截器（认证、CSRF Token、错误处理）
 */
import axios from 'axios'

// 复用 useApi 的 axios 实例，保证拦截器一致
const api = axios.create({
  timeout: 15000,
})

export async function $fetch<T = any>(url: string, options?: Record<string, any>): Promise<T> {
  const { params, headers, body, method = 'GET', ...rest } = options || {}

  const config: Record<string, any> = {
    url,
    method: method.toUpperCase(),
    params,
    headers,
    ...rest,
  }

  if (body) {
    config.data = body
  }

  const response = await api(config)
  return response.data as T
}
