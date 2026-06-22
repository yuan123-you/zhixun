import { post, get } from './request'
import type { LoginParams, LoginResult, UserInfo } from '@/types'

/** 用户登录 */
export function loginApi(data: LoginParams) {
  return post<LoginResult>('/auth/login', data as unknown as Record<string, unknown>)
}

/** 获取当前用户信息 */
export function getUserInfoApi() {
  return get<UserInfo>('/auth/user-info')
}

/** 用户登出 */
export function logoutApi() {
  return post('/auth/logout')
}

/** 修改密码 */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }) {
  return post('/auth/change-password', data as unknown as Record<string, unknown>)
}
