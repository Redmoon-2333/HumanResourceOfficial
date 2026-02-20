/**
 * Markdown处理服务（优化版）
 *
 * Why: 优化加载性能，使用轻量级emoji插件
 *      按需加载功能，减少首屏加载时间
 */

import MarkdownIt from 'markdown-it'
import taskLists from 'markdown-it-task-lists'
import container from 'markdown-it-container'
import DOMPurify from 'dompurify'

const MARKDOWN_SANITIZE_CONFIG = {
  ADD_ATTR: ['target', 'class', 'id', 'data-*'],
  ADD_TAGS: ['mark', 'kbd', 'samp', 'details', 'summary', 'figure', 'figcaption', 'input'],
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
  private md: MarkdownIt | null = null

  private initMarkdownIt(): void {
    if (this.md) return

    this.md = new MarkdownIt({
      html: true,
      linkify: true,
      typographer: true,
      breaks: true,
      highlight: this.highlightCode.bind(this)
    })

    this.md.use(taskLists, { enabled: true, label: true, lineNumber: true })
    this.md.use(container, 'warning')
    this.md.use(container, 'info')
    this.md.use(container, 'tip')
  }

  private highlightCode(code: string, lang: string): string {
    const languageAliases: Record<string, string> = {
      'js': 'javascript',
      'ts': 'typescript',
      'py': 'python',
      'rb': 'ruby',
      'sh': 'bash',
      'shell': 'bash',
      'yml': 'yaml',
      'c#': 'csharp',
      'c++': 'cpp'
    }
    
    const normalizedLang = languageAliases[lang.toLowerCase()] || lang.toLowerCase()
    const supportedLangs = ['javascript', 'typescript', 'python', 'java', 'c', 'cpp', 'go', 'rust', 'ruby', 'php', 'swift', 'kotlin', 'html', 'css', 'scss', 'json', 'xml', 'yaml', 'sql', 'bash', 'markdown', 'vue', 'react']
    
    if (supportedLangs.includes(normalizedLang)) {
      const escaped = this.escapeHtml(code)
      return `<pre class="code-block" data-lang="${lang}"><code class="language-${normalizedLang}">${escaped}</code></pre>`
    }
    return `<pre class="code-block"><code>${this.escapeHtml(code)}</code></pre>`
  }

  render(markdown: string): string {
    if (!markdown) return ''

    this.initMarkdownIt()

    try {
      const html = this.md!.render(markdown)
      return DOMPurify.sanitize(html, MARKDOWN_SANITIZE_CONFIG)
    } catch (error) {
      console.warn('Markdown渲染失败:', error)
      return this.escapeHtml(markdown).replace(/\n/g, '<br>')
    }
  }

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

  private removeMarkdownCodeBlock(content: string): string {
    let cleaned = content.trim()
    cleaned = cleaned.replace(/^```(?:html|HTML)?\s*\n?/g, '')
    cleaned = cleaned.replace(/\n?```\s*$/g, '')
    return cleaned.trim()
  }

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
