/**
 * 文件上传 API
 * 封装图片/附件/语音的上传接口
 */
export const fileApi = {
  /**
   * 上传图片
   * @param formData 包含 file 字段的 FormData
   * @param onProgress 上传进度回调（0-100）
   * @returns 上传成功后的图片访问URL（服务器相对路径）
   */
  uploadImage: (formData: FormData, onProgress?: (percent: number) => void) => {
    const { upload } = useApi()
    return upload<string>('/files/upload/image', formData, onProgress)
  },

  /**
   * 上传附件
   */
  uploadFile: (formData: FormData, onProgress?: (percent: number) => void) => {
    const { upload } = useApi()
    return upload<string>('/files/upload/file', formData, onProgress)
  },

  /**
   * 上传语音
   */
  uploadVoice: (formData: FormData, onProgress?: (percent: number) => void) => {
    const { upload } = useApi()
    return upload<string>('/files/upload/voice', formData, onProgress)
  },

  /**
   * 便捷方法：上传单个图片文件
   * @param file 图片文件
   * @param onProgress 上传进度回调
   * @returns 上传成功后的图片访问URL
   */
  uploadSingleImage: async (file: File, onProgress?: (percent: number) => void): Promise<string> => {
    const fd = new FormData()
    fd.append('file', file)
    const res = await fileApi.uploadImage(fd, onProgress)
    return res.data.data || ''
  },

  /**
   * 便捷方法：上传单个语音文件
   * @param blob 语音 Blob
   * @param onProgress 上传进度回调
   * @returns 上传成功后的语音访问URL
   */
  uploadSingleVoice: async (blob: Blob, onProgress?: (percent: number) => void): Promise<string> => {
    const fd = new FormData()
    fd.append('file', blob, 'voice.webm')
    const res = await fileApi.uploadVoice(fd, onProgress)
    return res.data.data || ''
  },
}
