/**
 * 水合完成且 DOM 稳定后移除 <html> 上的 pre-hydration 类
 *
 * 关键时序说明：
 * 1. SSR 阶段 server/plugins/pre-hydration.ts 在 <html> 上预置 pre-hydration，
 *    CSS 屏蔽所有 transition/动画/backdrop-filter。
 * 2. 客户端 hydration 时，Vue 完成挂载后，useBreakpoints 中 onMounted 同步设置 isMounted.value = true
 *    → 触发 isTablet/isDesktop/isLandscape 重新计算 → <aside v-if="isTablet">、<main :style> 等
 *    同步切换 → 这些 DOM 变化伴随 CSS transition（main 的 padding-left 0.3s、AppHeader 的 backdrop-blur 等）。
 * 3. 同时 color-mode 在 hydration 后会给 <html> 加 dark/light 类，CSS 变量重新计算也可能触发布局过渡。
 * 4. 若在步骤 2 之前移除 pre-hydration，浏览器就会把 transition 播放在初始帧上，可能停在中间态
 *    → 表现：layout 错乱。DevTools 打开时会强制同步 reflow 把过渡快进到最终态 → 表现正常。
 *    这就是用户报告的"开发者工具检查就是正确的效果"现象。
 *
 * 修复策略（v2）：使用 MutationObserver 监听 DOM 变化，在 DOM 持续稳定（无 class/style 变化、
 * 无子节点增删）一段静默期后才移除 pre-hydration。相比固定延迟，Observer 能自适应各种
 * hydration 耗时（color-mode 应用延迟、异步组件挂载、Teleport 渲染等），彻底解决
 * "devtools 正常、正常浏览错乱"问题。
 */
export default defineNuxtPlugin(() => {
  if (!import.meta.client) return

  const removePreHydration = () => {
    const html = document.documentElement
    if (html.classList.contains('pre-hydration')) {
      html.classList.remove('pre-hydration')
    }
  }

  /**
   * 使用 MutationObserver 等待 DOM 稳定后再移除 pre-hydration 类。
   *
   * 工作原理：
   * - 每次 DOM 减变化（class/style/子节点）都重置静默计时器
   * - 连续 QUIET_PERIOD ms 无变化 → 认为 DOM 已稳定
   * - 双 rAF 确保浏览器完成最后一帧的布局 + 绘制
   * - HARD_TIMEOUT 作为绝对兜底，防止 Observer 因异常未触发
   */
  const waitForStableDOM = () => {
    // 如果 pre-hydration 类已不存在（纯 SPA 客户端导航），直接返回
    if (!document.documentElement.classList.contains('pre-hydration')) return

    let stabilityTimer: ReturnType<typeof setTimeout> | null = null
    const QUIET_PERIOD = 200   // 连续无变化 200ms 后认为稳定
    const HARD_TIMEOUT = 1200  // 绝对超时 1200ms 强制移除

    const onStable = () => {
      observer.disconnect()
      // 双 rAF 确保浏览器完成最后一帧的布局 + 绘制
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          removePreHydration()
        })
      })
    }

    const observer = new MutationObserver(() => {
      // 每次 DOM 变化都重置计时器
      if (stabilityTimer) clearTimeout(stabilityTimer)
      stabilityTimer = setTimeout(onStable, QUIET_PERIOD)
    })

    observer.observe(document.documentElement, {
      attributes: true,
      childList: true,
      subtree: true,
      // 仅关注 class 和 style 属性变化（覆盖 color-mode、ClientOnly、v-if/v-show 等场景）
      attributeFilter: ['class', 'style'],
    })

    // 启动首轮静默计时器
    stabilityTimer = setTimeout(onStable, QUIET_PERIOD)

    // 绝对超时保护：即使 DOM 持续变化也强制移除
    setTimeout(() => {
      observer.disconnect()
      if (stabilityTimer) clearTimeout(stabilityTimer)
      removePreHydration()
    }, HARD_TIMEOUT)
  }

  const nuxtApp = useNuxtApp()
  nuxtApp.hook('page:finish', () => {
    waitForStableDOM()
  })
})
