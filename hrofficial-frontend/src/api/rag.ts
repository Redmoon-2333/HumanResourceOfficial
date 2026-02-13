/**
 * RAG 知识库 API 模块
 *
 * 提供知识库初始化、统计查询、测试检索等功能
 * @module api/rag
 */

import { http } from '@/utils/http'
import type {
  RagInitRequest,
  RagInitResponse,
  RagStatsResponse,
  ApiResponse
} from '@/types'

// ============================================
// 知识库管理
// ============================================

/**
 * 初始化知识库
 * @param params - 初始化参数
 * @returns 初始化结果
 *
 * @example
 * ```typescript
 * // 首次初始化
 * const result = await initRag()
 *
 * // 强制重新索引
 * const result = await initRag({ forceReindex: true })
 * console.log(`处理了${result.data.processedFiles}个文件`)
 * ```
 */
export const initRag = (
  params: RagInitRequest = {}
): Promise<ApiResponse<RagInitResponse>> => {
  return http.post<RagInitResponse>('/api/rag/initialize', params)
}

/**
 * 获取知识库统计信息
 * @returns 知识库统计
 *
 * @example
 * ```typescript
 * const stats = await getRagStats()
 * console.log(`总文件数: ${stats.data.totalFiles}`)
 * console.log(`总向量数: ${stats.data.totalVectors}`)
 * console.log(`是否已初始化: ${stats.data.isInitialized}`)
 * ```
 */
export const getRagStats = (): Promise<ApiResponse<RagStatsResponse>> => {
  return http.get<RagStatsResponse>('/api/rag/stats')
}

/**
 * 获取知识库状态（getRagStats 的别名）
 * @deprecated 请使用 getRagStats
 */
export const getRagStatus = getRagStats

/**
 * 测试检索（无相似度阈值）
 * @param query - 查询文本
 * @param topK - 返回结果数量（默认10）
 * @returns 检索结果
 *
 * @example
 * ```typescript
 * const results = await testRetrieve('活动策划', 5)
 * results.data.forEach(item => console.log(item))
 * ```
 */
export const testRetrieve = (
  query: string,
  topK: number = 10
): Promise<ApiResponse<any>> => {
  return http.get<any>('/api/rag/test-retrieve', { query, topK })
}

/**
 * 调试接口：列出所有文件
 * @returns 文件列表
 *
 * @example
 * ```typescript
 * const files = await getDebugFileList()
 * console.log(files.data)
 * ```
 */
export const getDebugFileList = (): Promise<ApiResponse<any>> => {
  return http.get<any>('/api/rag/debug/list-files')
}
