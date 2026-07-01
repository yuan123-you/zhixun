import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginUserInfo, LoginParams } from '@/types'
import { UserStatus } from '@/types'
import { loginApi, getUserInfoApi } from '@/api/auth'
import { sessionStore, STORAGE_KEYS } from '@/utils/storage'
import router from '@/router'

/** Token 即将过期的提前量（5分钟，单位毫秒） */
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000

/**
 * 用户状态管理
 * 管理用户认证信息、用户资料和权限
 */
export const useUserStore = defineStore('user', () => {
  /** 认证令牌 - 使用 sessionStorage 支持多账号同时登录 */
  const token = ref<string>(sessionStore.get<string>(STORAGE_KEYS.TOKEN) || '')

  /** 刷新令牌，用于 accessToken 过期时自动续期 */
  const refreshToken = ref<string>(sessionStore.get<string>(STORAGE_KEYS.REFRESH_TOKEN) || '')

  /** Token 过期时间戳（毫秒），用于判断是否需要提前刷新 */
  const tokenExpiresAt = ref<number>(sessionStore.get<number>(STORAGE_KEYS.TOKEN_EXPIRES_AT) || 0)

  /** 用户信息（初始从 sessionStorage 恢复，刷新后无需重新请求） */
  const userInfo = ref<UserInfo | null>(sessionStore.get<UserInfo>(STORAGE_KEYS.USER_INFO) || null)

  /** 用户权限列表 */
  const permissions = ref<string[]>(sessionStore.get<string[]>(STORAGE_KEYS.USER_PERMISSIONS) || [])

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
    sessionStore.set(STORAGE_KEYS.TOKEN, newToken)
    sessionStore.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken)
    if (expiresIn !== undefined) {
      const expiresAt = Date.now() + expiresIn * 1000
      tokenExpiresAt.value = expiresAt
      sessionStore.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
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
    try {
      const res = await loginApi(params)
      token.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      isLoggedIn.value = true

      // 计算并持久化 Token 过期时间
      if (res.data.expiresIn) {
        const expiresAt = Date.now() + res.data.expiresIn * 1000
        tokenExpiresAt.value = expiresAt
        sessionStore.set(STORAGE_KEYS.TOKEN_EXPIRES_AT, expiresAt)
      }

      const loginUserInfo: LoginUserInfo = res.data.userInfo
      permissions.value = loginUserInfo.permissions

      // 将登录用户信息转为 UserInfo 存储
      const userStatus = UserStatus.Active
      userInfo.value = {
        id: loginUserInfo.id,
        uid: '',
        username: loginUserInfo.username,
        nickname: loginUserInfo.nickname,
        avatar: loginUserInfo.avatar ?? '',
        email: '',
        phone: '',
        role: loginUserInfo.role,
        permissions: loginUserInfo.permissions,
        status: userStatus,
        createdAt: '',
        updatedAt: '',
      }

      // 持久化存储到 sessionStorage（每个标签页独立，支持多账号）
      sessionStore.set(STORAGE_KEYS.TOKEN, res.data.accessToken)
      sessionStore.set(STORAGE_KEYS.REFRESH_TOKEN, res.data.refreshToken)
      sessionStore.set(STORAGE_KEYS.USER_PERMISSIONS, loginUserInfo.permissions)
      sessionStore.set(STORAGE_KEYS.USER_INFO, userInfo.value)

      // 跳转到首页或来源页（安全校验：仅允许站内路径，防止开放重定向漏洞）
      const redirect = validateRedirect((router.currentRoute.value.query.redirect as string) || '/')
      router.push(redirect)
    } catch (error: any) {
      // 登录失败，清除可能的残留状态
      isLoggedIn.value = false
      token.value = ''
      refreshToken.value = ''
      throw error
    }
  }

  /**
   * 验证重定向 URL 是否为站内路径，防止开放重定向攻击
   */
  function validateRedirect(redirect: string): string {
    // 只允许以 / 开头的相对路径，拒绝完整 URL 或协议前缀
    if (!redirect || typeof redirect !== 'string') return '/'
    // 必须以 / 开头且不包含 :// 或 . 开头（防止 //evil.com 或 https://evil.com）
    if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.includes('://')) {
      return '/'
    }
    return redirect
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

    // 清除 sessionStorage
    sessionStore.remove(STORAGE_KEYS.TOKEN)
    sessionStore.remove(STORAGE_KEYS.REFRESH_TOKEN)
    sessionStore.remove(STORAGE_KEYS.TOKEN_EXPIRES_AT)
    sessionStore.remove(STORAGE_KEYS.USER_PERMISSIONS)
    sessionStore.remove(STORAGE_KEYS.USER_INFO)

    router.push('/login')
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    try {
      const res = await getUserInfoApi()
      userInfo.value = res.data
      permissions.value = res.data.permissions
      sessionStore.set(STORAGE_KEYS.USER_PERMISSIONS, res.data.permissions)
      sessionStore.set(STORAGE_KEYS.USER_INFO, userInfo.value)
    } catch {
      // 获取用户信息失败，不清除现有状态（可能是网络问题）
      // 如果 token 已过期，后续 API 调用将触发 401 处理
    }
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
