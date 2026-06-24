/** 应用水合完成后移除加载封面 */
export default defineNuxtPlugin(() => {
  if (!import.meta.client) return

  const removeSplash = () => {
    const splash = document.getElementById('app-splash')
    if (splash && !splash.classList.contains('fade-out')) {
      splash.classList.add('fade-out')
      setTimeout(() => splash.remove(), 500)
    }
  }

  // 监听 Nuxt 页面就绪事件
  const nuxtApp = useNuxtApp()
  nuxtApp.hook('page:finish', () => {
    nextTick(() => setTimeout(removeSplash, 100))
  })

  // 兜底：5秒后强制移除
  setTimeout(removeSplash, 5000)
})
