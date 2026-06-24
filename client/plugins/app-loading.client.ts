/** 应用水合完成后移除加载画面 */
export default defineNuxtPlugin(() => {
  if (!import.meta.client) return

  // 等待应用水合完成后移除加载画面
  const removeSplash = () => {
    const splash = document.getElementById('app-splash')
    if (splash) {
      splash.classList.add('fade-out')
      setTimeout(() => splash.remove(), 300)
    }
  }

  // 监听 Nuxt 页面就绪事件
  const nuxtApp = useNuxtApp()
  nuxtApp.hook('page:finish', () => {
    nextTick(() => setTimeout(removeSplash, 50))
  })

  // 兜底：3秒后强制移除
  setTimeout(removeSplash, 3000)
})
