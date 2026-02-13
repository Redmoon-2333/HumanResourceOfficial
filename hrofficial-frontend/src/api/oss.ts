/**
 * OSS 对象存储 API 模块
 *
 * 提供预签名URL生成功能，用于临时访问私有文件
 * @module api/oss
 */

import { http } from '@/utils/http'
import type {
  PresignedUrlRequest,
  PresignedUrlResponse,
  ApiResponse
} from '@/types'

// ============================================
// 预签名 URL 生成
// ============================================

/**
 * 生成预签名URL（通用）
 * @param data - 预签名URL请求数据
 * @returns 预签名URL信息
 *
 * @example
 * ```typescript
 * const result = await generatePresignedUrl({
 *   fileName: 'document.pdf',
 *   fileType: 'application/pdf'
 * })
 * // 使用uploadUrl上传文件
 * // 使用fileUrl访问文件
 * ```
 */
export const generatePresignedUrl = (
  data: PresignedUrlRequest
): Promise<ApiResponse<PresignedUrlResponse>> => {
  return http.post<PresignedUrlResponse>('/api/oss/presigned-url', data)
}

/**
 * 根据资料ID获取预签名URL
 * @param materialId - 资料ID
 * @param expirationSeconds - 过期时间（秒，默认3600）
 * @returns 预签名URL信息
 *
 * @example
 * ```typescript
 * const result = await getPresignedUrlForMaterial(1, 7200)
 * window.open(result.data.fileUrl, '_blank')
 * ```
 */
export const getPresignedUrlForMaterial = (
  materialId: number,
  expirationSeconds: number = 3600
): Promise<ApiResponse<PresignedUrlResponse>> => {
  return http.get<PresignedUrlResponse>(
    `/api/oss/presigned-url/material/${materialId}`,
    { expirationSeconds }
  )
}

/**
 * 根据活动图片ID获取预签名URL
 * @param imageId - 图片ID
 * @param expirationSeconds - 过期时间（秒，默认3600）
 * @returns 预签名URL信息
 *
 * @example
 * ```typescript
 * const result = await getPresignedUrlForActivityImage(1)
 * console.log(result.data.fileUrl)
 * ```
 */
export const getPresignedUrlForActivityImage = (
  imageId: number,
  expirationSeconds: number = 3600
): Promise<ApiResponse<PresignedUrlResponse>> => {
  return http.get<PresignedUrlResponse>(
    `/api/oss/presigned-url/activity-image/${imageId}`,
    { expirationSeconds }
  )
}
