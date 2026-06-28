/**
 * 友好错误处理 composable（管理后台版）
 *
 * 提供：
 *  - showError(error)    — 把任意错误翻译成大白话，用红色 ElMessage 弹出
 *  - showWarning(error)  — 警告色
 *  - showInfo(error)     — 信息色
 *  - toFriendly(error)   — 仅翻译成 { title, detail }，不弹窗
 */
import { ElMessage } from 'element-plus'
import { toFriendlyError, friendlyMessage, type FriendlyError } from '@/utils/friendlyError'

export function useFriendlyError() {
  const showError = (input: any, fallback?: string) => {
    const fe: FriendlyError = toFriendlyError({ raw: input, message: typeof input === 'string' ? input : input?.message, fallback })
    if (fe.detail && fe.detail !== fe.title) {
      ElMessage({
        message: `${fe.title}\n${fe.detail}`,
        type: 'error',
        duration: fe.title.length + fe.detail.length > 30 ? 4500 : 2500,
        customClass: 'zhixun-friendly-message',
        grouping: true,
        showClose: false,
      })
    } else {
      ElMessage({
        message: fe.title,
        type: 'error',
        duration: fe.title.length > 20 ? 4000 : 2200,
        customClass: 'zhixun-friendly-message',
        grouping: true,
        showClose: false,
      })
    }
  }

  const showWarning = (input: any, fallback?: string) => {
    const msg = friendlyMessage(input, fallback || '请注意当前操作')
    ElMessage({ message: msg, type: 'warning', duration: msg.length > 20 ? 3500 : 2000 })
  }

  const showInfo = (input: any, fallback?: string) => {
    const msg = friendlyMessage(input, fallback || '操作完成')
    ElMessage({ message: msg, type: 'info', duration: msg.length > 20 ? 3500 : 2000 })
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
