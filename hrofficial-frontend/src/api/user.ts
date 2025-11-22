import http from '@/utils/http'
import type {
  AlumniMember,
  PageResponse
} from '@/types'

// 获取往届成员列表
export const getAlumni = (params?: {
  pageNum?: number
  pageSize?: number
  grade?: string
  roleHistory?: string
  keyword?: string
}) => {
  const query = new URLSearchParams(params as any).toString()
  return http.get<PageResponse<AlumniMember>>(`/api/users/alumni${query ? '?' + query : ''}`)
}

// 根据ID获取往届成员
export const getAlumniById = (id: number) => {
  return http.get<AlumniMember>(`/api/users/alumni/${id}`)
}

// 搜索往届成员
export const searchAlumni = (keyword: string) => {
  return http.get<AlumniMember[]>(`/api/users/alumni/search?keyword=${keyword}`)
}
