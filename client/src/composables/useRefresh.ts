/**
 * useRefresh - 通用刷新组合式函数
 *
 * 功能：
 * - 统一的加载状态管理
 * - 错误处理与用户反馈（支持 toast 提示）
 * - 防抖保护：防止快速连续点击
 * - 刷新锁：防止并发刷新
 * - 支持静默刷新（不显示加载状态，适合后台自动刷新）
 * - 刷新成功/失败回调
 */

export interface RefreshOptions {
  /** 刷新回调函数 */
  onRefresh: () => Promise<void>
  /** 防抖间隔（毫秒），默认 300ms */
  debounceMs?: number
  /** 是否在刷新失败时显示错误提示，默认 true */
  showError?: boolean
  /** 错误提示消息，默认使用 catch 中的错误信息 */
  errorMessage?: string
  /** 刷新成功回调 */
  onSuccess?: () => void
  /** 刷新失败回调 */
  onError?: (error: Error) => void
}

export function useRefresh(options: RefreshOptions) {
  const {
    onRefresh,
    debounceMs = 300,
    showError = true,
    errorMessage,
    onSuccess,
    onError,
  } = options

  // 加载状态
  const loading = ref(false)
  // 错误信息
  const error = ref('')
  // 上次操作时间戳（防抖）
  let lastActionTime = 0

  /**
   * 执行刷新操作
   * @param silent 是否静默刷新（不改变 loading 状态）
   */
  const refresh = async (silent = false): Promise<boolean> => {
    // 防抖保护
    const now = Date.now()
    if (now - lastActionTime < debounceMs) {
      return false
    }
    lastActionTime = now

    // 刷新锁
    if (loading.value) {
      return false
    }

    if (!silent) {
      loading.value = true
    }
    error.value = ''

    try {
      await onRefresh()
      onSuccess?.()
      return true
    } catch (err: any) {
      const msg = errorMessage || err.message || '刷新失败，请检查网络后重试'
      error.value = msg
      onError?.(err instanceof Error ? err : new Error(msg))

      // 显示错误提示
      if (showError && !silent) {
        try {
          const { showAlert } = await import('./useAlert')
          showAlert(msg, 2000)
        } catch {
          // useAlert 不可用时静默处理
        }
      }

      return false
    } finally {
      if (!silent) {
        loading.value = false
      }
    }
  }

  /**
   * 静默刷新（不改变 loading 状态，不显示错误提示）
   */
  const silentRefresh = () => refresh(true)

  /**
   * 重置状态
   */
  const reset = () => {
    loading.value = false
    error.value = ''
    lastActionTime = 0
  }

  return {
    loading,
    error,
    refresh,
    silentRefresh,
    reset,
  }
}
