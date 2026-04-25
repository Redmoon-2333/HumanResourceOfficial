import { http } from '@/utils/http'
import type { ApiResponse, Message, UnreadCount, MPPageResponse } from '@/types'

export const getMessages = (params: {
  page?: number
  pageSize?: number
  type?: string
  isRead?: boolean
}): Promise<ApiResponse<MPPageResponse<Message>>> => {
  return http.get<MPPageResponse<Message>>('/api/messages', params)
}

export const getUnreadCount = (): Promise<ApiResponse<UnreadCount>> => {
  return http.get<UnreadCount>('/api/messages/unread-count')
}

export const markAsRead = (id: number): Promise<ApiResponse<void>> => {
  return http.post<void>(`/api/messages/${id}/read`)
}

export const markAllAsRead = (): Promise<ApiResponse<void>> => {
  return http.post<void>('/api/messages/read-all')
}

export const deleteMessage = (id: number): Promise<ApiResponse<void>> => {
  return http.delete<void>(`/api/messages/${id}`)
}

export const sendMessage = (data: {
  receiverId: number
  title: string
  content: string
  type?: string
}): Promise<ApiResponse<Message>> => {
  return http.post<Message>('/api/messages', data)
}
