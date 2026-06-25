import { storage, STORAGE_KEYS, TTL } from '~/utils/storage'

/** 本地浏览记录条目 */
interface ViewHistoryEntry {
  /** 文章ID */
  articleId: number
  /** 文章标题 */
  title: string
  /** 浏览时长（秒） */
  viewDuration: number
  /** 访问IP（仅本地记录） */
  ip?: string
  /** 用户代理（仅本地记录） */
  userAgent?: string
  /** 浏览时间戳 */
  visitedAt: number
}

/** 本地最大保留条目数 */
const MAX_LOCAL_ENTRIES = 500

/** 批量同步间隔（毫秒） */
const SYNC_INTERVAL = 5 * 60 * 1000 // 5 分钟

/**
 * 浏览历史本地化管理
 * - 本地存储完整浏览记录（含 IP、UA 等隐私字段）
 * - 定期批量同步精简数据到服务器（仅 article_id + view_duration）
 */
export const useViewHistory = () => {
  const { post } = useApi()
  const userStore = useUserStore()

  /**
   * 获取本地浏览记录
   */
  const getLocalHistory = (): ViewHistoryEntry[] => {
    return storage.get<ViewHistoryEntry[]>(STORAGE_KEYS.VIEW_HISTORY_LOCAL) || []
  }

  /**
   * 记录一次浏览
   * @param articleId 文章ID
   * @param title 文章标题
   */
  const recordView = (articleId: number, title: string) => {
    if (!import.meta.client) return

    const entries = getLocalHistory()

    // 移除同一文章的旧记录（保留最新一次）
    const filtered = entries.filter((e) => e.articleId !== articleId)

    // 新增记录
    const entry: ViewHistoryEntry = {
      articleId,
      title,
      viewDuration: 0,
      ip: '', // IP 由服务端在请求时获取，本地不记录
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
   * @param articleId 文章ID
   * @param durationSeconds 浏览时长（秒）
   */
  const updateDuration = (articleId: number, durationSeconds: number) => {
    if (!import.meta.client) return

    const entries = getLocalHistory()
    const entry = entries.find((e) => e.articleId === articleId)
    if (entry) {
      entry.viewDuration = durationSeconds
      storage.set(STORAGE_KEYS.VIEW_HISTORY_LOCAL, entries, TTL.DAY_30)
    }
  }

  /**
   * 批量同步浏览记录到服务器（仅同步精简数据）
   */
  const syncToServer = async () => {
    if (!userStore.isLoggedIn) return

    const entries = getLocalHistory()
    if (entries.length === 0) return

    // 只同步有浏览时长的记录
    const toSync = entries
      .filter((e) => e.viewDuration > 0)
      .map((e) => ({
        articleId: e.articleId,
        viewDuration: e.viewDuration,
      }))

    if (toSync.length === 0) return

    try {
      await post('/user/view-history/batch', { records: toSync })
      // 同步成功后，清除已同步的浏览时长标记（避免重复同步）
      const updated = entries.map((e) => ({
        ...e,
        viewDuration: 0, // 重置时长，表示已同步
      }))
      storage.set(STORAGE_KEYS.VIEW_HISTORY_LOCAL, updated, TTL.DAY_30)
    } catch {
      // 同步失败，保留数据下次重试
    }
  }

  /**
   * 清空本地浏览记录
   */
  const clearLocalHistory = () => {
    storage.remove(STORAGE_KEYS.VIEW_HISTORY_LOCAL)
  }

  /**
   * 启动定时同步
   */
  const startAutoSync = () => {
    if (!import.meta.client) return

    // 页面可见时同步
    syncToServer()

    // 定时同步
    const timer = setInterval(() => {
      syncToServer()
    }, SYNC_INTERVAL)

    // 页面卸载前同步
    const beforeUnload = () => {
      syncToServer()
    }
    window.addEventListener('beforeunload', beforeUnload)

    // 返回清理函数
    return () => {
      clearInterval(timer)
      window.removeEventListener('beforeunload', beforeUnload)
    }
  }

  return {
    getLocalHistory,
    recordView,
    updateDuration,
    syncToServer,
    clearLocalHistory,
    startAutoSync,
  }
}
