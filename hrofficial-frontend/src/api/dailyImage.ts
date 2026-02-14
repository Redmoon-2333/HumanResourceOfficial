import http from '@/utils/http'

/**
 * 日常活动图片接口
 * 用于"我们的日常"板块3D轮播图
 *
 * @author 人力资源中心技术组
 * @since 2026-02-13
 */

// 图片数据接口
export interface DailyImage {
  imageId: number
  imageUrl: string
  title: string
  description: string
  sortOrder: number
  isActive: boolean
  createTime: string
  updateTime: string
}

// API响应接口
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 上传图片文件
 * 支持直接上传图片文件，自动保存到服务器并创建数据库记录
 * @param file 图片文件
 * @param title 图片标题（可选）
 * @param description 图片描述（可选）
 * @param onProgress 上传进度回调
 * @returns 上传后的图片信息
 */
export const uploadDailyImage = (
  file: File,
  title?: string,
  description?: string,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<DailyImage>> => {
  const formData = new FormData()
  formData.append('file', file)
  if (title) {
    formData.append('title', title)
  }
  if (description) {
    formData.append('description', description)
  }

  return http.upload<DailyImage>('/api/daily-images/upload', file, onProgress, formData)
}

/**
 * 仅上传图片文件，不创建数据库记录
 * 用于编辑图片时替换文件
 * @param file 图片文件
 * @param onProgress 上传进度回调
 * @returns 上传后的图片URL
 */
export const uploadDailyImageFile = (
  file: File,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<string>> => {
  const formData = new FormData()
  formData.append('file', file)

  return http.upload<string>('/api/daily-images/upload-file', file, onProgress, formData)
}

/**
 * 图片类型
 */
export type ImageType = 'daily' | 'activity' | 'avatar'

/**
 * 按类型上传图片文件
 * 支持不同模块的图片上传到对应目录
 * @param file 图片文件
 * @param type 图片类型 (daily-我们的日常, activity-活动照片, avatar-用户头像)
 * @param onProgress 上传进度回调
 * @returns 上传后的图片URL
 */
export const uploadImageByType = (
  file: File,
  type: ImageType,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<string>> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)

  return http.upload<string>('/api/daily-images/upload-by-type', file, onProgress, formData)
}

/**
 * 删除图片及本地文件（会删除数据库记录）
 * @param id 图片ID
 */
export const deleteDailyImageWithFile = (id: number): Promise<ApiResponse<void>> => {
  return http.delete(`/api/daily-images/${id}/with-file`)
}

/**
 * 仅删除图片文件，不删除数据库记录
 * Why: 用于编辑图片时替换文件，保留原记录只更新URL
 * @param id 图片ID
 */
export const deleteDailyImageFileOnly = (id: number): Promise<ApiResponse<void>> => {
  return http.delete(`/api/daily-images/${id}/file-only`)
}

/**
 * 获取所有启用的图片列表
 * 用于前端首页轮播图展示
 * @returns 图片列表
 */
export const getActiveImages = (): Promise<ApiResponse<DailyImage[]>> => {
  return http.get('/api/daily-images')
}

/**
 * 获取所有图片列表（包含禁用状态）
 * 用于后台管理
 * @returns 图片列表
 */
export const getAllImages = (): Promise<ApiResponse<DailyImage[]>> => {
  return http.get('/api/daily-images/all')
}

/**
 * 根据ID获取图片详情
 * @param id 图片ID
 * @returns 图片详情
 */
export const getImageById = (id: number): Promise<ApiResponse<DailyImage>> => {
  return http.get(`/api/daily-images/${id}`)
}

/**
 * 添加新图片
 * @param data 图片数据
 * @returns 添加后的图片
 */
export const addImage = (data: Partial<DailyImage>): Promise<ApiResponse<DailyImage>> => {
  return http.post('/api/daily-images', data)
}

/**
 * 更新图片信息
 * @param id 图片ID
 * @param data 图片数据
 * @returns 更新后的图片
 */
export const updateImage = (id: number, data: Partial<DailyImage>): Promise<ApiResponse<DailyImage>> => {
  return http.put(`/api/daily-images/${id}`, data)
}

/**
 * 删除图片
 * @param id 图片ID
 */
export const deleteImage = (id: number): Promise<ApiResponse<void>> => {
  return http.delete(`/api/daily-images/${id}`)
}

/**
 * 批量删除图片
 * @param imageIds 图片ID列表
 */
export const batchDeleteImages = (imageIds: number[]): Promise<ApiResponse<void>> => {
  return http.post('/api/daily-images/batch-delete', { imageIds })
}

/**
 * 更新图片状态
 * @param id 图片ID
 * @param isActive 状态
 */
export const updateImageStatus = (id: number, isActive: boolean): Promise<ApiResponse<void>> => {
  return http.put(`/api/daily-images/${id}/status`, { isActive })
}

/**
 * 交换图片排序
 * @param imageId1 图片1 ID
 * @param imageId2 图片2 ID
 */
export const swapImageOrder = (imageId1: number, imageId2: number): Promise<ApiResponse<void>> => {
  return http.post('/api/daily-images/swap-order', { imageId1, imageId2 })
}
