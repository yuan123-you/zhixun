import { useBreakpoints as useVueUseBreakpoints, useWindowSize } from '@vueuse/core'

/** 通过 User-Agent 检测是否为移动设备（SSR 阶段使用） */
const detectMobileByUA = (): boolean => {
  if (!import.meta.server) return false
  const headers = useRequestHeaders(['user-agent'])
  const ua = headers['user-agent'] || ''
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua)
}

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

  // 客户端挂载标记：SSR 阶段断点值不可靠（窗口尺寸为0），需等客户端挂载后才使用真实值
  // 避免 hydration mismatch
  const isMounted = ref(false)
  onMounted(() => { isMounted.value = true })

  // SSR 阶段通过 User-Agent 预判移动端，减少水合闪烁
  const ssrIsMobile = import.meta.server ? detectMobileByUA() : false

  // 是否为移动端（小于768px）
  const isMobile = computed(() => {
    if (isMounted.value) return !breakpoints.md.value
    return ssrIsMobile
  })

  // 是否为平板端（768px - 1023px）
  const isTablet = computed(() => {
    if (!isMounted.value) return false
    const current = breakpoints.current()
    if (!current) return false
    const arr = Array.isArray(current) ? current : [current]
    return arr.includes('md') && !arr.includes('lg')
  })

  // 是否为桌面端（大于等于1024px）
  const isDesktop = computed(() => {
    if (isMounted.value) return breakpoints.lg.value
    return !ssrIsMobile
  })

  // 当前断点名称
  const currentBreakpoint = computed(() => {
    if (!isMounted.value) return ssrIsMobile ? 'mobile' : 'desktop'
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
