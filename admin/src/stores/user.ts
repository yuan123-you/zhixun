import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginUserInfo, LoginParams } from '@/types'
import { loginApi, getUserInfoApi } from '@/api/auth'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import router from '@/router'

/** Token 即将过期的提前量（5分钟，单位毫秒） */
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/**
 * 用户状态管理
 * 管理用户认证信息、用户资料和权限
 */
export const useUserStore = defineStore('user', () => {
  /** 认证令牌 */
  const token = ref<string>(storage.get<string>(STORAGE_KEYS.TOKEN) || '')

  /** 刷新令牌，用于 accessToken 过期时自动续期 */
  const refreshToken = ref<string>(storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN) || '')

  /** Token 过期时间戳（毫秒），用于判断是否需要提前刷新 */
  const tokenExpiresAt = ref<number>(storage.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT) || 0)

  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(null)

  /** 用户权限列表 */
  const permissions = ref<string[]>(storage.get<string[]>(STORAGE_KEYS.USER_PERMISSIONS) || [])

  /** 是否已登录 */
  const isLoggedIn = ref(!!token.value)

  /**
   * 设置 Token 并持久化
   * @param newToken 新的 accessToken
   * @param newRefreshToken 新的 refreshToken
   * @param expiresIn accessToken 有效期（秒）
   */
  function setToken(newToken: string, newRefreshToken: string, expiresIn?: number) {
    token.value = newToken
    refreshToken.value = newRefreshToken
    storage.set(STORAGE_KEYS.TOKEN, newToken)
    storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken)
    if (expiresIn !== undefined) {
      const expiresAt = Date.now() + expiresIn * 1000
      tokenExpiresAt.value = expiresAt
      storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
    }
  }

  /**
   * 判断 Token 是否即将过期（过期前5分钟内）
   */
  function isTokenExpiringSoon(): boolean {
    if (!tokenExpiresAt.value) return false
    return Date.now() >= tokenExpiresAt.value - TOKEN_REFRESH_THRESHOLD
  }

  /**
   * 用户登录
   * @param params 登录参数（用户名+密码）
   */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    isLoggedIn.value = true

    // 计算并持久化 Token 过期时间
    if (res.data.expiresIn) {
      const expiresAt = Date.now() + res.data.expiresIn * 1000
      tokenExpiresAt.value = expiresAt
      storage.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
    }

    const loginUserInfo: LoginUserInfo = res.data.userInfo
    permissions.value = loginUserInfo.permissions

    // 将登录用户信息转为 UserInfo 存储
    userInfo.value = {
      id: loginUserInfo.id,
      username: loginUserInfo.username,
      nickname: loginUserInfo.nickname,
      avatar: loginUserInfo.avatar ?? '',
      email: '',
      phone: '',
      role: loginUserInfo.role,
      permissions: loginUserInfo.permissions,
      status: 'active' as any,
      createdAt: '',
      updatedAt: '',
    }

    // 持久化存储
    storage.set(STORAGE_KEYS.TOKEN, res.data.accessToken)
    storage.set(STORAGE_KEYS.REFRESH_TOKEN, res.data.refreshToken)
    storage.set(STORAGE_KEYS.USER_PERMISSIONS, loginUserInfo.permissions)

    // 跳转到首页或来源页
    const redirect = (router.currentRoute.value.query.redirect as string) || '/'
    router.push(redirect)
  }

  /**
   * 用户登出
   */
  function logout() {
    token.value = ''
    refreshToken.value = ''
    tokenExpiresAt.value = 0
    userInfo.value = null
    permissions.value = []
    isLoggedIn.value = false

    // 清除持久化存储
    storage.remove(STORAGE_KEYS.TOKEN)
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN)
    storage.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    storage.remove(STORAGE_KEYS.USER_PERMISSIONS)

    router.push('/login')
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    const res = await getUserInfoApi()
    userInfo.value = res.data
    permissions.value = res.data.permissions
    storage.set(STORAGE_KEYS.USER_PERMISSIONS, res.data.permissions)
  }

  return {
    token,
    refreshToken,
    tokenExpiresAt,
    userInfo,
    permissions,
    isLoggedIn,
    setToken,
    isTokenExpiringSoon,
    login,
    logout,
    fetchUserInfo,
  }
})
