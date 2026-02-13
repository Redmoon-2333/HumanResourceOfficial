/**
 * 用户管理 API 模块
 *
 * 提供用户信息查询、往届成员查询、激活码管理等功能
 * @module api/user
 */

import { http } from '@/utils/http'
import type {
  User,
  PublicUserInfo,
  ActivationCode,
  ActivationCodeListResponse,
  AlumniResponse,
  ApiResponse
} from '@/types'

// ============================================
// 用户信息管理
// ============================================

/**
 * 获取当前登录用户信息
 * @returns 当前用户信息
 *
 * @example
 * ```typescript
 * const user = await getCurrentUser()
 * console.log(user.data.name)
 * ```
 */
export const getCurrentUser = (): Promise<ApiResponse<Record<string, any>>> => {
  return http.get<Record<string, any>>('/api/auth/current-user')
}

/**
 * 更新用户个人信息
 * @param data - 更新数据
 * @returns 更新后的用户信息
 *
 * @example
 * ```typescript
 * await updateProfile({
 *   name: '张三',
 *   roleHistory: '部长'
 * })
 * ```
 */
export const updateProfile = (
  data: Record<string, string>
): Promise<ApiResponse<Record<string, any>>> => {
  return http.put<Record<string, any>>('/api/auth/update-profile', data)
}

// ============================================
// 往届成员查询
// ============================================

/**
 * 获取往届部员信息（按年份分组）
 * @returns 往届成员列表（按年份分组）
 *
 * @example
 * ```typescript
 * const alumni = await getAlumni()
 * alumni.data.forEach(group => {
 *   console.log(`${group.year}年:`)
 *   group.members.forEach(m => console.log(`  ${m.name} - ${m.role}`))
 * })
 * ```
 */
export const getAlumni = (): Promise<ApiResponse<AlumniResponse[]>> => {
  return http.get<AlumniResponse[]>('/api/users/alumni')
}

/**
 * 根据姓名精确查找用户
 * @param name - 用户姓名
 * @returns 匹配的用户列表
 *
 * @example
 * ```typescript
 * const users = await searchUsersByName('张三')
 * ```
 */
export const searchUsersByName = (
  name: string
): Promise<ApiResponse<PublicUserInfo[]>> => {
  return http.get<PublicUserInfo[]>('/api/users/search/name', { name })
}

/**
 * 根据姓名模糊查找用户
 * @param name - 搜索关键词
 * @returns 匹配的用户列表
 *
 * @example
 * ```typescript
 * const users = await searchUsersByNameLike('张')
 * ```
 */
export const searchUsersByNameLike = (
  name: string
): Promise<ApiResponse<PublicUserInfo[]>> => {
  return http.get<PublicUserInfo[]>('/api/users/search/name/like', { name })
}

// ============================================
// 激活码管理
// ============================================

/**
 * 获取当前用户生成的所有激活码
 * @returns 激活码列表及统计数据
 *
 * @example
 * ```typescript
 * const result = await getActivationCodes()
 * console.log(result.data.stats.totalCount) // 总数
 * console.log(result.data.stats.usedCount)  // 已使用
 * console.log(result.data.stats.unusedCount) // 未使用
 * console.log(result.data.stats.expiredCount) // 已过期
 * result.data.codes.forEach(code => {
 *   console.log(code.code, code.used ? '已使用' : '未使用')
 * })
 * ```
 */
export const getActivationCodes = (): Promise<ApiResponse<ActivationCodeListResponse>> => {
  return http.get<ActivationCodeListResponse>('/api/users/activation-codes')
}

/**
 * 生成新的激活码
 * @param expireDays - 过期天数（默认30天）
 * @returns 生成的激活码信息
 *
 * @example
 * ```typescript
 * const code = await generateActivationCode(7) // 7天有效期
 * console.log(code.data.code)
 * ```
 */
export const generateActivationCode = (
  expireDays: number = 30
): Promise<ApiResponse<ActivationCode>> => {
  return http.post<ActivationCode>('/api/auth/generate-code', { expireDays })
}

/**
 * 删除激活码
 * @param codeId - 激活码ID
 * @returns 删除结果
 *
 * @example
 * ```typescript
 * await deleteActivationCode(123)
 * ```
 */
export const deleteActivationCode = (
  codeId: number
): Promise<ApiResponse<string>> => {
  return http.put<string>(`/api/users/activation-codes/${codeId}/delete`)
}

// ============================================
// 系统管理
// ============================================

/**
 * 手动清理Redis内存中的过期对话
 * @returns 清理结果
 *
 * @example
 * ```typescript
 * const result = await cleanupRedisMemory()
 * console.log(result.data)
 * ```
 */
export const cleanupRedisMemory = (): Promise<ApiResponse<Record<string, any>>> => {
  return http.post<Record<string, any>>('/api/users/cleanup-memory')
}

/**
 * 强制指定用户下线（管理员权限）
 * @param targetUserId - 目标用户ID
 * @returns 操作结果
 *
 * @example
 * ```typescript
 * await revokeUserTokens(123)
 * ```
 */
export const revokeUserTokens = (
  targetUserId: number
): Promise<ApiResponse<Record<string, any>>> => {
  return http.post<Record<string, any>>(`/api/auth/revoke-user/${targetUserId}`)
}

/**
 * 调试接口：获取所有用户信息（仅开发环境使用）
 * @returns 所有用户列表
 *
 * @example
 * ```typescript
 * const users = await getAllUsersDebug()
 * ```
 */
export const getAllUsersDebug = (): Promise<ApiResponse<User[]>> => {
  return http.get<User[]>('/api/users/debug/all')
}
