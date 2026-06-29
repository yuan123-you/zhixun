import { storage, STORAGE_KEYS, TTL } from '@/utils/storage'

/** 本地浏览记录条目 */
interface ViewHistoryEntry {
  /** 作品ID */
  articleId: number
  /** 作品标题 */
  title: string
  /** 浏览时长（秒） */
  viewDuration: number
  /** 用户代理（仅本地记录） */
  userAgent?: string
  /** 浏览时间戳 */
  visitedAt: number
}

/** 本地最大保留条目数 */
const MAX_LOCAL_ENTRIES = 500

/**
 * 浏览历史本地化管理（纯本地方案，不再向服务器同步）
 * - 所有记录仅存于 localStorage，浏览即记录，查看历史时直接读取
 * - 隐私友好：IP、同步标记、服务器字段均不再涉及
 */
export const useViewHistory = () => {
  /** 获取本地浏览记录 */
  const getLocalHistory = (): ViewHistoryEntry[] => {
    return storage.get<ViewHistoryEntry[]>(STORAGE_KEYS.VIEW_HISTORY_LOCAL) || []
  }

  /**
   * 记录一次浏览
   * @param articleId 作品ID
   * @param title 作品标题
   */
  const recordView = (articleId: number, title: string) => {
    if (typeof window === 'undefined') return
    if (!articleId) return

    const entries = getLocalHistory()

    // 移除同一作品的旧记录（保留最新一次）
    const filtered = entries.filter((e) => e.articleId !== articleId)

    // 新增记录
    const entry: ViewHistoryEntry = {
      articleId,
      title,
      viewDuration: 0,
      userAgent: navigator.userAgent,
      visitedAt: Date.now(),
    }

    filtered.unshift(entry)

    // 限制最大条目数
    if (filtered.length > MAX_LOCAL_ENTRIES) {
      filtered.splice(MAX_LOCAL_ENTRIES)
    }

    storage.set(STORAGE_KEYS.VIEW_HISTORY_LOCAL, filtered, TTL.DAY_30)
  }

  /**
   * 更新浏览时长
   * @param articleId 作品ID
   * @param durationSeconds 浏览时长（秒）
   */
  const updateDuration = (articleId: number, durationSeconds: number) => {
    if (typeof window === 'undefined') return
    if (!articleId || durationSeconds <= 0) return

    const entries = getLocalHistory()
    const entry = entries.find((e) => e.articleId === articleId)
    if (entry) {
      entry.viewDuration = durationSeconds
      storage.set(STORAGE_KEYS.VIEW_HISTORY_LOCAL, entries, TTL.DAY_30)
    }
  }

  /**
   * 清空本地浏览记录
   */
  const clearLocalHistory = () => {
    storage.remove(STORAGE_KEYS.VIEW_HISTORY_LOCAL)
  }

  return {
    getLocalHistory,
    recordView,
    updateDuration,
    clearLocalHistory,
  }
}
