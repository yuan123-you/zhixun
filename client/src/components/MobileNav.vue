<template>
  <!-- 移动端底部 Tab 导航 - 玻璃拟态 + 流畅动效 -->
  <nav class="mobile-nav">
    <div class="tab-list">
      <RouterLink to="/" class="tab-item" :class="{ active: isActive('/') }">
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span class="tab-label">首页</span>
      </RouterLink>

      <RouterLink to="/discover" class="tab-item" :class="{ active: isActive('/discover') }">
        <el-icon :size="22"><Compass /></el-icon>
        <span class="tab-label">发现</span>
      </RouterLink>

      <RouterLink to="/editor" class="tab-item tab-publish" :class="{ active: isActive('/editor') }">
        <div class="publish-btn">
          <el-icon :size="24"><Plus /></el-icon>
        </div>
        <span class="tab-label">发布</span>
      </RouterLink>

      <RouterLink to="/notifications" class="tab-item" :class="{ active: isActive('/notifications') || isActive('/messages') }">
        <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount <= 0">
          <el-icon :size="22"><Bell /></el-icon>
        </el-badge>
        <span class="tab-label">消息</span>
      </RouterLink>

      <RouterLink to="/user" class="tab-item" :class="{ active: isActive('/user') }">
        <el-icon :size="22"><User /></el-icon>
        <span class="tab-label">我的</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { HomeFilled, Compass, Plus, Bell, User } from '@element-plus/icons-vue'

const notificationStore = useNotificationStore()
const route = useRoute()

const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped>
.mobile-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: none;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid var(--zh-border);
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.04);
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
.dark .mobile-nav {
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: blur(20px) saturate(120%);
}
@media (max-width: 767.98px) { .mobile-nav { display: block; } }

.tab-list {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 50px;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 4px 0;
  color: var(--zh-text-tertiary);
  text-decoration: none;
  transition: color var(--zh-transition-fast), transform var(--zh-transition-fast);
  position: relative;
  min-width: 0;
}
.tab-item:active { transform: scale(0.92); }
.tab-item.active { color: var(--zh-primary); }

/* 激活指示器 */
.tab-item.active::after {
  content: '';
  position: absolute;
  top: 1px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: var(--zh-primary);
  border-radius: 0 0 2px 2px;
}

.tab-label {
  font-size: 10px;
  margin-top: 2px;
  line-height: 1;
  font-weight: 500;
}

/* 发布按钮:从突出的圆形按钮改为平面的实心矩形,
   与其他 4 个 tab 视觉一致,仅颜色不同 */
.tab-publish { margin-top: 0; }

.publish-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--zh-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(var(--zh-primary-rgb), 0.25);
  transition: transform var(--zh-transition-fast), box-shadow var(--zh-transition-fast);
}
.tab-publish.active .publish-btn {
  box-shadow: 0 2px 10px rgba(var(--zh-primary-rgb), 0.35);
}
.publish-btn:active { transform: scale(0.9); }
</style>
