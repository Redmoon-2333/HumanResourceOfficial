import http from '@/utils/http'
import type {
  Activity,
  ActivityRequest,
  ActivityImage,
  PageResponse
} from '@/types'

// 获取活动列表
export const getActivities = (params?: {
  pageNum?: number
  pageSize?: number
  status?: string
  keyword?: string
}) => {
  const query = new URLSearchParams(params as any).toString()
  return http.get<PageResponse<Activity>>(`/api/activities${query ? '?' + query : ''}`)
}

// 获取活动详情
export const getActivity = (id: number) => {
  return http.get<Activity>(`/api/activities/${id}`)
}

// 创建活动
export const createActivity = (data: ActivityRequest) => {
  return http.post<Activity>('/api/activities', data)
}

// 更新活动
export const updateActivity = (id: number, data: ActivityRequest) => {
  return http.put<Activity>(`/api/activities/${id}`, data)
}

// 删除活动
export const deleteActivity = (id: number) => {
  return http.delete(`/api/activities/${id}`)
}

// 获取活动图片
export const getActivityImages = (activityId: number) => {
  return http.get<ActivityImage[]>(`/api/activities/${activityId}/images`)
}

// 上传活动图片
export const uploadActivityImage = (
  activityId: number,
  file: File,
  description?: string,
  onProgress?: (percent: number) => void
) => {
  return http.upload<ActivityImage>(
    `/api/activities/${activityId}/images?description=${description || ''}`,
    file,
    onProgress
  )
}

// 删除活动图片
export const deleteActivityImage = (activityId: number, imageId: number) => {
  return http.delete(`/api/activities/${activityId}/images/${imageId}`)
}
