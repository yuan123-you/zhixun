/**
 * 全局 Toast 提示 composable
 * 统一管理 Toast 提示，消除各个页面中的重复代码
 *
 * 移动端适配要点：
 *  - 顶部居中悬浮，距离顶部 16px（更显眼，远离导航栏的视觉干扰）
 *  - 移动端左右各 12px 边距，桌面端最大宽 480px，避免溢出屏幕
 *  - 文本支持自动换行 + break-words，长消息也能完整展示
 *  - 图标与文本首行顶端对齐，多行也美观
 *  - 长消息自动延长展示时间（确保普通用户能读完）
 *  - 语义色：success / error / info / warning，调用方按业务语义选择
 *
 * 友好性：所有抛给用户的消息应通过 showToast(..., 'error') 弹出，
 *       底层配合 utils/friendlyError.ts 把技术消息翻译成大白话。
 */

type ToastType = 'success' | 'error' | 'info' | 'warning'

type ToastPosition = 'top' | 'top-right' | 'top-center' | 'bottom' | 'bottom-center'

interface ToastOptions {
  message: string
  type?: ToastType
  duration?: number
  zIndex?: number
  position?: ToastPosition
}

const TOAST_DEFAULTS = {
  duration: 2000,
  longDuration: 3500,
  zIndex: 9999,
  position: 'top' as ToastPosition,
  // 移动端左右各 12px 边距，桌面端最大 480px
  maxWidthClass: 'max-w-[calc(100vw-1.5rem)] sm:max-w-[480px]',
  // 文本长度超过该阈值时使用更长的展示时间
  longTextThreshold: 20,
  colors: {
    success: 'bg-emerald-500',
    error: 'bg-red-500',
    info: 'bg-blue-500',
    warning: 'bg-amber-500',
  },
  positionClasses: {
    'top': 'top-4 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px]',
    'top-right': 'top-4 right-3 sm:right-4 sm:left-auto sm:translate-x-0 sm:w-auto',
    'top-center': 'top-4 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px]',
    'bottom': 'bottom-4 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px]',
    'bottom-center': 'bottom-6 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px]',
  },
} as const

const iconPaths: Record<ToastType, string> = {
  success: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>',
  error: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>',
  info: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>',
  warning: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>',
}

/**
 * 估算显示时长：长文本自动延长，确保用户能读完。
 *  - 短消息：默认 2s；
 *  - 超过阈值：使用 longDuration，再按长度线性追加（最多 +1.5s）。
 */
function pickDuration(message: string, override?: number): number {
  if (typeof override === 'number') return override
  if (message.length <= TOAST_DEFAULTS.longTextThreshold) return TOAST_DEFAULTS.duration
  const extra = Math.min(1500, Math.floor((message.length - TOAST_DEFAULTS.longTextThreshold) * 30))
  return TOAST_DEFAULTS.longDuration + extra
}

/**
 * 限制单条消息的最大长度，超出截断避免移动端被截掉无法查看。
 * 移动端可视区域窄，长消息应该走"标题+详情"模式或显示在 Modal 中。
 */
function capMessage(message: string, max = 80): string {
  if (!message) return ''
  const trimmed = String(message).replace(/\s+/g, ' ').trim()
  if (trimmed.length <= max) return trimmed
  return trimmed.slice(0, max - 1) + '…'
}

export function showToast(
  message: string,
  type: ToastType = 'success',
  options?: Partial<Pick<ToastOptions, 'duration' | 'zIndex' | 'position'>>,
): void {
  const display = capMessage(message)
  if (!display) return

  const duration = pickDuration(display, options?.duration)
  const zIndex = options?.zIndex ?? TOAST_DEFAULTS.zIndex
  const position = options?.position ?? TOAST_DEFAULTS.position
  const positionClass = TOAST_DEFAULTS.positionClasses[position]

  const toast = document.createElement('div')
  // 使用 Tailwind 静态类 + inline style 处理动态 z-index（Tailwind JIT 无法编译动态类名）
  toast.className = [
    `fixed ${positionClass}`,
    'px-4 py-3 rounded-2xl shadow-2xl',
    'text-white text-sm font-medium',
    'transition-all duration-300 flex items-start gap-2',
    // 文本溢出处理：长消息在移动端也能完整展示且不超出屏幕
    `whitespace-normal break-words ${TOAST_DEFAULTS.maxWidthClass}`,
    'opacity-0 -translate-y-2',
    TOAST_DEFAULTS.colors[type],
  ].join(' ')
  toast.style.zIndex = String(zIndex)

  // 图标在多行时与第一行文字顶端对齐，文本自身负责换行
  const safeMessage = display.replace(/</g, '&lt;').replace(/>/g, '&gt;')
  toast.innerHTML = `
    <svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      ${iconPaths[type]}
    </svg>
    <span class="flex-1 leading-relaxed">${safeMessage}</span>
  `
  document.body.appendChild(toast)

  // 入场动画
  requestAnimationFrame(() => {
    toast.style.opacity = '1'
    toast.style.transform = 'translateY(0)'
  })

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateY(-8px)'
    setTimeout(() => toast.remove(), 300)
  }, duration)
}

/**
 * 弹出"标题+详情"式 Toast —— 适合需要告知原因 + 解决建议的长消息。
 * 移动端：详情会自动截断到 80 字内，避免溢出屏幕。
 */
export function showToastWithDetail(
  title: string,
  detail?: string,
  type: ToastType = 'error',
  options?: Partial<Pick<ToastOptions, 'duration' | 'zIndex' | 'position'>>,
): void {
  const safeTitle = capMessage(title)
  if (!safeTitle) return
  const safeDetail = detail ? capMessage(detail, 80) : ''

  const duration = options?.duration ?? 4000
  const zIndex = options?.zIndex ?? TOAST_DEFAULTS.zIndex
  const position = options?.position ?? TOAST_DEFAULTS.position
  const positionClass = TOAST_DEFAULTS.positionClasses[position]

  const toast = document.createElement('div')
  toast.className = [
    `fixed ${positionClass}`,
    'px-4 py-3 rounded-2xl shadow-2xl',
    'text-white text-sm font-medium',
    'transition-all duration-300 flex items-start gap-2',
    `whitespace-normal break-words ${TOAST_DEFAULTS.maxWidthClass}`,
    'opacity-0 -translate-y-2',
    TOAST_DEFAULTS.colors[type],
  ].join(' ')
  toast.style.zIndex = String(zIndex)

  const iconSvg = `<svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">${iconPaths[type]}</svg>`
  const detailHtml = safeDetail
    ? `<div class="text-white/90 text-xs font-normal mt-1 leading-relaxed break-words">${safeDetail.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</div>`
    : ''
  toast.innerHTML = `
    ${iconSvg}
    <div class="flex-1 leading-relaxed">
      <div>${safeTitle.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</div>
      ${detailHtml}
    </div>
  `
  document.body.appendChild(toast)

  requestAnimationFrame(() => {
    toast.style.opacity = '1'
    toast.style.transform = 'translateY(0)'
  })

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateY(-8px)'
    setTimeout(() => toast.remove(), 300)
  }, duration)
}

/**
 * Vue composable: 在 setup 中使用
 * 导出可在模板中使用的便捷方法
 */
export function useToast() {
  return {
    showToast,
    showToastWithDetail,
    success: (msg: string) => showToast(msg, 'success'),
    error: (msg: string) => showToast(msg, 'error'),
    info: (msg: string) => showToast(msg, 'info'),
    warning: (msg: string) => showToast(msg, 'warning'),
  }
}

export default useToast
