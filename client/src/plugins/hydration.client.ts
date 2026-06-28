/**
 * 客户端 hydration 完成后移除 pre-hydration 类，恢复 CSS 过渡/动画。
 *
 * 原理：
 *   index.html 中 <html class="pre-hydration"> 确保页面加载时所有
 *   transition/animation/backdrop-filter 被 main.css 中的规则屏蔽。
 *   DevTools 打开时会强制同步重排（reflow），把过渡"快进"到终态，
 *   导致"DevTools 检查布局正常、正常浏览布局错乱"的现象。
 *
 *   本插件在 hydration 完成后移除该类，恢复正常的过渡效果。
 */

const REMOVE_DELAY = 250 // DOM 稳定检测的静默期（ms）

/**
 * 使用 MutationObserver 监听 DOM 变化，当连续 REMOVE_DELAY 毫秒
 * 无任何 class/style/子节点变化时，认为 hydration 完成。
 */
function waitForDomStable(): Promise<void> {
  return new Promise((resolve) => {
    let timer: ReturnType<typeof setTimeout> | null = null

    const schedule = () => {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        observer.disconnect()
        resolve()
      }, REMOVE_DELAY)
    }

    const observer = new MutationObserver(() => schedule())

    observer.observe(document.documentElement, {
      attributes: true,
      childList: true,
      subtree: true,
      attributeFilter: ['class', 'style'],
    })

    // 首次调度
    schedule()
  })
}

/**
 * 移除 pre-hydration 类
 */
function removePreHydration() {
  const html = document.documentElement
  if (html.classList.contains('pre-hydration')) {
    html.classList.remove('pre-hydration')
  }
}

// 执行：等待 DOM 稳定后移除
(async () => {
  // 给页面 transition 完成留一点余量
  await nextTick()
  await waitForDomStable()
  removePreHydration()

  // 兜底：3 秒后无论如何移除，防止 observer 卡死
  setTimeout(removePreHydration, 3000)
})()
