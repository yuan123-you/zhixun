import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用全局状态管理
 * 管理侧边栏折叠状态、主题等全局UI状态
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref(false)

  /** 当前主题（light/dark） */
  const theme = ref<'light' | 'dark'>('light')

  /** 设备类型 */
  const device = ref<'desktop' | 'mobile'>('desktop')

  /**
   * 切换侧边栏折叠状态
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /**
   * 设置侧边栏折叠状态
   */
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  /**
   * 切换主题
   */
  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
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
