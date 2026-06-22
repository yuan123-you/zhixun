// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  // 项目名称
  name: 'zhixun-web',

  // SSR模式
  ssr: true,

  // 模块
  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@vueuse/nuxt',
    '@nuxtjs/color-mode',
  ],

  // 全局CSS
  css: [
    '~/assets/css/main.css',
  ],

  // 应用全局头部信息
  app: {
    head: {
      title: '知讯 - 优质内容平台',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: '知讯 - 发现优质内容，分享知识与见解' },
        { name: 'format-detection', content: 'telephone=no' },
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
      ],
    },
  },

  // 混合渲染路由规则
  routeRules: {
    // 首页SSR渲染
    '/': { ssr: true },
    // 文章详情页SSR渲染
    '/articles/**': { ssr: true },
    // 管理页CSR渲染
    '/user/**': { ssr: false },
    '/editor/**': { ssr: false },
    '/messages/**': { ssr: false },
  },

  // 运行时配置
  runtimeConfig: {
    // 服务端私有配置
    // 公共配置（客户端和服务端均可访问）
    public: {
      apiBase: 'http://localhost:8080/api/v1',
      wsBase: 'ws://localhost:8080/ws',
    },
  },

  // 代理API到后端8080端口
  nitro: {
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  // TypeScript配置
  typescript: {
    strict: true,
    shim: false,
  },

  // 暗色模式配置
  colorMode: {
    classSuffix: '',
    preference: 'system',
    fallback: 'light',
  },

  // 兼容性日期
  compatibilityDate: '2024-11-01',
})
