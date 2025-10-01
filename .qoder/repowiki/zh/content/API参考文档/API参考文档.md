
# API参考文档

<cite>
**本文档中引用的文件**  
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java)
- [UserController.java](file://src/main/java/com/redmoon2333/controller/UserController.java)
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java)
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java)
- [LoginRequest.java](file://src/main/java/com/redmoon2333/dto/LoginRequest.java)
- [RegisterRequest.java](file://src/main/java/com/redmoon2333/dto/RegisterRequest.java)
- [ActivityRequest.java](file://src/main/java/com/redmoon2333/dto/ActivityRequest.java)
- [ApiResponse.java](file://src/main/java/com/redmoon2333/dto/ApiResponse.java)
- [ErrorCode.java](file://src/main/java/com/redmoon2333/exception/ErrorCode.java)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)
- [application.yml](file://src/main/resources/application.yml)
</cite>

## 目录
1. [简介](#简介)
2. [认证API](#认证api)
3. [用户管理API](#用户管理api)
4. [活动管理API](#活动管理api)
5. [往届活动管理API](#往届活动管理api)
6. [API通用规范](#api通用规范)
7. [错误码体系](#错误码体系)

## 简介
本API参考文档详细说明了人力资源管理系统的所有公开RESTful接口。系统采用JWT进行身份认证，所有受保护的端点都需要在请求头中包含有效的JWT令牌。API主要分为认证、用户管理、活动管理和往届活动管理四大模块。

**Section sources**
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java)
- [UserController.java](file://src/main/java/com/redmoon2333/controller/UserController.java)
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java)
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java)

## 认证API
认证API提供用户登录、注册、获取当前用户信息和生成激活码的功能。所有端点均位于`/api/auth`路径下。

### 用户登录
用户通过用户名和密码进行登录，成功后返回包含JWT令牌的响应。

**HTTP方法**: `POST`  
**URL路径**: `/api/auth/login`  
**请求头**: 无特殊要求  
**请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```

**响应格式**:
```json
{
  "message": "登录成功",
  "code": 200,
  "data": {
    "token": "string",
    "userId": "string",
    "username": "string",
    "roleHistory": "string"
  }
}
```

**示例请求**:
```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**示例响应**:
```json
{
  "message": "登录成功",
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "1",
    "username": "admin",
    "roleHistory": "部长"
  }
}
```

**Section sources**
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java#L30-L58)
- [LoginRequest.java](file://src/main/java/com/redmoon2333/dto/LoginRequest.java)

### 用户注册
新用户通过激活码进行注册，需要提供用户名、密码、确认密码、姓名和角色历史。

**HTTP方法**: `POST`  
**URL路径**: `/api/auth/register`  
**请求头**: 无特殊要求  
**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "confirmPassword": "string",
  "name": "string",
  "activationCode": "string",
  "roleHistory": "string"
}
```

**响应格式**:
```json
{
  "message": "注册成功",
  "code": 200,
  "data": {
    "userId": "string",
    "username": "string",
    "roleHistory": "string",
    "name": "string",
    "message": "注册成功，请登录"
  }
}
```

**Section sources**
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java#L60-L94)
- [RegisterRequest.java](file://src/main/java/com/redmoon2333/dto/RegisterRequest.java)

### 获取当前用户信息
获取当前已认证用户的基本信息。

**HTTP方法**: `GET`  
**URL路径**: `/api/auth/current-user`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**响应格式**:
```json
{
  "message": "操作成功",
  "code": 200,
  "data": {
    "userId": "string",
    "username": "string",
    "roleHistory": "string"
  }
}
```

**Section sources**
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java#L96-L119)

### 生成激活码
只有具有"部长"角色的用户才能生成新的激活码，用于新用户注册。

**HTTP方法**: `POST`  
**URL路径**: `/api/auth/generate-code`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求参数**:
- `expireDays`: 激活码有效天数，默认30天

**响应格式**:
```json
{
  "message": "激活码生成成功",
  "code": 200,
  "data": {
    "code": "string",
    "expireTime": "datetime",
    "message": "激活码生成成功"
  }
}
```

**Section sources**
- [AuthController.java](file://src/main/java/com/redmoon2333/controller/AuthController.java#L121-L139)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

## 用户管理API
用户管理API提供获取往届部员信息和用户搜索功能。

### 获取往届部员信息
按年份分组获取所有往届部员、部长和副部长的信息。

**HTTP方法**: `GET`  
**URL路径**: `/api/users/alumni`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "查询成功",
  "code": 200,
  "data": [
    {
      "year": 2023,
      "members": [
        {
          "name": "张三",
          "roleHistory": "部长"
        },
        {
          "name": "李四",
          "roleHistory": "副部长"
        }
      ]
    }
  ]
}
```

**Section sources**
- [UserController.java](file://src/main/java/com/redmoon2333/controller/UserController.java#L70-L85)

### 根据姓名查找用户
根据完整姓名精确查找用户。

**HTTP方法**: `GET`  
**URL路径**: `/api/users/search/name`  
**请求参数**:
- `name`: 要查找的用户姓名

**响应格式**:
```json
{
  "message": "查找成功",
  "code": 200,
  "data": [
    {
      "name": "张三",
      "roleHistory": "部长"
    }
  ]
}
```

**Section sources**
- [UserController.java](file://src/main/java/com/redmoon2333/controller/UserController.java#L87-L102)

### 根据姓名模糊查找用户
根据姓名关键词进行模糊查找。

**HTTP方法**: `GET`  
**URL路径**: `/api/users/search/name/like`  
**请求参数**:
- `name`: 姓名关键词

**响应格式**:
```json
{
  "message": "查找成功",
  "code": 200,
  "data": [
    {
      "name": "张三",
      "roleHistory": "部长"
    },
    {
      "name": "张小三",
      "roleHistory": "部员"
    }
  ]
}
```

**Section sources**
- [UserController.java](file://src/main/java/com/redmoon2333/controller/UserController.java#L104-L119)

## 活动管理API
活动管理API提供创建、查询、更新和删除活动及其图片的功能。

### 创建活动
创建新的活动记录，需要部长或副部长权限。

**HTTP方法**: `POST`  
**URL路径**: `/api/activities`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求体**:
```json
{
  "activityName": "string",
  "background": "string",
  "significance": "string",
  "purpose": "string",
  "process": "string"
}
```

**响应格式**:
```json
{
  "message": "活动创建成功",
  "code": 200,
  "data": {
    "activityId": 1,
    "activityName": "string",
    "background": "string",
    "significance": "string",
    "purpose": "string",
    "process": "string",
    "createTime": "datetime",
    "updateTime": "datetime"
  }
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L45-L74)
- [ActivityRequest.java](file://src/main/java/com/redmoon2333/dto/ActivityRequest.java)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 获取活动详情
根据ID获取单个活动的详细信息。

**HTTP方法**: `GET`  
**URL路径**: `/api/activities/{activityId}`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "查询成功",
  "code": 200,
  "data": {
    "activityId": 1,
    "activityName": "string",
    "background": "string",
    "significance": "string",
    "purpose": "string",
    "process": "string",
    "createTime": "datetime",
    "updateTime": "datetime"
  }
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L76-L98)

### 获取所有活动列表
获取所有活动的列表。

**HTTP方法**: `GET`  
**URL路径**: `/api/activities`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "查询成功",
  "code": 200,
  "data": [
    {
      "activityId": 1,
      "activityName": "string",
      "background": "string",
      "significance": "string",
      "purpose": "string",
      "process": "string",
      "createTime": "datetime",
      "updateTime": "datetime"
    }
  ]
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L100-L124)

### 更新活动
更新现有活动的信息，需要部长或副部长权限。

**HTTP方法**: `PUT`  
**URL路径**: `/api/activities/{activityId}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求体**: 同创建活动的请求体

**响应格式**: 同创建活动的响应格式

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L126-L158)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 删除活动
删除指定的活动，需要部长或副部长权限。

**HTTP方法**: `DELETE`  
**URL路径**: `/api/activities/{activityId}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**响应格式**:
```json
{
  "message": "活动删除成功",
  "code": 200,
  "data": null
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L160-L182)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 为活动添加图片
为指定活动添加图片，需要部长或副部长权限。

**HTTP方法**: `POST`  
**URL路径**: `/api/activities/{activityId}/images`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  
- `Content-Type: multipart/form-data`  

**请求参数**:
- `file`: 要上传的图片文件
- `description`: 图片描述（可选）
- `sortOrder`: 排序序号，默认0

**响应格式**:
```json
{
  "message": "图片添加成功",
  "code": 200,
  "data": {
    "imageId": 1,
    "activityId": 1,
    "imageUrl": "string",
    "description": "string",
    "sortOrder": 0
  }
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L184-L238)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 获取活动图片列表
获取指定活动的所有图片。

**HTTP方法**: `GET`  
**URL路径**: `/api/activities/{activityId}/images`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "查询成功",
  "code": 200,
  "data": [
    {
      "imageId": 1,
      "activityId": 1,
      "imageUrl": "string",
      "description": "string",
      "sortOrder": 0
    }
  ]
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L240-L264)

### 更新活动图片
更新活动图片的描述和排序信息，需要部长或副部长权限。

**HTTP方法**: `PUT`  
**URL路径**: `/api/activities/images/{imageId}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求参数**:
- `description`: 图片描述（可选）
- `sortOrder`: 排序序号（可选）

**响应格式**:
```json
{
  "message": "图片更新成功",
  "code": 200,
  "data": {
    "imageId": 1,
    "activityId": 1,
    "imageUrl": "string",
    "description": "string",
    "sortOrder": 0
  }
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L266-L294)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 删除活动图片
删除指定的活动图片，需要部长或副部长权限。

**HTTP方法**: `DELETE`  
**URL路径**: `/api/activities/images/{imageId}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**响应格式**:
```json
{
  "message": "图片删除成功",
  "code": 200,
  "data": null
}
```

**Section sources**
- [ActivityController.java](file://src/main/java/com/redmoon2333/controller/ActivityController.java#L296-L318)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

## 往届活动管理API
往届活动管理API提供对历史活动的CRUD操作和统计功能。

### 分页查询往届活动
分页获取往届活动列表，支持按年份和标题筛选。

**HTTP方法**: `GET`  
**URL路径**: `/api/past-activities`  
**请求参数**:
- `pageNum`: 页码，默认1
- `pageSize`: 每页大小，默认10
- `year`: 年份筛选（可选）
- `title`: 活动名称搜索（可选）

**响应格式**:
```json
{
  "message": "操作成功",
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "string",
        "year": 2023,
        "description": "string",
        "imageUrls": ["string"]
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "pages": 10
  }
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L34-L48)

### 获取往届活动详情
根据ID获取往届活动的详细信息。

**HTTP方法**: `GET`  
**URL路径**: `/api/past-activities/{id}`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "操作成功",
  "code": 200,
  "data": {
    "id": 1,
    "title": "string",
    "year": 2023,
    "description": "string",
    "imageUrls": ["string"]
  }
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L50-L58)

### 创建往届活动
创建新的往届活动记录，需要部长权限。

**HTTP方法**: `POST`  
**URL路径**: `/api/past-activities`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求体**:
```json
{
  "title": "string",
  "year": 2023,
  "description": "string",
  "imageUrls": ["string"]
}
```

**响应格式**:
```json
{
  "message": "往届活动创建成功",
  "code": 200,
  "data": {
    "id": 1,
    "title": "string",
    "year": 2023,
    "description": "string",
    "imageUrls": ["string"]
  }
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L60-L70)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 更新往届活动
更新现有往届活动的信息，需要部长权限。

**HTTP方法**: `PUT`  
**URL路径**: `/api/past-activities/{id}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**请求体**: 同创建往届活动的请求体

**响应格式**: 同创建往届活动的响应格式

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L72-L82)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 删除往届活动
删除指定的往届活动，需要部长权限。

**HTTP方法**: `DELETE`  
**URL路径**: `/api/past-activities/{id}`  
**请求头**:  
- `Authorization: Bearer <JWT令牌>`  

**响应格式**:
```json
{
  "message": "往届活动删除成功",
  "code": 200,
  "data": null
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L84-L92)
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)

### 获取所有年份列表
获取所有存在往届活动的年份列表。

**HTTP方法**: `GET`  
**URL路径**: `/api/past-activities/years`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "操作成功",
  "code": 200,
  "data": [2023, 2022, 2021]
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L94-L102)

### 根据年份统计活动数量
获取指定年份的往届活动数量。

**HTTP方法**: `GET`  
**URL路径**: `/api/past-activities/years/{year}/count`  
**请求头**: 无特殊要求  
**响应格式**:
```json
{
  "message": "操作成功",
  "code": 200,
  "data": 5
}
```

**Section sources**
- [PastActivityController.java](file://src/main/java/com/redmoon2333/controller/PastActivityController.java#L104-L113)

## API通用规范
本节说明API的通用设计原则和规范。

### 响应格式
所有API响应均采用统一的`ApiResponse`格式：
```json
{
  "message": "响应消息",
  "code": 200,
  "data": {}
}
```
其中`data`字段包含实际的响应数据，`code`为状态码，`message`为人类可读的消息。

**Section sources**
- [ApiResponse.java](file://src/main/java/com/redmoon2333/dto/ApiResponse.java)

### 身份认证
系统采用JWT（JSON Web Token）进行身份认证。受保护的端点需要在请求头中包含`Authorization: Bearer <JWT令牌>`。

JWT令牌通过登录接口获取，有效期为2小时（7200秒），配置在`application.yml`文件中。

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L25-L26)

### 权限控制
系统使用`@RequireMinisterRole`注解来标记需要部长或副部长权限的端点。该注解由`PermissionAspect`切面处理，确保只有具有相应权限的用户才能访问受保护的资源。

**Section sources**
- [RequireMinisterRole.java](file://src/main/java/com/redmoon2333/annotation/RequireMinisterRole.java)
- [application.yml](file://src/main/resources/application.yml)

### API版本控制
当前API未显式实现版本控制，所有端点均位于根路径下。未来可通过在URL路径中添加版本号（如`/api/v1/activities`）来实现版本控制。

### 速率限制
当前系统未实现速率限制。建议在生产环境中为敏感端点（如登录、注册）添加速率限制，防止暴力破解和滥用。

## 错误码体系
系统定义了详细的错误码体系，用于标准化错误响应。

```mermaid
erDiagram
  ERROR_CODE {
    int code PK
    string