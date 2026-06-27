<template>
  <!-- 全局设置页 -->
  <div class="max-w-2xl mx-auto px-1.5 2xl:px-2 py-1.5">

    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
    </div>

    <template v-else>


    <!-- 通知设置 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-slate-900 mb-1.5">{{ '通知设置' }}</h2>
      <div class="space-y-2">
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <span class="text-sm text-slate-700">{{ '系统通知' }}</span>
          <input v-model="serverSettings.notification.notifySystem" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <span class="text-sm text-slate-700">{{ '互动通知（点赞/评论）' }}</span>
          <input v-model="serverSettings.notification.notifyInteract" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <span class="text-sm text-slate-700">{{ '关注通知' }}</span>
          <input v-model="serverSettings.notification.notifyFollow" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <span class="text-sm text-slate-700">{{ '私信通知' }}</span>
          <input v-model="serverSettings.notification.notifyMessage" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 隐私设置 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-slate-900 mb-1.5">{{ '隐私设置' }}</h2>
      <div class="space-y-2">
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <div>
            <span class="text-sm text-slate-700">{{ '显示在线状态' }}</span>
            <p class="text-xs text-slate-400">其他用户可以看到你是否在线</p>
          </div>
          <input v-model="serverSettings.privacy.showOnlineStatus" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <div>
            <span class="text-sm text-slate-700">{{ '仅关注者可以私信' }}</span>
            <p class="text-xs text-slate-400">开启后只有你关注的人能给你发私信</p>
          </div>
          <input v-model="serverSettings.privacy.messagePermission" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <div>
            <span class="text-sm text-slate-700">{{ '保存浏览历史' }}</span>
            <p class="text-xs text-slate-400">关闭后不再记录你的浏览历史</p>
          </div>
          <input v-model="serverSettings.privacy.saveViewHistory" :true-value="1" :false-value="0" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 阅读设置 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-slate-900 mb-1.5">{{ '阅读设置' }}</h2>
      <div class="space-y-2">
        <label class="flex items-center justify-between min-h-[44px] cursor-pointer">
          <div>
            <span class="text-sm text-slate-700">{{ '自动加载图片' }}</span>
            <p class="text-xs text-slate-400">关闭后仅显示占位图，节省流量</p>
          </div>
          <input v-model="localSettings.autoLoadImages" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <div>
          <label class="block text-sm text-slate-700 mb-1.5">{{ '默认排序' }}</label>
          <div class="flex space-x-1.5">
            <button
              v-for="sort in sortOptions"
              :key="sort.value"
              class="px-3 py-1.5 text-sm rounded-lg border transition-colors"
              :class="localSettings.defaultSort === sort.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white text-slate-700 border-slate-300'"
              @click="localSettings.defaultSort = sort.value"
            >
              {{ sort.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 显示设置 -->
    <section class="card p-2.5 mb-2">
      <h2 class="text-lg font-semibold text-slate-900 mb-1.5">{{ '显示设置' }}</h2>
      <div class="space-y-3">
        <!-- 主题 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '主题' }}</label>
          <div class="flex space-x-1.5">
            <button
              v-for="theme in themes"
              :key="theme.value"
              class="px-3 py-1.5 text-sm rounded-lg border transition-colors"
              :class="localSettings.theme === theme.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white text-slate-700 border-slate-300'"
              @click="localSettings.theme = theme.value"
            >
              {{ theme.label }}
            </button>
          </div>
        </div>

        <!-- 字体大小 -->
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '字体大小' }}</label>
          <div class="flex space-x-1.5">
            <button
              v-for="size in fontSizes"
              :key="size.value"
              class="px-3 py-1.5 text-sm rounded-lg border transition-colors"
              :class="localSettings.fontSize === size.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white text-slate-700 border-slate-300'"
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
</template>

<script setup lang="ts">
/** 全局设置页 - 支持服务端同步 + 本地存储，任意操作自动保存 */
import type { UserSettingsServer, UserSettingsLocal, UserSettingsNotification, UserSettingsPrivacy, UserSettingsDisplay, UserSettingsRecommend } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'
import { userApi } from '~/api'

const { setTitle } = usePageHeaderTitle()
setTitle('设置')

definePageMeta({
  middleware: 'auth',
})

const colorMode = useColorMode()

const STORAGE_KEY = STORAGE_KEYS.SETTINGS_LOCAL

const userStore = useUserStore()
const pageLoading = ref(true)
// 是否已完成初始化加载（加载完成后才启用自动保存）
const initialized = ref(false)

// 服务器端设置（嵌套结构，匹配后端 UserSettingsVO）
const defaultNotification = (): UserSettingsNotification => ({
  notifySystem: 1, notifyInteract: 1, notifyMessage: 1, notifyFollow: 1,
})
const defaultPrivacy = (): UserSettingsPrivacy => ({
  showOnlineStatus: 1, messagePermission: 0, saveViewHistory: 1,
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
const serverFontToLocal: Record<number, string> = {
  0: 'small', 1: 'medium', 2: 'large',
}
const localFontToServer: Record<string, number> = {
  small: 0, medium: 1, large: 2,
}

// 应用字体大小
const applyFontSize = () => {
  if (import.meta.client) {
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
  try {
    await userApi.updateSettings({ ...serverSettings } as any)
  } catch {
    // 自动保存静默失败
  }
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
