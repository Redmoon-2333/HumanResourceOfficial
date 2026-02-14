// ============================================
// API响应基础类型
// ============================================

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// ============================================
// 分页响应
// ============================================

export interface PageResponse<T> {
  content: T[]
  list?: T[]
  total: number
  pageNum: number
  pageSize: number
  pages?: number
  hasNext?: boolean
  hasPrevious?: boolean
}

// ============================================
// 用户相关类型
// ============================================

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

// 公开用户信息
export interface PublicUserInfo {
  id: number
  name: string
  grade: string
  major: string
  roleHistory: string
  joinDate: string
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  user: User
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

// 更新用户信息请求
export interface UpdateProfileRequest {
  name?: string
  phone?: string
  email?: string
  grade?: string
  major?: string
}

// ============================================
// 活动相关类型
// ============================================

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

// 活动请求
export interface ActivityRequest {
  activityName: string
  description: string
  startTime: string
  endTime: string
  location: string
  organizer: string
  participantCount: number
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
}

// 活动响应
export interface ActivityResponse {
  activityId: number
  activityName: string
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
  images?: ActivityImageDTO[]
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

// 活动图片DTO
export interface ActivityImageDTO {
  id: number
  activityId: number
  imageUrl: string
  description: string
  sortOrder: number
  displayOrder?: number  // 兼容字段
  uploadTime: string
}

// 活动介绍类型
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

// ============================================
// 往届活动相关类型
// ============================================

// 往届活动
export interface PastActivity {
  pastActivityId: number
  title: string
  coverImage: string
  pushUrl: string
  year: number
  createTime: string
  id?: number
  coverImageUrl?: string
  articleUrl?: string
}

// 往届活动请求
export interface PastActivityRequest {
  title: string
  coverImage: string
  pushUrl: string
  year: number
}

// 往届活动响应
export interface PastActivityResponse {
  pastActivityId: number
  title: string
  coverImage: string
  pushUrl: string
  year: number
  createTime: string
}

// ============================================
// 资料相关类型
// ============================================

// 资料类型 - 与后端 MaterialResponse 匹配
export interface Material {
  materialId: number
  categoryId: number
  subcategoryId: number
  materialName: string
  description: string | null
  fileUrl: string
  fileSize: number
  fileType: string
  uploadTime: string
  uploaderId: number
  downloadCount: number
  categoryName: string | null
  subcategoryName: string | null
}

// 资料请求 - 与后端 MaterialRequest 匹配
export interface MaterialRequest {
  materialName?: string
  description?: string
  categoryId?: number
  subcategoryId?: number
}

// 资料响应 - 与后端 MaterialResponse 匹配
export interface MaterialResponse {
  materialId: number
  categoryId: number
  subcategoryId: number
  materialName: string
  description: string | null
  fileUrl: string
  fileSize: number
  fileType: string
  uploadTime: string
  uploaderId: number
  downloadCount: number
  categoryName: string | null
  subcategoryName: string | null
}

// 资料分类 - 与后端 CategoryResponse 匹配
export interface MaterialCategory {
  categoryId: number
  categoryName: string
  sortOrder: number
  createTime: string
  subcategories?: MaterialSubcategory[]
}

// 分类请求 - 与后端 CategoryRequest 匹配
export interface CategoryRequest {
  categoryName: string
  sortOrder?: number
}

// 分类响应 - 与后端 CategoryResponse 匹配
export interface CategoryResponse {
  categoryId: number
  categoryName: string
  sortOrder: number
  createTime: string
  subcategories?: SubcategoryResponse[]
}

// 资料子分类 - 与后端 SubcategoryResponse 匹配
export interface MaterialSubcategory {
  subcategoryId: number
  categoryId: number
  subcategoryName: string
  sortOrder: number
  createTime: string
}

// 子分类请求 - 与后端 SubcategoryRequest 匹配
export interface SubcategoryRequest {
  categoryId: number
  subcategoryName: string
  sortOrder?: number
}

// 子分类响应 - 与后端 SubcategoryResponse 匹配
export interface SubcategoryResponse {
  subcategoryId: number
  categoryId: number
  subcategoryName: string
  sortOrder: number
  createTime: string
}

// ============================================
// AI相关类型
// ============================================

// AI聊天请求
export interface ChatRequest {
  message: string
}

// AI聊天响应
export interface ChatResponse {
  response: string
  conversationId: string
}

// RAG聊天请求
export interface RagChatRequest {
  message: string
  useRAG?: boolean
  enableTools?: boolean
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

// ============================================
// RAG知识库相关类型
// ============================================

// RAG初始化请求
export interface RagInitRequest {
  forceReindex?: boolean
}

// RAG初始化响应 - 与后端RagInitResponse.java对齐
export interface RagInitResponse {
  totalFiles: number
  processedFiles: number
  failedFiles: number
  totalChunks: number
  newChunks: number
  duplicateChunks: number
  errors?: Array<{
    fileName: string
    reason: string
  }>
}

// RAG统计响应 - 与后端RagStatsResponse.java对齐
export interface RagStatsResponse {
  totalDocuments: number
  totalVectors: number
  categoryStats?: Record<string, number>
  lastUpdateTime: string
  collectionName: string
  vectorDimension: number
}

// ============================================
// 激活码相关类型
// ============================================

// 激活码
export interface ActivationCode {
  id: number
  code: string
  createdBy: number
  creatorName: string
  createTime: string
  expireTime?: string
  used: boolean
  usedBy?: number
  usedByName?: string
  usedTime?: string
}

// 激活码统计数据
export interface ActivationCodeStats {
  totalCount: number
  unusedCount: number
  usedCount: number
  expiredCount: number
  // 兼容字段别名
  total?: number
  unused?: number
  used?: number
  expired?: number
}

// 激活码列表响应（包含统计数据）
export interface ActivationCodeListResponse {
  codes: ActivationCode[]
  stats: ActivationCodeStats
  // 兼容字段别名
  list?: ActivationCode[]
  activationCodes?: ActivationCode[]
  statistics?: ActivationCodeStats
}

// ============================================
// 往届成员相关类型
// ============================================

// 往届成员
export interface AlumniMember {
  name: string
  role: string
}

// 往届成员年份分组
export interface AlumniYearGroup {
  year: number
  members: AlumniMember[]
}

// 往届成员响应
export interface AlumniResponse {
  year: number
  members: AlumniMember[]
}

// ============================================
// OSS相关类型
// ============================================

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

// ============================================
// 性能监控相关类型
// ============================================

// 性能报告
export interface PerformanceReport {
  totalRequests: number
  averageResponseTime: number
  maxResponseTime: number
  minResponseTime: number
  errorRate: number
  requestCountByEndpoint: Record<string, number>
  responseTimeByEndpoint: Record<string, number>
}

// ============================================
// 通用工具类型
// ============================================

// 上传进度回调
export type UploadProgressCallback = (percent: number) => void

// 流式响应回调
export type StreamChunkCallback = (chunk: string) => void
