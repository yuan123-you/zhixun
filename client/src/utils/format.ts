/**
 * 日期时间格式化工具
 */
export const formatTimestamp = (date: string): string => {
  if (!date) return ''
  const d = new Date(date)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const formatDate = (date?: string): string => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

export const formatDateLong = (date: string): string => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

/**
 * 文本截断
 */
export const truncateText = (text: string, maxLength: number = 100): string => {
  if (!text || text.length <= maxLength) return text || ''
  return text.slice(0, maxLength) + '...'
}
