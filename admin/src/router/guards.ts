import type { Router } from 'vue-router'
import { storage, STORAGE_KEYS } from '@/utils/storage'

// 白名单路由，无需登录即可访问
const whiteList = ['/login']

/**
 * 设置路由守卫
 * - 认证检查：未登录用户跳转登录页
 * - 权限检查：登录用户访问登录页自动跳转首页
 */
export function setupGuards(router: Router) {
  router.beforeEach((to, _from, next) => {
    // 页面标题
    if (to.meta.title) {
      document.title = `${to.meta.title} - 知讯管理后台`
    }

    const token = storage.get<string>(STORAGE_KEYS.TOKEN)

    if (token) {
      // 已登录状态
      if (to.path === '/login') {
        // 已登录用户访问登录页，重定向到首页
        next({ path: '/' })
      } else {
        next()
      }
    } else {
      // 未登录状态
      if (whiteList.includes(to.path)) {
        // 白名单路由，直接放行
        next()
      } else {
        // 非白名单路由，跳转登录页并记录来源
        next(`/login?redirect=${to.path}`)
      }
    }
  })
}
