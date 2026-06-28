/**
 * 颜色模式管理 composable
 * 支持 light / dark / system 三种模式
 *
 * 使用方式：
 *   const colorMode = useColorMode()
 *   colorMode.preference = 'dark'  // 直接属性赋值
 */
import { watchEffect } from 'vue'

type ColorModePreference = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'color-mode-preference'

// 全局单例状态
let _preference: ColorModePreference =
  (typeof localStorage !== 'undefined' && (localStorage.getItem(STORAGE_KEY) as ColorModePreference | null)) || 'system'

let mediaQuery: MediaQueryList | null = null

function applyMode(pref: ColorModePreference) {
  if (typeof document === 'undefined') return
  const isDark =
    pref === 'dark' ||
    (pref === 'system' && mediaQuery !== null && mediaQuery.matches)
  document.documentElement.classList.toggle('dark', isDark)
}

// 调用方列表，用于通知主题变更
const _watchers = new Set<() => void>()

function notify() {
  _watchers.forEach(fn => fn())
}

// 首屏初始化：在模块加载阶段尽早执行，避免界面闪烁
if (typeof window !== 'undefined') {
  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

  mediaQuery.addEventListener('change', () => {
    if (_preference === 'system') {
      applyMode('system')
    }
  })

  applyMode(_preference)
}

export function useColorMode() {
  // 返回带有 getter/setter 的普通对象
  const instance = {
    get preference(): ColorModePreference {
      return _preference
    },
    set preference(val: ColorModePreference) {
      _preference = val
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(STORAGE_KEY, val)
      }
      applyMode(val)
    },
  }

  // 监听外部对 _preference 的修改
  _watchers.add(() => applyMode(_preference))

  return instance
}
