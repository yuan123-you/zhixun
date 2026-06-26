import { storage, STORAGE_KEYS, TTL } from '~/utils/storage'
import type { UserSettingsServer } from '~/types'

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
 * 用户设置本地 + 服务端双重持久化管理
 * - 所有设置存储在 localStorage，同时同步到服务端 user_settings 表
 * - 编辑器/阅读设置仅本地存储，不与服务端同步
 * - 主题/语言/通知设置双写：localStorage + 服务端
 * - 支持重置为默认设置
 */
export const useUserSettings = () => {
  /** 频繁写入的keys（编辑器、阅读），仅写本地避免频繁HTTP请求 */
  const LOCAL_ONLY_KEYS = new Set(['editor', 'reading'])

  /** 获取设置 */
  const getSettings = (): UserSettings => {
    const saved = storage.get<UserSettings>(STORAGE_KEYS.USER_PREFERENCES)
    return saved ? { ...DEFAULT_SETTINGS, ...saved } : { ...DEFAULT_SETTINGS }
  }

  /** 保存设置 */
  const saveSettings = (settings: Partial<UserSettings>) => {
    const current = getSettings()
    const updated = deepMerge(current, settings)
    storage.set(STORAGE_KEYS.USER_PREFERENCES, updated, TTL.PERMANENT)

    // 同步到服务端：仅同步非本地专属的设置项
    const hasNonLocalKeys = Object.keys(settings).some(k => !LOCAL_ONLY_KEYS.has(k))
    if (hasNonLocalKeys) {
      syncToServer(updated)
    }

    return updated
  }

  /** 将本地设置同步到服务端 */
  const syncToServer = async (settings: UserSettings) => {
    try {
      const { userApi } = await import('~/api')
      // 映射本地设置到服务端 SettingsUpdateRequest 结构
      const serverPayload: any = {}

      // display 设置：主题、语言
      const display: any = {}
      if (settings.theme !== undefined) display.theme = settings.theme
      if (settings.locale !== undefined) display.language = settings.locale
      if (Object.keys(display).length > 0) serverPayload.display = display

      // notification 设置：通知偏好
      if (settings.notification) {
        serverPayload.notification = {
          notifySystem: settings.notification.enabled ? 1 : 0,
          notifyInteract: settings.notification.sound ? 1 : 0,
          notifyMessage: settings.notification.sound ? 1 : 0,
          notifyFollow: settings.notification.desktop ? 1 : 0,
        }
      }

      if (Object.keys(serverPayload).length > 0) {
        await userApi.updateSettings(serverPayload as any)
      }
    } catch {
      // 静默失败，本地存储优先
    }
  }

  /** 从服务端拉取设置并合并到本地 */
  const syncFromServer = async () => {
    try {
      const { userApi } = await import('~/api')
      const { data } = await userApi.getSettings()
      const serverSettings: UserSettingsServer = data?.data
      if (!serverSettings) return

      const current = getSettings()

      // 合并服务端 display 设置（主题、语言）
      if (serverSettings.display) {
        if (serverSettings.display.theme) {
          current.theme = serverSettings.display.theme as UserSettings['theme']
        }
        if (serverSettings.display.language) {
          current.locale = serverSettings.display.language as UserSettings['locale']
        }
      }

      // 合并服务端 notification 设置
      if (serverSettings.notification) {
        current.notification = {
          enabled: serverSettings.notification.notifySystem === 1,
          sound: serverSettings.notification.notifyInteract === 1,
          desktop: serverSettings.notification.notifyFollow === 1,
        }
      }

      storage.set(STORAGE_KEYS.USER_PREFERENCES, current, TTL.PERMANENT)
    } catch {
      // 静默失败，使用本地设置
    }
  }

  /** 更新主题 */
  const setTheme = (theme: UserSettings['theme']) => {
    return saveSettings({ theme })
  }

  /** 更新语言 */
  const setLocale = (locale: UserSettings['locale']) => {
    return saveSettings({ locale })
  }

  /** 更新编辑器设置（仅本地，不同步服务端） */
  const setEditorSettings = (editor: Partial<UserSettings['editor']>) => {
    const current = getSettings()
    const updated = deepMerge(current, { editor: { ...current.editor, ...editor } })
    storage.set(STORAGE_KEYS.USER_PREFERENCES, updated, TTL.PERMANENT)
    return updated
  }

  /** 更新通知设置 */
  const setNotificationSettings = (notification: Partial<UserSettings['notification']>) => {
    const current = getSettings()
    return saveSettings({
      notification: { ...current.notification, ...notification },
    })
  }

  /** 更新阅读设置（仅本地，不同步服务端） */
  const setReadingSettings = (reading: Partial<UserSettings['reading']>) => {
    const current = getSettings()
    const updated = deepMerge(current, { reading: { ...current.reading, ...reading } })
    storage.set(STORAGE_KEYS.USER_PREFERENCES, updated, TTL.PERMANENT)
    return updated
  }

  /** 重置为默认设置 */
  const resetSettings = () => {
    storage.set(STORAGE_KEYS.USER_PREFERENCES, DEFAULT_SETTINGS, TTL.PERMANENT)
    // 同步默认设置到服务端
    syncToServer(DEFAULT_SETTINGS)
    return DEFAULT_SETTINGS
  }

  /** 深度合并对象 */
  const deepMerge = (target: any, source: any): any => {
    const result = { ...target }
    for (const key of Object.keys(source)) {
      const sourceValue = source[key]
      const targetValue = target[key]
      if (
        sourceValue !== null &&
        sourceValue !== undefined &&
        typeof sourceValue === 'object' &&
        !Array.isArray(sourceValue) &&
        targetValue !== null &&
        targetValue !== undefined &&
        typeof targetValue === 'object' &&
        !Array.isArray(targetValue)
      ) {
        result[key] = deepMerge(targetValue, sourceValue)
      } else {
        result[key] = sourceValue
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
    syncFromServer,
  }
}
