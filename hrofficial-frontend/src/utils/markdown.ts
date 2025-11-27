import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
})

export function markdownToHtml(markdown: string): string {
  if (!markdown) return ''
  
  try {
    let processed = markdown.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    
    // 修复连续的标题符号
    processed = processed.replace(/(#{1,6})\s+(#{1,6})\s+/g, '$1 ')
    
    // 只清理过多的换行（3个以上合并为2个）
    // 不要添加额外的换行，因为后端已经返回了正确的格式
    processed = processed.replace(/\n{3,}/g, '\n\n')
    
    return md.render(processed)
  } catch (error) {
    console.error('Markdown解析失败:', error)
    return `<p>${escapeHtml(markdown)}</p>`
  }
}

function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, (char) => map[char] || char)
}

export function sanitizeHtml(html: string): string {
  let cleaned = html.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
  cleaned = cleaned.replace(/\s*on\w+\s*=\s*["'][^"']*["']/gi, '')
  cleaned = cleaned.replace(/\s*on\w+\s*=\s*[^\s>]*/gi, '')
  return cleaned
}

export function markdownToSafeHtml(markdown: string): string {
  const html = markdownToHtml(markdown)
  return sanitizeHtml(html)
}
