/**
 * 图片URL处理工具
 * Why: 后端返回的相对路径需要拼接成完整URL
 */

// 后端基础URL（与http.ts保持一致）
const API_BASE_URL = import.meta.env.PROD
  ? (import.meta.env.VITE_API_BASE_URL || 'http://81.70.218.85:8080')
  : ''

/**
 * 获取完整的图片URL
 * @param imageUrl 后端返回的图片URL（可能是相对路径或完整URL）
 * @returns 完整的图片URL
 */
export function getFullImageUrl(imageUrl: string | undefined): string {
  if (!imageUrl) {
    return ''
  }

  // 如果已经是完整URL，直接返回
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl
  }

  // 确保路径以/开头
  const normalizedPath = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`

  // 开发环境使用相对路径
  if (!import.meta.env.PROD) {
    return normalizedPath
  }

  // 生产环境拼接完整URL
  const baseUrl = API_BASE_URL.endsWith('/')
    ? API_BASE_URL.slice(0, -1)
    : API_BASE_URL

  return `${baseUrl}${normalizedPath}`
}

/**
 * 获取图片预览列表（用于el-image的preview-src-list）
 * @param imageUrls 图片URL数组
 * @returns 完整的图片URL数组
 */
export function getFullImageUrlList(imageUrls: string[]): string[] {
  return imageUrls.map(url => getFullImageUrl(url))
}
