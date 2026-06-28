import { defineStore } from 'pinia'
import type { User } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'

/** 用户状态管理 Store */
export const useUserStore = defineStore('user', () => {
  // 用户Token - 初始为空，客户端挂载后从 localStorage 恢复
  const token = ref('')
  // 刷新Token - 仅用于内存显示，实际存储在 httpOnly Cookie 中（前端无法读取）
  const refreshToken = ref('')
  // Token过期时间戳（毫秒）
  const tokenExpiresAt = ref(0)
  // 用户信息
  const userInfo = ref<User | null>(null)

  // 是否已登录（同时校验 token 是否过期）
  const isLoggedIn = computed(() => {
    if (!token.value || !userInfo.value) return false
    if (tokenExpiresAt.value > 0 && Date.now() >= tokenExpiresAt.value) return false
    return true
  })

  // 设置Token
  const setToken = (newToken: string, newRefreshToken: string, expiresIn?: number) => {
    token.value = newToken
    refreshToken.value = newRefreshToken // 仅用于内存显示
    // accessToken 存储在 localStorage（用于 API Authorization Header）
    storage.set(STORAGE_KEYS.ACCESS_TOKEN, newToken)
    // refreshToken 不再存储在 localStorage（实际存储在 httpOnly Cookie 中）
    // storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken) // 移除
    if (expiresIn !== undefined) {
      const expiresAt = Date.now() + expiresIn * 1000
      tokenExpiresAt.value = expiresAt
      storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
    }
  }

  // 设置用户信息
  const setUser = (user: User) => {
    userInfo.value = user
    storage.set(STORAGE_KEYS.USER_SUMMARY, user)
  }

  // 更新UID
  const updateUid = (uid: string) => {
    if (userInfo.value) {
      userInfo.value.uid = uid
      storage.set(STORAGE_KEYS.USER_SUMMARY, userInfo.value)
    }
  }

  // 更新用户资料
  const updateProfile = (data: Partial<User>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
      storage.set(STORAGE_KEYS.USER_SUMMARY, userInfo.value)
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    tokenExpiresAt.value = 0
    userInfo.value = null
    // 立即清理 localStorage，确保下次路由守卫检查时 isLoggedIn 为 false
    storage.remove(STORAGE_KEYS.ACCESS_TOKEN)
    // refreshToken 不再从 localStorage 清除（实际存储在 httpOnly Cookie 中）
    // storage.remove(STORAGE_KEYS.REFRESH_TOKEN) // 移除
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    storage.remove(STORAGE_KEYS.USER_SUMMARY)
  }

  // 初始化：从localStorage恢复token（refreshToken 从 httpOnly Cookie 自动恢复）
  const init = () => {
    const savedAccessToken = storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN)
    if (savedAccessToken) {
      token.value = savedAccessToken
    }
    // refreshToken 不再从 localStorage 恢复（实际存储在 httpOnly Cookie 中）
    // const savedRefreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN)
    // if (savedRefreshToken) {
    //   refreshToken.value = savedRefreshToken
    // }
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
    updateUid,
    logout,
    init,
  }
})
