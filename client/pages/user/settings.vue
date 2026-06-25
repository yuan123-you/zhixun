<template>
  <!-- 全局设置页 -->
  <div class="max-w-2xl mx-auto px-2 2xl:px-3 py-2">
    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
    </div>

    <template v-else>
    <!-- 返回导航 -->
    <div class="flex items-center gap-3 mb-3">
      <button class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-primary-400 transition-colors" @click="goBack">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        {{ '返回' }}
      </button>
      <h1 class="text-2xl font-bold text-slate-900">{{ '设置' }}</h1>
    </div>

    <!-- 推荐偏好 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '推荐偏好' }}</h2>

      <!-- 感兴趣的分类 -->
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{{ '感兴趣的分类' }}</label>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="category in availableCategories"
            :key="category.id"
            class="px-2 py-1 text-sm rounded-full border transition-colors"
            :class="serverSettings.interestedCategories.includes(category.id)
              ? 'bg-primary text-white border-primary'
              : 'bg-white text-slate-700 border-slate-300 hover:border-primary-400'"
            @click="toggleCategory(category.id, 'interested')"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <!-- 屏蔽的分类 -->
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{{ '屏蔽的分类' }}</label>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="category in availableCategories"
            :key="category.id"
            class="px-2 py-1 text-sm rounded-full border transition-colors"
            :class="serverSettings.blockedCategories.includes(category.id)
              ? 'bg-danger text-white border-danger'
              : 'bg-white text-slate-700 border-slate-300 hover:border-red-400'"
            @click="toggleCategory(category.id, 'blocked')"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <!-- 感兴趣的标签 -->
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{{ '感兴趣的标签' }}</label>
        <div class="flex flex-wrap gap-1.5">
          <span v-for="tagId in serverSettings.interestedTags" :key="tagId" class="badge-primary flex items-center space-x-1">
            <span>标签{{ tagId }}</span>
            <button @click="removeTag(tagId, 'interested')">×</button>
          </span>
          <input
            v-model="tagInput"
            type="text"
            class="input py-1 text-sm w-auto"
            placeholder="添加标签"
            @keydown.enter.prevent="addTag('interested')"
          />
        </div>
      </div>
    </section>

    <!-- 通知设置 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '通知设置' }}</h2>
      <div class="space-y-2">
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '点赞通知' }}</span>
          <input v-model="serverSettings.enableLikeNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '评论通知' }}</span>
          <input v-model="serverSettings.enableCommentNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '关注通知' }}</span>
          <input v-model="serverSettings.enableFollowNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '系统通知' }}</span>
          <input v-model="serverSettings.enableSystemNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 隐私设置 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '隐私设置' }}</h2>
      <div class="space-y-2">
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '显示在线状态' }}</span>
          <input v-model="serverSettings.showOnlineStatus" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '允许陌生人私信' }}</span>
          <input v-model="serverSettings.allowStrangerMessage" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ '显示浏览历史' }}</span>
          <input v-model="serverSettings.showViewHistory" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 显示设置 -->
    <section class="card p-3 mb-3">
      <h2 class="text-lg font-semibold text-slate-900 mb-2">{{ '显示设置' }}</h2>
      <div class="space-y-3">
        <!-- 主题 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{{ '主题' }}</label>
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
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{{ '字体大小' }}</label>
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

    <!-- 保存按钮 -->
    <div class="flex justify-end">
      <button class="btn-primary" :disabled="saving" @click="saveSettings">
        {{ saving ? '保存中...' : '保存设置' }}
      </button>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 全局设置页 */
import type { UserSettingsServer, UserSettingsLocal, Category } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'
import { userApi } from '~/api'

definePageMeta({
  middleware: 'auth',
})

const router = useRouter()

// Toast 提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  if (!import.meta.client) return
  const toast = document.createElement('div')
  toast.className = `fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-white text-sm transition-all duration-300 transform translate-x-0 opacity-100 ${
    type === 'success' ? 'bg-green-500' : 'bg-red-500'
  }`
  toast.textContent = message
  document.body.appendChild(toast)
  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateX(100%)'
    setTimeout(() => toast.remove(), 300)
  }, 2000)
}

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}

const colorMode = useColorMode()

// 页面加载状态
const pageLoading = ref(true)

// 服务器端设置（通知/隐私）
const serverSettings = reactive<UserSettingsServer>({
  interestedCategories: [],
  interestedTags: [],
  blockedCategories: [],
  blockedTags: [],
  enableLikeNotification: true,
  enableCommentNotification: true,
  enableFollowNotification: true,
  enableSystemNotification: true,
  showOnlineStatus: true,
  allowStrangerMessage: true,
  showViewHistory: true,
})

// 本地设置（主题/字体，仅存 localStorage）
const localSettings = reactive<UserSettingsLocal>(
  storage.get<UserSettingsLocal>(STORAGE_KEYS.SETTINGS_LOCAL) || {
    theme: 'system',
    fontSize: 'medium',
    language: 'zh-CN',
  }
)

const availableCategories = ref<Category[]>([])
const tagInput = ref('')
const saving = ref(false)

const themes = [
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' },
  { label: '跟随系统', value: 'system' },
]

const fontSizes = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]

// 字体大小映射
const fontSizeMap: Record<string, string> = {
  small: '14px',
  medium: '16px',
  large: '18px',
}

// 应用字体大小
const applyFontSize = () => {
  if (import.meta.client) {
    document.documentElement.style.setProperty('font-size', fontSizeMap[localSettings.fontSize] || '16px')
  }
}

// 加载服务器设置
const loadServerSettings = async () => {
  try {
    const { data } = await userApi.getSettings()
    const settings = data.data
    if (settings) {
      Object.assign(serverSettings, {
        interestedCategories: settings.interestedCategories || [],
        interestedTags: settings.interestedTags || [],
        blockedCategories: settings.blockedCategories || [],
        blockedTags: settings.blockedTags || [],
        enableLikeNotification: settings.enableLikeNotification ?? true,
        enableCommentNotification: settings.enableCommentNotification ?? true,
        enableFollowNotification: settings.enableFollowNotification ?? true,
        enableSystemNotification: settings.enableSystemNotification ?? true,
        showOnlineStatus: settings.showOnlineStatus ?? true,
        allowStrangerMessage: settings.allowStrangerMessage ?? true,
        showViewHistory: settings.showViewHistory ?? true,
      })
    }
  } catch {
    // 加载失败使用默认值
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const { get } = useApi()
    const response = await get<Category[]>('/categories')
    availableCategories.value = response.data.data || []
  } catch {
    // 分类加载失败，静默处理
  }
}

// 切换分类
const toggleCategory = (categoryId: number, type: 'interested' | 'blocked') => {
  const key = type === 'interested' ? 'interestedCategories' : 'blockedCategories'
  const index = serverSettings[key].indexOf(categoryId)
  if (index > -1) {
    serverSettings[key].splice(index, 1)
  } else {
    serverSettings[key].push(categoryId)
  }
}

// 添加标签
const addTag = (type: 'interested' | 'blocked') => {
  if (!tagInput.value.trim()) return
  const key = type === 'interested' ? 'interestedTags' : 'blockedTags'
  const tagId = Number(tagInput.value.trim())
  if (isNaN(tagId)) {
    showToast('请输入有效的标签ID', 'error')
    return
  }
  if (!serverSettings[key].includes(tagId)) {
    serverSettings[key].push(tagId)
  }
  tagInput.value = ''
}

// 移除标签
const removeTag = (tagId: number, type: 'interested' | 'blocked') => {
  const key = type === 'interested' ? 'interestedTags' : 'blockedTags'
  serverSettings[key] = serverSettings[key].filter((id) => id !== tagId)
}

// 保存本地设置（主题/字体）
const saveLocalSettings = () => {
  storage.set(STORAGE_KEYS.SETTINGS_LOCAL, { ...localSettings })
  // 应用主题
  if (localSettings.theme === 'system') {
    colorMode.preference = 'system'
  } else {
    colorMode.preference = localSettings.theme
  }
  // 应用字体大小
  applyFontSize()
}

// 保存服务器设置（通知/隐私）
const saveServerSettings = async () => {
  saving.value = true
  try {
    await userApi.updateSettings(serverSettings)
    showToast('设置保存成功')
  } catch {
    showToast('设置保存失败，请稍后重试', 'error')
  } finally {
    saving.value = false
  }
}

// 保存所有设置
const saveSettings = async () => {
  saveLocalSettings()
  await saveServerSettings()
}

// 页面初始化
onMounted(async () => {
  await Promise.all([
    loadServerSettings(),
    loadCategories(),
  ])
  applyFontSize()
  pageLoading.value = false
})

// 页面元信息
useHead({
  title: () => '设置' + ' - 知讯',
})
</script>
