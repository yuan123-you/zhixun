import { defineStore } from 'pinia'
import type { User } from '~/types'

/** 用户状态管理 Store */
export const useUserStore = defineStore('user', () => {
  // 用户Token
  const token = ref<string>('')
  // 刷新Token
  const refreshToken = ref<string>('')
  // 用户信息
  const userInfo = ref<User | null>(null)

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // 设置Token
  const setToken = (newToken: string, newRefreshToken: string) => {
    token.value = newToken
    refreshToken.value = newRefreshToken
    // 持久化refreshToken到localStorage
    if (import.meta.client) {
      localStorage.setItem('refreshToken', newRefreshToken)
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
    userInfo.value = null
    if (import.meta.client) {
      localStorage.removeItem('refreshToken')
    }
  }

  // 初始化：从localStorage恢复refreshToken
  const init = () => {
    if (import.meta.client) {
      const savedRefreshToken = localStorage.getItem('refreshToken')
      if (savedRefreshToken) {
        refreshToken.value = savedRefreshToken
      }
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLoggedIn,
    setToken,
    setUser,
    updateProfile,
    logout,
    init,
  }
})
