/**
 * 资料管理 API 模块
 * 
 * 提供资料的增删改查、分类管理等功能
 * @module api/materials
 */

import { http } from '@/utils/http'
import type {
  ApiResponse,
  CategoryResponse,
  CategoryRequest,
  SubcategoryResponse,
  SubcategoryRequest,
  MaterialResponse,
  MaterialRequest
} from '@/types'

// ============================================
// 资料 CRUD 操作
// ============================================

/**
 * 上传资料文件
 * @param file - 文件
 * @param categoryId - 分类ID
 * @param subcategoryId - 子分类ID
 * @param materialName - 资料名称
 * @param description - 描述（可选）
 * @param onProgress - 上传进度回调
 * @returns 上传的资料信息
 */
export const uploadMaterial = (
  file: File,
  categoryId: number,
  subcategoryId: number,
  materialName: string,
  description?: string,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<MaterialResponse>> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('categoryId', categoryId.toString())
  formData.append('subcategoryId', subcategoryId.toString())
  formData.append('materialName', materialName)
  if (description) {
    formData.append('description', description)
  }
  return http.upload<MaterialResponse>('/api/materials/upload', file, onProgress, formData)
}

/**
 * 获取所有资料
 * @returns 资料列表
 */
export const getAllMaterials = (): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>('/api/materials')
}

/**
 * 根据ID获取资料详情
 * @param materialId - 资料ID
 * @returns 资料详情
 */
export const getMaterialById = (materialId: number): Promise<ApiResponse<MaterialResponse>> => {
  return http.get<MaterialResponse>(`/api/materials/${materialId}`)
}

/**
 * 根据分类ID获取资料列表
 * @param categoryId - 分类ID（可选，不传则返回所有资料）
 * @returns 资料列表
 */
export const getMaterialsByCategory = (categoryId?: number): Promise<ApiResponse<MaterialResponse[]>> => {
  if (categoryId === undefined || categoryId === null) {
    return http.get<MaterialResponse[]>('/api/materials')
  }
  return http.get<MaterialResponse[]>(`/api/materials/category/${categoryId}`)
}

/**
 * 根据子分类ID获取资料列表
 * @param subcategoryId - 子分类ID
 * @returns 资料列表
 */
export const getMaterialsBySubcategory = (subcategoryId: number): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>(`/api/materials/subcategory/${subcategoryId}`)
}

/**
 * 搜索资料（根据名称模糊查询）
 * @param name - 资料名称关键词
 * @returns 匹配的资料列表
 */
export const searchMaterials = (name: string): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>('/api/materials/search', { name })
}

/**
 * 更新资料信息
 * @param materialId - 资料ID
 * @param data - 更新数据
 * @returns 更新后的资料信息
 */
export const updateMaterial = (
  materialId: number,
  data: MaterialRequest
): Promise<ApiResponse<MaterialResponse>> => {
  return http.put<MaterialResponse>(`/api/materials/${materialId}`, data)
}

/**
 * 删除资料
 * @param materialId - 资料ID
 * @returns 删除结果
 */
export const deleteMaterial = (materialId: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/materials/${materialId}`)
}

/**
 * 获取资料下载链接（预签名URL）
 * @param materialId - 资料ID
 * @param expirationSeconds - 过期时间（秒，默认3600）
 * @returns 下载URL
 */
export const getDownloadUrl = (
  materialId: number,
  expirationSeconds: number = 3600
): Promise<ApiResponse<string>> => {
  return http.get<string>(`/api/materials/download-url/${materialId}`, { expirationSeconds })
}

// ============================================
// 分类管理
// ============================================

/**
 * 获取所有分类
 * @returns 分类列表
 */
export const getCategories = (): Promise<ApiResponse<CategoryResponse[]>> => {
  return http.get<CategoryResponse[]>('/api/materials/categories')
}

/**
 * 创建分类
 * @param data - 分类数据
 * @returns 创建的分类信息
 */
export const createCategory = (data: CategoryRequest): Promise<ApiResponse<CategoryResponse>> => {
  return http.post<CategoryResponse>('/api/materials/category', data)
}

/**
 * 更新分类
 * @param categoryId - 分类ID
 * @param data - 更新数据
 * @returns 更新后的分类信息
 */
export const updateCategory = (
  categoryId: number,
  data: CategoryRequest
): Promise<ApiResponse<CategoryResponse>> => {
  return http.put<CategoryResponse>(`/api/materials/category/${categoryId}`, data)
}

// ============================================
// 子分类管理
// ============================================

/**
 * 获取分类下的所有子分类
 * @param categoryId - 分类ID
 * @returns 子分类列表
 */
export const getSubcategories = (categoryId: number): Promise<ApiResponse<SubcategoryResponse[]>> => {
  return http.get<SubcategoryResponse[]>(`/api/materials/category/${categoryId}/subcategories`)
}

/**
 * 创建子分类
 * @param data - 子分类数据
 * @returns 创建的子分类信息
 */
export const createSubcategory = (data: SubcategoryRequest): Promise<ApiResponse<SubcategoryResponse>> => {
  return http.post<SubcategoryResponse>('/api/materials/subcategory', data)
}

/**
 * 更新子分类
 * @param subcategoryId - 子分类ID
 * @param data - 更新数据
 * @returns 更新后的子分类信息
 */
export const updateSubcategory = (
  subcategoryId: number,
  data: SubcategoryRequest
): Promise<ApiResponse<SubcategoryResponse>> => {
  return http.put<SubcategoryResponse>(`/api/materials/subcategory/${subcategoryId}`, data)
}

// ============================================
// 兼容性别名（保持向后兼容）
// ============================================

/**
 * 下载资料（getDownloadUrl 的别名）
 * @deprecated 请使用 getDownloadUrl
 */
export const downloadMaterial = getDownloadUrl
