/**
 * 日期格式化工具函数
 */

/**
 * 格式化日期为指定格式
 * @param date 日期对象、时间戳或日期字符串
 * @param format 格式字符串，默认 'YYYY-MM-DD HH:mm:ss'
 */
export function formatDate(
  date: Date | string | number | null | undefined,
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return '-'

  const d = typeof date === 'string' || typeof date === 'number'
    ? new Date(date)
    : date

  if (isNaN(d.getTime())) return '-'

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化为简短日期（年-月-日）
 */
export function formatDateShort(date: Date | string | number | null | undefined): string {
  return formatDate(date, 'YYYY-MM-DD')
}

/**
 * 格式化为时间（时:分:秒）
 */
export function formatTime(date: Date | string | number | null | undefined): string {
  return formatDate(date, 'HH:mm:ss')
}

/**
 * 获取相对时间描述（如：刚刚、5分钟前、昨天等）
 */
export function formatRelativeTime(date: Date | string | number | null | undefined): string {
  if (!date) return '-'

  const d = typeof date === 'string' || typeof date === 'number'
    ? new Date(date)
    : date

  if (isNaN(d.getTime())) return '-'

  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

/**
 * 文件大小格式化
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined) return '-'
  if (bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${units[i]}`
}

/**
 * 数字格式化（添加千分位）
 */
export function formatNumber(num: number | null | undefined): string {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString('zh-CN')
}

/**
 * 截断文本
 */
export function truncateText(text: string | null | undefined, maxLength: number = 100): string {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}
