/**
 * 屏幕方向检测组合式函数
 * 提供响应式方向状态，支持方向锁定提示
 */

export interface OrientationState {
  /** 当前方向：portrait / landscape */
  orientation: Ref<'portrait' | 'landscape'>
  /** 是否为横屏 */
  isLandscape: Ref<boolean>
  /** 是否为竖屏 */
  isPortrait: Ref<boolean>
  /** 是否支持屏幕方向 API */
  isOrientationApiSupported: Ref<boolean>
  /** 是否正在显示方向锁定提示 */
  showOrientationPrompt: Ref<boolean>
  /** 显示方向锁定提示 */
  promptOrientationLock: (desiredOrientation: 'portrait' | 'landscape') => void
  /** 关闭方向锁定提示 */
  dismissOrientationPrompt: () => void
}

export const useOrientation = (): OrientationState => {
  const { isLandscape: bpIsLandscape, orientation: bpOrientation } = useBreakpoints()

  const orientation = ref<'portrait' | 'landscape'>(bpOrientation.value)
  const isLandscape = computed(() => orientation.value === 'landscape')
  const isPortrait = computed(() => orientation.value === 'portrait')
  const isOrientationApiSupported = ref(false)
  const showOrientationPrompt = ref(false)

  let desiredOrientation: 'portrait' | 'landscape' | null = null

  // 检测是否支持屏幕方向 API
  if (import.meta.client) {
    isOrientationApiSupported.value =
      typeof screen !== 'undefined' &&
      'orientation' in screen &&
      typeof screen.orientation?.lock === 'function'
  }

  // 监听方向变化
  const updateOrientation = () => {
    if (import.meta.client) {
      orientation.value = window.innerWidth > window.innerHeight ? 'landscape' : 'portrait'
    }
  }

  onMounted(() => {
    updateOrientation()

    // 使用 screen.orientation API（更精确）
    if (isOrientationApiSupported.value) {
      const handleOrientationChange = () => {
        const type = screen.orientation.type
        orientation.value = type.includes('landscape') ? 'landscape' : 'portrait'
      }
      screen.orientation.addEventListener('change', handleOrientationChange)

      onUnmounted(() => {
        screen.orientation.removeEventListener('change', handleOrientationChange)
      })
    } else {
      // 降级：使用 resize 事件
      const handleResize = () => {
        updateOrientation()
      }
      window.addEventListener('resize', handleResize)
      // 同时监听 orientationchange 事件（部分旧设备）
      window.addEventListener('orientationchange', handleResize)

      onUnmounted(() => {
        window.removeEventListener('resize', handleResize)
        window.removeEventListener('orientationchange', handleResize)
      })
    }
  })

  // 显示方向锁定提示
  const promptOrientationLock = (desired: 'portrait' | 'landscape') => {
    desiredOrientation = desired

    // 如果当前方向已经是期望方向，不需要提示
    if (orientation.value === desired) return

    showOrientationPrompt.value = true
  }

  // 关闭方向锁定提示
  const dismissOrientationPrompt = () => {
    showOrientationPrompt.value = false
    desiredOrientation = null
  }

  return {
    orientation,
    isLandscape,
    isPortrait,
    isOrientationApiSupported,
    showOrientationPrompt,
    promptOrientationLock,
    dismissOrientationPrompt,
  }
}
