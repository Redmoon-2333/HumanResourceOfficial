import http from '@/utils/http'
import type {
  PastActivity,
  PageResponse
} from '@/types'

// 获取往届活动列表
export const getPastActivities = (params?: {
  pageNum?: number
  pageSize?: number
  year?: number
}) => {
  const query = new URLSearchParams(params as any).toString()
  return http.get<PageResponse<PastActivity>>(`/api/past-activities${query ? '?' + query : ''}`)
}

// 创建往届活动
export const createPastActivity = (data: {
  title: string
  description?: string
  articleUrl: string
  coverImageUrl: string
  year: number
  activityDate?: string
}) => {
  // 转换字段名以匹配后端
  const requestData = {
    title: data.title,
    coverImage: data.coverImageUrl,
    pushUrl: data.articleUrl,
    year: data.year
  }
  return http.post<PastActivity>('/api/past-activities', requestData)
}

// 更新往届活动
export const updatePastActivity = (id: number, data: {
  title: string
  description?: string
  articleUrl: string
  coverImageUrl: string
  year: number
  activityDate?: string
}) => {
  const requestData = {
    title: data.title,
    coverImage: data.coverImageUrl,
    pushUrl: data.articleUrl,
    year: data.year
  }
  return http.put<PastActivity>(`/api/past-activities/${id}`, requestData)
}

// 删除往届活动
export const deletePastActivity = (id: number) => {
  return http.delete(`/api/past-activities/${id}`)
}

// 获取所有年份
export const getYears = () => {
  return http.get<number[]>('/api/past-activities/years')
}
