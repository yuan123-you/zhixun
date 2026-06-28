export interface Report {
  id: number
  type: string
  targetId: number
  targetTitle: string
  reporterId: number
  reporterName: string
  reason: string
  description: string
  status: number
  handledBy?: number
  handlerName?: string
  handledAt?: string
  createdAt: string
}

export const reportApi = {
  reportArticle: (articleId: number, reason: string, description?: string) => {
    const { post } = useApi()
    return post<void>('/reports/article', { articleId, reason, description })
  },
  reportUser: (reportedUserId: number, reason: string, description?: string) => {
    const { post } = useApi()
    return post<void>('/reports/user', { reportedUserId, reason, description })
  },
}