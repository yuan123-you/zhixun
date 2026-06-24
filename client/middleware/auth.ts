/** 认证中间件：未登录用户友好提示并跳转到登录页 */
export default defineNuxtRouteMiddleware((to) => {
  const userStore = useUserStore()

  // 未登录则提示并跳转登录页，记录来源路径
  if (!userStore.isLoggedIn) {
    // 客户端环境下显示友好提示
    if (import.meta.client) {
      showAuthToast()
    }
    return navigateTo({
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    })
  }
})

/** 未登录提示 Toast（防止重复弹出） */
let authToastVisible = false
function showAuthToast() {
  if (authToastVisible) return
  authToastVisible = true

  const toast = document.createElement('div')
  toast.className = 'fixed top-4 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl shadow-lg text-white text-sm font-medium transition-all duration-300 transform bg-amber-500 flex items-center gap-2'
  toast.innerHTML = `
    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
    </svg>
    <span>请先登录后再访问</span>
  `
  document.body.appendChild(toast)

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translate(-50%, -20px)'
    setTimeout(() => {
      toast.remove()
      authToastVisible = false
    }, 300)
  }, 2000)
}
