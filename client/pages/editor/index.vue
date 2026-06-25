<template>
  <!-- 文章编辑器 -->
  <div class="max-w-7xl mx-auto px-4 py-6">
    <!-- 返回导航 -->
    <div class="flex items-center gap-3 mb-6">
      <button class="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400 hover:text-primary dark:hover:text-primary-400 transition-colors" @click="goBack">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        {{ t('common.back') }}
      </button>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('nav.write') }}</h1>
    </div>

    <!-- 标题输入 -->
    <input
      v-model="form.title"
      type="text"
      class="input text-2xl font-bold mb-6"
      :placeholder="t('article.enterTitle')"
    />

    <!-- 摘要输入 -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('article.summary') }}</label>
      <textarea
        v-model="form.summary"
        class="input resize-none"
        rows="2"
        :placeholder="t('article.enterSummary')"
        maxlength="200"
      ></textarea>
      <p class="text-xs text-gray-400 mt-1 text-right">{{ form.summary.length }}/200</p>
    </div>

    <!-- 移动端/平板端：编辑/预览切换Tab -->
    <div class="md:hidden mb-4">
      <div class="flex border-b border-gray-200 dark:border-gray-700">
        <button
          class="flex-1 py-2 text-sm font-medium text-center transition-colors"
          :class="activeTab === 'edit' ? 'text-primary border-b-2 border-primary' : 'text-gray-500 dark:text-gray-400'"
          @click="activeTab = 'edit'"
        >
          {{ t('common.edit') }}
        </button>
        <button
          class="flex-1 py-2 text-sm font-medium text-center transition-colors"
          :class="activeTab === 'preview' ? 'text-primary border-b-2 border-primary' : 'text-gray-500 dark:text-gray-400'"
          @click="activeTab = 'preview'"
        >
          预览
        </button>
      </div>
    </div>

    <!-- 编辑器区域：桌面端双栏，移动端Tab切换 -->
    <div class="mb-6">
      <div class="border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
        <!-- 工具栏 -->
        <div class="flex items-center space-x-1 p-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-300 dark:border-gray-600">
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="加粗" @click="insertMarkdown('**', '**')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="斜体" @click="insertMarkdown('*', '*')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10 4h4m-2 0v16m-4 0h8" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="标题" @click="insertMarkdown('## ', '')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="链接" @click="insertMarkdown('[', '](url)')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="图片" @click="insertMarkdown('![alt](', ')')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          </button>
          <button class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded" title="代码" @click="insertMarkdown('`', '`')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" /></svg>
          </button>
        </div>

        <!-- 桌面端：双栏布局 -->
        <div class="hidden md:flex">
          <!-- 左侧编辑区 -->
          <div class="w-1/2 border-r border-gray-300 dark:border-gray-600">
            <div class="px-3 py-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600 text-xs text-gray-500 dark:text-gray-400 font-medium">
              {{ t('common.edit') }}
            </div>
            <textarea
              ref="editorRef"
              v-model="form.content"
              class="w-full min-h-[400px] p-4 bg-white dark:bg-gray-800 text-gray-900 dark:text-white resize-none outline-none font-mono text-sm"
              :placeholder="t('article.enterContent')"
            ></textarea>
          </div>
          <!-- 右侧预览区 -->
          <div class="w-1/2">
            <div class="px-3 py-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600 text-xs text-gray-500 dark:text-gray-400 font-medium">
              预览
            </div>
            <div
              class="min-h-[400px] p-4 bg-white dark:bg-gray-800 prose dark:prose-invert max-w-none overflow-auto text-sm"
              v-html="renderedContent"
            ></div>
          </div>
        </div>

        <!-- 移动端/平板端：Tab切换 -->
        <div class="md:hidden">
          <div v-show="activeTab === 'edit'">
            <textarea
              ref="editorRef"
              v-model="form.content"
              class="w-full min-h-[400px] p-4 bg-white dark:bg-gray-800 text-gray-900 dark:text-white resize-none outline-none font-mono text-sm"
              :placeholder="t('article.enterContent')"
            ></textarea>
          </div>
          <div v-show="activeTab === 'preview'">
            <div
              class="min-h-[400px] p-4 bg-white dark:bg-gray-800 prose dark:prose-invert max-w-none overflow-auto text-sm"
              v-html="renderedContent"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类和标签选择 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <!-- 分类选择 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('article.category') }}</label>
        <select v-model="form.categoryId" class="input">
          <option value="">{{ t('article.noCategory') }}</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
      </div>

      <!-- 标签选择 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('article.tags') }}</label>
        <input
          v-model="tagInput"
          type="text"
          class="input"
          :placeholder="t('settings.addTag')"
          @keydown.enter.prevent="addTag"
        />
        <div v-if="form.tags.length" class="flex flex-wrap gap-2 mt-2">
          <span v-for="tag in form.tags" :key="tag" class="badge-primary flex items-center space-x-1">
            <span>{{ tag }}</span>
            <button class="hover:text-danger" @click="removeTag(tag)">×</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 封面图上传 -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('article.coverImage') }}</label>
      <div class="flex items-center space-x-4">
        <div v-if="form.coverImage" class="relative w-40 h-28 rounded-lg overflow-hidden">
          <img :src="resolveUrl(form.coverImage) || ''" alt="封面" class="w-full h-full object-cover" />
          <button class="absolute top-1 right-1 w-6 h-6 bg-black/50 text-white rounded-full flex items-center justify-center text-xs" @click="form.coverImage = ''">×</button>
        </div>
        <label class="btn-secondary cursor-pointer">
          <span>{{ t('article.selectImage') }}</span>
          <input type="file" accept="image/*" class="hidden" @change="handleCoverUpload" />
        </label>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex items-center justify-end space-x-4">
      <button class="btn-ghost" @click="saveDraft">{{ t('article.saveDraft') }}</button>
      <button class="btn-primary" :disabled="!canPublish" @click="publishArticle">{{ t('article.publishArticle') }}</button>
    </div>

    <!-- 定时发布选项 -->
    <div class="mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
      <div class="flex items-center space-x-3">
        <label class="relative inline-flex items-center cursor-pointer">
          <input type="checkbox" v-model="scheduledPublish" class="sr-only peer" />
          <div class="w-9 h-5 bg-gray-300 dark:bg-gray-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary"></div>
        </label>
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ t('article.scheduledPublishLabel') }}</span>
      </div>
      <div v-if="scheduledPublish" class="mt-3">
        <label class="block text-sm text-gray-600 dark:text-gray-400 mb-1">{{ t('article.selectPublishTime') }}</label>
        <input
          type="datetime-local"
          v-model="publishAt"
          class="input"
          :min="minPublishAt"
        />
        <p class="text-xs text-gray-400 mt-1">{{ t('article.scheduledPublishHint') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 文章编辑器页 */
import type { Category } from '~/types'

definePageMeta({
  middleware: 'auth',
})

const router = useRouter()
const { resolveUrl } = useResourceUrl()
const { t } = useI18n()

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    navigateTo('/')
  }
}

// 移动端Tab状态
const activeTab = ref<'edit' | 'preview'>('edit')

// 表单数据
const form = reactive({
  title: '',
  content: '',
  summary: '',
  categoryId: '',
  tags: [] as string[],
  coverImage: '',
})

const tagInput = ref('')
const categories = ref<Category[]>([])
const editorRef = ref<HTMLTextAreaElement | null>(null)

// 定时发布相关
const scheduledPublish = ref(false)
const publishAt = ref('')

// 最小可选时间（当前时间，格式化为 datetime-local 需要的格式）
const minPublishAt = computed(() => {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  const local = new Date(now.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
})

// 是否可以发布
const canPublish = computed(() => {
  return form.title.trim() && form.content.trim() && form.categoryId
})

// 简易 Markdown 渲染
const renderedContent = computed(() => {
  return renderMarkdown(form.content)
})

/**
 * 基础 Markdown 渲染函数
 * 支持：标题、加粗、斜体、行内代码、代码块、链接、图片、列表、引用、分割线、段落
 */
function renderMarkdown(text: string): string {
  if (!text) return '<p class="text-gray-400">预览区域 — 开始编辑后内容将在此处实时显示</p>'

  let html = escapeHtml(text)

  // 代码块（需先处理，避免内部被其他规则替换）
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    return `<pre class="bg-gray-100 dark:bg-gray-900 rounded-lg p-4 overflow-x-auto"><code class="language-${lang}">${code.trim()}</code></pre>`
  })

  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="bg-gray-100 dark:bg-gray-900 px-1.5 py-0.5 rounded text-sm text-primary">$1</code>')

  // 标题
  html = html.replace(/^######\s+(.+)$/gm, '<h6 class="text-base font-semibold mt-4 mb-2">$1</h6>')
  html = html.replace(/^#####\s+(.+)$/gm, '<h5 class="text-lg font-semibold mt-4 mb-2">$1</h5>')
  html = html.replace(/^####\s+(.+)$/gm, '<h4 class="text-xl font-semibold mt-5 mb-2">$1</h4>')
  html = html.replace(/^###\s+(.+)$/gm, '<h3 class="text-2xl font-semibold mt-5 mb-3">$1</h3>')
  html = html.replace(/^##\s+(.+)$/gm, '<h2 class="text-3xl font-semibold mt-6 mb-3">$1</h2>')
  html = html.replace(/^#\s+(.+)$/gm, '<h1 class="text-4xl font-bold mt-6 mb-4">$1</h1>')

  // 引用
  html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote class="border-l-4 border-primary pl-4 py-1 my-2 text-gray-600 dark:text-gray-400 italic">$1</blockquote>')

  // 分割线
  html = html.replace(/^---+$/gm, '<hr class="my-6 border-gray-300 dark:border-gray-600" />')

  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 斜体
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // 图片
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" class="max-w-full rounded-lg my-2" />')

  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="text-primary hover:underline" target="_blank" rel="noopener">$1</a>')

  // 无序列表
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li class="ml-4 list-disc">$1</li>')
  html = html.replace(/((?:<li class="ml-4 list-disc">.*<\/li>\n?)+)/g, '<ul class="my-2">$1</ul>')

  // 有序列表
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li class="ml-4 list-decimal">$1</li>')

  // 段落：将连续非标签行包裹为 <p>
  html = html.replace(/^(?!<[hublop]|<li|<hr|<pre|<blockquote)(.+)$/gm, '<p class="my-2">$1</p>')

  // 清理多余换行
  html = html.replace(/\n{2,}/g, '\n')

  return html
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// 自定义 toast 提示
function showToast(message: string, type: 'success' | 'error' = 'success') {
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

// 加载分类列表
const loadCategories = async () => {
  try {
    const { get } = useApi()
    const response = await get<Category[]>('/categories')
    categories.value = response.data.data || []
  } catch {
    // 分类加载失败，静默处理
  }
}

// 添加标签
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !form.tags.includes(tag)) {
    form.tags.push(tag)
  }
  tagInput.value = ''
}

// 移除标签
const removeTag = (tag: string) => {
  form.tags = form.tags.filter((t) => t !== tag)
}

// 插入Markdown语法
const insertMarkdown = (prefix: string, suffix: string) => {
  const textarea = editorRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.content.substring(start, end)

  form.content =
    form.content.substring(0, start) +
    prefix + selectedText + suffix +
    form.content.substring(end)

  // 恢复光标位置
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, start + prefix.length + selectedText.length)
  })
}

// 封面图上传
const handleCoverUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  // TODO: 调用上传接口
  const reader = new FileReader()
  reader.onload = (e) => {
    form.coverImage = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

// 保存草稿
const saveDraft = async () => {
  try {
    const { articleApi } = await import('~/api')
    await articleApi.createArticle({ ...form, status: 0 } as any)
    showToast(t('article.draftSaved'))
  } catch (error: any) {
    showToast(error.message || t('article.saveDraftFailed'), 'error')
  }
}

// 检测设备信息
const getDeviceInfo = (): string | undefined => {
  if (!import.meta.client) return undefined
  const ua = navigator.userAgent
  // 提取设备型号
  const mobileMatch = ua.match(/\b(Android|iPhone|iPad|iPod)\b[^;]*/i)
  if (mobileMatch) {
    // 尝试提取更详细的Android设备型号
    const androidBuild = ua.match(/;\s*([^;)]+)\s+Build\//)
    if (androidBuild) return androidBuild[1].trim()
    // iPhone/iPad
    const iosMatch = ua.match(/(iPhone|iPad|iPod)(?:\s*OS\s*[\d_]+)?/i)
    if (iosMatch) return iosMatch[0].replace(/_/g, '.')
    return mobileMatch[0]
  }
  // 桌面端
  if (ua.includes('Windows')) return 'Windows'
  if (ua.includes('Mac OS')) return 'macOS'
  if (ua.includes('Linux')) return 'Linux'
  return undefined
}

// 发布文章
const publishArticle = async () => {
  if (!canPublish.value) return
  try {
    const { articleApi } = await import('~/api')
    const data: any = { ...form, status: 1, deviceInfo: getDeviceInfo() }
    // 定时发布：传递 publishAt 字段
    if (scheduledPublish.value && publishAt.value) {
      data.publishAt = new Date(publishAt.value).toISOString()
    }
    const response = await articleApi.createArticle(data)
    if (scheduledPublish.value && publishAt.value) {
      showToast(t('article.scheduledPublish'))
      navigateTo('/user')
    } else {
      navigateTo(`/articles/${response.data.data.id}`)
    }
  } catch (error: any) {
    showToast(error.message || t('article.publishFailed'), 'error')
  }
}

// 页面加载时获取分类列表
onMounted(() => {
  loadCategories()
})

// 页面元信息
useHead({
  title: () => t('nav.write') + ' - 知讯',
})
</script>
