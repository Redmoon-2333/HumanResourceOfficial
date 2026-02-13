/**
 * 性能监控 API 模块
 *
 * 提供性能报告查询和统计数据重置功能（管理员权限）
 * @module api/performance
 */

import { http } from '@/utils/http'
import type {
  PerformanceReport,
  ApiResponse
} from '@/types'

// ============================================
// 性能监控
// ============================================

/**
 * 获取性能报告
 * @returns 性能统计数据
 *
 * @example
 * ```typescript
 * const report = await getPerformanceReport()
 * console.log(`总请求数: ${report.data.totalRequests}`)
 * console.log(`平均响应时间: ${report.data.averageResponseTime}ms`)
 * console.log(`错误率: ${report.data.errorRate}%`)
 * ```
 */
export const getPerformanceReport = (): Promise<ApiResponse<PerformanceReport>> => {
  return http.get<PerformanceReport>('/api/performance/report')
}

/**
 * 重置性能统计数据
 * @returns 重置结果
 *
 * @example
 * ```typescript
 * await resetPerformanceStats()
 * console.log('性能统计已重置')
 * ```
 */
export const resetPerformanceStats = (): Promise<ApiResponse<string>> => {
  return http.post<string>('/api/performance/reset')
}
