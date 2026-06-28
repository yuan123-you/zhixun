import DOMPurify from 'dompurify'

/** DOMPurify 配置：允许常见的富文本标签和属性 */
const ALLOWED_TAGS = [
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'del',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
  'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'div', 'span', 'hr', 'sub', 'sup',
]

const ALLOWED_ATTR = [
  'href', 'target', 'rel', 'src', 'alt', 'title',
  'class', 'id', 'style', 'width', 'height',
  'colspan', 'rowspan',
]

/** 创建 DOMPurify 实例 */
const purify = DOMPurify

/**
 * 净化 HTML 内容，防止 XSS 攻击
 * 用于 v-html 渲染前的安全处理
 */
export function sanitizeHtml(html: string): string {
  if (!html) return ''
  return purify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
  })
}

/**
 * 净化纯文本内容，移除所有 HTML 标签
 * 用于评论、消息等纯文本输入场景
 */
export function sanitizeText(text: string): string {
  if (!text) return ''
  return purify.sanitize(text, { ALLOWED_TAGS: [] })
}