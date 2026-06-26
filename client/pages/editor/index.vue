<template>
  <!-- 作品编辑器 -->
  <div class="max-w-[1200px] 2xl:max-w-[1400px] mx-auto px-2 2xl:px-3 py-2">
    <!-- 标题输入 -->
    <input
      v-model="form.title"
      type="text"
      class="input text-2xl font-bold mb-3"
      placeholder="请输入作品标题..."
    />

    <!-- 摘要输入 -->
    <div class="mb-3">
      <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '摘要' }}</label>
      <textarea
        v-model="form.summary"
        class="input resize-none"
        rows="2"
        placeholder="请输入作品摘要（选填，不超过200字）..."
        maxlength="200"
      ></textarea>
      <p class="text-xs text-gray-400 mt-1 text-right">{{ form.summary.length }}/200</p>
    </div>

    <!-- 移动端/平板端：编辑/预览切换Tab -->
    <div class="md:hidden mb-4">
      <div class="flex border-b border-slate-200">
        <button
          class="flex-1 py-2 text-sm font-medium text-center transition-colors"
          :class="activeTab === 'edit' ? 'text-primary border-b-2 border-primary' : 'text-slate-500'"
          @click="activeTab = 'edit'"
        >
          {{ '编辑' }}
        </button>
        <button
          class="flex-1 py-2 text-sm font-medium text-center transition-colors"
          :class="activeTab === 'preview' ? 'text-primary border-b-2 border-primary' : 'text-slate-500'"
          @click="activeTab = 'preview'"
        >
          预览
        </button>
      </div>
    </div>

    <!-- 编辑器区域：桌面端双栏，移动端Tab切换 -->
    <div class="mb-3">
      <div class="border border-slate-300 rounded-lg overflow-hidden">
        <!-- 工具栏 -->
        <div class="flex items-center gap-0.5 p-2 bg-slate-50 border-b border-slate-300 flex-wrap">
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="加粗" @click="insertMarkdown('**', '**')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z" /></svg>
          </button>
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="斜体" @click="insertMarkdown('*', '*')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10 4h4m-2 0v16m-4 0h8" /></svg>
          </button>
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="标题" @click="insertMarkdown('## ', '')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" /></svg>
          </button>
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="链接" @click="insertMarkdown('[', '](url)')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
          </button>
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="图片" @click="insertMarkdown('![alt](', ')')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
          </button>
          <button class="p-2 text-slate-600 hover:bg-slate-200 rounded" title="代码" @click="insertMarkdown('`', '`')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" /></svg>
          </button>
          <span class="mx-1 w-px h-5 bg-slate-300"></span>
          <EmojiPicker @select="(emoji: string) => insertAtCursor(emoji)" />
          <VoiceRecorderButton @send="(blob: Blob) => handleEditorVoice(blob)" />
          <span class="mx-1 w-px h-5 bg-slate-300"></span>
          <button class="px-2 py-1.5 text-xs text-purple-600 hover:bg-purple-50 rounded transition font-medium" :disabled="aiLoading" @click="handleAIWrite('expand')" title="AI 扩写">
            <svg class="w-4 h-4 inline mr-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            AI扩写
          </button>
          <button class="px-2 py-1.5 text-xs text-indigo-600 hover:bg-indigo-50 rounded transition font-medium" :disabled="aiLoading" @click="handleAIWrite('polish')" title="AI 润色">润色</button>
          <button class="px-2 py-1.5 text-xs text-cyan-600 hover:bg-cyan-50 rounded transition font-medium" :disabled="aiLoading" @click="handleAIWrite('summarize')" title="AI 摘要">摘要</button>
          <button class="px-2 py-1.5 text-xs text-orange-600 hover:bg-orange-50 rounded transition font-medium" :disabled="aiLoading" @click="handleAIWrite('review')" title="AI 审核">审核</button>
        </div>

        <!-- 桌面端：双栏布局 -->
        <div class="hidden md:flex">
          <!-- 左侧编辑区 -->
          <div class="w-1/2 border-r border-slate-300">
            <div class="px-3 py-2 bg-slate-50 border-b border-slate-200 text-xs text-slate-500 font-medium">
              {{ '编辑' }}
            </div>
            <MentionInput
              ref="editorRef"
              v-model="form.content"
              placeholder="开始写作..."
              :rows="18"
              class="w-full min-h-[400px] p-4 bg-white text-slate-900 resize-none outline-none font-mono text-sm"
            />
          </div>
          <!-- 右侧预览区 -->
          <div class="w-1/2">
            <div class="px-3 py-2 bg-slate-50 border-b border-slate-200 text-xs text-slate-500 font-medium">
              预览
            </div>
            <div
              class="min-h-[400px] p-4 bg-white prose max-w-none overflow-auto text-sm"
              v-html="renderedContent"
            ></div>
          </div>
        </div>

        <!-- 移动端/平板端：Tab切换 -->
        <div class="md:hidden">
          <div v-show="activeTab === 'edit'">
            <MentionInput
              ref="editorRef"
              v-model="form.content"
              placeholder="开始写作..."
              :rows="18"
              class="w-full min-h-[400px] p-4 bg-white text-slate-900 resize-none outline-none font-mono text-sm"
            />
          </div>
          <div v-show="activeTab === 'preview'">
            <div
              class="min-h-[400px] p-4 bg-white prose max-w-none overflow-auto text-sm"
              v-html="renderedContent"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类和标签选择 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-2.5 md:gap-3 mb-3">
      <!-- 分类选择 -->
      <div>
        <label class="block text-xs md:text-sm font-medium text-slate-700 mb-1">{{ '分类' }}</label>
        <select v-model="form.categoryId" class="input !h-9 !text-sm !px-3">
          <option value="">{{ '请选择分类' }}</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
      </div>

      <!-- 标签选择 -->
      <div>
        <label class="block text-xs md:text-sm font-medium text-slate-700 mb-1">{{ '标签' }}</label>
        <input
          v-model="tagInput"
          type="text"
          class="input !h-9 !text-sm !px-3"
          placeholder="添加标签"
          @keydown.enter.prevent="addTag"
        />
        <div v-if="form.tags.length" class="flex flex-wrap gap-1.5 mt-1.5">
          <span v-for="tag in form.tags" :key="tag" class="badge-primary flex items-center space-x-1 !text-xs !px-2 !py-0.5">
            <span>{{ tag }}</span>
            <button class="hover:text-danger" @click="removeTag(tag)">×</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 发布位置 -->
    <div class="mb-3">
      <label class="block text-sm font-medium text-slate-700 mb-1.5">
        <span>{{ '发布位置' }}</span>
        <span class="text-xs text-slate-400 font-normal ml-1">(选填)</span>
      </label>
      <RegionSelector
        ref="regionSelectorRef"
        :locating="locating"
        :show-auto-locate="true"
        @change="onRegionChange"
        @auto-locate="autoLocate"
      />
    </div>

    <!-- 封面图上传 -->
    <div class="mb-3">
      <label class="block text-sm font-medium text-slate-700 mb-1.5">{{ '封面图' }}</label>
      <div class="flex items-center space-x-3">
        <div v-if="form.coverImage" class="relative w-40 h-28 rounded-lg overflow-hidden">
          <img :src="resolveUrl(form.coverImage) || ''" alt="封面" class="w-full h-full object-cover" />
          <button class="absolute top-1 right-1 w-6 h-6 bg-black/50 text-white rounded-full flex items-center justify-center text-xs" @click="form.coverImage = ''">×</button>
        </div>
        <label class="btn-secondary cursor-pointer">
          <span>{{ '选择图片' }}</span>
          <input type="file" accept="image/*" class="hidden" @change="handleCoverUpload" />
        </label>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex items-center justify-end space-x-3">
      <button class="btn-ghost" @click="saveDraft">{{ '保存草稿' }}</button>
      <button class="btn-primary" :disabled="!canPublish" @click="publishArticle">{{ '发布作品' }}</button>
    </div>

    <!-- 定时发布选项 -->
    <div class="mt-3 p-3 bg-slate-50 rounded-lg">
      <div class="flex items-center space-x-2">
        <label class="relative inline-flex items-center cursor-pointer">
          <input type="checkbox" v-model="scheduledPublish" class="sr-only peer" />
          <div class="w-9 h-5 bg-slate-300 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary"></div>
        </label>
        <span class="text-sm font-medium text-slate-700">{{ '定时发布' }}</span>
      </div>
      <div v-if="scheduledPublish" class="mt-3">
        <label class="block text-sm text-slate-600 mb-1">{{ '选择发布时间' }}</label>
        <input
          type="datetime-local"
          v-model="publishAt"
          class="input"
          :min="minPublishAt"
        />
        <p class="text-xs text-gray-400 mt-1">{{ '作品将在指定时间自动发布' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 作品编辑器页 */
import type { Category } from '~/types'
import { matchRegionByCoord, getRegionByIP, reverseGeocode } from '~/utils/regions'
import { aiApi } from '~/api/ai'

definePageMeta({
  middleware: 'auth',
})

const { resolveUrl } = useResourceUrl()

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
  location: '',
})

const tagInput = ref('')
const categories = ref<Category[]>([])
const editorRef = ref<any>(null)
const locating = ref(false)

// 自动定位
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
    return `<pre class="bg-slate-100 rounded-lg p-4 overflow-x-auto"><code class="language-${lang}">${code.trim()}</code></pre>`
  })

  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="bg-slate-100 px-1.5 py-0.5 rounded text-sm text-primary">$1</code>')

  // 标题
  html = html.replace(/^######\s+(.+)$/gm, '<h6 class="text-base font-semibold mt-4 mb-2">$1</h6>')
  html = html.replace(/^#####\s+(.+)$/gm, '<h5 class="text-lg font-semibold mt-4 mb-2">$1</h5>')
  html = html.replace(/^####\s+(.+)$/gm, '<h4 class="text-xl font-semibold mt-5 mb-2">$1</h4>')
  html = html.replace(/^###\s+(.+)$/gm, '<h3 class="text-2xl font-semibold mt-5 mb-3">$1</h3>')
  html = html.replace(/^##\s+(.+)$/gm, '<h2 class="text-3xl font-semibold mt-6 mb-3">$1</h2>')
  html = html.replace(/^#\s+(.+)$/gm, '<h1 class="text-4xl font-bold mt-6 mb-4">$1</h1>')

  // 引用
  html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote class="border-l-4 border-primary pl-4 py-1 my-2 text-slate-600 italic">$1</blockquote>')

  // 分割线
  html = html.replace(/^---+$/gm, '<hr class="my-6 border-slate-300" />')

  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 斜体
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // 图片
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_full: string, alt: string, src: string) => {
    // 防止 javascript: URL 注入
    if (/^\s*javascript:/i.test(src.trim())) {
      return `<img src="" alt="${escapeHtml(alt)}" class="max-w-full rounded-lg my-2" />`
    }
    const safeSrc = escapeHtml(src)
    const safeAlt = escapeHtml(alt)
    return `<img src="${safeSrc}" alt="${safeAlt}" class="max-w-full rounded-lg my-2" />`
  })

  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_full: string, text: string, href: string) => {
    // 防止 javascript: / data: URL 注入
    const trimmedHref = href.trim()
    if (/^\s*(javascript|data):/i.test(trimmedHref)) {
      return escapeHtml(text)
    }
    const safeText = escapeHtml(text)
    const safeHref = escapeHtml(trimmedHref)
    return `<a href="${safeHref}" class="text-primary hover:underline" target="_blank" rel="noopener noreferrer">${safeText}</a>`
  })

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
  // MentionInput 内部包含 textarea，支持 ref
  const el = editorRef.value
  if (!el) return
  // 尝试获取内部 textarea（MentionInput 包装的组件）
  const textarea = (el as any)?.textareaRef || (el as any)?.$el?.querySelector?.('textarea') || el
  const start = textarea.selectionStart ?? 0
  const end = textarea.selectionEnd ?? 0
  const selectedText = form.content.substring(start, end)

  form.content =
    form.content.substring(0, start) +
    prefix + selectedText + suffix +
    form.content.substring(end)

  // 恢复光标位置
  nextTick(() => {
    const ta = (el as any)?.textareaRef || (el as any)?.$el?.querySelector?.('textarea') || el
    ta?.focus()
    ta?.setSelectionRange(start + prefix.length, start + prefix.length + selectedText.length)
  })
}

// 在光标处插入文本
const insertAtCursor = (text: string) => {
  const el = editorRef.value
  if (!el) return
  const textarea = (el as any)?.textareaRef || (el as any)?.$el?.querySelector?.('textarea') || el
  const pos = textarea?.selectionStart ?? form.content.length
  form.content = form.content.substring(0, pos) + text + form.content.substring(pos)
  nextTick(() => {
    const ta = (el as any)?.textareaRef || (el as any)?.$el?.querySelector?.('textarea') || el
    ta?.focus()
    ta?.setSelectionRange(pos + text.length, pos + text.length)
  })
}

// AI 写作
const aiLoading = ref(false)
const handleAIWrite = async (mode: string) => {
  const selectedText = window.getSelection()?.toString() || form.content
  if (!selectedText.trim()) {
    showToast('请先输入或选中文本', 'error')
    return
  }
  aiLoading.value = true
  try {
    const { data } = await aiApi.generateText(selectedText, form.title.trim() || undefined, mode)
    const result = data?.data?.content || data?.data?.text || ''
    if (result) {
      form.content = result
      showToast(`AI ${mode === 'expand' ? '扩写' : mode === 'polish' ? '润色' : mode === 'summarize' ? '摘要' : '审核'}完成`)
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || 'AI 处理失败', 'error')
  } finally { aiLoading.value = false }
}

// 语音转文字
const handleEditorVoice = async (blob: Blob) => {
  try {
    const formData = new FormData()
    formData.append('file', blob, 'voice.webm')
    const { post } = useApi()
    const uploadRes = await post<any>('/files/upload/voice', formData)
    const voiceUrl = uploadRes.data?.data
    if (voiceUrl) {
      insertAtCursor(`\n[语音消息: ${voiceUrl}]\n`)
      showToast('语音已插入')
    }
  } catch { showToast('语音上传失败', 'error') }
}

/** 地区选择器引用 */
const regionSelectorRef = ref<InstanceType<typeof import('~/components/RegionSelector.vue')['default']> | null>(null)

/** 地区选择变化回调 */
const onRegionChange = (location: string) => {
  form.location = location
}

// 自动获取位置（优先浏览器GPS + 反向地理编码，实现区县级精确匹配）
const autoLocate = async () => {
  if (!import.meta.client) return
  locating.value = true
  try {
    let province = ''
    let city = ''
    let district = ''

    // 1. 优先使用浏览器 Geolocation API 获取高精度坐标
    let hasCoords = false
    let gpsLat = 0
    let gpsLng = 0
    if ('geolocation' in navigator) {
      try {
        const position = await new Promise<GeolocationPosition>((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 120000,
          })
        })
        gpsLat = position.coords.latitude
        gpsLng = position.coords.longitude
        hasCoords = true

        // 2. 使用反向地理编码获取精确的省/市/区
        const geocodeResult = await reverseGeocode(gpsLat, gpsLng)
        if (geocodeResult && geocodeResult.province) {
          province = geocodeResult.province
          city = geocodeResult.city
          district = geocodeResult.district
        }
      } catch {
        // GPS 定位失败/被拒绝，继续尝试IP定位
      }
    }

    // 3. 如果反向地理编码未获得完整结果，补充 IP 定位
    if (!hasCoords || !province) {
      const ipRegion = await getRegionByIP()
      if (ipRegion) {
        province = province || ipRegion.province
        city = city || ipRegion.city
        district = district || ipRegion.district
      }
    }

    // 4. 如果仍未获得结果，用本地坐标数据库做最后兜底
    if (!province && hasCoords) {
      const matched = matchRegionByCoord(gpsLat, gpsLng)
      if (matched) {
        province = matched.province
        city = matched.city
      }
    }

    // 5. 如果获得了位置信息，设置到选择器中
    if (province && regionSelectorRef.value) {
      regionSelectorRef.value.setRegion({ province, city, district })
      const locationDisplay = district
        ? `${province}·${city}·${district}`
        : `${province}${city ? ' · ' + city : ''}`
      showToast(`已定位到：${locationDisplay}`)
    } else {
      showToast('定位失败，请手动选择位置', 'error')
    }
  } catch {
    showToast('定位失败，请手动选择位置', 'error')
  } finally {
    locating.value = false
  }
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
  if (!form.title.trim()) {
    showToast('请输入作品标题', 'error')
    return
  }
  try {
    const { articleApi } = await import('~/api')
    const requestData = buildArticleRequest(0)
    await articleApi.createArticle(requestData)
    showToast('草稿已保存')
  } catch (error: any) {
    const msg = error.message || ''
    if (msg.includes('敏感词') || msg.includes('禁止')) {
      showToast(msg, 'error')
    } else {
      showToast(error.message || '保存失败，请稍后重试', 'error')
    }
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

/**
 * 构建发送给后端的创建文章请求数据
 * 确保字段名和类型与后端 ArticleCreateRequest 完全匹配
 */
function buildArticleRequest(status: number): Record<string, any> {
  const data: Record<string, any> = {
    title: form.title.trim(),
    content: form.content,
    summary: form.summary.trim() || undefined,
    categoryId: form.categoryId ? Number(form.categoryId) : undefined,
    tagIds: [], // 标签通过 tagIds (Long[]) 传递，当前暂不支持自由标签
    coverImage: form.coverImage || undefined,
    location: form.location || undefined,
    status,
  }
  // 清除 undefined 字段，避免发送多余的空值
  Object.keys(data).forEach((key) => {
    if (data[key] === undefined) delete data[key]
  })
  return data
}

// 发布作品
const publishArticle = async () => {
  if (!canPublish.value) return
  try {
    const { articleApi } = await import('~/api')
    const data = buildArticleRequest(1)
    data.deviceInfo = getDeviceInfo()
    // 定时发布：传递 publishAt 字段
    if (scheduledPublish.value && publishAt.value) {
      data.publishAt = new Date(publishAt.value).toISOString()
    }
    const response = await articleApi.createArticle(data)
    if (scheduledPublish.value && publishAt.value) {
      showToast('作品已设置为定时发布')
      navigateTo('/user')
    } else {
      navigateTo(`/articles/${response.data.data.id}`)
    }
  } catch (error: any) {
    const msg = error.message || ''
    if (msg.includes('敏感词') || msg.includes('禁止')) {
      showToast(msg, 'error')
    } else {
      showToast(error.message || '发布失败，请稍后重试', 'error')
    }
  }
}

// 页面加载时获取分类列表
onMounted(() => {
  loadCategories()
})

// 页面元信息
useHead({
  title: () => '创作' + ' - 知讯',
})
</script>
