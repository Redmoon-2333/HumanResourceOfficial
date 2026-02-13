# API 接口文档

本文档详细说明了人力资源中心官网前端与后端的所有接口交互。

## 目录

- [认证授权 (Auth)](#认证授权-auth)
- [用户管理 (User)](#用户管理-user)
- [活动管理 (Activity)](#活动管理-activity)
- [往届活动 (PastActivity)](#往届活动-pastactivity)
- [资料管理 (Material)](#资料管理-material)
- [AI 对话 (AI)](#ai-对话-ai)
- [RAG 知识库 (RAG)](#rag-知识库-rag)
- [OSS 存储 (OSS)](#oss-存储-oss)
- [性能监控 (Performance)](#性能监控-performance)

## 认证授权 (Auth)

### 登录

```typescript
import { login } from '@/api'

const result = await login({
  username: 'zhangsan',
  password: 'password123'
})
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**响应数据：**

```typescript
{
  code: 200,
  message: '登录成功',
  data: {
    token: 'eyJhbGciOiJIUzI1NiIs...',
    user: {
      id: 1,
      username: 'zhangsan',
      name: '张三',
      // ...
    }
  }
}
```

### 注册

```typescript
import { register } from '@/api'

const result = await register({
  username: 'zhangsan',
  password: 'password123',
  name: '张三',
  phone: '13800138000',
  email: 'zhangsan@example.com',
  studentId: '2021001001',
  grade: '2021级',
  major: '计算机科学与技术',
  activationCode: 'ABC123'
})
```

### 退出登录

```typescript
import { logout } from '@/api'

await logout()
```

## 用户管理 (User)

### 获取当前用户信息

```typescript
import { getCurrentUser } from '@/api'

const user = await getCurrentUser()
```

### 更新个人信息

```typescript
import { updateProfile } from '@/api'

await updateProfile({
  name: '张三',
  phone: '13800138000',
  email: 'zhangsan@example.com'
})
```

### 获取往届成员

```typescript
import { getAlumni } from '@/api'

const alumni = await getAlumni()
// 返回按年份分组的成员列表
```

### 激活码管理

```typescript
import { getActivationCodes, generateActivationCode, deleteActivationCode } from '@/api'

// 获取激活码列表
const codes = await getActivationCodes()

// 生成新激活码（7天有效期）
const newCode = await generateActivationCode(7)

// 删除激活码
await deleteActivationCode('code-id')
```

## 活动管理 (Activity)

### CRUD 操作

```typescript
import {
  createActivity,
  getActivities,
  getActivityById,
  updateActivity,
  deleteActivity
} from '@/api'

// 创建活动
const activity = await createActivity({
  title: '迎新晚会',
  description: '欢迎新成员加入',
  startTime: '2024-09-01T19:00:00',
  endTime: '2024-09-01T21:00:00',
  location: '学生活动中心',
  organizer: '人力资源中心',
  participantCount: 100,
  status: 'PUBLISHED'
})

// 获取所有活动
const activities = await getActivities()

// 获取活动详情
const detail = await getActivityById(1)

// 更新活动
await updateActivity(1, { ...activityData })

// 删除活动
await deleteActivity(1)
```

### 图片管理

```typescript
import {
  uploadActivityImage,
  addImageToActivity,
  getActivityImages,
  updateActivityImage,
  deleteActivityImage
} from '@/api'

// 上传图片
const result = await uploadActivityImage(file, (percent) => {
  console.log(`上传进度: ${percent}%`)
})

// 为活动添加图片
await addImageToActivity(1, file, '活动海报', 1)

// 获取活动图片
const images = await getActivityImages(1)

// 更新图片信息
await updateActivityImage(1, '新描述', 2)

// 删除图片
await deleteActivityImage(1)
```

## 往届活动 (PastActivity)

```typescript
import {
  getPastActivities,
  getPastActivityById,
  createPastActivity,
  updatePastActivity,
  deletePastActivity,
  getPastActivityYears,
  countPastActivitiesByYear
} from '@/api'

// 分页查询（第1页，每页20条，2023年的活动）
const activities = await getPastActivities(1, 20, 2023)

// 获取所有年份
const years = await getPastActivityYears()

// 统计某年的活动数量
const count = await countPastActivitiesByYear(2023)
```

## 资料管理 (Material)

### 资料文件操作

```typescript
import {
  uploadMaterial,
  getMaterials,
  getMaterialsByCategory,
  getMaterialsBySubcategory,
  getMaterialById,
  searchMaterials,
  updateMaterial,
  deleteMaterial,
  getDownloadUrl
} from '@/api'

// 上传资料
const result = await uploadMaterial(
  file,
  1, // categoryId
  2, // subcategoryId
  '资料名称',
  '资料描述',
  (percent) => console.log(`${percent}%`)
)

// 获取下载链接
const download = await getDownloadUrl(1, 7200) // 2小时有效期
window.open(download.data, '_blank')
```

### 分类管理

```typescript
import {
  getCategories,
  createCategory,
  updateCategory,
  getSubcategories,
  createSubcategory,
  updateSubcategory
} from '@/api'

// 获取所有分类（包含子分类）
const categories = await getCategories()

// 创建分类
await createCategory({
  name: '活动策划',
  description: '各类活动策划文档',
  displayOrder: 1
})
```

## AI 对话 (AI)

### 普通对话

```typescript
import { chat, chatStream, createStreamController } from '@/api'

// 非流式
const response = await chat({ message: '你好' })

// 流式
const controller = createStreamController()
const content = await chatStream(
  { message: '你好' },
  (chunk) => {
    // 实时更新UI
    console.log(chunk)
  },
  controller.signal
)

// 停止生成
controller.abort()
```

### 策划案生成

```typescript
import { generatePlan, generatePlanStream } from '@/api'

// 非流式
const plan = await generatePlan({
  theme: '迎新晚会',
  organizer: '人力资源中心',
  eventTime: '2024年9月1日',
  eventLocation: '学生活动中心',
  participants: '全体新生',
  purpose: '欢迎新成员'
})

// 流式
const content = await generatePlanStream(
  { theme: '迎新晚会' },
  (chunk) => {
    appendContent(chunk)
  }
)
```

### RAG 增强对话

```typescript
import { chatWithRag } from '@/api'

const content = await chatWithRag(
  {
    message: '介绍一下往届的迎新活动',
    useRAG: true,
    enableTools: true
  },
  (chunk) => console.log(chunk)
)
```

## RAG 知识库 (RAG)

```typescript
import { initRag, getRagStats, testRetrieve } from '@/api'

// 初始化知识库
const result = await initRag({ forceReindex: true })
console.log(`处理了${result.data.processedFiles}个文件`)

// 获取统计
const stats = await getRagStats()

// 测试检索
const results = await testRetrieve('活动策划', 5)
```

## OSS 存储 (OSS)

```typescript
import {
  generatePresignedUrl,
  getPresignedUrlForMaterial,
  getPresignedUrlForActivityImage
} from '@/api'

// 通用预签名URL
const url = await generatePresignedUrl({
  fileName: 'document.pdf',
  fileType: 'application/pdf'
})

// 获取资料下载URL
const materialUrl = await getPresignedUrlForMaterial(1, 7200)
```

## 性能监控 (Performance)

```typescript
import { getPerformanceReport, resetPerformanceStats } from '@/api'

// 获取性能报告（管理员权限）
const report = await getPerformanceReport()
console.log(`平均响应时间: ${report.data.averageResponseTime}ms`)

// 重置统计
await resetPerformanceStats()
```

## 错误处理

所有 API 调用都会返回统一的响应格式：

```typescript
interface ApiResponse<T> {
  code: number      // 状态码，200表示成功
  message: string   // 提示信息
  data: T          // 响应数据
}
```

错误处理示例：

```typescript
try {
  const result = await getActivityById(1)
  if (result.code === 200) {
    // 处理成功
  } else {
    // 处理业务错误
    ElMessage.error(result.message)
  }
} catch (error: any) {
  // 处理网络错误或服务器错误
  ElMessage.error(error.message || '请求失败')
}
```

### 常见错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 权限说明

| 接口 | 权限要求 |
|------|----------|
| 公开接口 | 无需登录 |
| 需登录接口 | 需要有效的 JWT Token |
| 部员接口 | 需要部员及以上角色 |
| 部长接口 | 需要部长或副部长角色 |

Token 通过 HTTP Header 传递：

```
Authorization: Bearer <token>
```

当 Token 过期时，系统会自动跳转到登录页面。
