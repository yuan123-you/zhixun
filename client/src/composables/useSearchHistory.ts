import { storage, STORAGE_KEYS, TTL } from '@/utils/storage'

/** 搜索历史条目 */
export interface SearchHistoryEntry {
  /** 搜索关键词 */
  keyword: string
  /** 搜索时间 */
  searchedAt: number
}

/** 最大保留条目数 */
const MAX_ENTRIES = 50

/**
 * 搜索历史本地管理
 * - 最多保留 50 条搜索记录
 * - 重复搜索提升到最前
 * - 支持清空
 */
export const useSearchHistory = () => {
  /**
   * 获取搜索历史
   */
  const getHistory = (): SearchHistoryEntry[] => {
    return storage.get<SearchHistoryEntry[]>(STORAGE_KEYS.SEARCH_HISTORY) || []
  }

  /**
   * 添加搜索记录
   * @param keyword 搜索关键词
   */
  const addHistory = (keyword: string): void => {
    if (!keyword.trim()) return

    const entries = getHistory()

    // 移除相同关键词的旧记录
    const filtered = entries.filter((e) => e.keyword !== keyword.trim())

    // 添加到头部
    filtered.unshift({
      keyword: keyword.trim(),
      searchedAt: Date.now(),
    })

    // 限制条目数
    if (filtered.length > MAX_ENTRIES) {
      filtered.splice(MAX_ENTRIES)
    }

    storage.set(STORAGE_KEYS.SEARCH_HISTORY, filtered, TTL.DAY_30)
  }

  /**
   * 删除单条搜索记录
   */
  const removeHistory = (keyword: string): void => {
    const entries = getHistory()
    const filtered = entries.filter((e) => e.keyword !== keyword)
    storage.set(STORAGE_KEYS.SEARCH_HISTORY, filtered, TTL.DAY_30)
  }

  /**
   * 清空搜索历史
   */
  const clearHistory = (): void => {
    storage.remove(STORAGE_KEYS.SEARCH_HISTORY)
  }

  return {
    getHistory,
    addHistory,
    removeHistory,
    clearHistory,
  }
}
