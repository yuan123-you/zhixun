/** 仪表盘统计数据（对齐后端 DashboardVO） */
export interface DashboardData {
  userTotal: number
  articleTotal: number
  todayDau: number
  todayView: number
  todayLike: number
  todayComment: number
  trend: TrendData
  retentionRates: RetentionRate[]
  activityDistributions: ActivityDistribution[]
  growthTrends: GrowthTrend[]
  categoryDistributions: CategoryDistribution[]
  hotArticleRanks: HotArticleRank[]
  creatorRanks: CreatorRank[]
}

/** 趋势数据 */
export interface TrendData {
  dates: string[]
  views: number[]
  users: number[]
}

/** 留存率 */
export interface RetentionRate {
  day: number
  rate: number
}

/** 活跃度分布 */
export interface ActivityDistribution {
  level: string
  count: number
  percentage: number
}

/** 增长趋势 */
export interface GrowthTrend {
  periodLabel: string
  dimension: string
  newUserCount: number
  newArticleCount: number
  viewCount: number
}

/** 分类分布 */
export interface CategoryDistribution {
  categoryId: number
  categoryName: string
  articleCount: number
  percentage: number
}

/** 热门作品排行 */
export interface HotArticleRank {
  articleId: number
  title: string
  viewCount: number
  likeCount: number
  commentCount: number
  authorName: string
  rank: number
}

/** 创作者排行 */
export interface CreatorRank {
  userId: number
  nickname: string
  avatar: string
  articleCount: number
  totalViews: number
  totalLikes: number
  followerCount: number
  rank: number
}