import { http } from '@/utils/http'
import type { ApiResponse, RoleUpdateRequest, User, MPPageResponse } from '@/types'

export const getUsers = (params: {
  page?: number
  pageSize?: number
}): Promise<ApiResponse<MPPageResponse<User>>> => {
  return http.get<MPPageResponse<User>>('/api/roles/users', params)
}

export const updateRole = (userId: number, data: RoleUpdateRequest): Promise<ApiResponse<void>> => {
  return http.put<void>(`/api/roles/users/${userId}`, data)
}

export const appointMinister = (userId: number, data: { year: number; reason?: string }): Promise<ApiResponse<void>> => {
  return http.post<void>(`/api/roles/users/${userId}/appoint-minister`, data)
}

export const appointDeputy = (userId: number, data: { year: number; reason?: string }): Promise<ApiResponse<void>> => {
  return http.post<void>(`/api/roles/users/${userId}/appoint-deputy`, data)
}
