import type { RankItem } from '@/types'

/** 排行API */
export const rankApi = {
  /** 获取热榜排行 */
  getHotRank: (type: 'daily' | 'weekly' | 'monthly') => {
    const { get } = useApi()
    return get<RankItem[]>('/rank/hot', { period: type })
  },
}
