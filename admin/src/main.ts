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

import { sessionStore, STORAGE_KEYS } from '@/utils/storage'

const app = createApp(App)

// 注册 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 全局 Element Plus 提示配置（移动端适配 / 防溢出 / 显眼）
app.use(ElementPlus, { locale: zhCn, size: 'default' })

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
    const permissions = sessionStore.get<string[]>(STORAGE_KEYS.USER_PERMISSIONS) || []
    if (value && value.length > 0) {
      const hasPermission = value.some((perm: string) => permissions.includes(perm))
      if (!hasPermission) {
        el.parentNode?.removeChild(el)
      }
    }
  },
})

// 全局 ElMessage 移动端适配配置（设置后所有页面 ElMessage 弹出的提示都更显眼、不溢出）
// 通过覆写 .el-message CSS 自定义属性来集中控制位置、宽度、圆角、阴影（见 styles/index.scss）
import { ElMessage } from 'element-plus'
;(ElMessage as any).options = {
  ...((ElMessage as any).options || {}),
  // 顶部距离：让消息贴近顶部，比默认更显眼（不被 header 遮挡）
  offset: 24,
  // 多个消息合并展示（去重提示）
  grouping: true,
  // 默认展示时长（被调用方 options.duration 覆盖）
  duration: 2500,
  // 不显示关闭按钮（小屏上 X 容易误触）
  showClose: false,
}

app.mount('#app')
