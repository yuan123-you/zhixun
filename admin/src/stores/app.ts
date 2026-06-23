import { defineStore } from 'pinia'
import { ref } from 'vue'
import { storage, STORAGE_KEYS } from '@/utils/storage'

/**
 * 应用全局状态管理
 * 管理侧边栏折叠状态、主题等全局UI状态
 * UI状态持久化到 localStorage
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref<boolean>(
    storage.get<boolean>(STORAGE_KEYS.SIDEBAR_COLLAPSED) || false
  )

  /** 当前主题（light/dark） */
  const theme = ref<'light' | 'dark'>(
    storage.get<'light' | 'dark'>(STORAGE_KEYS.THEME) || 'light'
  )

  /** 设备类型 */
  const device = ref<'desktop' | 'mobile'>('desktop')

  /**
   * 切换侧边栏折叠状态
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    storage.set(STORAGE_KEYS.SIDEBAR_COLLAPSED, sidebarCollapsed.value)
  }

  /**
   * 设置侧边栏折叠状态
   */
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
    storage.set(STORAGE_KEYS.SIDEBAR_COLLAPSED, collapsed)
  }

  /**
   * 切换主题
   */
  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    storage.set(STORAGE_KEYS.THEME, theme.value)
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }

  /**
   * 设置设备类型
   */
  function setDevice(dev: 'desktop' | 'mobile') {
    device.value = dev
    // 移动端自动折叠侧边栏
    if (dev === 'mobile') {
      sidebarCollapsed.value = true
      storage.set(STORAGE_KEYS.SIDEBAR_COLLAPSED, true)
    }
  }

  return {
    sidebarCollapsed,
    theme,
    device,
    toggleSidebar,
    setSidebarCollapsed,
    toggleTheme,
    setDevice,
  }
})
