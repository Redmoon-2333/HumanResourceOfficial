// API响应基础类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  content: T[]  // 后端返回的字段名
  list?: T[]    // 兼容旧字段名
  total: number
  pageNum: number
  pageSize: number
  pages?: number
  hasNext?: boolean
  hasPrevious?: boolean
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

// 活动介绍类型（区别于标准Activity）
export interface ActivityIntro {
  id: number
  activityName: string
  background: string
  significance: string
  purpose: string
  process: string
  createTime: string
  updateTime: string
  images?: ActivityImage[]
}

// 活动介绍请求
export interface ActivityIntroRequest {
  activityName: string
  background: string
  significance: string
  purpose: string
  process: string
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
  pastActivityId: number  // 后端返回的ID字段
  title: string
  coverImage: string      // 后端返回的封面图片字段
  pushUrl: string         // 后端返回的推文链接字段
  year: number
  createTime: string
  
  // 前端展示用的别名（为了兼容现有代码）
  id?: number
  coverImageUrl?: string
  articleUrl?: string
}

// AI聊天请求
export interface ChatRequest {
  message: string
}

// RAG聊天请求
export interface RagChatRequest {
  message: string
  useRAG?: boolean
  enableTools?: boolean
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
