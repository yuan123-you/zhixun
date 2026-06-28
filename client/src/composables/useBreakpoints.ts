import { useBreakpoints as useVueUseBreakpoints, useWindowSize } from '@vueuse/core'

/** 响应式断点组合式函数（纯 SPA） */
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

  // 客户端挂载标记：避免 hydration mismatch
  const isMounted = ref(false)
  onMounted(() => { isMounted.value = true })

  // 是否为移动端（小于768px）
  const isMobile = computed(() => {
    if (isMounted.value) return !breakpoints.md.value
    return false
  })

  // 是否为平板端（768px - 1023px）
  const isTablet = computed(() => {
    if (!isMounted.value) return false
    const current = breakpoints.current().value
    if (!current) return false
    const arr = Array.isArray(current) ? current : [current]
    return arr.includes('md') && !arr.includes('lg')
  })

  // 是否为桌面端（大于等于1024px）
  const isDesktop = computed(() => {
    if (isMounted.value) return breakpoints.lg.value
    return true
  })

  // 当前断点名称
  const currentBreakpoint = computed(() => {
    if (!isMounted.value) return 'desktop'
    if (isMobile.value) return 'mobile'
    if (isTablet.value) return 'tablet'
    return 'desktop'
  })

  // 是否为横屏（window.innerWidth > window.innerHeight）
  const isLandscape = computed(() => isMounted.value && width.value > height.value)

  // 屏幕方向
  const orientation = computed<'portrait' | 'landscape'>(() => {
    if (!isMounted.value) return 'portrait'
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
