import type { NuxtApp } from 'nuxt/app'

/** 在服务端渲染的HTML中注入现代风格加载封面，确保JS加载前就有视觉反馈 */
export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('render:html', (html, { event }) => {
    const splashHtml = `<div id="app-splash"><div class="splash-bg"></div><div class="splash-content"><div class="splash-logo"><svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg"><rect width="48" height="48" rx="12" fill="white" fill-opacity="0.2"/><text x="24" y="33" font-family="Arial, sans-serif" font-size="28" font-weight="bold" fill="white" text-anchor="middle">知</text></svg></div><div class="splash-brand">知讯</div><div class="splash-tagline">发现优质内容，分享知识与见解</div><div class="splash-loader"><div class="splash-loader-bar"></div></div></div></div>`

    if (html.bodyAppend) {
      html.bodyAppend.unshift(splashHtml)
    }
  })
})
