import type { PaginationParams, PageResult } from '@/types'

export interface Topic {
  id: number
  name: string
  description: string
  coverImage: string
  articleCount: number
  followCount: number
  hotScore: number
  isOfficial: number
  isFollowed: boolean
}

export const topicApi = {
  createTopic: (data: { name: string; description?: string; coverImage?: string }) => {
    const { post } = useApi()
    return post<number>('/topics', data)
  },
  getTopics: (params?: PaginationParams & { keyword?: string; orderBy?: string }) => {
    const { get } = useApi()
    return get<PageResult<Topic>>('/topics', params)
  },
  getTopicDetail: (id: number) => {
    const { get } = useApi()
    return get<Topic>(`/topics/${id}`)
  },
  toggleFollow: (id: number) => {
    const { post } = useApi()
    return post<void>(`/topics/${id}/follow`)
  },
  getHotTopics: (limit = 20) => {
    const { get } = useApi()
    return get<Topic[]>('/topics/hot', { limit })
  },
  searchTopics: (keyword: string, limit = 10) => {
    const { get } = useApi()
    return get<Topic[]>('/topics/search', { keyword, limit })
  },
  getFollowedTopics: () => {
    const { get } = useApi()
    return get<Topic[]>('/topics/followed')
  },
}