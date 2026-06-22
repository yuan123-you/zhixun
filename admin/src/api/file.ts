import { post } from './request'
import type { UploadResult } from '@/types'
import service from './request'

/** 上传图片 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return service.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as Promise<{ code: number; message: string; data: UploadResult }>
}

/** 上传富文本编辑器图片 */
export function uploadEditorImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<UploadResult>('/files/upload/editor', formData as unknown as Record<string, unknown>)
}
