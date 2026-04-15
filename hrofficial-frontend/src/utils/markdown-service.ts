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
  ADD_ATTR: ['target', 'class', 'id', 'data-*', 'align'],
  ADD_TAGS: ['mark', 'kbd', 'samp', 'details', 'summary', 'figure', 'figcaption', 'input', 'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'colgroup', 'col'],
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

    this.md = new MarkdownIt('default', {
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

  private preprocess(text: string): string {
    if (!text) return ''

    const lines = text.split('\n')
    const result: string[] = []
    let i = 0

    while (i < lines.length) {
      const line = lines[i] ?? ''
      const trimmed = line.trim()

      // Detect table-like region (consecutive lines containing |)
      if (trimmed.includes('|') && this.isTableLikeLine(trimmed)) {
        const tableLines: string[] = []
        while (i < lines.length && (lines[i] ?? '').trim() !== '') {
          tableLines.push(lines[i] ?? '')
          i++
        }
        // Skip separator rows (|---|---|) at the start
        let startIdx = 0
        if (tableLines.length > 0 && /^[\s|:-]+$/.test((tableLines[0] ?? '').trim())) {
          startIdx = 1
        }
        const dataRows = tableLines.slice(startIdx)
        if (dataRows.length > 0) {
          const converted = this.convertMalformedTable(dataRows)
          if (converted) {
            result.push(converted)
          } else {
            dataRows.forEach(row => result.push(row ?? ''))
          }
        }
        continue
      }

      // Rule: Remove debris lines (• | patterns, empty pipe lines)
      if (/^\u2022\s*\|/.test(trimmed)) { i++; continue }
      if (/^\|\s*$/.test(trimmed)) { i++; continue }

      // Rule: Remove standalone --- lines (preserve Setext headings)
      if (/^[-_]{3,}$/.test(trimmed)) {
        const prevLine = result.length > 0 ? (result[result.length - 1] ?? '').trim() : ''
        const isSetextUnderline = prevLine.length > 0 &&
          !prevLine.startsWith('#') &&
          !prevLine.startsWith('-') &&
          !prevLine.startsWith('*')
        if (!isSetextUnderline) { i++; continue }
      }

      result.push(line)
      i++
    }

    return result.join('\n')
  }

  private isTableLikeLine(line: string): boolean {
    const trimmed = line.trim()
    if (!trimmed.includes('|')) return false
    // Must have at least 2 pipe-separated segments
    const segments = trimmed.split('|').filter(s => s.trim().length > 0)
    return segments.length >= 2
  }
  
  /**
   * Check if lines starting at index i form a malformed table
   * Only returns true if debris patterns (• |, :-•) are found
   */
  private isMalformedTable(lines: string[], i: number): boolean {
    if (!lines[i] || !lines[i].includes('|')) return false
    
    // Look ahead for debris patterns in next 10 lines
    for (let j = i; j < Math.min(i + 10, lines.length); j++) {
      const trimmed = (lines[j] ?? '').trim()
      // Skip empty lines
      if (/^\s*$/.test(trimmed)) continue
      
      // Has debris patterns - this is malformed
      if (/•\s*\|/.test(trimmed)) return true
      if (/:-•/.test(trimmed)) return true
      if (/^\|[\s:•-]+$/.test(trimmed) && /:-•/.test(trimmed)) return true
    }
    
    // No debris found - this is a standard GFM table, don't convert
    return false
  }
  
  /**
   * Convert malformed table to list format
   * Format: **Title** then - Header: Value for remaining columns
   */
  private convertMalformedTable(tableRows: string[]): string {
    const result: string[] = []
    let headers: string[] = []
    
    for (let i = 0; i < tableRows.length; i++) {
      const row = tableRows[i] ?? ''
      const trimmed = row.trim()
      
      // Skip separator rows (|---|---|)
      if (/^[\s|:-]+$/.test(trimmed)) continue
      // Skip empty rows
      if (!trimmed) continue
      
      // Extract columns from | col1 | col2 | format
      if (trimmed.includes('|')) {
        const columns = trimmed.split('|')
          .map(c => c.trim())
          .filter(c => c.length > 0)
        
        if (columns.length === 0) continue
        
        // First row is header
        if (headers.length === 0) {
          headers = columns
          continue
        }
        
        // Data row: first column becomes bold title
        const title = columns[0]
        result.push(`**${title}**`)
        
        // Remaining columns become "- Header: Value" list items
        for (let j = 1; j < columns.length && j < headers.length; j++) {
          if (columns[j]) {
            result.push(`- ${headers[j]}: ${columns[j]}`)
          }
        }
        
        // Add empty line between entries
        if (i < tableRows.length - 1) {
          result.push('')
        }
      }
    }
    
    return result.join('\n')
  }
  
  private isValidTableRow(line: string, isFirstRow: boolean): boolean {
    const trimmed = (line ?? '').trim()
    
    // Must contain |
    if (!trimmed.includes('|')) return false
    
    // First row (header) must have at least 2 columns
    if (isFirstRow) {
      const columns = trimmed.split('|').filter(c => c.trim().length > 0)
      return columns.length >= 2
    }
    
    // Data rows must have at least one non-empty value
    const hasContent = /[^\s|•:-]/.test(trimmed)
    return hasContent
  }
  
  private convertTableToList(tableRows: string[]): string {
    if (tableRows.length === 0) return ''
    
    // Parse header row
    const headerLine = (tableRows[0] ?? '').trim()
    const headers = headerLine.split('|')
      .map(h => h.trim())
      .filter(h => h.length > 0)
    
    if (headers.length === 0) return ''
    
    const converted: string[] = []
    
    // Process data rows
    for (let i = 1; i < tableRows.length; i++) {
      const dataLine = (tableRows[i] ?? '').trim()
      if (!dataLine || /^[\s|:-]+$/.test(dataLine)) continue
      
      const columns = dataLine.split('|')
        .map(c => c.trim())
        .filter(c => c.length > 0)
      
      if (columns.length === 0) continue
      
      // Use first column as bold title
      const title = columns[0]
      converted.push(`**${title}**`)
      
      // Add remaining columns as list items
      for (let j = 1; j < columns.length && j <= headers.length; j++) {
        const headerName = headers[j] || `列${j}`
        const value = columns[j]
        if (value && value.trim().length > 0) {
          converted.push(`- ${headerName}: ${value}`)
        }
      }
      
      // Add empty line between entries
      if (i < tableRows.length - 1) {
        converted.push('')
      }
    }
    
    return converted.join('\n')
  }

  render(markdown: string): string {
    if (!markdown) return ''

    this.initMarkdownIt()

    try {
      const processed = this.preprocess(markdown)
      const html = this.md!.render(processed)
      return DOMPurify.sanitize(html, MARKDOWN_SANITIZE_CONFIG)
    } catch (error) {
      console.warn('Markdown 渲染失败:', error)
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
