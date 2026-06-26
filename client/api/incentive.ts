export interface CheckInStatus {
  hasCheckedIn: boolean
  consecutiveDays: number
  todayPoints: number
  totalExp: number
  level: number
  levelName: string
}

export interface Badge {
  id: number
  name: string
  description: string
  icon: string
  category: string
  isOwned: boolean
  earnedAt?: string
}

export const incentiveApi = {
  checkIn: () => {
    const { post } = useApi()
    return post<CheckInStatus>('/incentive/checkin')
  },
  getCheckInStatus: () => {
    const { get } = useApi()
    return get<CheckInStatus>('/incentive/checkin/status')
  },
  getAllBadges: () => {
    const { get } = useApi()
    return get<Badge[]>('/incentive/badges')
  },
  getMyBadges: () => {
    const { get } = useApi()
    return get<Badge[]>('/incentive/my-badges')
  },
}