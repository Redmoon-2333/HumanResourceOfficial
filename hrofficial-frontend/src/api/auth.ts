/**
 * 认证授权 API 模块
 *
 * 提供用户登录、注册、退出等认证功能
 * @module api/auth
 */

import { http } from '@/utils/http'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  User,
  ApiResponse
} from '@/types'

// ============================================
// 认证操作
// ============================================

/**
 * 用户登录
 * @param data - 登录请求数据
 * @returns 登录结果（包含token和用户信息）
 *
 * @example
 * ```typescript
 * const result = await login({
 *   username: 'zhangsan',
 *   password: 'password123'
 * })
 * // 保存token
 * localStorage.setItem('token', result.data.token)
 * ```
 */
export const login = (
  data: LoginRequest
): Promise<ApiResponse<LoginResponse>> => {
  return http.post<LoginResponse>('/api/auth/login', data)
}

/**
 * 用户注册
 * @param data - 注册请求数据
 * @returns 注册结果
 *
 * @example
 * ```typescript
 * const result = await register({
 *   username: 'zhangsan',
 *   password: 'password123',
 *   name: '张三',
 *   phone: '13800138000',
 *   email: 'zhangsan@example.com',
 *   studentId: '2021001001',
 *   grade: '2021级',
 *   major: '计算机科学与技术',
 *   activationCode: 'ABC123'
 * })
 * ```
 */
export const register = (
  data: RegisterRequest
): Promise<ApiResponse<LoginResponse>> => {
  return http.post<LoginResponse>('/api/auth/register', data)
}

/**
 * 用户退出登录
 * @returns 退出结果
 *
 * @example
 * ```typescript
 * await logout()
 * localStorage.removeItem('token')
 * ```
 */
export const logout = (): Promise<ApiResponse<Record<string, any>>> => {
  return http.post<Record<string, any>>('/api/auth/logout')
}
