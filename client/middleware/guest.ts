/** 游客中间件：已登录用户跳转首页 */
export default defineNuxtRouteMiddleware(() => {
  const userStore = useUserStore()

  // 已登录则跳转首页
  if (userStore.isLoggedIn) {
    return navigateTo('/')
  }
})
