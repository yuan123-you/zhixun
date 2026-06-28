/** 应用水合完成后移除加载封面（纯 SPA 无需水合，直接执行） */
const removeSplash = () => {
  const splash = document.getElementById('app-splash')
  if (splash && !splash.classList.contains('fade-out')) {
    splash.classList.add('fade-out')
    setTimeout(() => splash.remove(), 500)
  }
}

// 页面挂载后移除
nextTick(() => setTimeout(removeSplash, 100))

// 兜底：5 秒后强制移除
setTimeout(removeSplash, 5000)
