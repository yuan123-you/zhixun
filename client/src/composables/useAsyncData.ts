/**
 * useAsyncData - 异步数据加载 composable
 * 同步返回 refs，异步填充数据
 *
 * 增强点：
 * - 失败时保留旧数据，不显示空白页
 * - 支持响应式 key：当 key 变化时自动重新执行 fetcher
 *   兼容形式：string | (() => string) | Ref<string>
 *   可选 watch: true（默认 false）开启自动监听
 *   key 内部加版本号后缀，watch 触发时会同时清空 data，避免旧值闪烁
 */
import type { Ref } from 'vue'

export interface AsyncDataOptions<T> {
  default?: () => T
  server?: boolean
  lazy?: boolean
  immediate?: boolean
  /**
   * 是否监听 key 变化自动重新获取（仅在 key 为函数/ref 时生效）
   * 适用于动态路由参数页面（如 /user/[id] → /user/[id]）
   */
  watch?: boolean
}

export interface AsyncDataResult<T> {
  data: Ref<T>
  pending: Ref<boolean>
  error: Ref<any>
  refresh: () => Promise<void>
}

let asyncDataSeq = 0

export function useAsyncData<T>(
  key: string | (() => string) | Ref<string>,
  fetcher: () => Promise<T>,
  options?: AsyncDataOptions<T>
): AsyncDataResult<T> {
  asyncDataSeq++  // 调试用：追踪 useAsyncData 调用次数
  const data = ref(options?.default?.() ?? undefined) as Ref<T>
  const pending = ref(options?.lazy !== true)
  const error = ref<any>(null)

  // 用于标记本次 refresh 的有效性：watch 触发时若版本不匹配则丢弃结果
  let currentVersion = 0

  const refresh = async () => {
    const myVersion = ++currentVersion
    pending.value = true
    error.value = null
    try {
      const newData = await fetcher()
      // 版本已变（key 在请求中又变了），丢弃本次结果
      if (myVersion !== currentVersion) return
      // 仅当获取到有效数据时才更新，避免空数据覆盖已有内容
      if (newData !== undefined && newData !== null) {
        data.value = newData
      }
    } catch (err: any) {
      if (myVersion !== currentVersion) return
      error.value = err?.message || err || '数据加载失败'
      // 仅在首次加载且无默认值时设置默认数据，避免覆盖已有数据
      if (options?.default && data.value === undefined) {
        data.value = options.default()
      }
    } finally {
      if (myVersion === currentVersion) {
        pending.value = false
      }
    }
  }

  // 延迟执行初始请求，不阻塞组件渲染
  if (options?.lazy !== true) {
    nextTick(() => refresh())
  }

  // 响应式 key：监听变化时清空旧数据并重新拉取
  if (options?.watch && (typeof key === 'function' || typeof key !== 'string')) {
    const keySource = (): unknown =>
      typeof key === 'function' ? key() : (key as Ref<string>).value

    const keyRef = ref(keySource())
    watchEffect(() => {
      keyRef.value = keySource() as any
    })

    watch(keyRef, async (newKey, oldKey) => {
      if (newKey === oldKey) return
      data.value = options?.default?.() ?? (undefined as any)
      error.value = null
      await refresh()
    })
  }

  return { data, pending, error, refresh }
}
