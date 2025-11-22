import http from '@/utils/http'
import type {
  LoginRequest,
  RegisterRequest,
  User,
  ApiResponse
} from '@/types'

// 登录响应类型
interface LoginResponse {
  token: string
  user: User
  tokenType: string
}

// 登录
export const login = (data: LoginRequest) => {
  return http.post<LoginResponse>('/api/auth/login', data)
}

// 注册
export const register = (data: RegisterRequest) => {
  return http.post<string>('/api/auth/register', data)
}

// 获取当前用户信息
export const getCurrentUser = () => {
  return http.get<User>('/api/auth/current-user')
}

// 退出登录
export const logout = () => {
  return http.post('/api/auth/logout')
}
