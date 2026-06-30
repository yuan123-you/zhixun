/**
 * 聊天时间格式化 - 私信/群聊共用
 * 统一 today/yesterday/same-year/full-date 逻辑，消除三处重复
 */

const pad = (n: number) => n.toString().padStart(2, '0')

/**
 * 格式化消息时间（用于气泡下方 / 时间分隔线）
 * - 今天 → HH:MM
 * - 昨天 → 昨天 HH:MM
 * - 同年 → MM-DD HH:MM
 * - 其他 → YYYY-MM-DD HH:MM
 */
export function formatChatTime(time: string): string {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now.getTime() - 86400000)
  const isYesterday = date.toDateString() === yesterday.toDateString()
  const timeStr = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (isToday) return timeStr
  if (isYesterday) return `昨天 ${timeStr}`
  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${timeStr}`
}

/**
 * 仅返回 HH:MM（群聊气泡下方始终显示的简短时间）
 */
export function formatMsgTime(dateStr: string): string {
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 两条消息之间的时间差（分钟）
 */
export function getTimeDiff(prevTime: string, currTime: string): number {
  if (!prevTime || !currTime) return 0
  const prevMs = new Date(prevTime).getTime()
  const currMs = new Date(currTime).getTime()
  if (isNaN(prevMs) || isNaN(currMs)) return 0
  return Math.abs(currMs - prevMs) / 60000
}

/**
 * 判断是否应显示时间分隔线（与前一条消息间隔超过 threshold 分钟）
 */
export function shouldShowTimeSeparator(
  messages: { createdAt: string }[],
  idx: number,
  thresholdMinutes = 5
): boolean {
  if (idx === 0) return true
  const prev = new Date(messages[idx - 1].createdAt).getTime()
  const curr = new Date(messages[idx].createdAt).getTime()
  return (curr - prev) > thresholdMinutes * 60 * 1000
}
