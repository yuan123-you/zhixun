import type { Comment, PaginationParams, PageResult } from '~/types'

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
  getComments: (articleId: number, params?: PaginationParams & { sort?: string }) => {
    const { get } = useApi()
    return get<PageResult<Comment>>(`/articles/${articleId}/comments`, params)
  },

  /** 发表评论 */
  createComment: (articleId: number, data: { content: string; parentId?: number; replyUserId?: number }) => {
    const { post } = useApi()
    return post<Comment>(`/articles/${articleId}/comments`, data)
  },

  /** 删除评论 */
  deleteComment: (commentId: number) => {
    const { delete: del } = useApi()
    return del(`/comments/${commentId}`)
  },

  /** 举报评论 */
  reportComment: (commentId: number, data: { reason?: string }) => {
    const { post } = useApi()
    return post(`/comments/${commentId}/report`, data)
  },

  /** 点赞评论 */
  likeComment: (commentId: number) => {
    const { post } = useApi()
    return post<{ isLiked: boolean; likeCount: number }>(`/comments/${commentId}/like`)
  },
}
