/**
 * sanitize 工具函数单元测试
 * @vitest-environment jsdom
 *
 * 测试覆盖：
 * - sanitizeHtml 对合法 HTML 的保留
 * - sanitizeHtml 对 XSS 攻击的过滤
 * - sanitizeHtml 对空输入的处理
 * - sanitizeText 对 HTML 标签的移除
 * - sanitizeText 对纯文本的保留
 * - sanitizeText 对空输入的处理
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { sanitizeHtml, sanitizeText } from '~/utils/sanitize'

describe('sanitizeHtml', () => {
  describe('合法 HTML 保留', () => {
    it('应保留合法标签和属性', () => {
      const html = '<p>Hello <strong>World</strong></p>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<p>')
      expect(result).toContain('<strong>')
      expect(result).toContain('World')
    })

    it('应保留链接标签', () => {
      const html = '<a href="https://example.com" target="_blank">Link</a>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<a')
      expect(result).toContain('href="https://example.com"')
      expect(result).toContain('target="_blank"')
    })

    it('应保留图片标签', () => {
      const html = '<img src="https://img.example.com/1.jpg" alt="test">'
      const result = sanitizeHtml(html)
      expect(result).toContain('<img')
      expect(result).toContain('src="https://img.example.com/1.jpg"')
      expect(result).toContain('alt="test"')
    })

    it('应保留表格标签', () => {
      const html = '<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Data</td></tr></tbody></table>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<table>')
      expect(result).toContain('<th>')
      expect(result).toContain('<td>')
    })

    it('应保留标题标签', () => {
      const html = '<h1>Title</h1><h2>Subtitle</h2>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<h1>')
      expect(result).toContain('<h2>')
    })

    it('应保留列表标签', () => {
      const html = '<ul><li>Item 1</li><li>Item 2</li></ul>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<ul>')
      expect(result).toContain('<li>')
    })
  })

  describe('XSS 攻击防护', () => {
    it('应移除 script 标签', () => {
      const html = '<p>Safe</p><script>alert("xss")</script>'
      const result = sanitizeHtml(html)
      expect(result).not.toContain('<script>')
      expect(result).not.toContain('alert')
      expect(result).toContain('<p>Safe</p>')
    })

    it('应移除 onclick 事件属性', () => {
      const html = '<p onclick="alert(1)">Text</p>'
      const result = sanitizeHtml(html)
      expect(result).not.toContain('onclick')
    })

    it('应移除 onerror 事件属性', () => {
      const html = '<img src="x" onerror="alert(1)">'
      const result = sanitizeHtml(html)
      expect(result).not.toContain('onerror')
    })

    it('应移除 iframe 标签', () => {
      const html = '<p>Content</p><iframe src="evil.com"></iframe>'
      const result = sanitizeHtml(html)
      expect(result).not.toContain('<iframe>')
      expect(result).toContain('<p>Content</p>')
    })

    it('应移除 javascript: 协议链接', () => {
      const html = '<a href="javascript:alert(1)">Click</a>'
      const result = sanitizeHtml(html)
      expect(result).not.toContain('javascript:')
    })
  })

  describe('边界情况', () => {
    it('空字符串应返回空字符串', () => {
      expect(sanitizeHtml('')).toBe('')
    })

    it('null/undefined 应返回空字符串', () => {
      expect(sanitizeHtml(null as any)).toBe('')
      expect(sanitizeHtml(undefined as any)).toBe('')
    })

    it('纯文本应原样返回', () => {
      const text = 'Hello World, this is plain text.'
      const result = sanitizeHtml(text)
      expect(result).toBe(text)
    })

    it('嵌套复杂 HTML 应安全处理', () => {
      const html = '<div><p>Hello <b>World</b></p><script>evil()</script></div>'
      const result = sanitizeHtml(html)
      expect(result).toContain('<p>')
      expect(result).toContain('<b>')
      expect(result).not.toContain('<script>')
    })
  })
})

describe('sanitizeText', () => {
  describe('HTML 标签移除', () => {
    it('应移除所有 HTML 标签', () => {
      const text = '<p>Hello <strong>World</strong></p>'
      const result = sanitizeText(text)
      expect(result).toBe('Hello World')
    })

    it('应移除 script 标签及内容', () => {
      const text = 'Hello<script>alert("xss")</script>World'
      const result = sanitizeText(text)
      expect(result).not.toContain('<script>')
      expect(result).not.toContain('alert')
      expect(result).toContain('Hello')
      expect(result).toContain('World')
    })

    it('应移除 img 标签', () => {
      const text = '<img src="x" onerror="alert(1)">Text'
      const result = sanitizeText(text)
      expect(result).not.toContain('<img')
      expect(result).toContain('Text')
    })
  })

  describe('纯文本保留', () => {
    it('应保留纯文本内容', () => {
      const text = 'This is a normal message.'
      expect(sanitizeText(text)).toBe('This is a normal message.')
    })

    it('应保留 emoji 和特殊字符', () => {
      const text = 'Hello 😊 你好 👋'
      expect(sanitizeText(text)).toBe('Hello 😊 你好 👋')
    })
  })

  describe('边界情况', () => {
    it('空字符串应返回空字符串', () => {
      expect(sanitizeText('')).toBe('')
    })

    it('null/undefined 应返回空字符串', () => {
      expect(sanitizeText(null as any)).toBe('')
      expect(sanitizeText(undefined as any)).toBe('')
    })
  })
})