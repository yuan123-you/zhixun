/**
 * Markdown 渲染工具
 * 支持：标题、加粗、斜体、行内代码、代码块、链接、图片、列表、引用、分割线、段落
 */

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const XSS_PATTERNS = {
  jsUri: /^\s*javascript:/i,
  dangerousUri: /^\s*(javascript|data):/i,
}

export function renderMarkdown(
  text: string,
  fallbackMessage: string = '预览区域 — 开始编辑后内容将在此处实时显示',
): string {
  if (!text) return `<p class="text-[var(--zh-text-tertiary)]">${fallbackMessage}</p>`

  let html = escapeHtml(text)

  // 代码块（需先处理，避免内部被其他规则替换）
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) =>
    `<pre class="bg-slate-100 rounded-lg p-4 overflow-x-auto"><code class="language-${lang}">${code.trim()}</code></pre>`)

  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="bg-slate-100 px-1.5 py-0.5 rounded text-sm text-primary">$1</code>')

  // 标题
  const headingRules: [RegExp, string][] = [
    [/^######\s+(.+)$/gm, '<h6 class="text-base font-semibold mt-4 mb-2">$1</h6>'],
    [/^#####\s+(.+)$/gm, '<h5 class="text-lg font-semibold mt-4 mb-2">$1</h5>'],
    [/^####\s+(.+)$/gm, '<h4 class="text-xl font-semibold mt-5 mb-2">$1</h4>'],
    [/^###\s+(.+)$/gm, '<h3 class="text-2xl font-semibold mt-5 mb-3">$1</h3>'],
    [/^##\s+(.+)$/gm, '<h2 class="text-3xl font-semibold mt-6 mb-3">$1</h2>'],
    [/^#\s+(.+)$/gm, '<h1 class="text-4xl font-bold mt-6 mb-4">$1</h1>'],
  ]
  headingRules.forEach(([regex, template]) => {
    html = html.replace(regex, template)
  })

  // 引用
  html = html.replace(/^&gt;\s+(.+)$/gm, '<blockquote class="border-l-4 border-primary pl-4 py-1 my-2 text-[var(--zh-text-secondary)] italic">$1</blockquote>')
  // 分割线
  html = html.replace(/^---+$/gm, '<hr class="my-6 border-[var(--zh-border)]" />')
  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // 斜体
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // 图片（防 XSS）
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_full: string, alt: string, src: string) => {
    if (XSS_PATTERNS.jsUri.test(src.trim())) {
      return `<img src="" alt="${escapeHtml(alt)}" class="max-w-full rounded-lg my-2" />`
    }
    return `<img src="${escapeHtml(src)}" alt="${escapeHtml(alt)}" class="max-w-full rounded-lg my-2" />`
  })

  // 链接（防 XSS）
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_full: string, text: string, href: string) => {
    const trimmedHref = href.trim()
    if (XSS_PATTERNS.dangerousUri.test(trimmedHref)) {
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
  html = html.replace(/^(?!<[hublop]|<li|<hr|<pre|<blockquote)(.+)$/gm, '<p class="my-2">$1</p>')
  // 清理多余换行
  html = html.replace(/\n{2,}/g, '\n')

  return html
}
