import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginParams } from '@/types'
import { loginApi, getUserInfoApi } from '@/api/auth'
import router from '@/router'

/**
 * 用户状态管理
 * 管理用户认证信息、用户资料和权限
 */
export const useUserStore = defineStore('user', () => {
  /** 认证令牌 */
  const token = ref<string>(localStorage.getItem('token') || '')

  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(null)

  /** 用户权限列表 */
  const permissions = ref<string[]>([])

  /** 是否已登录 */
  const isLoggedIn = ref(!!token.value)

  /**
   * 用户登录
   * @param params 登录参数（用户名+密码）
   */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    permissions.value = res.data.userInfo.permissions
    isLoggedIn.value = true

    // 持久化存储
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user_permissions', JSON.stringify(res.data.userInfo.permissions))

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
    localStorage.removeItem('token')
    localStorage.removeItem('user_permissions')

    router.push('/login')
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    const res = await getUserInfoApi()
    userInfo.value = res.data
    permissions.value = res.data.permissions
    localStorage.setItem('user_permissions', JSON.stringify(res.data.permissions))
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
