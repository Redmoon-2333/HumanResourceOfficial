/**
 * 活动管理 API 模块
 * 
 * 提供活动的增删改查以及图片管理功能
 * @module api/activity
 */

import { http } from '@/utils/http'
import type {
  Activity,
  ActivityRequest,
  ActivityResponse,
  ActivityImageDTO,
  ApiResponse
} from '@/types'

// ============================================
// 活动 CRUD 操作
// ============================================

/**
 * 创建新活动
 * @param data - 活动请求数据
 * @returns 创建的活动信息
 * 
 * @example
 * ```typescript
 * const activity = await createActivity({
 *   title: '迎新晚会',
 *   description: '欢迎新成员加入',
 *   startTime: '2024-09-01T19:00:00',
 *   endTime: '2024-09-01T21:00:00',
 *   location: '学生活动中心',
 *   organizer: '人力资源中心',
 *   participantCount: 100,
 *   status: 'PUBLISHED'
 * })
 * ```
 */
export const createActivity = (data: ActivityRequest): Promise<ApiResponse<ActivityResponse>> => {
  return http.post<ActivityResponse>('/api/activities', data)
}

/**
 * 获取所有活动列表
 * @returns 活动列表
 * 
 * @example
 * ```typescript
 * const activities = await getActivities()
 * console.log(activities.data) // ActivityResponse[]
 * ```
 */
export const getActivities = (): Promise<ApiResponse<ActivityResponse[]>> => {
  return http.get<ActivityResponse[]>('/api/activities')
}

/**
 * 根据ID获取活动详情
 * @param activityId - 活动ID
 * @returns 活动详情
 * 
 * @example
 * ```typescript
 * const activity = await getActivityById(1)
 * console.log(activity.data.title)
 * ```
 */
export const getActivityById = (activityId: number): Promise<ApiResponse<ActivityResponse>> => {
  return http.get<ActivityResponse>(`/api/activities/${activityId}`)
}

/**
 * 更新活动信息
 * @param activityId - 活动ID
 * @param data - 活动更新数据
 * @returns 更新后的活动信息
 * 
 * @example
 * ```typescript
 * const updated = await updateActivity(1, {
 *   title: '更新后的标题',
 *   description: '更新后的描述',
 *   // ...其他字段
 * })
 * ```
 */
export const updateActivity = (
  activityId: number, 
  data: ActivityRequest
): Promise<ApiResponse<ActivityResponse>> => {
  return http.put<ActivityResponse>(`/api/activities/${activityId}`, data)
}

/**
 * 删除活动
 * @param activityId - 活动ID
 * @returns 删除结果
 * 
 * @example
 * ```typescript
 * await deleteActivity(1)
 * ```
 */
export const deleteActivity = (activityId: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/activities/${activityId}`)
}

// ============================================
// 活动图片管理
// ============================================

/**
 * 通用图片上传接口
 * @param file - 图片文件
 * @param onProgress - 上传进度回调
 * @returns 上传后的图片URL
 * 
 * @example
 * ```typescript
 * const file = document.getElementById('fileInput').files[0]
 * const result = await uploadActivityImage(file, (percent) => {
 *   console.log(`上传进度: ${percent}%`)
 * })
 * ```
 */
export const uploadActivityImage = (
  file: File, 
  onProgress?: (percent: number) => void
): Promise<ApiResponse<string>> => {
  return http.upload<string>('/api/activities/upload-image', file, onProgress)
}

/**
 * 为活动添加图片
 * @param activityId - 活动ID
 * @param file - 图片文件
 * @param description - 图片描述（可选）
 * @param sortOrder - 排序顺序（可选，默认0）
 * @returns 添加的图片信息
 * 
 * @example
 * ```typescript
 * const image = await addImageToActivity(1, file, '活动海报', 1)
 * ```
 */
export const addImageToActivity = (
  activityId: number,
  file: File,
  description?: string,
  sortOrder?: number
): Promise<ApiResponse<ActivityImageDTO>> => {
  const formData = new FormData()
  formData.append('file', file)
  if (description) {
    formData.append('description', description)
  }
  if (sortOrder !== undefined) {
    formData.append('sortOrder', sortOrder.toString())
  }
  
  return http.post<ActivityImageDTO>(`/api/activities/${activityId}/images`, formData)
}

/**
 * 获取活动的所有图片
 * @param activityId - 活动ID
 * @returns 图片列表
 * 
 * @example
 * ```typescript
 * const images = await getActivityImages(1)
 * images.data.forEach(img => console.log(img.imageUrl))
 * ```
 */
export const getActivityImages = (activityId: number): Promise<ApiResponse<ActivityImageDTO[]>> => {
  return http.get<ActivityImageDTO[]>(`/api/activities/${activityId}/images`)
}

/**
 * 更新活动图片信息
 * @param imageId - 图片ID
 * @param description - 新描述（可选）
 * @param sortOrder - 新排序顺序（可选）
 * @returns 更新后的图片信息
 * 
 * @example
 * ```typescript
 * await updateActivityImage(1, '新的描述', 2)
 * ```
 */
export const updateActivityImage = (
  imageId: number,
  description?: string,
  sortOrder?: number
): Promise<ApiResponse<ActivityImageDTO>> => {
  const params: Record<string, any> = {}
  if (description !== undefined) {
    params.description = description
  }
  if (sortOrder !== undefined) {
    params.sortOrder = sortOrder
  }
  
  return http.put<ActivityImageDTO>(`/api/activities/images/${imageId}`, params)
}

/**
 * 删除活动图片
 * @param imageId - 图片ID
 * @returns 删除结果
 * 
 * @example
 * ```typescript
 * await deleteActivityImage(1)
 * ```
 */
export const deleteActivityImage = (imageId: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/activities/images/${imageId}`)
}

/**
 * 获取活动（getActivityById 的别名）
 * @deprecated 请使用 getActivityById
 */
export const getActivity = getActivityById
