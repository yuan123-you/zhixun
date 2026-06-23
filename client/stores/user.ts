import { defineStore } from 'pinia'
import type { User } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'

/** 用户状态管理 Store */
export const useUserStore = defineStore('user', () => {
  // 用户Token
  const token = ref<string>('')
  // 刷新Token
  const refreshToken = ref<string>(storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN) || '')
  // Token过期时间戳（毫秒）
  const tokenExpiresAt = ref<number>(storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT) || 0)
  // 用户信息
  const userInfo = ref<User | null>(null)

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // 设置Token
  const setToken = (newToken: string, newRefreshToken: string, expiresIn?: number) => {
    token.value = newToken
    refreshToken.value = newRefreshToken
    // 持久化refreshToken到localStorage
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
  }

  // 更新用户资料
  const updateProfile = (data: Partial<User>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    tokenExpiresAt.value = 0
    userInfo.value = null
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
  }

  // 初始化：从localStorage恢复refreshToken和token过期时间
  const init = () => {
    const savedRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken
    }
    const savedExpiresAt = storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    if (savedExpiresAt) {
      tokenExpiresAt.value = savedExpiresAt
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
