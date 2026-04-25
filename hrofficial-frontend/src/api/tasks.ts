import { http } from '@/utils/http'
import type { ApiResponse, TaskDetail, TaskCreateRequest, TaskUpdateRequest, Candidate, Assignment, MPPageResponse } from '@/types'

export const createTask = (data: TaskCreateRequest): Promise<ApiResponse<TaskDetail>> => {
  return http.post<TaskDetail>('/api/tasks', data)
}

export const getMyTasks = (params: {
  page?: number
  pageSize?: number
}): Promise<ApiResponse<MPPageResponse<Assignment>>> => {
  return http.get<MPPageResponse<Assignment>>('/api/tasks/mine', params)
}

export const getCreatedTasks = (params: {
  page?: number
  pageSize?: number
}): Promise<ApiResponse<MPPageResponse<TaskDetail>>> => {
  return http.get<MPPageResponse<TaskDetail>>('/api/tasks/created', params)
}

export const getTaskDetail = (id: number): Promise<ApiResponse<TaskDetail>> => {
  return http.get<TaskDetail>(`/api/tasks/${id}`)
}

export const markTaskDone = (assignmentId: number, remark?: string): Promise<ApiResponse<void>> => {
  return http.post<void>(`/api/tasks/assignments/${assignmentId}/done`, { doneRemark: remark })
}

export const remindTask = (assignmentId: number): Promise<ApiResponse<void>> => {
  return http.post<void>(`/api/tasks/assignments/${assignmentId}/remind`)
}

export const updateTask = (id: number, data: TaskUpdateRequest): Promise<ApiResponse<TaskDetail>> => {
  return http.put<TaskDetail>(`/api/tasks/${id}`, data)
}

export const deleteTask = (id: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/tasks/${id}`)
}

export const getCandidates = (): Promise<ApiResponse<Candidate[]>> => {
  return http.get<Candidate[]>('/api/tasks/candidates')
}
