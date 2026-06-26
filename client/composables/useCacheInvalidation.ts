/**
 * useCacheInvalidation - 缓存失效管理
 *
 * 在数据变更操作（点赞、收藏、评论等）后，自动失效相关缓存，
 * 确保后续请求获取最新数据。
 */

/** 需要失效的缓存 URL 前缀映射 */
const INVALIDATION_MAP = {
  /** 作品相关操作后需要失效的缓存前缀 */
  article: [
    '/feed/recommend',
    '/feed/latest',
    '/feed/hot',
    '/feed/following',
    '/articles',
    '/rank',
  ],
  /** 用户相关操作后需要失效的缓存前缀 */
  user: [
    '/users',
  ],
  /** 标签相关操作后需要失效的缓存前缀 */
  tag: [
    '/tags',
  ],
  /** 分类相关操作后需要失效的缓存前缀 */
  category: [
    '/categories',
  ],
  /** Banner 相关操作后需要失效的缓存前缀 */
  banner: [
    '/banners',
  ],
} as const

type InvalidationCategory = keyof typeof INVALIDATION_MAP

export function useCacheInvalidation() {
  const { invalidateByPrefix, clearAll } = useRequestCache()

  /**
   * 失效指定类别的缓存
   */
  const invalidate = (...categories: InvalidationCategory[]) => {
    for (const category of categories) {
      const prefixes = INVALIDATION_MAP[category]
      for (const prefix of prefixes) {
        invalidateByPrefix(prefix)
      }
    }
  }

  /**
   * 失效所有缓存
   */
  const invalidateAll = () => {
    clearAll()
  }

  /**
   * 失效作品相关缓存（点赞/收藏/评论后调用）
   */
  const invalidateArticle = () => {
    invalidate('article')
  }

  /**
   * 失效用户相关缓存（关注/取关后调用）
   */
  const invalidateUser = () => {
    invalidate('user')
  }

  /**
   * 失效标签相关缓存
   */
  const invalidateTag = () => {
    invalidate('tag')
  }

  /**
   * 失效分类相关缓存
   */
  const invalidateCategory = () => {
    invalidate('category')
  }

  /**
   * 失效 Banner 相关缓存
   */
  const invalidateBanner = () => {
    invalidate('banner')
  }

  return {
    invalidate,
    invalidateAll,
    invalidateArticle,
    invalidateUser,
    invalidateTag,
    invalidateCategory,
    invalidateBanner,
  }
}
