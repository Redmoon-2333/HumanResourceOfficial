import { http } from '@/utils/http'
import type { ApiResponse, PageResponse, PastActivityResponse, PastActivityRequest } from '@/types'

/**
 * 获取往届活动列表（分页）
 * @param pageNum - 页码（默认1）
 * @param pageSize - 每页大小（默认10）
 * @param year - 年份筛选（可选）
 * @param title - 标题筛选（可选）
 * @returns 分页活动列表
 */
export function getPastActivities(
  pageNum: number = 1,
  pageSize: number = 10,
  year?: number,
  title?: string
): Promise<ApiResponse<PageResponse<PastActivityResponse>>> {
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
 * @param id - 往届活动ID
 * @returns 往届活动详情
 */
export function getPastActivityById(id: number): Promise<ApiResponse<PastActivityResponse>> {
  return http.get<PastActivityResponse>(`/api/past-activities/${id}`)
}

/**
 * 创建往届活动
 * @param data - 往届活动数据
 * @returns 创建的往届活动信息
 */
export function createPastActivity(data: PastActivityRequest): Promise<ApiResponse<PastActivityResponse>> {
  return http.post<PastActivityResponse>('/api/past-activities', data)
}

/**
 * 更新往届活动
 * @param id - 往届活动ID
 * @param data - 往届活动更新数据
 * @returns 更新后的往届活动信息
 */
export function updatePastActivity(id: number, data: PastActivityRequest): Promise<ApiResponse<PastActivityResponse>> {
  return http.put<PastActivityResponse>(`/api/past-activities/${id}`, data)
}

/**
 * 删除往届活动
 * @param id - 往届活动ID
 * @returns 删除结果
 */
export function deletePastActivity(id: number): Promise<ApiResponse<string>> {
  return http.delete<string>(`/api/past-activities/${id}`)
}

/**
 * 获取所有年份列表
 * @returns 年份列表
 */
export function getAllYears(): Promise<ApiResponse<number[]>> {
  return http.get<number[]>('/api/past-activities/years')
}

/**
 * 根据年份统计活动数量
 * @param year - 年份
 * @returns 活动数量
 */
export function countActivitiesByYear(year: number): Promise<ApiResponse<number>> {
  return http.get<number>(`/api/past-activities/years/${year}/count`)
}
