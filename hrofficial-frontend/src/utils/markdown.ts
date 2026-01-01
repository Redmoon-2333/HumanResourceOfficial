import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
})

/**
 * 基础Markdown转HTML功能，使用markdown-it
 */
export function markdownToHtml(markdown: string): string {
  if (!markdown) return ''
  
  try {
    // 日志记录：原始markdown内容
    console.log('原始Markdown内容:', JSON.stringify(markdown))
    
    // 1. 统一处理换行符
    let processed = markdown.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    
    // 2. 特别处理：将\n-和\rn-转换为真正的换行符+列表项
    processed = processed.replace(/\\r?n-/g, '\n-')
    
    // 3. 确保列表项的-后面有空格
    processed = processed.replace(/(\n-)([^\s])/g, '$1 $2')
    
    // 4. 处理标题格式
    processed = processed.replace(/(#{1,6})([^\s#])/g, '$1 $2')
    
    // 5. 处理有序列表项
    processed = processed.replace(/(^|\n)(\d+\.)([^\s])/g, '$1$2 $3')
    
    // 6. 修复多行文本的换行问题
    processed = processed.replace(/(\S)\n(\S)/g, '$1\n\n$2')
    
    // 7. 清理过多的换行
    processed = processed.replace(/\n{3,}/g, '\n\n')
    
    // 日志记录：处理后的markdown内容
    console.log('处理后的Markdown内容:', JSON.stringify(processed))
    
    const html = md.render(processed)
    
    // 日志记录：渲染后的HTML内容
    console.log('渲染后的HTML内容:', JSON.stringify(html))
    
    return html
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
  return text.replace(/[&<>'"]/g, (char) => map[char] || char)
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

/**
 * Markstream-vue专用：预处理markdown内容，确保兼容markstream-vue的格式要求
 */
export function preprocessForMarkstream(markdown: string): string {
  if (!markdown) return ''
  
  // 1. 处理特殊换行符格式
  let processed = markdown.replace(/\\r?n-/g, '\n-')
  
  // 2. 确保列表项格式正确
  processed = processed.replace(/(\n-)([^\s])/g, '$1 $2')
  
  // 3. 处理有序列表
  processed = processed.replace(/(^|\n)(\d+\.)([^\s])/g, '$1$2 $3')
  
  // 4. 清理转义字符
  processed = processed.replace(/\\(?!n)/g, '')
  
  return processed
}
