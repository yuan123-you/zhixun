import type { ApiResponse } from '~/types'

/** API插件：提供全局 $api 访问 */
export default defineNuxtPlugin(() => {
  const { get, post, put, delete: del } = useApi()

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
