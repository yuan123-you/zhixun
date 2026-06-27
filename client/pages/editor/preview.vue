<template>
  <!-- 编辑器预览页面 - 纯净预览，无编辑功能 -->
  <div class="max-w-[800px] 2xl:max-w-[900px] mx-auto px-3 py-4">
    <!-- 顶部信息栏 -->
    <div class="flex items-center justify-between mb-4">
      <button class="flex items-center gap-1 text-sm text-slate-500 hover:text-primary transition-colors" @click="goBack">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        返回编辑
      </button>
      <span class="text-xs text-slate-400 bg-slate-100 px-2 py-1 rounded">预览模式</span>
    </div>

    <!-- 文章标题 -->
    <h1 v-if="title" class="text-3xl font-bold text-slate-900 mb-4">{{ title }}</h1>

    <!-- 摘要 -->
    <div v-if="summary" class="mb-6 p-3 bg-slate-50 rounded-lg border border-slate-200">
      <p class="text-sm text-slate-600 leading-relaxed">{{ summary }}</p>
    </div>

    <!-- 正文（Markdown 渲染） -->
    <div class="prose max-w-none mb-6" v-html="renderedContent"></div>

    <!-- 图片网格 -->
    <div v-if="images.length > 0" class="mb-6">
      <div class="grid gap-1" :class="images.length === 1 ? 'grid-cols-1' : images.length === 2 || images.length === 4 ? 'grid-cols-2' : 'grid-cols-3'">
        <div
          v-for="(img, idx) in images"
          :key="idx"
          :class="images.length === 1 ? '' : 'aspect-square'"
          class="overflow-hidden rounded-lg"
        >
          <img :src="resolveUrl(img) || img" alt="" class="w-full h-full object-cover" />
        </div>
      </div>
    </div>

    <!-- 发布位置 -->
    <div v-if="location" class="flex items-center gap-1 text-xs text-slate-400 mb-4">
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      {{ location }}
    </div>

    <!-- 标签 -->
    <div v-if="tags.length > 0" class="flex flex-wrap gap-1.5 mb-6">
      <span v-for="tag in tags" :key="tag" class="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">{{ tag }}</span>
    </div>

    <!-- 分隔线 -->
    <hr class="my-6 border-slate-200" />

    <!-- 返回编辑 -->
    <div class="flex justify-center">
      <button class="btn-primary text-sm" @click="goBack">返回编辑</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 纯净预览页 - 不包含编辑工具栏和选项 */
definePageMeta({
  middleware: 'auth',
})

const { resolveUrl } = useResourceUrl()

// 从 sessionStorage 读取预览数据
const title = ref('')
const content = ref('')
const summary = ref('')
const images = ref<string[]>([])
const tags = ref<string[]>([])
const location = ref('')

onMounted(() => {
  if (import.meta.client) {
    try {
      const data = sessionStorage.getItem('editor_preview_data')
      if (data) {
        const parsed = JSON.parse(data)
        title.value = parsed.title || ''
        content.value = parsed.content || ''
        summary.value = parsed.summary || ''
        images.value = parsed.images || []
        tags.value = parsed.tags || []
        location.value = parsed.location || ''
        // 清除数据
        sessionStorage.removeItem('editor_preview_data')
      }
    } catch {
      // ignore
    }
  }
})

// Markdown 渲染
const renderedContent = computed(() => {
  if (!content.value) return '<p class="text-gray-400 italic">暂无内容</p>'
  return renderMarkdown(content.value)
})

function renderMarkdown(text: string): string {
  if (!text) return ''

  let html = escapeHtml(text)

  // 代码块
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    return `<pre class="bg-slate-100 rounded-lg p-4 overflow-x-auto my-3"><code class="language-${lang}">${code.trim()}</code></pre>`
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
    if (/^\s*javascript:/i.test(src.trim())) {
      return `<img src="" alt="${escapeHtml(alt)}" class="max-w-full rounded-lg my-2" />`
    }
    return `<img src="${escapeHtml(src)}" alt="${escapeHtml(alt)}" class="max-w-full rounded-lg my-2" />`
  })

  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_full: string, text: string, href: string) => {
    const trimmedHref = href.trim()
    if (/^\s*(javascript|data):/i.test(trimmedHref)) {
      return escapeHtml(text)
    }
    return `<a href="${escapeHtml(trimmedHref)}" class="text-primary hover:underline" target="_blank" rel="noopener noreferrer">${escapeHtml(text)}</a>`
  })

  // 无序列表
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li class="ml-4 list-disc">$1</li>')
  html = html.replace(/((?:<li class="ml-4 list-disc">.*<\/li>\n?)+)/g, '<ul class="my-2">$1</ul>')

  // 有序列表
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li class="ml-4 list-decimal">$1</li>')

  // 段落
  html = html.replace(/^(?!<[hublop]|<li|<hr|<pre|<blockquote)(.+)$/gm, '<p class="my-2 leading-relaxed">$1</p>')

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

function goBack() {
  navigateTo('/editor')
}
</script>
