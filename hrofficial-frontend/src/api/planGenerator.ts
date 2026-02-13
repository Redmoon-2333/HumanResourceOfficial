import http from '@/utils/http'
import type { StreamChunkCallback } from '@/types'

/**
 * 策划案生成接口
 * 用于AI生成活动策划案
 *
 * @author 人力资源中心技术组
 * @since 2026-02-13
 */

// 策划案生成请求接口 - 与后端 PlanGeneratorRequest 完全对应
export interface PlanGeneratorRequest {
  /** 活动主题/名称 */
  theme: string
  /** 主办单位 */
  organizer: string
  /** 活动时间 */
  eventTime: string
  /** 活动地点 */
  eventLocation: string
  /** 工作人员 */
  staff: string
  /** 参与人员 */
  participants: string
  /** 活动目的 */
  purpose: string
  /** 部长/副部长数量 */
  leaderCount: number
  /** 部员数量 */
  memberCount: number
}

// API响应接口
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 生成活动策划案
 * 同步接口，返回完整的HTML格式策划案
 * @param data 策划案生成请求参数
 * @returns HTML格式的策划案内容
 */
export const generatePlan = (data: PlanGeneratorRequest): Promise<ApiResponse<string>> => {
  return http.post('/api/ai/generate-plan', data)
}

/**
 * 流式生成活动策划案
 * 使用http.stream方法实现流式返回
 * @param data 策划案生成请求参数
 * @param onChunk 流式响应回调函数
 * @param signal 用于中断请求的AbortSignal
 * @returns 完整的策划案内容
 */
export const generatePlanStream = (
  data: PlanGeneratorRequest,
  onChunk: StreamChunkCallback,
  signal?: AbortSignal
): Promise<string> => {
  return http.stream('/api/ai/generate-plan-stream', data, onChunk, signal)
}

/**
 * 创建可中断的流式请求控制器
 * @returns 包含signal和abort方法的对象
 */
export const createStreamController = (): {
  signal: AbortSignal
  abort: () => void
} => {
  return http.createStreamRequest()
}
