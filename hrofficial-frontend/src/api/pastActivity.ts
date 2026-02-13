/**
 * 往届活动 API 模块
 * 
 * 提供往届活动的增删改查以及年份统计功能
 * @module api/pastActivity
 */

import { http } from '@/utils/http'
import type {
  PastActivity,
  PastActivityRequest,
  PastActivityResponse,
  PageResponse,
  ApiResponse
} from '@/types'

// ============================================
// 往届活动 CRUD 操作
// ============================================

/**
 * 分页查询往届活动
 * @param pageNum - 页码（默认1）
 * @param pageSize - 每页大小（默认10）
 * @param year - 年份筛选（可选）
 * @param title - 标题筛选（可选）
 * @returns 分页活动列表
 * 
 * @example
 * ```typescript
 * // 获取第一页，每页20条
 * const result = await getPastActivities(1, 20)
 * 
 * // 按年份筛选
 * const result = await getPastActivities(1, 10, 2023)
 * 
 * // 按标题搜索
 * const result = await getPastActivities(1, 10, undefined, '迎新')
 * ```
 */
export const getPastActivities = (
  pageNum: number = 1,
  pageSize: number = 10,
  year?: number,
  title?: string
): Promise<ApiResponse<PageResponse<PastActivityResponse>>> => {
  const params: Record<string, any> = { pageNum, pageSize }
  if (year !== undefined) {
    params.year = year
  }
  if (title) {
    params.title = title
  }
  return http.get<PageResponse<PastActivityResponse>>('/api/past-activities', params)
}

/**
 * 根据ID获取往届活动详情
 * @param id - 活动ID
 * @returns 活动详情
 * 
 * @example
 * ```typescript
 * const activity = await getPastActivityById(1)
 * console.log(activity.data.title)
 * ```
 */
export const getPastActivityById = (id: number): Promise<ApiResponse<PastActivityResponse>> => {
  return http.get<PastActivityResponse>(`/api/past-activities/${id}`)
}

/**
 * 创建往届活动
 * @param data - 活动请求数据
 * @returns 创建的活动信息
 * 
 * @example
 * ```typescript
 * const activity = await createPastActivity({
 *   title: '2023年迎新晚会',
 *   coverImage: 'https://example.com/cover.jpg',
 *   pushUrl: 'https://mp.weixin.qq.com/xxx',
 *   year: 2023
 * })
 * ```
 */
export const createPastActivity = (
  data: PastActivityRequest
): Promise<ApiResponse<PastActivityResponse>> => {
  return http.post<PastActivityResponse>('/api/past-activities', data)
}

/**
 * 更新往届活动
 * @param id - 活动ID
 * @param data - 活动更新数据
 * @returns 更新后的活动信息
 * 
 * @example
 * ```typescript
 * await updatePastActivity(1, {
 *   title: '更新后的标题',
 *   coverImage: 'https://example.com/new-cover.jpg',
 *   pushUrl: 'https://mp.weixin.qq.com/yyy',
 *   year: 2023
 * })
 * ```
 */
export const updatePastActivity = (
  id: number,
  data: PastActivityRequest
): Promise<ApiResponse<PastActivityResponse>> => {
  return http.put<PastActivityResponse>(`/api/past-activities/${id}`, data)
}

/**
 * 删除往届活动
 * @param id - 活动ID
 * @returns 删除结果
 * 
 * @example
 * ```typescript
 * await deletePastActivity(1)
 * ```
 */
export const deletePastActivity = (id: number): Promise<ApiResponse<string>> => {
  return http.delete<string>(`/api/past-activities/${id}`)
}

// ============================================
// 年份统计相关
// ============================================

/**
 * 获取所有年份列表
 * @returns 年份列表（降序排列）
 * 
 * @example
 * ```typescript
 * const years = await getPastActivityYears()
 * // [2024, 2023, 2022, ...]
 * ```
 */
export const getPastActivityYears = (): Promise<ApiResponse<number[]>> => {
  return http.get<number[]>('/api/past-activities/years')
}

/**
 * 根据年份统计活动数量
 * @param year - 年份
 * @returns 活动数量
 * 
 * @example
 * ```typescript
 * const count = await countPastActivitiesByYear(2023)
 * console.log(count.data) // 5
 * ```
 */
export const countPastActivitiesByYear = (year: number): Promise<ApiResponse<number>> => {
  return http.get<number>(`/api/past-activities/years/${year}/count`)
}
