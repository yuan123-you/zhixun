<template>
  <!-- 顶部导航栏 - 玻璃拟态 + 流畅动效 -->
  <header class="app-header">
    <div class="header-inner">
      <!-- 左侧：返回按钮（移动端需要时显示）+ 品牌 -->
      <div class="header-left">
        <button
          v-if="isBackButtonVisible"
          class="back-btn-header md:hidden"
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

      <!-- 桌面端导航 -->
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

        <RouterLink v-else to="/login">
          <el-button type="primary" size="small" round>登录</el-button>
        </RouterLink>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, Bell, Setting, ArrowDown, User, SwitchButton, EditPen } from '@element-plus/icons-vue'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { logout } = useAuth()
const route = useRoute()
const router = useRouter()

const isBackButtonVisible = computed(() => {
  const tabPages = ['/', '/discover', '/editor', '/notifications', '/user']
  if (tabPages.includes(route.path)) return false
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  if (/^\/user\/\d+\/(followers|following)/.test(route.path)) return false
  return true
})

const showBrandIcon = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile': router.push('/user'); break
    case 'settings': router.push('/user/settings'); break
    case 'logout':
      // 先清空 userStore 状态，让 AppHeader 立即切换到"登录"按钮状态，
      // 再异步执行退出登录逻辑（包括后端 logout 调用和路由跳转）。
      // 这样无论后端是否响应、router.push 是否成功，UI 都会立即响应。
      try {
        await logout()
      } catch (e) {
        // 兜底：即使 logout 内部抛错，也强制跳转到登录页
        console.error('退出登录失败:', e)
        router.replace('/login')
      }
      break
  }
}
</script>

<style scoped>
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
  padding: 0 12px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
@media (min-width: 768px) {
  .header-inner { height: 64px; padding: 0 20px; }
}
@media (min-width: 1536px) {
  .header-inner { max-width: 1400px; padding: 0 32px; }
}

/* ---- 品牌 ---- */
.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

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
  display: none;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}
@media (min-width: 768px) { .brand-text { display: inline; } }

/* ---- 导航链接 ---- */
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

/* ---- 右侧 ---- */
.header-right { display: flex; align-items: center; gap: 2px; }
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

.arrow-icon {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  transition: transform var(--zh-transition-fast);
}
</style>
