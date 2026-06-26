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
    '@vite-pwa/nuxt',
  ],

  // PWA 配置
  pwa: {
    registerType: 'autoUpdate',
    // 开发环境禁用 Service Worker，避免缓存旧错误响应导致登录/API 异常
    disable: process.env.NODE_ENV === 'development',
    manifest: {
      name: '知讯 - 内容资讯平台',
      short_name: '知讯',
      description: '图文创作与社交平台',
      theme_color: '#3b82f6',
      background_color: '#ffffff',
      display: 'standalone',
      orientation: 'portrait-primary',
      icons: [
        { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
        { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
        { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
      ],
      shortcuts: [
        { name: '首页', url: '/', description: '浏览推荐内容' },
        { name: '话题广场', url: '/topics', description: '发现热门话题' },
        { name: '创作', url: '/editor', description: '开始创作' },
      ],
    },
    workbox: {
      navigateFallback: '/',
      globPatterns: ['**/*.{js,css,html,png,svg,ico,json,woff2}'],
    },
    client: {
      installPrompt: true,
    },
  },

  // 全局CSS
  css: [
    '~/assets/css/main.css',
  ],

  // 应用全局头部信息
  app: {
    head: {
      title: '知讯 - 优质内容平台',
      // pre-hydration 类由 server/plugins/pre-hydration.ts 在 SSR 阶段直接写入 <html> 标签，
      // 避免与 color-mode 等模块的 htmlAttrs 合并产生不确定性。
      // 用于在水合 + useBreakpoints 稳定前禁用所有 transition/动画，避免布局停留在中间帧。
      // 客户端水合完成后由 plugins/hydration.client.ts 移除。
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
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
    // 作品详情页SSR渲染
    '/articles/**': { ssr: true },
    // 管理页CSR渲染
    '/user/**': { ssr: false },
    '/editor/**': { ssr: false },
    '/messages/**': { ssr: false },
    '/topics/**': { swr: 120 },
    '/groups/**': { ssr: false },
    // 管理后台：开发模式下重定向到独立 admin 开发服务器；线上由 Nginx 代理
    '/admin/**': {
      ssr: false,
    },
    // API代理到后端（本地开发时使用；线上由 Nginx 代理，此规则不生效）
    '/api/**': { proxy: 'http://localhost:8080/api/**' },
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

  // 开发服务器配置：使用独立端口避免冲突
  devServer: {
    port: 3500,
    host: 'localhost',
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
