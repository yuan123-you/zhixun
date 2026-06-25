/** 简洁提示弹框 */

let alertVisible = false

export function showAlert(message: string, duration = 1500) {
  if (!import.meta.client || alertVisible) return
  alertVisible = true

  const el = document.createElement('div')
  el.className = 'fixed top-5 left-1/2 -translate-x-1/2 z-[9999] px-5 py-2.5 rounded-lg shadow-lg text-sm text-white bg-red-500 transition-opacity duration-150'
  el.textContent = message
  document.body.appendChild(el)

  const close = () => {
    el.style.opacity = '0'
    setTimeout(() => { el.remove(); alertVisible = false }, 150)
  }

  setTimeout(close, duration)
}
