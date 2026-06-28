<template>
  <!-- 全局设置页 -->
  <div class="settings-page">
    <!-- 内容区 -->
    <div class="settings-content">
      <!-- 加载状态 - 骨架屏 -->
      <div v-if="pageLoading" class="settings-skeleton">
        <div v-for="i in 4" :key="i" class="sk-section">
          <div class="sk-section-header">
            <div class="sk-icon skeleton"></div>
            <div class="sk-section-title skeleton skeleton-text"></div>
          </div>
          <div class="sk-section-body">
            <div v-for="j in 3" :key="j" class="sk-row">
              <div class="sk-label skeleton skeleton-text" style="width: 55%;"></div>
              <div class="sk-toggle skeleton"></div>
            </div>
          </div>
        </div>
      </div>

      <template v-else>
        <!-- 通知设置 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-notify">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            </span>
            <h2 class="section-title">通知设置</h2>
          </div>
          <div class="section-body">
            <label class="toggle-row">
              <span class="toggle-label">系统通知</span>
              <span class="toggle-switch">
                <input v-model="serverSettings.notification.notifySystem" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <span class="toggle-label">互动通知（点赞/评论）</span>
              <span class="toggle-switch">
                <input v-model="serverSettings.notification.notifyInteract" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <span class="toggle-label">关注通知</span>
              <span class="toggle-switch">
                <input v-model="serverSettings.notification.notifyFollow" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <span class="toggle-label">私信通知</span>
              <span class="toggle-switch">
                <input v-model="serverSettings.notification.notifyMessage" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
          </div>
        </section>

        <!-- 区块分隔装饰 -->
        <div class="section-divider"></div>

        <!-- 隐私设置 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-privacy">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </span>
            <h2 class="section-title">隐私设置</h2>
          </div>
          <div class="section-body">
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">显示在线状态</span>
                <span class="toggle-hint">其他用户可以看到你是否在线</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.showOnlineStatus" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">仅关注者可以私信</span>
                <span class="toggle-hint">开启后只有你关注的人能给你发私信</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.messagePermission" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">保存浏览历史</span>
                <span class="toggle-hint">关闭后不再记录你的浏览历史</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.saveViewHistory" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">允许被搜索</span>
                <span class="toggle-hint">关闭后其他用户无法通过搜索找到你</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.allowSearch" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">显示阅读量</span>
                <span class="toggle-hint">开启后你的作品会公开展示阅读量</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.showViewCount" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
          </div>
        </section>

        <div class="section-divider"></div>

        <!-- 内容偏好 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-content">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </span>
            <h2 class="section-title">内容偏好</h2>
          </div>
          <div class="section-body">
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">个性化内容推荐</span>
                <span class="toggle-hint">根据你的阅读偏好推荐相关内容</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.contentRecommend" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            
          </div>
        </section>

        <div class="section-divider"></div>

        <!-- 免打扰设置 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-dnd">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            </span>
            <h2 class="section-title">免打扰设置</h2>
          </div>
          <div class="section-body">
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">开启免打扰时段</span>
                <span class="toggle-hint">在指定时段内不接收任何通知</span>
              </div>
              <span class="toggle-switch">
                <input v-model="serverSettings.privacy.quietHoursEnabled" :true-value="1" :false-value="0" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <div v-if="serverSettings.privacy.quietHoursEnabled" class="time-range-row">
              <span class="time-range-label">从</span>
              <input
                v-model="serverSettings.privacy.quietHoursStart"
                type="time"
                class="time-input"
              />
              <span class="time-range-label">到</span>
              <input
                v-model="serverSettings.privacy.quietHoursEnd"
                type="time"
                class="time-input"
              />
            </div>
          </div>
        </section>

        <div class="section-divider"></div>

        <!-- 阅读设置 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-reading">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
              </svg>
            </span>
            <h2 class="section-title">阅读设置</h2>
          </div>
          <div class="section-body">
            <label class="toggle-row">
              <div class="toggle-label-group">
                <span class="toggle-label">自动加载图片</span>
                <span class="toggle-hint">关闭后仅显示占位图，节省流量</span>
              </div>
              <span class="toggle-switch">
                <input v-model="localSettings.autoLoadImages" type="checkbox" />
                <span class="toggle-track"></span>
              </span>
            </label>
            <div class="pill-group-wrapper">
              <span class="pill-group-label">默认排序</span>
              <div class="pill-group">
                <button
                  v-for="sort in sortOptions"
                  :key="sort.value"
                  class="pill-btn"
                  :class="{ 'pill-btn-active': localSettings.defaultSort === sort.value }"
                  @click="localSettings.defaultSort = sort.value"
                >
                  {{ sort.label }}
                </button>
              </div>
            </div>
          </div>
        </section>

        <div class="section-divider"></div>

        <!-- 显示设置 -->
        <section class="settings-section">
          <div class="section-header">
            <span class="section-icon section-icon-display">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                <line x1="8" y1="21" x2="16" y2="21" />
                <line x1="12" y1="17" x2="12" y2="21" />
              </svg>
            </span>
            <h2 class="section-title">显示设置</h2>
          </div>
          <div class="section-body">
            <div class="pill-group-wrapper">
              <span class="pill-group-label">主题</span>
              <div class="pill-group">
                <button
                  v-for="theme in themes"
                  :key="theme.value"
                  class="pill-btn"
                  :class="{ 'pill-btn-active': localSettings.theme === theme.value }"
                  @click="localSettings.theme = theme.value"
                >
                  {{ theme.label }}
                </button>
              </div>
            </div>
            <div class="pill-group-wrapper">
              <span class="pill-group-label">字体大小</span>
              <div class="pill-group">
                <button
                  v-for="size in fontSizes"
                  :key="size.value"
                  class="pill-btn"
                  :class="{ 'pill-btn-active': localSettings.fontSize === size.value }"
                  @click="localSettings.fontSize = size.value"
                >
                  {{ size.label }}
                </button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </div>

    <!-- 保存状态微提示 -->
    <Transition name="toast-fade">
      <div v-if="saveStatus" class="save-toast" :class="'save-toast--' + saveStatus">
        <span v-if="saveStatus === 'saving'" class="save-toast-spinner"></span>
        <svg v-else-if="saveStatus === 'saved'" class="save-toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        <span>{{ saveStatus === 'saving' ? '保存中...' : '已保存' }}</span>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/** 全局设置页 - 支持服务端同步 + 本地存储，任意操作自动保存 */
import type { UserSettingsServer, UserSettingsLocal, UserSettingsNotification, UserSettingsPrivacy, UserSettingsDisplay, UserSettingsRecommend } from '@/types'
import { storage, STORAGE_KEYS } from '@/utils/storage'
import { userApi } from '@/api'

const colorMode = useColorMode()

const STORAGE_KEY = STORAGE_KEYS.SETTINGS_LOCAL

const pageLoading = ref(true)
// 是否已完成初始化加载（加载完成后才启用自动保存）
const initialized = ref(false)

// 保存状态：null | 'saving' | 'saved'
const saveStatus = ref<'saving' | 'saved' | null>(null)
let saveStatusTimer: ReturnType<typeof setTimeout> | null = null

// 服务器端设置（嵌套结构，匹配后端 UserSettingsVO）
const defaultNotification = (): UserSettingsNotification => ({
  notifySystem: 1, notifyInteract: 1, notifyMessage: 1, notifyFollow: 1,
})
const defaultPrivacy = (): UserSettingsPrivacy => ({
  showOnlineStatus: 1, messagePermission: 0, saveViewHistory: 1,
  contentRecommend: 1, autoPlayVideo: 1,
  quietHoursEnabled: 0, quietHoursStart: '22:00', quietHoursEnd: '08:00',
  showViewCount: 1, allowSearch: 1,
})
const defaultDisplay = (): UserSettingsDisplay => ({
  fontSize: 1, theme: 'light', language: 'zh-CN',
})
const defaultRecommend = (): UserSettingsRecommend => ({
  interestedCategories: [], interestedTags: [], blockedCategories: [], blockedTags: [],
})

const serverSettings = reactive<UserSettingsServer>({
  notification: defaultNotification(),
  privacy: defaultPrivacy(),
  display: defaultDisplay(),
  recommend: defaultRecommend(),
})

// 本地设置（仅存 localStorage）
const defaultLocal = (): UserSettingsLocal => ({
  theme: 'system', fontSize: 'medium', language: 'zh-CN',
  autoLoadImages: true, defaultSort: 'latest' as const,
})

const localSettings = reactive<UserSettingsLocal>(
  storage.get<UserSettingsLocal>(STORAGE_KEY) || defaultLocal()
)

const themes = [
  { label: '浅色', value: 'light' as const },
  { label: '深色', value: 'dark' as const },
  { label: '跟随系统', value: 'system' as const },
]

const fontSizes = [
  { label: '小', value: 'small' as const },
  { label: '中', value: 'medium' as const },
  { label: '大', value: 'large' as const },
]

const sortOptions = [
  { label: '最新', value: 'latest' as const },
  { label: '热门', value: 'hot' as const },
]

// 字体大小映射（本地 string ↔ CSS px）
const fontSizePxMap: Record<string, string> = {
  small: '14px', medium: '16px', large: '18px',
}

// 服务器端字体 int ↔ 本地 string 映射
const serverFontToLocal: Record<number, 'small' | 'medium' | 'large'> = {
  0: 'small', 1: 'medium', 2: 'large',
}
const localFontToServer: Record<string, number> = {
  small: 0, medium: 1, large: 2,
}

// 应用字体大小
const applyFontSize = () => {
  if (true) {
    document.documentElement.style.setProperty('font-size', fontSizePxMap[localSettings.fontSize] || '16px')
  }
}

// 加载服务器设置
const loadServerSettings = async () => {
  try {
    const { data } = await userApi.getSettings()
    const s = data.data as any
    if (s) {
      if (s.notification) {
        Object.assign(serverSettings.notification, {
          notifySystem: s.notification.notifySystem ?? 1,
          notifyInteract: s.notification.notifyInteract ?? 1,
          notifyMessage: s.notification.notifyMessage ?? 1,
          notifyFollow: s.notification.notifyFollow ?? 1,
        })
      }
      if (s.privacy) {
        Object.assign(serverSettings.privacy, {
          showOnlineStatus: s.privacy.showOnlineStatus ?? 1,
          messagePermission: s.privacy.messagePermission ?? 0,
          saveViewHistory: s.privacy.saveViewHistory ?? 1,
          contentRecommend: s.privacy.contentRecommend ?? 1,
          autoPlayVideo: s.privacy.autoPlayVideo ?? 1,
          quietHoursEnabled: s.privacy.quietHoursEnabled ?? 0,
          quietHoursStart: s.privacy.quietHoursStart ?? '22:00',
          quietHoursEnd: s.privacy.quietHoursEnd ?? '08:00',
          showViewCount: s.privacy.showViewCount ?? 1,
          allowSearch: s.privacy.allowSearch ?? 1,
        })
      }
      if (s.display) {
        Object.assign(serverSettings.display, {
          fontSize: s.display.fontSize ?? 1,
          theme: s.display.theme ?? 'light',
          language: s.display.language ?? 'zh-CN',
        })
        // 同步服务器显示设置到本地设置（仅首次加载，本地修改优先）
        const savedLocal = storage.get<UserSettingsLocal>(STORAGE_KEY)
        if (!savedLocal) {
          localSettings.theme = s.display.theme === 'auto' ? 'system' : (s.display.theme as any) || 'system'
          localSettings.fontSize = serverFontToLocal[s.display.fontSize ?? 1] || 'medium'
          localSettings.language = s.display.language ?? 'zh-CN'
        }
      }
      if (s.recommend) {
        Object.assign(serverSettings.recommend, {
          interestedCategories: s.recommend.interestedCategories || [],
          interestedTags: s.recommend.interestedTags || [],
          blockedCategories: s.recommend.blockedCategories || [],
          blockedTags: s.recommend.blockedTags || [],
        })
      }
    }
  } catch {
    // 加载失败使用默认值
  }
}

// ========== 自动保存 ==========
let saveTimer: ReturnType<typeof setTimeout> | null = null

const syncAndSaveServer = async () => {
  // 同步本地显示设置到服务器数据
  serverSettings.display.theme = localSettings.theme === 'system' ? 'auto' : localSettings.theme
  serverSettings.display.fontSize = localFontToServer[localSettings.fontSize] ?? 1
  serverSettings.display.language = localSettings.language
  // 显示保存中状态
  saveStatus.value = 'saving'
  try {
    await userApi.updateSettings({ ...serverSettings } as any)
    // 显示已保存状态
    saveStatus.value = 'saved'
  } catch {
    saveStatus.value = null
    // 自动保存静默失败
  }
  // 2秒后自动隐藏
  if (saveStatusTimer) clearTimeout(saveStatusTimer)
  saveStatusTimer = setTimeout(() => {
    saveStatus.value = null
  }, 2000)
}

const debouncedServerSave = () => {
  if (!initialized.value) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    syncAndSaveServer()
  }, 600)
}

// 监听本地设置变化：立即生效并保存到 localStorage
watch(localSettings, () => {
  if (!initialized.value) return
  storage.set(STORAGE_KEY, { ...localSettings })
  applyFontSize()
  colorMode.preference = localSettings.theme === 'system' ? 'system' : localSettings.theme
  // 延迟同步到服务器
  debouncedServerSave()
}, { deep: true })

// 监听服务器设置变化：自动同步到服务器
watch(serverSettings, () => {
  if (!initialized.value) return
  debouncedServerSave()
}, { deep: true })

// 页面初始化
onMounted(async () => {
  await loadServerSettings()
  applyFontSize()
  pageLoading.value = false
  // 标记初始化完成，之后的操作才会触发自动保存
  initialized.value = true
})

// 页面元信息
useHead({
  title: () => '设置' + ' - 知讯',
})
</script>

<style scoped>
/* ==========================================================================
   设置页面 - 现代化设计风格
   ========================================================================== */

.settings-page {
  min-height: 100dvh;
  background: var(--zh-bg);
}

/* ---- 内容区 ---- */
.settings-content {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

/* ---- 骨架屏 ---- */
.settings-skeleton {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.sk-section {
  padding: 20px 0;
  border-bottom: 1px solid var(--zh-border-light);
}

.sk-section:last-child {
  border-bottom: none;
}

.sk-section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.sk-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--zh-radius-md);
}

.sk-section-title {
  width: 110px;
  height: 20px;
  border-radius: var(--zh-radius-sm);
}

.sk-section-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sk-label {
  height: 14px;
  border-radius: var(--zh-radius-sm);
}

.sk-toggle {
  width: 44px;
  height: 24px;
  border-radius: var(--zh-radius-full);
}

/* ---- 设置区块 ---- */
.settings-section {
  padding: 12px 0;
  animation: settings-fade-in 0.4s var(--zh-transition-base) both;
}

.settings-section:nth-child(1) { animation-delay: 0ms; }
.settings-section:nth-child(3) { animation-delay: 50ms; }
.settings-section:nth-child(5) { animation-delay: 100ms; }
.settings-section:nth-child(7) { animation-delay: 150ms; }
.settings-section:nth-child(9) { animation-delay: 200ms; }
.settings-section:nth-child(11) { animation-delay: 250ms; }

@keyframes settings-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- 区块头部（带图标） ---- */
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.section-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--zh-radius-md);
  flex-shrink: 0;
}

.section-icon svg {
  width: 17px;
  height: 17px;
}

.section-icon-notify {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.section-icon-privacy {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.section-icon-content {
  background: rgba(139, 92, 246, 0.12);
  color: #7c3aed;
}

.section-icon-dnd {
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
}

.section-icon-reading {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}

.section-icon-display {
  background: rgba(236, 72, 153, 0.12);
  color: #db2777;
}

.dark .section-icon-notify {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.dark .section-icon-privacy {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}

.dark .section-icon-content {
  background: rgba(167, 139, 250, 0.15);
  color: #a78bfa;
}

.dark .section-icon-dnd {
  background: rgba(129, 140, 248, 0.15);
  color: #818cf8;
}

.dark .section-icon-reading {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

.dark .section-icon-display {
  background: rgba(244, 114, 182, 0.15);
  color: #f472b6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--zh-text);
  margin: 0;
  letter-spacing: -0.01em;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* ---- 区块分隔装饰 ---- */
.section-divider {
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    var(--zh-border-light) 20%,
    var(--zh-border) 50%,
    var(--zh-border-light) 80%,
    transparent 100%
  );
}

/* ---- Toggle Switch 开关 ---- */
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 10px 0;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}

.toggle-label-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
  padding-right: 16px;
}

.toggle-label {
  font-size: 14px;
  color: var(--zh-text-secondary);
  line-height: 1.4;
}

.toggle-hint {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  line-height: 1.4;
}

.toggle-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.toggle-switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.toggle-track {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: var(--zh-radius-full);
  background: var(--zh-bg-active);
  transition: background var(--zh-transition-base);
  cursor: pointer;
}

.toggle-track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15), 0 1px 1px rgba(0, 0, 0, 0.08);
  transition: transform var(--zh-transition-spring);
}

.toggle-switch input:checked + .toggle-track {
  background: var(--zh-primary);
}

.toggle-switch input:checked + .toggle-track::after {
  transform: translateX(20px);
}

.toggle-switch input:focus-visible + .toggle-track {
  outline: 2px solid var(--zh-primary);
  outline-offset: 2px;
}

.dark .toggle-track {
  background: var(--zh-bg-active);
}

.dark .toggle-track::after {
  background: #cbd5e1;
}

.dark .toggle-switch input:checked + .toggle-track::after {
  background: #ffffff;
}

/* ---- Pill 按钮组 ---- */
.pill-group-wrapper {
  padding: 10px 0;
}

.pill-group-label {
  display: block;
  font-size: 14px;
  color: var(--zh-text-secondary);
  margin-bottom: 10px;
  font-weight: 500;
}

.pill-group {
  display: inline-flex;
  gap: 6px;
  background: var(--zh-bg-hover);
  border-radius: var(--zh-radius-full);
  padding: 3px;
}

.pill-btn {
  position: relative;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  border: none;
  border-radius: var(--zh-radius-full);
  background: transparent;
  color: var(--zh-text-tertiary);
  cursor: pointer;
  transition: all var(--zh-transition-base);
  white-space: nowrap;
  -webkit-tap-highlight-color: transparent;
  font-family: inherit;
}

.pill-btn:hover {
  color: var(--zh-text-secondary);
}

.pill-btn-active {
  background: var(--zh-bg-elevated);
  color: var(--zh-text);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  font-weight: 600;
}

.dark .pill-btn-active {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  background: var(--zh-bg-elevated);
}

/* ---- 时间范围输入 ---- */
.time-range-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0 10px;
  animation: settings-fade-in 0.3s var(--zh-transition-base) both;
}

.time-range-label {
  font-size: 14px;
  color: var(--zh-text-secondary);
  flex-shrink: 0;
}

.time-input {
  height: 38px;
  padding: 0 10px;
  font-size: 14px;
  font-family: inherit;
  border: 1.5px solid var(--zh-border);
  border-radius: var(--zh-radius-md);
  background: var(--zh-bg-elevated);
  color: var(--zh-text);
  outline: none;
  transition: border-color var(--zh-transition-fast), box-shadow var(--zh-transition-fast);
  -webkit-appearance: none;
}

.time-input:focus {
  border-color: var(--zh-input-focus);
  box-shadow: 0 0 0 3px var(--zh-input-focus-ring);
}

/* ---- 保存状态微提示 ---- */
.save-toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: var(--zh-radius-full);
  background: var(--zh-bg-elevated);
  box-shadow: var(--zh-shadow-lg);
  border: 1px solid var(--zh-border);
  font-size: 13px;
  font-weight: 500;
  color: var(--zh-text-secondary);
  z-index: 100;
  pointer-events: none;
  white-space: nowrap;
}

.save-toast--saved {
  color: var(--zh-success);
}

.save-toast--saved .save-toast-icon {
  width: 16px;
  height: 16px;
}

.save-toast-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin-slow 0.8s linear infinite;
}

@keyframes spin-slow {
  to { transform: rotate(360deg); }
}

/* Toast 过渡动画 */
.toast-fade-enter-active {
  transition: all 0.3s var(--zh-transition-spring);
}

.toast-fade-leave-active {
  transition: all 0.25s ease-in;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(12px) scale(0.92);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px) scale(0.95);
}

/* ==========================================================================
   响应式 - 移动端适配
   ========================================================================== */
@media (max-width: 767.98px) {
  .settings-content {
    padding: 0 12px 32px;
  }

  .section-icon {
    width: 30px;
    height: 30px;
  }

  .section-icon svg {
    width: 15px;
    height: 15px;
  }

  .section-title {
    font-size: 15px;
  }

  .toggle-row {
    min-height: 48px;
    padding: 12px 0;
  }

  .toggle-label {
    font-size: 15px;
  }

  .toggle-hint {
    font-size: 13px;
  }

  .pill-btn {
    padding: 7px 14px;
    font-size: 14px;
  }

  .save-toast {
    bottom: 24px;
    padding: 10px 16px;
    font-size: 12px;
  }
}

/* 小屏手机 */
@media (max-width: 374.98px) {
  .pill-group {
    gap: 3px;
  }

  .pill-btn {
    padding: 5px 10px;
    font-size: 12px;
  }
}
</style>