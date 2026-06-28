/**
 * 全局错误处理插件（Vite + Vue 3 SPA 版）
 * 捕获 Vue 运行时错误，转换为用户友好的提示信息
 *
 * 设计要点：
 *  - 任何会触达"普通用户"的提示，都走 utils/friendlyError 翻译
 *  - 弹层位置：移动端顶部居中悬浮，距离顶部 16px，不会被导航栏遮挡
 *  - 移动端左右各 12px 边距，桌面端最大 480px，不会溢出屏幕
 *  - 文本支持换行（break-words），长消息也能完整展示
 */
import type { App } from 'vue'
import { friendlyMessage } from '@/utils/friendlyError'

export default {
  install(app: App) {
    // 全局 Vue 错误处理
    app.config.errorHandler = (err: any, instance, info) => {
      if (shouldSkip(err)) return
      const friendly = friendlyMessage(err, '页面出现异常，请稍后重试')
      console.warn('[Vue Error]', { message: friendly, raw: err?.message || err, info })
      if (isFatalError(err)) {
        showErrorOverlay(friendly, true)
      } else {
        // 非致命错误也提示一下，避免用户对着白屏发愣
        showErrorOverlay(friendly, false)
      }
    }

    // 全局 Promise 未捕获异常处理
    window.addEventListener('unhandledrejection', (event) => {
      if (isNavigationError(event.reason)) { event.preventDefault(); return }
      if (shouldSkip(event.reason)) return
      if (isDuplicateError(event.reason)) { event.preventDefault(); return }
      console.warn('[Promise Error]', event.reason?.message || event.reason)
      const friendly = friendlyMessage(event.reason, '操作未能完成，请稍后重试')
      // 异步未捕获错误默认是轻提示
      showErrorOverlay(friendly, false)
    })
  }
}

/** 可以忽略的非致命错误 */
function shouldSkip(err: any): boolean {
  const raw = typeof err === 'string' ? err : (err?.message || '')
  // HMR 热更新 → Vite 内部，连接丢失时是正常行为（开发环境后端重启等）
  if (/hmr|hot.?reload|vite.*reload|server connection lost|websocket|Polling for restart/i.test(raw)) return true
  // 路由中断 / 动态模块加载失败 → 正常行为
  if (/NavigationDuplicated|redirect.*navigation|Failed to fetch dynamically imported module/i.test(raw)) return true
  return false
}

function isFatalError(err: any): boolean {
  const raw = typeof err === 'string' ? err : (err?.message || '')
  return /instance\s*unavailable/i.test(raw) || /Cannot\s*(read|access).*null|undefined/i.test(raw)
}

function isNavigationError(reason: any): boolean {
  const raw = typeof reason === 'string' ? reason : (reason?.message || '')
  return /NavigationDuplicated|redirect.*navigation/i.test(raw)
}

const recentErrors = new Set<string>()
function isDuplicateError(err: any): boolean {
  const key = typeof err === 'string' ? err : (err?.message || '')
  if (recentErrors.has(key)) return true
  recentErrors.add(key)
  setTimeout(() => recentErrors.delete(key), 3000)
  return false
}

let errorOverlayVisible = false
function showErrorOverlay(message: string, showRefreshBtn: boolean) {
  if (errorOverlayVisible) return
  errorOverlayVisible = true

  const safe = String(message || '页面出现异常，请稍后重试')
    .replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // 移动端：顶部 Toast；桌面端：顶部居中卡片。
  // 两种形态都不溢出屏幕，且都显眼。
  const isMobile = typeof window !== 'undefined' && window.matchMedia('(max-width: 640px)').matches

  if (isMobile) {
    const t = document.createElement('div')
    t.className = 'fixed top-4 left-3 right-3 z-[99999] px-4 py-3 rounded-2xl shadow-2xl text-white text-sm font-medium bg-amber-500 transition-all duration-300 flex items-start gap-2 whitespace-normal break-words max-w-[calc(100vw-1.5rem)] opacity-0 -translate-y-2'
    t.innerHTML = `
      <svg class="w-4 h-4 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>
      </svg>
      <span class="flex-1 leading-relaxed">${safe}</span>
    `
    document.body.appendChild(t)
    requestAnimationFrame(() => { t.style.opacity = '1'; t.style.transform = 'translateY(0)' })
    setTimeout(() => {
      t.style.opacity = '0'
      t.style.transform = 'translateY(-8px)'
      setTimeout(() => { t.remove(); errorOverlayVisible = false }, 300)
    }, 3500)
    return
  }

  const overlay = document.createElement('div')
  Object.assign(overlay.style, {
    position: 'fixed', inset: '0', zIndex: '99999',
    display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
    background: 'rgba(0,0,0,0.4)',
    paddingTop: '15vh',
  })

  const inner = document.createElement('div')
  Object.assign(inner.style, {
    background: '#fff', borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
    padding: '24px', margin: '16px', textAlign: 'center',
    maxWidth: '420px', width: 'calc(100% - 32px)',
  })
  inner.innerHTML = `
    <svg style="width:48px;height:48px;margin:0 auto 12px;color:#E6A23C" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
    </svg>
    <p style="font-size:14px;color:#334155;margin-bottom:16px;line-height:1.6;word-break:break-word">${safe}</p>
    ${showRefreshBtn
      ? `<button id="__error_refresh_btn" style="padding:8px 20px;background:#4f46e5;color:#fff;font-size:14px;border:none;border-radius:999px;cursor:pointer">刷新页面</button>`
      : `<button id="__error_close_btn" style="padding:8px 20px;background:#4f46e5;color:#fff;font-size:14px;border:none;border-radius:999px;cursor:pointer">我知道了</button>`}
  `

  overlay.appendChild(inner)
  document.body.appendChild(overlay)

  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) {
      closeOverlay()
    }
  })
  const refreshBtn = overlay.querySelector('#__error_refresh_btn')
  if (refreshBtn) refreshBtn.addEventListener('click', () => { closeOverlay(); window.location.reload() })
  const closeBtn = overlay.querySelector('#__error_close_btn')
  if (closeBtn) closeBtn.addEventListener('click', () => closeOverlay())
}

function closeOverlay() {
  document.querySelectorAll('div[style*="z-index: 99999"]').forEach(o => o.remove())
  errorOverlayVisible = false
}
