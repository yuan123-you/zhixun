/**
 * 友好错误处理 composable
 *
 * 提供：
 *  - showError(error)    — 把任意错误翻译成大白话，用红色 toast 弹出
 *  - showWarning(error)  — 警告色
 *  - showInfo(error)     — 信息色
 *  - toFriendly(error)   — 仅翻译成 { title, detail }，不弹窗
 *
 * 用法：
 *  ```ts
 *  const { showError } = useFriendlyError()
 *  try { await api.x() } catch (e) { showError(e) }
 *  ```
 */
import { showToast, showToastWithDetail } from './useToast'
import { toFriendlyError, friendlyMessage, type FriendlyError } from '@/utils/friendlyError'

export function useFriendlyError() {
  const showError = (input: any, fallback?: string) => {
    const fe: FriendlyError = toFriendlyError({ raw: input, message: typeof input === 'string' ? input : input?.message, fallback })
    if (fe.detail && fe.detail !== fe.title) {
      showToastWithDetail(fe.title, fe.detail, 'error')
    } else {
      showToast(fe.title, 'error')
    }
  }

  const showWarning = (input: any, fallback?: string) => {
    const fe: FriendlyError = toFriendlyError({ raw: input, message: typeof input === 'string' ? input : input?.message, fallback })
    if (fe.detail && fe.detail !== fe.title) {
      showToastWithDetail(fe.title, fe.detail, 'warning')
    } else {
      showToast(fe.title, 'warning')
    }
  }

  const showInfo = (input: any, fallback?: string) => {
    showToast(friendlyMessage(input, fallback || '操作完成'), 'info')
  }

  return {
    showError,
    showWarning,
    showInfo,
    toFriendly: toFriendlyError,
    friendlyMessage,
  }
}

export default useFriendlyError
