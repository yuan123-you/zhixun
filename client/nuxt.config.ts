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
    '@nuxt/image',
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
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'preconnect', href: '/api' },
      ],
      // 预连接常用外部域名，加速资源加载
    },
    // 页面级 preload/prefetch 策略：减少不必要的预加载
    pageTransition: { name: 'page', mode: 'out-in' },
  },

  // 混合渲染路由规则
  routeRules: {
    // 首页SSR渲染 + SWR缓存60秒
    '/': { ssr: true, swr: 60 },
    // 排行榜SSR + SWR缓存5分钟
    '/rank': { swr: 300 },
    // 发现页SSR + SWR缓存2分钟
    '/discover': { swr: 120 },
    // 标签页SSR + SWR缓存5分钟
    '/tags': { swr: 300 },
    // 文章详情页SSR渲染
    '/articles/**': { ssr: true },
    // 管理页CSR渲染
    '/user/**': { ssr: false },
    '/editor/**': { ssr: false },
    '/messages/**': { ssr: false },
    // API代理到后端（本地开发时使用；线上由 Nginx 代理，此规则不生效）
    '/api/**': { proxy: `${process.env.API_BASE || 'http://localhost:8080'}/api/**` },
  },

  // 运行时配置
  // 注意：Nuxt 运行时通过 NUXT_ 前缀的环境变量覆盖这些值
  // - apiBase → NUXT_API_BASE
  // - public.apiBase → NUXT_PUBLIC_API_BASE
  // - public.wsBase → NUXT_PUBLIC_WS_BASE
  runtimeConfig: {
    // 服务端私有配置（运行时通过 NUXT_API_BASE 覆盖）
    apiBase: process.env.API_BASE || 'http://localhost:8080',
    // 公共配置（客户端和服务端均可访问，运行时通过 NUXT_PUBLIC_* 覆盖）
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || '/api/v1',
      wsBase: process.env.NUXT_PUBLIC_WS_BASE || '',
      // MinIO 内部地址（用于 SSR 时识别并替换为公网地址）
      minioInternalUrl: process.env.NUXT_PUBLIC_MINIO_INTERNAL_URL || 'http://minio:9000',
      // MinIO 公网访问路径（浏览器通过 Nginx 代理访问）
      minioPublicUrl: process.env.NUXT_PUBLIC_MINIO_PUBLIC_URL || '/minio',
    },
  },

  // TypeScript配置
  typescript: {
    strict: true,
    shim: false,
  },

  // 暗色模式配置 - 支持浅色/深色/跟随系统切换
  colorMode: {
    classSuffix: '',
    preference: 'system',
    fallback: 'light',
    hid: 'nuxt-color-mode-script',
  },

  // 页面切换加载指示器
  loadingIndicator: {
    name: 'chasing-dots',
    color: '#6366f1',
    height: '3px',
    throttle: 200,
  },

  // 兼容性日期
  compatibilityDate: '2024-11-01',

  // Vite 构建优化
  vite: {
    build: {
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router'],
            'vendor-utils': ['axios', 'qrcode', 'html2canvas'],
          },
        },
      },
    },
  },

  // Nitro 服务端配置
  nitro: {
    compressPublicAssets: true,
  },

  // 图片优化模块
  image: {
    quality: 80,
    format: ['webp', 'avif'],
    screens: {
      xs: 375,
      sm: 480,
      md: 768,
      lg: 1024,
      xl: 1200,
      '2xl': 1920,
    },
  },
})
