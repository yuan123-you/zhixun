/**
 * 简洁提示弹框（移动端适配版）
 *
 *  - 顶部居中悬浮，距离顶部 16px，远离导航栏
 *  - 移动端左右各 12px 边距，桌面端最大 480px，不会溢出
 *  - 文本支持换行，长消息也能完整展示
 *  - 颜色采用语义红色，提示"出错"或"注意"
 */

let alertVisible = false

export function showAlert(message: string, duration = 1800) {
  if (typeof window === 'undefined' || alertVisible) return
  const safe = String(message || '').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  if (!safe.trim()) return
  alertVisible = true

  const el = document.createElement('div')
  el.className = 'fixed top-4 left-3 right-3 sm:left-1/2 sm:right-auto sm:-translate-x-1/2 sm:w-auto sm:max-w-[480px] z-[9999] px-4 py-3 rounded-2xl shadow-2xl text-sm text-white font-medium bg-red-500 transition-all duration-300 flex items-start gap-2 whitespace-normal break-words max-w-[calc(100vw-1.5rem)] opacity-0 -translate-y-2'
  el.innerHTML = `
    <svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
    </svg>
    <span class="flex-1 leading-relaxed">${safe}</span>
  `
  document.body.appendChild(el)

  requestAnimationFrame(() => {
    el.style.opacity = '1'
    el.style.transform = 'translateY(0)'
  })

  const close = () => {
    el.style.opacity = '0'
    el.style.transform = 'translateY(-8px)'
    setTimeout(() => { el.remove(); alertVisible = false }, 300)
  }

  setTimeout(close, duration)
}
