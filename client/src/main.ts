import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { setRouter, navigateTo } from './composables/navigateTo'

// 初始化 navigateTo 全局函数
setRouter(router)

// 全局样式
import './assets/css/main.css'

const app = createApp(App)

// 显式注册 navigateTo 为全局 property —— 解决 unplugin-auto-import
// 仅扫描 <script setup> 不会扫描 template 引用导致的 "_ctx.navigateTo is not a function" 问题
// 这样所有 SFC 模板中可以直接使用 `navigateTo('/some-path')`
app.config.globalProperties.navigateTo = navigateTo

// 注册 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 注册 Element Plus，使用中文语言包
app.use(ElementPlus, { locale: zhCn })

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册全局错误处理器
import errorHandler from './plugins/error-handler'
app.use(errorHandler)

// 注册路由
app.use(router)

// 挂载应用
app.mount('#app')

// 水合完成后：显示 #app 并移除加载封面
const appEl = document.getElementById('app')
if (appEl) {
  appEl.classList.add('app-ready')
}

const removeSplash = () => {
  const splash = document.getElementById('app-splash')
  if (splash && !splash.classList.contains('fade-out')) {
    splash.classList.add('fade-out')
    setTimeout(() => splash.remove(), 500)
  }
}

// 监听 Vue 应用挂载完成
router.isReady().then(() => {
  removeSplash()
})
