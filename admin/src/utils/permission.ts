/**
 * 权限工具函数
 */

import { useUserStore } from '@/stores/user'

/**
 * 判断当前用户是否拥有指定权限
 * @param permission 权限标识
 * @returns 是否拥有权限
 */
export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  // 管理员拥有所有权限
  if (userStore.userInfo?.role === 'admin') return true
  return userStore.permissions.includes(permission)
}

/**
 * 判断当前用户是否拥有任一权限
 * @param permissions 权限标识列表
 * @returns 是否拥有任一权限
 */
export function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some((perm) => hasPermission(perm))
}

/**
 * 判断当前用户是否拥有所有权限
 * @param permissions 权限标识列表
 * @returns 是否拥有所有权限
 */
export function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every((perm) => hasPermission(perm))
}

/**
 * 检查用户角色
 * @param role 角色标识
 * @returns 是否拥有该角色
 */
export function checkRole(role: string): boolean {
  const userStore = useUserStore()
  return userStore.userInfo?.role === role
}

/**
 * 检查用户是否为管理员
 * @returns 是否为管理员
 */
export function isAdmin(): boolean {
  return checkRole('admin')
}
