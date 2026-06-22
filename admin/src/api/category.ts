import { get, post, put, del } from './request'
import type { Category, PageResult, PageParams } from '@/types'

/** 获取分类列表（树形） */
export function getCategoryTree() {
  return get<Category[]>('/categories/tree')
}

/** 获取分类列表（平铺） */
export function getCategoryList(params?: PageParams & { keyword?: string }) {
  return get<PageResult<Category>>('/categories', params as unknown as Record<string, unknown>)
}

/** 获取分类详情 */
export function getCategoryDetail(id: number) {
  return get<Category>(`/categories/${id}`)
}

/** 创建分类 */
export function createCategory(data: Partial<Category>) {
  return post<Category>('/categories', data as unknown as Record<string, unknown>)
}

/** 更新分类 */
export function updateCategory(id: number, data: Partial<Category>) {
  return put<Category>(`/categories/${id}`, data as unknown as Record<string, unknown>)
}

/** 删除分类 */
export function deleteCategory(id: number) {
  return del(`/categories/${id}`)
}
