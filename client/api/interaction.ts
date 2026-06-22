import type { Comment, PaginationParams, SearchResult } from '~/types'

/** 互动API */
export const interactionApi = {
  /** 切换点赞状态 */
  toggleLike: (articleId: number) => {
    const { post } = useApi()
    return post<{ isLiked: boolean; likeCount: number }>(`/articles/${articleId}/like`)
  },

  /** 切换收藏状态 */
  toggleCollect: (articleId: number) => {
    const { post } = useApi()
    return post<{ isCollected: boolean; collectCount: number }>(`/articles/${articleId}/collect`)
  },

  /** 获取评论列表 */
  getComments: (articleId: number, params?: PaginationParams) => {
    const { get } = useApi()
    return get<SearchResult<Comment>>(`/articles/${articleId}/comments`, params)
  },

  /** 发表评论 */
  createComment: (articleId: number, data: { content: string; parentId?: number }) => {
    const { post } = useApi()
    return post<Comment>(`/articles/${articleId}/comments`, data)
  },

  /** 删除评论 */
  deleteComment: (articleId: number, commentId: number) => {
    const { delete: del } = useApi()
    return del(`/articles/${articleId}/comments/${commentId}`)
  },
}
