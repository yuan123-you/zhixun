import { storage, STORAGE_KEYS, TTL } from '@/utils/storage'

/** 作品草稿 */
export interface ArticleDraft {
  /** 唯一标识（时间戳） */
  id: string
  /** 标题 */
  title: string
  /** 内容 */
  content: string
  /** 分类ID */
  categoryId?: number
  /** 标签ID列表 */
  tagIds: number[]
  /** 封面图URL（已废弃，保留兼容） */
  coverImage?: string
  /** 作品图片列表（最多9张） */
  images?: string[]
  /** 保存时间 */
  savedAt: number
}

/** 最大草稿数 */
const MAX_DRAFTS = 10

/** 单份草稿最大大小（字节） */
const MAX_DRAFT_SIZE = 500 * 1024 // 500KB

/** 自动保存间隔（毫秒） */
const AUTO_SAVE_INTERVAL = 30 * 1000 // 30 秒

/**
 * 作品草稿本地存储
 * - 自动保存编辑中的作品
 * - 支持多份草稿
 * - 提交成功后自动清理
 */
export const useDraft = () => {
  /**
   * 获取所有草稿
   */
  const getDrafts = (): ArticleDraft[] => {
    return storage.get<ArticleDraft[]>(STORAGE_KEYS.ARTICLE_DRAFT) || []
  }

  /**
   * 保存草稿
   */
  const saveDraft = (draft: Omit<ArticleDraft, 'id' | 'savedAt'>): ArticleDraft => {
    const drafts = getDrafts()

    const newDraft: ArticleDraft = {
      ...draft,
      id: Date.now().toString(),
      savedAt: Date.now(),
    }

    // 检查单份草稿大小
    const draftSize = JSON.stringify(newDraft).length * 2
    if (draftSize > MAX_DRAFT_SIZE) {
      // 内容过大，截断内容
      const maxContentLength = Math.floor((MAX_DRAFT_SIZE - 2000) / 2)
      newDraft.content = newDraft.content.slice(0, maxContentLength) + '...（内容过长已截断）'
    }

    // 添加到列表头部
    drafts.unshift(newDraft)

    // 限制最大草稿数
    if (drafts.length > MAX_DRAFTS) {
      drafts.splice(MAX_DRAFTS)
    }

    storage.set(STORAGE_KEYS.ARTICLE_DRAFT, drafts, TTL.DAY_7)
    return newDraft
  }

  /**
   * 更新已有草稿
   */
  const updateDraft = (id: string, updates: Partial<ArticleDraft>): void => {
    const drafts = getDrafts()
    const index = drafts.findIndex((d) => d.id === id)
    if (index > -1) {
      drafts[index] = { ...drafts[index], ...updates, savedAt: Date.now() }
      storage.set(STORAGE_KEYS.ARTICLE_DRAFT, drafts, TTL.DAY_7)
    }
  }

  /**
   * 删除草稿
   */
  const removeDraft = (id: string): void => {
    const drafts = getDrafts()
    const filtered = drafts.filter((d) => d.id !== id)
    storage.set(STORAGE_KEYS.ARTICLE_DRAFT, filtered, TTL.DAY_7)
  }

  /**
   * 清空所有草稿
   */
  const clearDrafts = (): void => {
    storage.remove(STORAGE_KEYS.ARTICLE_DRAFT)
  }

  /**
   * 自动保存（防抖）
   * 返回清理函数
   */
  const startAutoSave = (
    getDraftData: () => Omit<ArticleDraft, 'id' | 'savedAt'>,
    draftId: Ref<string | null>
  ) => {
    let timer: ReturnType<typeof setInterval> | null = null

    timer = setInterval(() => {
      const data = getDraftData()
      if (!data.title && !data.content) return

      if (draftId.value) {
        updateDraft(draftId.value, data)
      } else {
        const draft = saveDraft(data)
        draftId.value = draft.id
      }
    }, AUTO_SAVE_INTERVAL)

    return () => {
      if (timer) clearInterval(timer)
    }
  }

  return {
    getDrafts,
    saveDraft,
    updateDraft,
    removeDraft,
    clearDrafts,
    startAutoSave,
  }
}
