<template>
  <!-- 全局返回按钮 -->
  <template v-if="show">
    <!-- 移动端迷你顶栏 -->
    <div class="back-bar-mobile">
      <button class="back-btn" aria-label="返回上一页" @click="goBack">
        <el-icon :size="16"><ArrowLeft /></el-icon>
      </button>
      <span v-if="pageTitle" class="back-title">{{ pageTitle }}</span>
    </div>

    <!-- 桌面端浮动箭头 -->
    <Transition name="back-fade">
      <button class="back-btn-desktop" aria-label="返回上一页" @click="goBack">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </button>
    </Transition>
  </template>
</template>

<script setup lang="ts">
/** 全局返回按钮 */
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const { title: pageTitle } = usePageHeaderTitle()

const show = computed(() => {
  const tabPages = ['/', '/discover', '/editor', '/notifications', '/user']
  if (tabPages.includes(route.path)) return false
  // 所有 /user/* 路径由 AppHeader 内部提供返回按钮，不再显示独立返回栏
  if (route.path.startsWith('/user')) return false
  if (/^\/articles\/\d+/.test(route.path)) return false
  if (route.path === '/rank') return false
  if (route.path === '/tags') return false
  if (route.path === '/topics') return false
  if (route.path === '/search') return false
  if (/^\/messages\/\d+/.test(route.path)) return false
  if (/^\/groups\/\d+/.test(route.path)) return false
  return true
})

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<style scoped>
.back-bar-mobile {
  display: flex;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 60;
  height: 40px;
  padding: 0 6px;
  background: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
@media (min-width: 768px) {
  .back-bar-mobile { display: none; }
}

.back-btn {
  width: 32px;
  height: 32px;
  min-height: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  color: var(--el-text-color-regular);
  cursor: pointer;
  border-radius: 8px;
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
}
.back-btn:active { transform: scale(0.95); }

.back-title {
  margin-left: 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.back-btn-desktop {
  display: none;
  align-items: center;
  justify-content: center;
  position: fixed;
  top: 8px;
  left: 8px;
  z-index: 60;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  border-radius: 8px;
  -webkit-tap-highlight-color: transparent;
}
@media (min-width: 768px) {
  .back-btn-desktop { display: flex; }
}
.back-btn-desktop:hover { color: var(--el-text-color-primary); }
.back-btn-desktop:active { transform: scale(0.95); }

.back-fade-enter-active,
.back-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.back-fade-enter-from,
.back-fade-leave-to { opacity: 0; transform: translateX(-8px); }
</style>
