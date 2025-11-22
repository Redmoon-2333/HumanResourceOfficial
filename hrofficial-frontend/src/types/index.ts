// API响应基础类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// 用户类型
export interface User {
  id: number
  username: string
  name: string
  phone: string
  email: string
  studentId: string
  grade: string
  major: string
  roleHistory: string
  joinDate: string
  createTime: string
  updateTime: string
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  password: string
  name: string
  phone: string
  email: string
  studentId: string
  grade: string
  major: string
  activationCode: string
}

// 活动类型
export interface Activity {
  id: number
  title: string
  description: string
  startTime: string
  endTime: string
  location: string
  organizer: string
  participantCount: number
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
  creatorId: number
  creatorName: string
  createTime: string
  updateTime: string
  images?: ActivityImage[]
}

// 活动图片
export interface ActivityImage {
  id: number
  activityId: number
  imageUrl: string
  description: string
  displayOrder: number
  uploadTime: string
}

// 活动请求
export interface ActivityRequest {
  title: string
  description: string
  startTime: string
  endTime: string
  location: string
  organizer: string
  participantCount: number
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
}

// 资料类型
export interface Material {
  id: number
  title: string
  description: string
  fileUrl: string
  fileName: string
  fileSize: number
  fileType: string
  categoryId: number
  categoryName: string
  subcategoryId: number
  subcategoryName: string
  uploaderId: number
  uploaderName: string
  downloadCount: number
  uploadTime: string
}

// 资料分类
export interface MaterialCategory {
  id: number
  name: string
  description: string
  displayOrder: number
  subcategories?: MaterialSubcategory[]
}

// 资料子分类
export interface MaterialSubcategory {
  id: number
  categoryId: number
  name: string
  description: string
  displayOrder: number
}

// 往届活动
export interface PastActivity {
  id: number
  title: string
  description: string
  activityDate: string
  year: number
  imageUrl: string
  participantCount: number
  createTime: string
}

// AI聊天请求
export interface ChatRequest {
  message: string
}

// AI聊天响应
export interface ChatResponse {
  response: string
  conversationId: string
}

// 策划案生成请求
export interface PlanGeneratorRequest {
  theme: string
  organizer?: string
  eventTime?: string
  eventLocation?: string
  staff?: string
  participants?: string
  purpose?: string
  leaderCount?: number
  memberCount?: number
}

// 往届成员（简化版，仅包含姓名和当年职位）
export interface AlumniMember {
  name: string
  role: string  // 当年的职位（单一）
}

// 往届成员年份分组
export interface AlumniYearGroup {
  year: number
  members: AlumniMember[]
}

// OSS预签名URL请求
export interface PresignedUrlRequest {
  fileName: string
  fileType: string
}

// OSS预签名URL响应
export interface PresignedUrlResponse {
  uploadUrl: string
  fileUrl: string
  fileName: string
  expiresIn: number
}
