import { storage, STORAGE_KEYS } from '~/utils/storage'
import type { UserSettingsLocal } from '~/types'

/**
 * 读取本地设置（仅从 localStorage 读取，不需要服务端同步）
 * 用于在设置页以外的组件中消费 autoLoadImages / defaultSort 等本地设置
 */
export const useLocalSettings = () => {
  /** 获取本地存储的设置（带默认值） */
  const getSettings = (): UserSettingsLocal => {
    const saved = storage.get<UserSettingsLocal>(STORAGE_KEYS.SETTINGS_LOCAL)
    return {
      theme: 'system',
      fontSize: 'medium',
      language: 'zh-CN',
      autoLoadImages: true,
      defaultSort: 'latest',
      ...(saved || {}),
    }
  }

  /** 获取单个设置项 */
  const getSetting = <K extends keyof UserSettingsLocal>(key: K): UserSettingsLocal[K] => {
    return getSettings()[key]
  }

  /** 是否自动加载图片 */
  const autoLoadImages = computed(() => getSetting('autoLoadImages'))

  /** 默认排序 */
  const defaultSort = computed(() => getSetting('defaultSort'))

  /** 当前字体大小 */
  const fontSize = computed(() => {
    const val = getSetting('fontSize')
    const pxMap: Record<string, string> = { small: '14px', medium: '16px', large: '18px' }
    return pxMap[val] || '16px'
  })

  return {
    getSettings,
    getSetting,
    autoLoadImages,
    defaultSort,
    fontSize,
  }
}
