<template>
  <!-- 全局设置页 -->
  <div class="max-w-2xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">设置</h1>

    <!-- 推荐偏好 -->
    <section class="card p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">推荐偏好</h2>

      <!-- 感兴趣的分类 -->
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">感兴趣的分类</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="category in availableCategories"
            :key="category.id"
            class="px-3 py-1.5 text-sm rounded-full border transition-colors"
            :class="serverSettings.interestedCategories.includes(category.id)
              ? 'bg-primary text-white border-primary'
              : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:border-primary'"
            @click="toggleCategory(category.id, 'interested')"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <!-- 屏蔽的分类 -->
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">屏蔽的分类</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="category in availableCategories"
            :key="category.id"
            class="px-3 py-1.5 text-sm rounded-full border transition-colors"
            :class="serverSettings.blockedCategories.includes(category.id)
              ? 'bg-danger text-white border-danger'
              : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:border-danger'"
            @click="toggleCategory(category.id, 'blocked')"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <!-- 感兴趣的标签 -->
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">感兴趣的标签</label>
        <div class="flex flex-wrap gap-2">
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
    <section class="card p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">通知设置</h2>
      <div class="space-y-3">
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">点赞通知</span>
          <input v-model="serverSettings.enableLikeNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">评论通知</span>
          <input v-model="serverSettings.enableCommentNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">关注通知</span>
          <input v-model="serverSettings.enableFollowNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">系统通知</span>
          <input v-model="serverSettings.enableSystemNotification" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 隐私设置 -->
    <section class="card p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">隐私设置</h2>
      <div class="space-y-3">
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">显示在线状态</span>
          <input v-model="serverSettings.showOnlineStatus" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">允许陌生人私信</span>
          <input v-model="serverSettings.allowStrangerMessage" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
        <label class="flex items-center justify-between">
          <span class="text-sm text-gray-700 dark:text-gray-300">显示浏览历史</span>
          <input v-model="serverSettings.showViewHistory" type="checkbox" class="w-5 h-5 text-primary rounded" />
        </label>
      </div>
    </section>

    <!-- 显示设置 -->
    <section class="card p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">显示设置</h2>
      <div class="space-y-4">
        <!-- 主题 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">主题</label>
          <div class="flex space-x-2">
            <button
              v-for="theme in themes"
              :key="theme.value"
              class="px-4 py-2 text-sm rounded-lg border transition-colors"
              :class="localSettings.theme === theme.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600'"
              @click="localSettings.theme = theme.value"
            >
              {{ theme.label }}
            </button>
          </div>
        </div>

        <!-- 字体大小 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">字体大小</label>
          <div class="flex space-x-2">
            <button
              v-for="size in fontSizes"
              :key="size.value"
              class="px-4 py-2 text-sm rounded-lg border transition-colors"
              :class="localSettings.fontSize === size.value
                ? 'bg-primary text-white border-primary'
                : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600'"
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
  </div>
</template>

<script setup lang="ts">
/** 全局设置页 */
import type { UserSettingsServer, UserSettingsLocal, Category } from '~/types'
import { storage, STORAGE_KEYS } from '~/utils/storage'

definePageMeta({
  middleware: 'auth',
})

const colorMode = useColorMode()

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

// 本地设置（主题/字体/语言，仅存 localStorage）
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

// 保存本地设置（主题/字体/语言）
const saveLocalSettings = () => {
  storage.set(STORAGE_KEYS.SETTINGS_LOCAL, { ...localSettings })
  // 应用主题
  if (localSettings.theme !== 'system') {
    colorMode.preference = localSettings.theme
  }
}

// 保存服务器设置（通知/隐私）
const saveServerSettings = async () => {
  saving.value = true
  try {
    const { userApi } = await import('~/api')
    await userApi.updateSettings(serverSettings)
  } catch {
    // 保存失败处理
  } finally {
    saving.value = false
  }
}

// 保存所有设置
const saveSettings = async () => {
  saveLocalSettings()
  await saveServerSettings()
}

// 页面元信息
useHead({
  title: '设置 - 知讯',
})
</script>
