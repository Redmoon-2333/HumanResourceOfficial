import http from '@/utils/http'
import type {
  AlumniMember,
  AlumniYearGroup,
  PageResponse
} from '@/types'

// 获取往届成员列表（按年份分组）
export const getAlumni = () => {
  return http.get<AlumniYearGroup[]>('/api/users/alumni')
}

// 根据ID获取往届成员
export const getAlumniById = (id: number) => {
  return http.get<AlumniMember>(`/api/users/alumni/${id}`)
}

// 搜索往届成员
export const searchAlumni = (keyword: string) => {
  return http.get<AlumniMember[]>(`/api/users/alumni/search?keyword=${keyword}`)
}
