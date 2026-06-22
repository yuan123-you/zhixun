import { get, post, del } from './request'
import type { SensitiveWord, SensitiveWordLevel, PageResult, PageParams } from '@/types'

/** 获取敏感词列表 */
export function getSensitiveWordList(params?: PageParams & { keyword?: string; level?: SensitiveWordLevel }) {
  return get<PageResult<SensitiveWord>>('/sensitive-words', params as unknown as Record<string, unknown>)
}

/** 添加敏感词 */
export function createSensitiveWord(data: { word: string; level: SensitiveWordLevel }) {
  return post<SensitiveWord>('/sensitive-words', data as unknown as Record<string, unknown>)
}

/** 批量添加敏感词 */
export function batchCreateSensitiveWords(data: { words: string[]; level: SensitiveWordLevel }) {
  return post('/sensitive-words/batch', data as unknown as Record<string, unknown>)
}

/** 删除敏感词 */
export function deleteSensitiveWord(id: number) {
  return del(`/sensitive-words/${id}`)
}

/** 更新敏感词级别 */
export function updateSensitiveWordLevel(id: number, level: SensitiveWordLevel) {
  return post(`/sensitive-words/${id}/level`, { level } as unknown as Record<string, unknown>)
}
