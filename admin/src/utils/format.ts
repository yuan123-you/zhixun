/**
 * 格式化工具函数
 */

/**
 * 格式化数字（添加千分位）
 * @param value 数值
 * @returns 格式化后的字符串
 */
export function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null) return '0'
  return value.toLocaleString('zh-CN')
}

/**
 * 格式化日期
 * @param date 日期字符串或Date对象
 * @param format 格式化模板，默认 YYYY-MM-DD HH:mm:ss
 * @returns 格式化后的日期字符串
 */
export function formatDate(
  date: string | Date | undefined | null,
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return '-'

  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '-'

  const pad = (n: number) => n.toString().padStart(2, '0')

  const year = d.getFullYear().toString()
  const month = pad(d.getMonth() + 1)
  const day = pad(d.getDate())
  const hours = pad(d.getHours())
  const minutes = pad(d.getMinutes())
  const seconds = pad(d.getSeconds())

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的文件大小字符串
 */
export function formatFileSize(bytes: number | undefined | null): string {
  if (bytes === undefined || bytes === null || bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${units[i]}`
}

/**
 * 格式化相对时间（如"3分钟前"）
 * @param date 日期字符串或Date对象
 * @returns 相对时间字符串
 */
export function formatRelativeTime(date: string | Date | undefined | null): string {
  if (!date) return '-'

  const d = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diff = now.getTime() - d.getTime()

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return formatDate(d, 'YYYY-MM-DD')
}
