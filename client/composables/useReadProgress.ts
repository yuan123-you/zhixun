import { storage, STORAGE_KEYS, TTL } from '~/utils/storage'

/** 阅读进度 */
interface ReadProgress {
  /** 滚动位置（像素） */
  scrollTop: number
  /** 阅读百分比（0-100） */
  percentage: number
  /** 保存时间 */
  savedAt: number
}

/** 阅读进度保留天数 */
const PROGRESS_TTL = TTL.DAY_7

/**
 * 文章阅读进度本地记录
 * - 记录每篇文章的滚动位置和阅读百分比
 * - 返回文章时自动恢复滚动位置
 * - 7 天后自动过期
 */
export const useReadProgress = () => {
  /**
   * 获取阅读进度
   * @param articleId 文章ID
   */
  const getProgress = (articleId: number): ReadProgress | null => {
    return storage.get<ReadProgress>(`${STORAGE_KEYS.READ_PROGRESS_PREFIX}${articleId}`)
  }

  /**
   * 保存阅读进度
   * @param articleId 文章ID
   * @param scrollTop 滚动位置
   * @param percentage 阅读百分比
   */
  const saveProgress = (articleId: number, scrollTop: number, percentage: number): void => {
    storage.set(
      `${STORAGE_KEYS.READ_PROGRESS_PREFIX}${articleId}`,
      {
        scrollTop,
        percentage,
        savedAt: Date.now(),
      },
      PROGRESS_TTL
    )
  }

  /**
   * 删除阅读进度
   * @param articleId 文章ID
   */
  const removeProgress = (articleId: number): void => {
    storage.remove(`${STORAGE_KEYS.READ_PROGRESS_PREFIX}${articleId}`)
  }

  /**
   * 恢复文章滚动位置
   * @param articleId 文章ID
   * @param containerEl 容器元素或选择器
   */
  const restoreScroll = (articleId: number, containerEl: HTMLElement | string): void => {
    if (!import.meta.client) return

    const progress = getProgress(articleId)
    if (!progress || progress.scrollTop === 0) return

    const container = typeof containerEl === 'string'
      ? document.querySelector(containerEl)
      : containerEl

    if (container) {
      // 延迟恢复，等待内容渲染
      requestAnimationFrame(() => {
        container.scrollTop = progress.scrollTop
      })
    }
  }

  /**
   * 监听滚动并自动保存进度
   * @param articleId 文章ID
   * @param containerEl 容器元素
   * @param contentHeight 内容总高度
   * @returns 清理函数
   */
  const watchScroll = (
    articleId: number,
    containerEl: HTMLElement | string,
    contentHeight: () => number
  ): () => void => {
    if (!import.meta.client) return () => {}

    const container = typeof containerEl === 'string'
      ? document.querySelector(containerEl)
      : containerEl

    if (!container) return () => {}

    let saveTimer: ReturnType<typeof setTimeout> | null = null

    const onScroll = () => {
      const el = container as HTMLElement
      const scrollTop = el.scrollTop
      const totalHeight = contentHeight() - el.clientHeight
      const percentage = totalHeight > 0 ? Math.round((scrollTop / totalHeight) * 100) : 0

      // 防抖保存
      if (saveTimer) clearTimeout(saveTimer)
      saveTimer = setTimeout(() => {
        saveProgress(articleId, scrollTop, Math.min(percentage, 100))
      }, 500)
    }

    container.addEventListener('scroll', onScroll, { passive: true })

    return () => {
      container.removeEventListener('scroll', onScroll)
      if (saveTimer) clearTimeout(saveTimer)
    }
  }

  return {
    getProgress,
    saveProgress,
    removeProgress,
    restoreScroll,
    watchScroll,
  }
}
