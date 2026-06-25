import { storage, STORAGE_KEYS, TTL } from '~/utils/storage'

/** 用户设置 */
export interface UserSettings {
  /** 主题 */
  theme: 'light' | 'dark' | 'system'
  /** 语言 */
  locale: 'zh-CN' | 'en-US'
  /** 编辑器设置 */
  editor: {
    /** 自动保存间隔（毫秒） */
    autoSaveInterval: number
    /** 默认分类 */
    defaultCategory: string
    /** 默认标签 */
    defaultTags: string[]
  }
  /** 通知设置 */
  notification: {
    /** 是否启用通知 */
    enabled: boolean
    /** 声音提醒 */
    sound: boolean
    /** 桌面通知 */
    desktop: boolean
  }
  /** 阅读设置 */
  reading: {
    /** 字体大小 */
    fontSize: number
    /** 行高 */
    lineHeight: number
    /** 是否显示目录 */
    showToc: boolean
  }
}

/** 默认设置 */
const DEFAULT_SETTINGS: UserSettings = {
  theme: 'system',
  locale: 'zh-CN',
  editor: {
    autoSaveInterval: 30000,
    defaultCategory: '',
    defaultTags: [],
  },
  notification: {
    enabled: true,
    sound: true,
    desktop: false,
  },
  reading: {
    fontSize: 16,
    lineHeight: 1.75,
    showToc: true,
  },
}

/**
 * 用户设置本地管理
 * - 所有设置存储在 localStorage，无需同步到数据库
 * - 支持重置为默认设置
 */
export const useUserSettings = () => {
  /** 获取设置 */
  const getSettings = (): UserSettings => {
    const saved = storage.get<UserSettings>(STORAGE_KEYS.SETTINGS_LOCAL)
    return saved ? { ...DEFAULT_SETTINGS, ...saved } : { ...DEFAULT_SETTINGS }
  }

  /** 保存设置 */
  const saveSettings = (settings: Partial<UserSettings>) => {
    const current = getSettings()
    const updated = deepMerge(current, settings)
    storage.set(STORAGE_KEYS.SETTINGS_LOCAL, updated, TTL.PERMANENT)
    return updated
  }

  /** 更新主题 */
  const setTheme = (theme: UserSettings['theme']) => {
    return saveSettings({ theme })
  }

  /** 更新语言 */
  const setLocale = (locale: UserSettings['locale']) => {
    return saveSettings({ locale })
  }

  /** 更新编辑器设置 */
  const setEditorSettings = (editor: Partial<UserSettings['editor']>) => {
    const current = getSettings()
    return saveSettings({
      editor: { ...current.editor, ...editor },
    })
  }

  /** 更新通知设置 */
  const setNotificationSettings = (notification: Partial<UserSettings['notification']>) => {
    const current = getSettings()
    return saveSettings({
      notification: { ...current.notification, ...notification },
    })
  }

  /** 更新阅读设置 */
  const setReadingSettings = (reading: Partial<UserSettings['reading']>) => {
    const current = getSettings()
    return saveSettings({
      reading: { ...current.reading, ...reading },
    })
  }

  /** 重置为默认设置 */
  const resetSettings = () => {
    storage.set(STORAGE_KEYS.USER_SETTINGS, DEFAULT_SETTINGS, TTL.PERMANENT)
    return DEFAULT_SETTINGS
  }

  /** 深度合并对象 */
  const deepMerge = (target: any, source: any): any => {
    const result = { ...target }
    for (const key of Object.keys(source)) {
      if (
        source[key] &&
        typeof source[key] === 'object' &&
        !Array.isArray(source[key]) &&
        target[key] &&
        typeof target[key] === 'object'
      ) {
        result[key] = deepMerge(target[key], source[key])
      } else {
        result[key] = source[key]
      }
    }
    return result
  }

  return {
    getSettings,
    saveSettings,
    setTheme,
    setLocale,
    setEditorSettings,
    setNotificationSettings,
    setReadingSettings,
    resetSettings,
  }
}
