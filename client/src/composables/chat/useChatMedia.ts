/**
 * 聊天媒体/文件解析工具 - 私信/群聊共用
 * 统一 voice URL、file URL、file name、file size 的解析逻辑
 */
import { useResourceUrl } from '@/composables/useResourceUrl'

export function useChatMedia() {
  const { resolveUrl } = useResourceUrl()
  const resolve = (url: string) => resolveUrl(url) || ''

  /** 解析语音/文件 URL（兼容 JSON 元数据和纯 URL 字符串） */
  const getMediaUrl = (content: string): string => {
    try {
      const data = JSON.parse(content)
      return typeof data?.url === 'string' ? resolve(data.url) : ''
    } catch {
      // 损坏的 JSON 元数据不是可请求的资源地址。
      if (/^[{\[]/.test(content.trim())) return ''
      return resolve(content)
    }
  }

  /** 解析语音消息时长 */
  const getVoiceDuration = (content: string): number => {
    try {
      const data = JSON.parse(content)
      return data.duration || 0
    } catch {
      return 0
    }
  }

  /** 从文件消息 JSON 中提取文件名 */
  const getFileName = (content: string): string => {
    try {
      return JSON.parse(content).name || '未知文件'
    } catch {
      return '文件'
    }
  }

  /** 从文件消息 JSON 中提取文件大小（格式化） */
  const getFileSize = (content: string): string => {
    try {
      const size = JSON.parse(content).size
      if (!size) return ''
      return size > 1048576
        ? (size / 1048576).toFixed(1) + ' MB'
        : (size / 1024).toFixed(1) + ' KB'
    } catch {
      return ''
    }
  }

  /** 解析消息中的资源 URL（图片等） */
  const resolveMsgUrl = (url: string): string => resolve(url)

  return {
    resolveMsgUrl,
    getVoiceUrl: getMediaUrl,
    getVoiceDuration,
    getFileUrl: getMediaUrl,
    getFileName,
    getFileSize,
  }
}
