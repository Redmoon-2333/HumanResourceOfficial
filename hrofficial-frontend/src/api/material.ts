/**
 * 资料管理 API 模块
 * 
 * 提供资料文件和分类的增删改查功能
 * @module api/material
 */

import { http } from '@/utils/http'
import type {
  Material,
  MaterialRequest,
  MaterialResponse,
  CategoryRequest,
  CategoryResponse,
  SubcategoryRequest,
  SubcategoryResponse,
  ApiResponse,
  UploadProgressCallback
} from '@/types'

// ============================================
// 资料文件 CRUD 操作
// ============================================

/**
 * 上传资料文件
 * @param file - 文件对象
 * @param categoryId - 分类ID
 * @param subcategoryId - 子分类ID
 * @param materialName - 资料名称
 * @param description - 资料描述（可选）
 * @param onProgress - 上传进度回调（可选）
 * @returns 上传后的资料信息
 * 
 * @example
 * ```typescript
 * const file = document.getElementById('fileInput').files[0]
 * const result = await uploadMaterial(
 *   file,
 *   1,
 *   2,
 *   '活动策划模板',
 *   '这是一个通用的活动策划模板',
 *   (percent) => console.log(`上传进度: ${percent}%`)
 * )
 * ```
 */
export const uploadMaterial = (
  file: File,
  categoryId: number,
  subcategoryId: number,
  materialName: string,
  description?: string,
  onProgress?: UploadProgressCallback
): Promise<ApiResponse<MaterialResponse>> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('categoryId', categoryId.toString())
  formData.append('subcategoryId', subcategoryId.toString())
  formData.append('materialName', materialName)
  if (description) {
    formData.append('description', description)
  }
  
  return http.upload<MaterialResponse>('/api/materials/upload', file, onProgress)
}

/**
 * 获取所有资料列表
 * @returns 资料列表
 * 
 * @example
 * ```typescript
 * const materials = await getMaterials()
 * materials.data.forEach(m => console.log(m.title))
 * ```
 */
export const getMaterials = (): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>('/api/materials')
}

/**
 * 根据分类ID获取资料列表
 * @param categoryId - 分类ID
 * @returns 该分类下的资料列表
 * 
 * @example
 * ```typescript
 * const materials = await getMaterialsByCategory(1)
 * ```
 */
export const getMaterialsByCategory = (
  categoryId: number
): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>(`/api/materials/category/${categoryId}`)
}

/**
 * 根据子分类ID获取资料列表
 * @param subcategoryId - 子分类ID
 * @returns 该子分类下的资料列表
 * 
 * @example
 * ```typescript
 * const materials = await getMaterialsBySubcategory(2)
 * ```
 */
export const getMaterialsBySubcategory = (
  subcategoryId: number
): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>(`/api/materials/subcategory/${subcategoryId}`)
}

/**
 * 根据ID获取资料详情
 * @param materialId - 资料ID
 * @returns 资料详情
 * 
 * @example
 * ```typescript
 * const material = await getMaterialById(1)
 * console.log(material.data.fileUrl)
 * ```
 */
export const getMaterialById = (
  materialId: number
): Promise<ApiResponse<MaterialResponse>> => {
  return http.get<MaterialResponse>(`/api/materials/${materialId}`)
}

/**
 * 根据名称搜索资料
 * @param name - 搜索关键词
 * @returns 匹配的资料列表
 * 
 * @example
 * ```typescript
 * const results = await searchMaterials('策划')
 * ```
 */
export const searchMaterials = (name: string): Promise<ApiResponse<MaterialResponse[]>> => {
  return http.get<MaterialResponse[]>('/api/materials/search', { name })
}

/**
 * 更新资料信息
 * @param materialId - 资料ID
 * @param data - 更新数据
 * @returns 更新后的资料信息
 * 
 * @example
 * ```typescript
 * await updateMaterial(1, {
 *   title: '新标题',
 *   description: '新描述',
 *   categoryId: 2
 * })
 * ```
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
 * 
 * @example
 * ```typescript
 * await deleteMaterial(1)
 * ```
 */
export const deleteMaterial = (materialId: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/materials/${materialId}`)
}

/**
 * 获取资料下载链接（预签名URL）
 * @param materialId - 资料ID
 * @param expiration - 链接过期时间（秒，默认3600）
 * @returns 预签名下载URL
 * 
 * @example
 * ```typescript
 * const result = await getDownloadUrl(1, 7200) // 2小时有效期
 * window.open(result.data, '_blank')
 * ```
 */
export const getDownloadUrl = (
  materialId: number,
  expiration: number = 3600
): Promise<ApiResponse<string>> => {
  return http.get<string>(`/api/materials/download-url/${materialId}`, { expiration })
}

// ============================================
// 分类管理
// ============================================

/**
 * 获取所有分类
 * @returns 分类列表（包含子分类）
 * 
 * @example
 * ```typescript
 * const categories = await getCategories()
 * categories.data.forEach(cat => {
 *   console.log(cat.name)
 *   cat.subcategories?.forEach(sub => console.log('  -', sub.name))
 * })
 * ```
 */
export const getCategories = (): Promise<ApiResponse<CategoryResponse[]>> => {
  return http.get<CategoryResponse[]>('/api/materials/categories')
}

/**
 * 创建分类
 * @param data - 分类请求数据
 * @returns 创建的分类信息
 * 
 * @example
 * ```typescript
 * const category = await createCategory({
 *   name: '活动策划',
 *   description: '各类活动策划相关文档',
 *   displayOrder: 1
 * })
 * ```
 */
export const createCategory = (
  data: CategoryRequest
): Promise<ApiResponse<CategoryResponse>> => {
  return http.post<CategoryResponse>('/api/materials/category', data)
}

/**
 * 更新分类信息
 * @param categoryId - 分类ID
 * @param data - 更新数据
 * @returns 更新后的分类信息
 * 
 * @example
 * ```typescript
 * await updateCategory(1, {
 *   name: '更新后的名称',
 *   description: '更新后的描述'
 * })
 * ```
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
 * 
 * @example
 * ```typescript
 * const subcategories = await getSubcategories(1)
 * ```
 */
export const getSubcategories = (
  categoryId: number
): Promise<ApiResponse<SubcategoryResponse[]>> => {
  return http.get<SubcategoryResponse[]>(`/api/materials/category/${categoryId}/subcategories`)
}

/**
 * 创建子分类
 * @param data - 子分类请求数据
 * @returns 创建的子分类信息
 * 
 * @example
 * ```typescript
 * const subcategory = await createSubcategory({
 *   categoryId: 1,
 *   name: '团建策划',
 *   description: '团队建设活动策划',
 *   displayOrder: 1
 * })
 * ```
 */
export const createSubcategory = (
  data: SubcategoryRequest
): Promise<ApiResponse<SubcategoryResponse>> => {
  return http.post<SubcategoryResponse>('/api/materials/subcategory', data)
}

/**
 * 更新子分类信息
 * @param subcategoryId - 子分类ID
 * @param data - 更新数据
 * @returns 更新后的子分类信息
 * 
 * @example
 * ```typescript
 * await updateSubcategory(1, {
 *   name: '更新后的名称',
 *   description: '更新后的描述'
 * })
 * ```
 */
export const updateSubcategory = (
  subcategoryId: number,
  data: SubcategoryRequest
): Promise<ApiResponse<SubcategoryResponse>> => {
  return http.put<SubcategoryResponse>(`/api/materials/subcategory/${subcategoryId}`, data)
}
