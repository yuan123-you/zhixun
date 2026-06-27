/**
 * 全局错误处理插件
 * 捕获 Vue/Nuxt 运行时错误，转换为用户友好的提示信息
 */
export default defineNuxtPlugin((nuxtApp) => {
  // 全局 Vue 错误处理
  nuxtApp.vueApp.config.errorHandler = (err, instance, info) => {
    const message = toFriendlyMessage(err)

    // 忽略开发环境下的热更新相关错误
    if (isHmrError(err)) return

    console.error('[应用异常]', message, { info })

    // 如果是致命错误（如 Nuxt instance 不可用），刷新页面
    if (isFatalError(err) && import.meta.client) {
      showErrorOverlay(message, true)
    }
  }

  // 全局 Promise 未捕获异常处理
  if (import.meta.client) {
    window.addEventListener('unhandledrejection', (event) => {
      // Nuxt 导航重复等非关键错误忽略
      if (isNavigationError(event.reason)) {
        event.preventDefault()
        return
      }

      const message = toFriendlyMessage(event.reason)

      // 防止重复弹窗
      if (isDuplicateError(event.reason)) {
        event.preventDefault()
        return
      }

      console.error('[未处理的异常]', message)
    })
  }
})

/** 映射 Nuxt/Vue 内部错误为用户友好文案 */
function toFriendlyMessage(err: any): string {
  const raw = typeof err === 'string' ? err : (err?.message || err?.toString?.() || '')

  // [nuxt] instance unavailable → 页面组件初始化失败
  if (/instance\s*unavailable/i.test(raw)) {
    return '页面加载异常，正在尝试恢复...'
  }

  // Nuxt 内部组件生命周期错误
  if (/nuxt.*instance|nuxtApp|useNuxtApp/i.test(raw) && /not\s*(found|available)/i.test(raw)) {
    return '页面加载异常，请刷新页面重试'
  }

  // Vue 水合不匹配（SSR hydration mismatch）
  if (/hydration\s*mismatch|hydrate/i.test(raw)) {
    return '页面显示异常，请刷新页面重试'
  }

  // 组件渲染错误
  if (/render|component/i.test(raw) && /fail|error|missing/i.test(raw)) {
    return '页面加载异常，请稍后重试'
  }

  // 网络相关
  if (/fetch|network|ECONNREFUSED|ERR_NETWORK/i.test(raw)) {
    return '网络连接失败，请检查网络'
  }

  // 超时
  if (/timeout/i.test(raw)) {
    return '请求超时，请稍后重试'
  }

  return '页面出现异常，请刷新后重试'
}

/** 判断是否为 Nuxt 致命错误（需刷新页面） */
function isFatalError(err: any): boolean {
  const raw = typeof err === 'string' ? err : (err?.message || '')
  return /instance\s*unavailable/i.test(raw) ||
    /Cannot\s*(read|access).*null|undefined/i.test(raw)
}

/** 判断是否为 HMR 热更新相关错误（可忽略） */
function isHmrError(err: any): boolean {
  const raw = typeof err === 'string' ? err : (err?.message || '')
  return /hmr|hot\s*reload|vite.*reload/i.test(raw)
}

/** 判断是否为导航重复错误 */
function isNavigationError(reason: any): boolean {
  const raw = typeof reason === 'string' ? reason : (reason?.message || '')
  return /NavigationDuplicated|redirect.*navigation/i.test(raw)
}

/** 防止同一错误在短时间内反复弹窗 */
const recentErrors = new Set<string>()
function isDuplicateError(err: any): boolean {
  const key = typeof err === 'string' ? err : (err?.message || '')
  if (recentErrors.has(key)) return true
  recentErrors.add(key)
  setTimeout(() => recentErrors.delete(key), 3000)
  return false
}

/** 显示错误遮罩层 */
let errorOverlayVisible = false
function showErrorOverlay(message: string, showRefreshBtn: boolean) {
  if (errorOverlayVisible) return
  errorOverlayVisible = true

  const overlay = document.createElement('div')
  overlay.className = 'fixed inset-0 z-[99999] flex items-center justify-center bg-black/40'
  overlay.innerHTML = `
    <div class="bg-white rounded-2xl shadow-xl px-6 py-5 mx-4 text-center max-w-xs w-full">
      <div class="w-12 h-12 mx-auto mb-3 text-amber-500">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
      </div>
      <p class="text-sm text-slate-700 mb-4">${message}</p>
      <button id="__error_refresh_btn" class="px-5 py-2 bg-primary text-white text-sm rounded-full hover:bg-primary-600 transition-colors">
        刷新页面
      </button>
    </div>
  `

  document.body.appendChild(overlay)

  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) {
      closeOverlay()
      window.location.reload()
    }
  })

  const btn = overlay.querySelector('#__error_refresh_btn')
  btn?.addEventListener('click', () => {
    closeOverlay()
    window.location.reload()
  })
}

function closeOverlay() {
  const overlay = document.querySelector('.fixed.inset-0.z-\\[99999\\]')
  if (overlay) overlay.remove()
  errorOverlayVisible = false
}
