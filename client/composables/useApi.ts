import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '~/types'

/** API请求封装组合式函数 */
export const useApi = () => {
  const config = useRuntimeConfig()
  const userStore = useUserStore()

  // 创建axios实例
  const instance: AxiosInstance = axios.create({
    baseURL: config.public.apiBase as string,
    timeout: 15000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 请求拦截器：自动携带Token
  instance.interceptors.request.use(
    (requestConfig) => {
      const token = userStore.token
      if (token) {
        requestConfig.headers.Authorization = `Bearer ${token}`
      }
      return requestConfig
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器：统一错误处理
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { data } = response
      // 业务错误码处理
      if (data.code !== 0 && data.code !== 200) {
        // Token过期，尝试刷新
        if (data.code === 401) {
          return handleTokenRefresh(response.config)
        }
        // 其他业务错误
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return response
    },
    async (error) => {
      // HTTP状态码错误处理
      if (error.response) {
        const { status } = error.response
        switch (status) {
          case 401:
            // Token过期，尝试刷新
            return handleTokenRefresh(error.response.config)
          case 403:
            return Promise.reject(new Error('没有权限访问'))
          case 404:
            return Promise.reject(new Error('请求的资源不存在'))
          case 429:
            return Promise.reject(new Error('请求过于频繁，请稍后再试'))
          case 500:
            return Promise.reject(new Error('服务器内部错误'))
          default:
            return Promise.reject(new Error(error.response.data?.message || '请求失败'))
        }
      }
      // 网络错误
      if (error.code === 'ECONNABORTED') {
        return Promise.reject(new Error('请求超时，请稍后重试'))
      }
      return Promise.reject(new Error('网络异常，请检查网络连接'))
    }
  )

  // Token过期自动刷新
  let isRefreshing = false
  let pendingRequests: Array<(token: string) => void> = []

  const handleTokenRefresh = async (requestConfig: AxiosRequestConfig) => {
    const refreshToken = userStore.refreshToken
    if (!refreshToken) {
      userStore.logout()
      navigateTo('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    if (!isRefreshing) {
      isRefreshing = true
      try {
        const response = await axios.post(`${config.public.apiBase}/auth/refresh`, {
          refreshToken,
        })
        const { token: newToken, refreshToken: newRefreshToken } = response.data.data
        userStore.setToken(newToken, newRefreshToken)

        // 重试所有挂起的请求
        pendingRequests.forEach((callback) => callback(newToken))
        pendingRequests = []

        // 重试当前请求
        if (requestConfig.headers) {
          requestConfig.headers.Authorization = `Bearer ${newToken}`
        }
        return instance(requestConfig)
      } catch {
        userStore.logout()
        navigateTo('/login')
        return Promise.reject(new Error('登录已过期，请重新登录'))
      } finally {
        isRefreshing = false
      }
    }

    // 正在刷新Token，将请求加入队列
    return new Promise((resolve) => {
      pendingRequests.push((token: string) => {
        if (requestConfig.headers) {
          requestConfig.headers.Authorization = `Bearer ${token}`
        }
        resolve(instance(requestConfig))
      })
    })
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

  return {
    instance,
    get,
    post,
    put,
    delete: del,
  }
}
