export interface ProfileTheme {
  themeColor: string
  backgroundImage: string | null
  backgroundStyle: string
  fontFamily: string | null
  bioBgColor: string | null
  cardStyle: string
}

export const profileThemeApi = {
  getTheme: (userId: number) => {
    const { get } = useApi()
    return get<ProfileTheme>(`/profile/theme/${userId}`)
  },
  saveTheme: (data: Partial<ProfileTheme>) => {
    const { put } = useApi()
    return put<void>('/profile/theme', data)
  },
}