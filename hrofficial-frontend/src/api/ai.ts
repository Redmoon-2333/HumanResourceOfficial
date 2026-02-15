/**
 * AI 对话 API 模块
 *
 * 提供AI对话、策划案生成、RAG增强对话等功能
 * @module api/ai
 */

import { http } from '@/utils/http'
import type {
  ChatRequest,
  ChatResponse,
  PlanGeneratorRequest,
  RagChatRequest,
  ApiResponse,
  StreamChunkCallback
} from '@/types'

// ============================================
// AI 对话
// ============================================

/**
 * AI对话（非流式）
 * @param data - 对话请求数据
 * @returns AI响应
 *
 * @example
 * ```typescript
 * const response = await chat({
 *   message: '你好，请介绍一下人力资源中心'
 * })
 * console.log(response.data.response)
 * ```
 */
export const chat = (
  data: ChatRequest
): Promise<ApiResponse<ChatResponse>> => {
  return http.post<ChatResponse>('/api/ai/chat', data)
}

/**
 * AI对话（流式）
 * @param data - 对话请求数据
 * @param onChunk - 流式响应回调函数
 * @param signal - 用于中断请求的AbortSignal
 * @returns 完整的AI响应内容
 *
 * @example
 * ```typescript
 * const controller = createStreamController()
 * const content = await chatStream(
 *   { message: '你好' },
 *   (chunk) => {
 *     console.log('收到:', chunk)
 *     // 实时更新UI
 *   },
 *   controller.signal
 * )
 * ```
 */
export const chatStream = (
  data: ChatRequest,
  onChunk: StreamChunkCallback,
  signal?: AbortSignal
): Promise<string> => {
  return http.stream('/api/ai/chat-stream', data, onChunk, signal)
}

// ============================================
// 策划案生成
// ============================================

/**
 * 生成活动策划案（非流式）
 * @param data - 策划案生成请求数据
 * @returns 生成的策划案内容
 *
 * @example
 * ```typescript
 * const result = await generatePlan({
 *   theme: '迎新晚会',
 *   organizer: '人力资源中心',
 *   eventTime: '2024年9月1日',
 *   eventLocation: '学生活动中心',
 *   participants: '全体新生',
 *   purpose: '欢迎新成员，增进了解'
 * })
 * console.log(result.data)
 * ```
 */
export const generatePlan = (
  data: PlanGeneratorRequest
): Promise<ApiResponse<string>> => {
  return http.post<string>('/api/ai/generate-plan', data)
}

/**
 * 生成活动策划案（流式）
 * @param data - 策划案生成请求数据
 * @param onChunk - 流式响应回调函数
 * @param signal - 用于中断请求的AbortSignal
 * @returns 完整的策划案内容
 *
 * @example
 * ```typescript
 * const controller = createStreamController()
 * const content = await generatePlanStream(
 *   { theme: '迎新晚会' },
 *   (chunk) => {
 *     // 实时显示生成内容
 *     appendContent(chunk)
 *   },
 *   controller.signal
 * )
 * ```
 */
export const generatePlanStream = (
  data: PlanGeneratorRequest,
  onChunk: StreamChunkCallback,
  signal?: AbortSignal
): Promise<string> => {
  return http.stream('/api/ai/generate-plan-stream', data, onChunk, signal)
}

// ============================================
// RAG 增强对话
// ============================================

/**
 * RAG增强的AI流式对话
 * @param data - RAG对话请求数据
 * @param onChunk - 流式响应回调函数
 * @param signal - 用于中断请求的AbortSignal
 * @returns 完整的AI响应内容
 *
 * @example
 * ```typescript
 * const controller = createStreamController()
 * const content = await chatWithRag(
 *   {
 *     message: '介绍一下往届的迎新活动',
 *     useRAG: true,
 *     enableTools: true
 *   },
 *   (chunk) => {
 *     console.log('收到:', chunk)
 *   },
 *   controller.signal
 * )
 * ```
 */
export const chatWithRag = (
  data: RagChatRequest,
  onChunk: StreamChunkCallback,
  signal?: AbortSignal
): Promise<string> => {
  return http.stream('/api/ai/chat-with-rag', data, onChunk, signal)
}

// ============================================
// 流式请求控制
// ============================================

/**
 * 创建可中断的流式请求控制器
 * @returns 包含signal和abort方法的对象
 *
 * @example
 * ```typescript
 * const { signal, abort } = createStreamController()
 *
 * // 开始流式请求
 * generatePlanStream(data, onChunk, signal)
 *
 * // 用户点击停止按钮时
 * abort()
 * ```
 */
export const createStreamController = (): {
  signal: AbortSignal
  abort: () => void
} => {
  return http.createStreamRequest()
}

/**
 * AI对话（简化版，chat的别名）
 * @deprecated 请使用 chat
 */
export const chatWithAI = chat

// ============================================
// 对话历史管理
// ============================================

/**
 * 对话历史消息记录
 */
export interface ChatHistoryMessage {
  index: number
  role: 'user' | 'assistant'
  content: string
  rawType: string
}

/**
 * 对话历史统计信息
 */
export interface ChatHistoryStats {
  totalMessages: number
  userMessages: number
  assistantMessages: number
  systemMessages: number
  maxMessages: number
  maxPairs: number
}

/**
 * 获取当前用户的对话历史
 * @returns 对话历史和统计信息
 *
 * @example
 * ```typescript
 * const { history, stats } = await getChatHistory()
 * console.log(`当前有${stats.userMessages}条用户消息`)
 * ```
 */
export const getChatHistory = (): Promise<
  ApiResponse<{ history: ChatHistoryMessage[]; stats: ChatHistoryStats }>
> => {
  return http.get('/api/ai/chat-history')
}

/**
 * 删除当前用户的对话历史
 * @returns 删除结果
 *
 * @example
 * ```typescript
 * await clearChatHistory()
 * console.log('对话历史已清除')
 * ```
 */
export const clearChatHistory = (): Promise<ApiResponse<void>> => {
  return http.delete('/api/ai/chat-history')
}
