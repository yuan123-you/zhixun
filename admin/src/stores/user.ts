import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginUserInfo, LoginParams } from '@/types'
import { loginApi, getUserInfoApi } from '@/api/auth'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import router from '@/router'

/**
 * 用户状态管理
 * 管理用户认证信息、用户资料和权限
 */
export const useUserStore = defineStore('user', () => {
  /** 认证令牌 */
  const token = ref<string>(storage.get<string>(STORAGE_KEYS.TOKEN) || '')

  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(null)

  /** 用户权限列表 */
  const permissions = ref<string[]>(storage.get<string[]>(STORAGE_KEYS.USER_PERMISSIONS) || [])

  /** 是否已登录 */
  const isLoggedIn = ref(!!token.value)

  /**
   * 用户登录
   * @param params 登录参数（用户名+密码）
   */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.data.accessToken
    const loginUserInfo: LoginUserInfo = res.data.userInfo
    permissions.value = loginUserInfo.permissions
    isLoggedIn.value = true

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
    userInfo.value = null
    permissions.value = []
    isLoggedIn.value = false

    // 清除持久化存储
    storage.remove(STORAGE_KEYS.TOKEN)
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
    userInfo,
    permissions,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
  }
})
