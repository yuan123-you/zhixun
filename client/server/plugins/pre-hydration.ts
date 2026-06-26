import type { NuxtApp } from 'nuxt/app'

/**
 * 在 SSR 渲染的 HTML <html> 标签上注入 pre-hydration 类。
 *
 * 原因：
 * 1. <html> 上的类作为 CSS 选择器锚点，配合 .pre-hydration * 规则禁用所有 transition/动画/backdrop-filter。
 * 2. 必须 SSR 阶段就存在（不能等客户端 JS 注入），否则水合前那 0~N 帧浏览器就会按正常 CSS 渲染，
 *    transition 已被 color-mode/useBreakpoints 触发，导致布局停留在中间帧。
 * 3. 不能用 useHead 设置 htmlAttrs.class：
 *    - 多个模块（@nuxtjs/color-mode、@nuxt/image 等）都可能在 SSR 写入 htmlAttrs
 *    - 合并顺序和行为在不同 Nuxt 版本有差异，不保证 pre-hydration 一定在最前面
 * 4. 直接在 render:html 钩子里用字符串替换修改 <html> 标签，最可靠。
 * 5. 已存在的类（dark/light）通过正则保留并追加 pre-hydration。
 *
 * 客户端水合完成且布局稳定后，由 plugins/hydration.client.ts 移除该类。
 */
export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('render:html', (html) => {
    // html.htmlAttrs 是数组（可能包含 lang, class 等属性）
    // 找到 class 属性并追加 pre-hydration
    const idx = html.htmlAttrs.findIndex((attr: string) => attr.startsWith('class='))
    if (idx >= 0) {
      const original = html.htmlAttrs[idx]
      // original 形如 `class="dark"` 或 `class="dark light-mode"`
      // 在原有 class 值里追加 pre-hydration
      const match = original.match(/^class=(["'])(.*)\1$/)
      if (match) {
        const quote = match[1]
        const value = match[2]
        if (!value.split(/\s+/).includes('pre-hydration')) {
          html.htmlAttrs[idx] = `class=${quote}${value} pre-hydration${quote}`
        }
      } else {
        // 没有匹配到 class 值的引号包裹，添加新的 class 属性
        html.htmlAttrs.push('class="pre-hydration"')
      }
    } else {
      // 没有 class 属性，直接添加
      html.htmlAttrs.push('class="pre-hydration"')
    }
  })
})
