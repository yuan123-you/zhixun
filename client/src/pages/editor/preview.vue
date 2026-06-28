<template>
  <!-- 预览页（与编辑页分离）：仅显示预览区占位 -->
  <div class="editor-preview-area h-full min-h-[360px] p-4 bg-[var(--zh-bg-elevated)] overflow-auto text-sm flex flex-col">
    <!-- 空状态预览引导 -->
    <div v-if="!title && !content" class="editor-preview-empty flex flex-col items-center justify-center h-full min-h-[360px] text-[var(--zh-text-tertiary)]">
      <svg class="w-16 h-16 mb-4 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
      </svg>
      <p class="text-sm">在左侧输入内容，这里将实时预览</p>
    </div>
    <!-- 预览标题与正文 -->
    <template v-else>
      <h1 v-if="title" class="text-2xl font-bold text-[var(--zh-text)] mb-3 pb-2 border-b border-[var(--zh-border-light)]">{{ title }}</h1>
      <div v-if="content" class="prose max-w-none" v-html="renderedContent"></div>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 简化预览页 - 仅展示 editor-preview-area 模块 */
import { sanitizeHtml } from '@/utils/sanitize'

const router = useRouter()
const { resolveUrl } = useResourceUrl()

const title = ref('')
const content = ref('')
const summary = ref('')
const images = ref<string[]>([])
const tags = ref<string[]>([])
const location = ref('')

onMounted(() => {
  if (true) {
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
      }
    } catch { /* ignore */ }
  }
})

// Markdown 渲染（使用 DOMPurify 净化）
const renderedContent = computed(() => {
  if (!content.value) return '<p class="text-[var(--zh-text-tertiary)] italic">暂无内容</p>'
  return sanitizeHtml(renderMarkdown(content.value))
})

function renderMarkdown(text: string): string {
  if (!text) return ''

  let html = text

  // 代码块（保留原始内容，后续由 DOMPurify 净化）
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    return `<pre class="bg-[var(--zh-bg)] rounded-lg p-4 overflow-x-auto my-3 border border-[var(--zh-border)]"><code class="language-${lang} text-[var(--zh-text)]">${escapeHtml(code.trim())}</code></pre>`
  })

  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="bg-[var(--zh-bg-hover)] px-1.5 py-0.5 rounded text-sm text-[var(--zh-primary)]">$1</code>')

  // 标题
  html = html.replace(/^######\s+(.+)$/gm, '<h6 class="text-base font-semibold mt-4 mb-2 text-[var(--zh-text)]">$1</h6>')
  html = html.replace(/^#####\s+(.+)$/gm, '<h5 class="text-lg font-semibold mt-4 mb-2 text-[var(--zh-text)]">$1</h5>')
  html = html.replace(/^####\s+(.+)$/gm, '<h4 class="text-xl font-semibold mt-5 mb-2 text-[var(--zh-text)]">$1</h4>')
  html = html.replace(/^###\s+(.+)$/gm, '<h3 class="text-2xl font-semibold mt-5 mb-3 text-[var(--zh-text)]">$1</h3>')
  html = html.replace(/^##\s+(.+)$/gm, '<h2 class="text-3xl font-semibold mt-6 mb-3 text-[var(--zh-text)]">$1</h2>')
  html = html.replace(/^#\s+(.+)$/gm, '<h1 class="text-4xl font-bold mt-6 mb-4 text-[var(--zh-text)]">$1</h1>')

  // 引用
  html = html.replace(/^>\s+(.+)$/gm, '<blockquote class="border-l-4 border-[var(--zh-primary)] pl-4 py-1 my-2 text-[var(--zh-text-secondary)] italic">$1</blockquote>')

  // 分割线
  html = html.replace(/^---+$/gm, '<hr class="my-6 border-[var(--zh-border)]" />')

  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong class="text-[var(--zh-text)]">$1</strong>')

  // 斜体
  html = html.replace(/\*(.+?)\*/g, '<em class="text-[var(--zh-text-secondary)]">$1</em>')

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
    return `<a href="${escapeHtml(trimmedHref)}" class="text-[var(--zh-primary)] hover:underline" target="_blank" rel="noopener noreferrer">${escapeHtml(text)}</a>`
  })

  // 无序列表
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li class="ml-4 list-disc text-[var(--zh-text)]">$1</li>')
  html = html.replace(/((?:<li class="ml-4 list-disc">.*<\/li>\n?)+)/g, '<ul class="my-2">$1</ul>')

  // 有序列表
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li class="ml-4 list-decimal text-[var(--zh-text)]">$1</li>')

  // 段落
  html = html.replace(/^(?!<[hublop]|<li|<hr|<pre|<blockquote)(.+)$/gm, '<p class="my-2 leading-relaxed text-[var(--zh-text)]">$1</p>')

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
</script>
