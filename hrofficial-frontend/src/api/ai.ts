import http from '@/utils/http'
import type {
  ChatRequest,
  ChatResponse,
  PlanGeneratorRequest
} from '@/types'

// AI对话
export const chat = (data: ChatRequest) => {
  return http.post<ChatResponse>('/api/ai/chat', data)
}

// 生成策划案（非流式）
export const generatePlan = (data: PlanGeneratorRequest) => {
  return http.post<string>('/api/ai/generate-plan', data)
}

// 生成策划案（流式）
export const generatePlanStream = (
  data: PlanGeneratorRequest,
  onChunk: (chunk: string) => void
) => {
  return http.stream('/api/ai/generate-plan-stream', data, onChunk)
}
