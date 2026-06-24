import { defineStore } from 'pinia'
import type { User } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'

/** 用户状态管理 Store */
export const useUserStore = defineStore('user', () => {
  // 用户Token
  const token = ref<string>(storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN) || '')
  // 刷新Token
  const refreshToken = ref<string>(storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN) || '')
  // Token过期时间戳（毫秒）
  const tokenExpiresAt = ref<number>(storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT) || 0)
  // 用户信息（从 localStorage 恢复，确保刷新后仍可用）
  const userInfo = ref<User | null>(storage.get<User>(STORAGE_KEYS.USER_SUMMARY) || null)

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // 设置Token
  const setToken = (newToken: string, newRefreshToken: string, expiresIn?: number) => {
    token.value = newToken
    refreshToken.value = newRefreshToken
    // 持久化accessToken和refreshToken到localStorage
    storage.set(STORAGE_KEYS.ACCESS_TOKEN, newToken)
    storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken)
    // 计算并持久化Token过期时间
    if (expiresIn !== undefined) {
      const expiresAt = Date.now() + expiresIn * 1000
      tokenExpiresAt.value = expiresAt
      storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
    }
  }

  // 设置用户信息
  const setUser = (user: User) => {
    userInfo.value = user
    // 持久化用户信息到localStorage，确保页面刷新后登录状态不丢失
    storage.set(STORAGE_KEYS.USER_SUMMARY, user)
  }

  // 更新用户资料
  const updateProfile = (data: Partial<User>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
      // 同步更新localStorage中的用户信息
      storage.set(STORAGE_KEYS.USER_SUMMARY, userInfo.value)
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    tokenExpiresAt.value = 0
    userInfo.value = null
    storage.remove(STORAGE_KEYS.ACCESS_TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    storage.remove(STORAGE_KEYS.USER_SUMMARY)
  }

  // 初始化：从localStorage恢复token
  const init = () => {
    const savedAccessToken = storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN)
    if (savedAccessToken) {
      token.value = savedAccessToken
    }
    const savedRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken
    }
    const savedExpiresAt = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    if (savedExpiresAt) {
      tokenExpiresAt.value = savedExpiresAt
    }
    const savedUserInfo = storage.get<User>(STORAGE_KEYS.USER_SUMMARY)
    if (savedUserInfo) {
      userInfo.value = savedUserInfo
    }
  }

  return {
    token,
    refreshToken,
    tokenExpiresAt,
    userInfo,
    isLoggedIn,
    setToken,
    setUser,
    updateProfile,
    logout,
    init,
  }
})
