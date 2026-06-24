import type { NuxtApp } from 'nuxt/app'

/** 在服务端渲染的HTML中注入加载画面，确保JS加载前就有视觉反馈 */
export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('render:html', (html, { event }) => {
    // 在 body 开头注入加载画面
    const splashHtml = `<div id="app-splash"><div class="spinner"></div><div class="splash-text">加载中...</div></div>`

    if (html.bodyAppend) {
      html.bodyAppend.unshift(splashHtml)
    }
  })
})
