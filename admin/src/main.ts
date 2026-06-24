import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

// 全局样式
import './styles/index.scss'

import { storage, STORAGE_KEYS } from '@/utils/storage'

const app = createApp(App)

// 注册 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 注册 Element Plus，使用中文语言包
app.use(ElementPlus, { locale: zhCn })

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册路由
app.use(router)

// 自定义指令：权限控制
app.directive('permission', {
  mounted(el: HTMLElement, binding) {
    const { value } = binding
    // 从用户状态中获取权限列表
    const permissions = storage.get<string[]>(STORAGE_KEYS.USER_PERMISSIONS) || []
    if (value && value.length > 0) {
      const hasPermission = value.some((perm: string) => permissions.includes(perm))
      if (!hasPermission) {
        el.parentNode?.removeChild(el)
      }
    }
  },
})

app.mount('#app')
