import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * Markdown处理服务（精简重构版）
 * 
 * Why: 后端通过Prompt Engineering严格控制输出格式，前端无需复杂预处理
 * 
 * 核心职责：
 * 1. Markdown渲染 - 使用marked库
 * 2. HTML安全过滤 - 使用DOMPurify
 * 3. 内容类型检测
 */

const MARKDOWN_SANITIZE_CONFIG = {
  ADD_ATTR: ['target', 'class', 'id'],
  ADD_TAGS: ['mark', 'kbd', 'samp', 'details', 'summary', 'figure', 'figcaption'],
  FORBID_TAGS: ['script', 'object', 'embed', 'form'],
  FORBID_ATTR: ['on*', 'srcdoc']
}

const HTML_SANITIZE_CONFIG = {
  ADD_ATTR: ['target', 'class', 'style', 'id', 'data-*'],
  ADD_TAGS: ['style', 'section', 'article', 'mark', 'kbd', 'samp', 'details', 'summary', 'figure', 'figcaption', 'head', 'body', 'html', 'title', 'meta', 'link'],
  FORBID_TAGS: ['script', 'object', 'embed', 'form'],
  FORBID_ATTR: ['on*', 'srcdoc'],
  ALLOW_DATA_ATTR: true,
  WHOLE_DOCUMENT: true
}

class MarkdownService {
  private md: typeof marked

  constructor() {
    this.md = marked
    this.md.setOptions({
      breaks: true,
      gfm: true
    })
  }

  /**
   * Markdown转HTML渲染
   * Why: 使用marked库进行标准Markdown解析，DOMPurify进行安全过滤
   */
  render(markdown: string): string {
    if (!markdown) return ''

    try {
      const html = this.md.parse(markdown, { async: false }) as string
      return DOMPurify.sanitize(html, MARKDOWN_SANITIZE_CONFIG)
    } catch (error) {
      console.warn('Markdown渲染失败:', error)
      return this.escapeHtml(markdown).replace(/\n/g, '<br>')
    }
  }

  /**
   * HTML内容渲染（策划案专用）
   * Why: 策划案直接输出HTML，只需安全过滤，不做任何删减
   * Warning: 保持AI输出的HTML完整性，仅过滤危险标签
   */
  renderHtml(html: string): string {
    if (!html) return ''

    try {
      let cleaned = html.trim()
      cleaned = this.removeMarkdownCodeBlock(cleaned)
      return DOMPurify.sanitize(cleaned, HTML_SANITIZE_CONFIG)
    } catch (error) {
      console.warn('HTML渲染失败:', error)
      return this.escapeHtml(html).replace(/\n/g, '<br>')
    }
  }

  /**
   * 移除可能的Markdown代码块包裹
   * Why: AI可能用```html包裹HTML，需要移除
   */
  private removeMarkdownCodeBlock(content: string): string {
    let cleaned = content.trim()
    cleaned = cleaned.replace(/^```(?:html|HTML)?\s*\n?/g, '')
    cleaned = cleaned.replace(/\n?```\s*$/g, '')
    return cleaned.trim()
  }

  /**
   * 检测内容类型
   */
  detectContentType(content: string): 'markdown' | 'html' {
    if (!content) return 'markdown'

    const trimmed = content.trim()
    
    if (trimmed.startsWith('<!DOCTYPE') || trimmed.startsWith('<html')) {
      return 'html'
    }

    if (trimmed.startsWith('<') && /<\/[a-zA-Z][^>]*>/.test(trimmed)) {
      return 'html'
    }

    const htmlTagPattern = /<[a-zA-Z][^>]*>[\s\S]*<\/[a-zA-Z][^>]*>/
    if (htmlTagPattern.test(trimmed)) {
      const markdownPatterns = [
        /^#{1,6}\s/m,
        /^\s*[-*+]\s/m,
        /^\s*\d+\.\s/m,
        /\*\*.*?\*\*/,
        /\[.*?\]\(.*?\)/
      ]
      const hasMarkdown = markdownPatterns.some(p => p.test(trimmed))
      if (!hasMarkdown) return 'html'
    }

    return 'markdown'
  }

  /**
   * HTML转义
   */
  escapeHtml(text: string): string {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    }
    return text.replace(/[&<>'"]/g, (char) => map[char] || char)
  }
}

export const markdownService = new MarkdownService()

export const {
  render: renderMarkdown,
  renderHtml,
  detectContentType,
  escapeHtml
} = markdownService

export default MarkdownService
