/**
 * 颜色工具函数
 */

const AVATAR_COLORS = [
  'bg-primary',
  'bg-[#8B5CF6]',
  'bg-[#EC4899]',
  'bg-[#F97316]',
  'bg-[#14B8A6]',
  'bg-[#06B6D4]',
  'bg-[#84CC16]',
  'bg-[#E11D48]',
] as const

/**
 * 根据名称生成固定的头像背景色
 */
export const avatarColor = (name: string): string => {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length]
}
