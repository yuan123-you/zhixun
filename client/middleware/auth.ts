/** 认证中间件：未登录用户跳转到登录页 */
export default defineNuxtRouteMiddleware((to) => {
  const userStore = useUserStore()

  // 未登录则跳转登录页，并记录来源路径
  if (!userStore.isLoggedIn) {
    return navigateTo({
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    })
  }
})
