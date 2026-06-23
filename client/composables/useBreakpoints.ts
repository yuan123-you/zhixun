import { useBreakpoints as useVueUseBreakpoints, useWindowSize } from '@vueuse/core'

/** 响应式断点组合式函数 */
export const useBreakpoints = () => {
  // 使用 @vueuse/core 的 useBreakpoints，与 Tailwind 配置的断点一致
  const breakpoints = useVueUseBreakpoints({
    sm: 480,
    md: 768,
    lg: 1024,
    xl: 1200,
    '2xl': 1920,
  })

  // 使用 useWindowSize 获取响应式的窗口尺寸
  const { width, height } = useWindowSize()

  // 是否为移动端（小于768px）
  const isMobile = computed(() => !breakpoints.md.value)

  // 是否为平板端（768px - 1023px）
  const isTablet = computed(() => {
    const current = breakpoints.current()
    return current.includes('md') && !current.includes('lg')
  })

  // 是否为桌面端（大于等于1024px）
  const isDesktop = computed(() => breakpoints.lg.value)

  // 当前断点名称
  const currentBreakpoint = computed(() => {
    if (isMobile.value) return 'mobile'
    if (isTablet.value) return 'tablet'
    return 'desktop'
  })

  // 是否为横屏（window.innerWidth > window.innerHeight）
  const isLandscape = computed(() => width.value > height.value)

  // 屏幕方向
  const orientation = computed<'portrait' | 'landscape'>(() => {
    return isLandscape.value ? 'landscape' : 'portrait'
  })

  return {
    breakpoints,
    isMobile,
    isTablet,
    isDesktop,
    currentBreakpoint,
    isLandscape,
    orientation,
  }
}
