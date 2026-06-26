/**
 * 水合完成稳定后移除 <html> 上的 pre-hydration 类
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
 * 修复策略：双 rAF + 50ms 延时，确保所有 onMounted 钩子链触发完毕、所有 v-if/v-show 切换完成、
 * color-mode 应用后，再恢复 transition。devtools 与正常浏览行为一致。
 */
export default defineNuxtPlugin(() => {
  if (!import.meta.client) return

  // 双 rAF：第一个 rAF 等待当前帧绘制完成；第二个 rAF 等待 Vue 在当前帧完成 reactive flush
  // （包括所有组件的 onMounted 钩子同步触发的响应式更新）
  // 之后再加 50ms 兜底延时，确保浏览器有足够时间开始绘制稳定帧
  const removeClass = () => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setTimeout(() => {
          const html = document.documentElement
          if (html.classList.contains('pre-hydration')) {
            html.classList.remove('pre-hydration')
          }
        }, 50)
      })
    })
  }

  // 优先在 page:finish 时移除（页面真正稳定）
  const nuxtApp = useNuxtApp()
  nuxtApp.hook('page:finish', () => {
    nextTick(removeClass)
  })

  // 兜底：客户端启动后 200ms 强制移除（防止 page:finish 异常未触发）
  setTimeout(removeClass, 200)
})
