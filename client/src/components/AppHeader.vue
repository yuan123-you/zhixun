<template>
  <!-- 顶部导航栏 - 玻璃拟态 + 流畅动效 (仅首页和我的页面显示) -->
  <header v-if="showHeaderInner" class="app-header">
    <div class="header-inner">
      <!-- 左侧：汉堡菜单 + 返回按钮(子页面) + 品牌 -->
      <div class="header-left">
        <!-- 汉堡菜单按钮（全端可见） -->
        <button
          class="hamburger-btn"
          aria-label="打开导航菜单"
          @click="toggleDrawer"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <!-- 返回按钮（子页面显示） -->
        <button
          v-if="isBackButtonVisible"
          class="back-btn-header"
          aria-label="返回上一页"
          @click="goBack"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        <RouterLink to="/" class="brand-link" aria-label="知讯首页">
          <span v-if="showBrandIcon" class="brand-icon">Z</span>
          <span class="brand-text">知讯</span>
        </RouterLink>
      </div>

      <!-- 桌面端导航链接（>=768px 可见） -->
      <nav class="header-nav">
        <RouterLink to="/" class="nav-link" exact-active-class="nav-link--active">
          <span>首页</span>
        </RouterLink>
        <RouterLink to="/discover" class="nav-link" active-class="nav-link--active">
          <span>发现</span>
        </RouterLink>
        <RouterLink to="/rank" class="nav-link" active-class="nav-link--active">
          <span>排行</span>
        </RouterLink>
        <RouterLink to="/topics" class="nav-link" active-class="nav-link--active">
          <span>话题</span>
        </RouterLink>
        <RouterLink to="/groups" class="nav-link" active-class="nav-link--active">
          <span>群组</span>
        </RouterLink>
        <RouterLink v-if="userStore.isLoggedIn" to="/editor" class="nav-link nav-link--create" active-class="nav-link--active">
          <el-icon :size="14"><EditPen /></el-icon>
          <span>创作</span>
        </RouterLink>
      </nav>

      <!-- 右侧操作区 -->
      <div class="header-right">
        <!-- 搜索 -->
        <RouterLink to="/search" class="icon-btn" title="搜索" aria-label="搜索">
          <el-icon :size="20"><Search /></el-icon>
        </RouterLink>

        <!-- 消息 -->
        <RouterLink v-if="userStore.isLoggedIn" to="/notifications" class="icon-btn" aria-label="通知">
          <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount <= 0">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
        </RouterLink>

        <!-- 设置 -->
        <RouterLink v-if="userStore.isLoggedIn" to="/user/settings" class="icon-btn" title="设置" aria-label="设置">
          <el-icon :size="20"><Setting /></el-icon>
        </RouterLink>

        <!-- 用户菜单 / 登录 -->
        <el-dropdown v-if="userStore.isLoggedIn" trigger="click" @command="handleCommand" placement="bottom-end">
          <span class="avatar-trigger">
            <UserAvatar :src="userStore.userInfo?.avatar" :alt="userStore.userInfo?.nickname" size="sm" />
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>设置
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <RouterLink v-else to="/login" class="login-link">
          <span class="login-btn-text">登录</span>
        </RouterLink>
      </div>
    </div>
  </header>

  <!-- 侧边栏导航抽屉（汉堡菜单触发） —— Teleport 到 body 避免 header backdrop-filter 层叠上下文问题 -->
  <Teleport to="body">
    <Transition name="drawer-mask">
      <div v-if="isDrawerOpen" class="drawer-mask" @click="closeDrawer" />
    </Transition>
      <Transition name="drawer-slide">
        <aside v-if="isDrawerOpen" class="nav-drawer">
          <div class="drawer-content">
            <nav class="drawer-nav">
              <!-- 主导航 -->
              <RouterLink to="/" class="drawer-link" :class="{ 'drawer-link--active': isActive('/') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
                <span>首页</span>
              </RouterLink>
              <RouterLink to="/discover" class="drawer-link" :class="{ 'drawer-link--active': isActive('/discover') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                <span>发现</span>
              </RouterLink>
              <RouterLink to="/rank" class="drawer-link" :class="{ 'drawer-link--active': isActive('/rank') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
                </svg>
                <span>排行</span>
              </RouterLink>
              <RouterLink to="/search" class="drawer-link" :class="{ 'drawer-link--active': isActive('/search') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>搜索</span>
              </RouterLink>
              <RouterLink to="/tags" class="drawer-link" :class="{ 'drawer-link--active': isActive('/tags') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
                <span>标签</span>
              </RouterLink>
              <RouterLink to="/topics" class="drawer-link" :class="{ 'drawer-link--active': isActive('/topics') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
                </svg>
                <span>话题</span>
              </RouterLink>
              <RouterLink to="/groups" class="drawer-link" :class="{ 'drawer-link--active': isActive('/groups') }" @click="closeDrawer">
                <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                <span>群组</span>
              </RouterLink>
            </nav>

            <!-- 已登录：个人功能区 -->
            <template v-if="userStore.isLoggedIn">
              <div class="drawer-divider" />
              <div class="drawer-section">
                <h3 class="drawer-section-title">个人</h3>
                <div class="drawer-nav">
                  <RouterLink to="/editor" class="drawer-link" :class="{ 'drawer-link--active': isActive('/editor') }" @click="closeDrawer">
                    <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                    <span>创作</span>
                  </RouterLink>
                  <RouterLink to="/notifications" class="drawer-link" :class="{ 'drawer-link--active': isActive('/notifications') || isActive('/messages') }" @click="closeDrawer">
                    <div class="drawer-icon-wrap">
                      <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                      </svg>
                      <span v-if="notificationStore.unreadCount > 0" class="drawer-badge">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
                    </div>
                    <span>消息</span>
                  </RouterLink>
                  <RouterLink to="/user" class="drawer-link" :class="{ 'drawer-link--active': isActive('/user') }" @click="closeDrawer">
                    <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span>个人中心</span>
                  </RouterLink>
                  <RouterLink to="/user/settings" class="drawer-link" :class="{ 'drawer-link--active': isActive('/user/settings') }" @click="closeDrawer">
                    <svg class="drawer-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    <span>设置</span>
                  </RouterLink>
                </div>
              </div>
            </template>

            <!-- 未登录：登录按钮 -->
            <template v-else>
              <div class="drawer-divider" />
              <div class="drawer-login-area">
                <RouterLink to="/login" class="drawer-login-btn" @click="closeDrawer">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                  </svg>
                  <span>登录 / 注册</span>
                </RouterLink>
              </div>
            </template>
          </div>
        </aside>
      </Transition>
    </Teleport>
</template>

<script setup lang="ts">
import { Search, Bell, Setting, ArrowDown, User, SwitchButton, EditPen } from '@element-plus/icons-vue'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { logout } = useAuth()
const route = useRoute()
const router = useRouter()
const { title: pageTitle } = usePageHeaderTitle()
const { showHeaderInner } = useHeaderVisibility()

// ---- 侧边栏抽屉状态 ----
const isDrawerOpen = ref(false)
const toggleDrawer = () => { isDrawerOpen.value = !isDrawerOpen.value }
const closeDrawer = () => { isDrawerOpen.value = false }

// 路由变化时自动关闭抽屉
watch(() => route.path, closeDrawer)

// 抽屉打开时锁定 body 滚动，关闭时恢复
watch(isDrawerOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

// ---- 返回按钮（子页面显示） ----
const isBackButtonVisible = computed(() => {
  const tabPages = ['/', '/discover', '/editor', '/notifications', '/user']
  if (tabPages.includes(route.path)) return false
  if (/^\/articles\/\d+/.test(route.path)) return false
  if (route.path === '/rank') return false
  if (route.path === '/tags') return false
  if (route.path === '/topics') return false
  if (route.path === '/search') return false
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  return true
})

const showBrandIcon = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

// header-inner 仅在首页和我的页面显示，其他页面完全隐藏 header（含背景）
watch(() => route.path, (path) => {
  showHeaderInner.value = path === '/' || path.startsWith('/user')
}, { immediate: true })

// ---- 判断路由是否激活（抽屉高亮用） ----
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const goBack = () => {
  // 优先使用导航时携带的 from 状态，确保返回到来源页面（如私信详情页、群组页等）
  const from = (window.history.state as any)?.from
  if (from && typeof from === 'string') {
    router.replace(from)
  } else if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile': router.push('/user'); break
    case 'settings': router.push('/user/settings'); break
    case 'logout':
      try {
        await logout()
      } catch (e) {
        console.error('退出登录失败:', e)
        router.replace('/login')
      }
      break
  }
}
</script>

<style scoped>
/* ==================== Header 本体 ==================== */
.app-header {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--zh-border);
  transition: background var(--zh-transition-base);
}
.dark .app-header {
  background: rgba(15, 23, 42, 0.84);
  backdrop-filter: blur(20px) saturate(120%);
}

.header-inner {
  max-width: var(--zh-content-max);
  margin: 0 auto;
  padding: 0 8px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
@media (min-width: 768px) {
  .header-inner { height: 64px; padding: 0 20px; gap: 12px; }
}
@media (min-width: 1536px) {
  .header-inner { max-width: 1400px; padding: 0 32px; }
}

/* ==================== 左侧区域 ==================== */
.header-left {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
@media (min-width: 768px) {
  .header-left { gap: 4px; }
}

/* ---- 汉堡菜单 ---- */
.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: var(--zh-text-secondary);
  cursor: pointer;
  border-radius: var(--zh-radius-md);
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
  transition: color var(--zh-transition-fast), background var(--zh-transition-fast);
}
.hamburger-btn:hover { color: var(--zh-primary); background: var(--zh-bg-hover); }
.hamburger-btn:active { transform: scale(0.95); }
@media (min-width: 768px) {
  .hamburger-btn { width: 36px; height: 36px; }
}

/* ---- 返回按钮 ---- */
.back-btn-header {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: var(--zh-text-secondary);
  cursor: pointer;
  border-radius: var(--zh-radius-md);
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
  transition: color var(--zh-transition-fast), background var(--zh-transition-fast);
}
.back-btn-header:hover { color: var(--zh-primary); background: var(--zh-bg-hover); }
.back-btn-header:active { transform: scale(0.95); }
@media (min-width: 768px) {
  .back-btn-header { width: 36px; height: 36px; }
  .back-btn-header svg { width: 18px; height: 18px; }
}

/* ---- 品牌 ---- */
.brand-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--zh-text);
  transition: opacity var(--zh-transition-fast);
}
.brand-link:hover { opacity: 0.8; }

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--zh-radius-md);
  background: var(--zh-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -1px;
  box-shadow: 0 2px 8px rgba(var(--zh-primary-rgb), 0.3);
}
.brand-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}

/* ==================== 桌面端导航链接 ==================== */
.header-nav {
  display: none;
  flex: 1;
  justify-content: center;
}
@media (min-width: 768px) {
  .header-nav { display: flex; align-items: center; gap: 2px; }
}
@media (min-width: 1024px) {
  .header-nav { gap: 4px; }
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: var(--zh-radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--zh-text-secondary);
  text-decoration: none;
  transition: all var(--zh-transition-fast);
  position: relative;
}
.nav-link:hover { color: var(--zh-primary); background: var(--zh-primary-bg); }
.nav-link--active {
  color: var(--zh-primary);
  background: var(--zh-primary-bg);
}
.nav-link--create {
  color: var(--zh-primary);
  border: 1px dashed var(--zh-border-focus);
}

/* ==================== 右侧操作区 ==================== */
.header-right { display: flex; align-items: center; gap: 0; }
@media (min-width: 768px) { .header-right { gap: 4px; } }

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--zh-radius-md);
  color: var(--zh-text-secondary);
  transition: all var(--zh-transition-fast);
  position: relative;
}
.icon-btn:hover { color: var(--zh-primary); background: var(--zh-bg-hover); }
@media (min-width: 768px) { .icon-btn { width: 40px; height: 40px; } }

.avatar-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px 2px 2px;
  border-radius: var(--zh-radius-full);
  cursor: pointer;
  transition: background var(--zh-transition-fast);
}
.avatar-trigger:hover { background: var(--zh-bg-hover); }

/* ---- 登录按钮（替代 el-button，确保移动端 44px 触摸目标）---- */
.login-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  min-width: 56px;
  padding: 4px 16px;
  border-radius: var(--zh-radius-full);
  background: var(--zh-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  letter-spacing: 0.5px;
  transition: opacity var(--zh-transition-fast), transform var(--zh-transition-fast);
  -webkit-tap-highlight-color: transparent;
}
.login-link:hover { opacity: 0.9; }
.login-link:active { transform: scale(0.96); }
@media (pointer: coarse) {
  .login-link {
    min-height: 44px;
    min-width: 64px;
    padding: 6px 20px;
    font-size: 14px;
  }
}
.login-btn-text {
  pointer-events: none;
}

.arrow-icon {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  transition: transform var(--zh-transition-fast);
}

/* ==================== 侧边栏抽屉 ==================== */
.drawer-mask {
  position: fixed;
  top: 52px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 9998;
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}
@media (min-width: 768px) {
  .drawer-mask { top: 64px; }
}

.nav-drawer {
  position: fixed;
  top: 52px;
  left: 0;
  bottom: 0;
  width: 280px;
  z-index: 9999;
  background: var(--zh-bg-elevated, #fff);
  border-right: 1px solid var(--zh-border);
  overflow-y: auto;
  overscroll-behavior: contain;
}
@media (min-width: 768px) {
  .nav-drawer { top: 64px; width: 300px; }
}
.dark .nav-drawer {
  background: #1e293b;
  border-right-color: #374151;
}

.drawer-content {
  padding: 8px 0;
}

/* ---- 抽屉导航链接 ---- */
.drawer-nav {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 8px;
}

.drawer-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--zh-text-secondary, #64748b);
  text-decoration: none;
  transition: all 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}
.drawer-link:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text, #0f172a);
}
.drawer-link--active {
  background: rgba(var(--zh-primary-rgb, 99, 102, 241), 0.08);
  color: var(--zh-primary, #6366f1);
}
.dark .drawer-link--active {
  background: rgba(99, 102, 241, 0.15);
}

.drawer-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

/* 带角标的图标容器 */
.drawer-icon-wrap {
  position: relative;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.drawer-icon-wrap .drawer-icon {
  width: 20px;
  height: 20px;
}

.drawer-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  white-space: nowrap;
}

/* ---- 抽屉分区 ---- */
.drawer-divider {
  height: 1px;
  margin: 6px 20px;
  background: var(--zh-border, #e2e8f0);
}
.dark .drawer-divider {
  background: #374151;
}

.drawer-section {
  padding-top: 4px;
}

.drawer-section-title {
  padding: 4px 20px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--zh-text-tertiary, #94a3b8);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ---- 登录按钮 ---- */
.drawer-login-area {
  padding: 8px 20px;
}
.drawer-login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}
.drawer-login-btn:hover {
  opacity: 0.9;
}
.drawer-login-btn:active {
  transform: scale(0.98);
}

/* ==================== 抽屉动画 ==================== */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

.drawer-mask-enter-active,
.drawer-mask-leave-active {
  transition: opacity 0.28s ease;
}
.drawer-mask-enter-from,
.drawer-mask-leave-to {
  opacity: 0;
}
</style>
