# 往届活动API接口文档

## 概述
往届活动模块提供完整的增删改查功能，支持分页查询、按年份分类和按活动名称搜索。

## 基础信息
- **基础URL**: `/api/past-activities`
- **数据格式**: JSON
- **权限说明**: 
  - 查询操作（GET）：公开访问
  - 增删改操作（POST/PUT/DELETE）：需要部长权限

## API接口列表

### 1. 分页查询往届活动
**接口**: `GET /api/past-activities`

**请求参数**:
- `pageNum` (可选): 页码，默认1
- `pageSize` (可选): 每页大小，默认10
- `year` (可选): 年份筛选
- `title` (可选): 活动名称搜索（模糊匹配）

**请求示例**:
```
# 查询所有往届活动（第1页，每页10条）
GET /api/past-activities

# 按年份筛选
GET /api/past-activities?year=2023

# 按名称搜索
GET /api/past-activities?title=文艺晚会

# 组合查询（2023年的文艺相关活动）
GET /api/past-activities?year=2023&title=文艺&pageNum=1&pageSize=5
```

**响应格式**:
```json
{
  "message": "操作成功",
  "data": {
    "content": [
      {
        "pastActivityId": 1,
        "title": "2023年度文艺晚会",
        "coverImage": "/images/cover1.jpg",
        "pushUrl": "/activities/2023-gala",
        "year": 2023,
        "createTime": "2024-01-15T10:30:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "total": 25,
    "pages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "code": 200
}
```

### 2. 查询单个往届活动详情
**接口**: `GET /api/past-activities/{id}`

**请求示例**:
```
GET /api/past-activities/1
```

**响应格式**:
```json
{
  "message": "操作成功",
  "data": {
    "pastActivityId": 1,
    "title": "2023年度文艺晚会",
    "coverImage": "/images/cover1.jpg",
    "pushUrl": "/activities/2023-gala",
    "year": 2023,
    "createTime": "2024-01-15T10:30:00"
  },
  "code": 200
}
```

### 3. 创建往届活动
**接口**: `POST /api/past-activities`
**权限**: 需要部长权限

**请求体**:
```json
{
  "title": "2024年迎新晚会",
  "coverImage": "/images/welcome2024.jpg",
  "pushUrl": "/activities/welcome-2024",
  "year": 2024
}
```

**响应格式**:
```json
{
  "message": "往届活动创建成功",
  "data": {
    "pastActivityId": 2,
    "title": "2024年迎新晚会",
    "coverImage": "/images/welcome2024.jpg",
    "pushUrl": "/activities/welcome-2024",
    "year": 2024,
    "createTime": "2024-10-02T00:15:00"
  },
  "code": 200
}
```

### 4. 更新往届活动
**接口**: `PUT /api/past-activities/{id}`
**权限**: 需要部长权限

**请求体**:
```json
{
  "title": "2024年迎新晚会（已更新）",
  "coverImage": "/images/welcome2024_new.jpg",
  "pushUrl": "/activities/welcome-2024-updated",
  "year": 2024
}
```

**响应格式**:
```json
{
  "message": "往届活动更新成功",
  "data": {
    "pastActivityId": 2,
    "title": "2024年迎新晚会（已更新）",
    "coverImage": "/images/welcome2024_new.jpg",
    "pushUrl": "/activities/welcome-2024-updated",
    "year": 2024,
    "createTime": "2024-10-02T00:15:00"
  },
  "code": 200
}
```

### 5. 删除往届活动
**接口**: `DELETE /api/past-activities/{id}`
**权限**: 需要部长权限

**请求示例**:
```
DELETE /api/past-activities/2
```

**响应格式**:
```json
{
  "message": "往届活动删除成功",
  "data": null,
  "code": 200
}
```

### 6. 获取所有年份列表
**接口**: `GET /api/past-activities/years`

**响应格式**:
```json
{
  "message": "操作成功",
  "data": [2024, 2023, 2022, 2021],
  "code": 200
}
```

### 7. 按年份统计活动数量
**接口**: `GET /api/past-activities/years/{year}/count`

**请求示例**:
```
GET /api/past-activities/years/2023/count
```

**响应格式**:
```json
{
  "message": "操作成功",
  "data": 8,
  "code": 200
}
```

## 数据模型

### PastActivity 实体
```json
{
  "pastActivityId": "整型，主键ID",
  "title": "字符串，活动标题（必填）",
  "coverImage": "字符串，封面图片URL（可选）",
  "pushUrl": "字符串，推广链接（可选）",
  "year": "整型，年份（必填）",
  "createTime": "日期时间，创建时间"
}
```

### PageResponse 分页响应
```json
{
  "content": "数组，当前页的数据列表",
  "pageNum": "整型，当前页码",
  "pageSize": "整型，每页大小",
  "total": "长整型，总记录数",
  "pages": "整型，总页数",
  "hasNext": "布尔型，是否有下一页",
  "hasPrevious": "布尔型，是否有上一页"
}
```

## 错误响应示例

### 权限不足
```json
{
  "message": "权限不足",
  "data": null,
  "code": 3001
}
```

### 资源不存在
```json
{
  "message": "往届活动不存在",
  "data": null,
  "code": 7001
}
```

### 参数验证失败
```json
{
  "message": "活动标题不能为空",
  "data": null,
  "code": 4002
}
```

## 使用场景示例

### 1. 前端展示往届活动列表
```javascript
// 获取第一页数据
fetch('/api/past-activities?pageNum=1&pageSize=12')
  .then(response => response.json())
  .then(data => {
    // 渲染活动列表
    renderActivities(data.data.content);
    // 渲染分页组件
    renderPagination(data.data);
  });
```

### 2. 按年份筛选
```javascript
// 用户选择年份后筛选
const selectedYear = 2023;
fetch(`/api/past-activities?year=${selectedYear}`)
  .then(response => response.json())
  .then(data => {
    renderActivities(data.data.content);
  });
```

### 3. 搜索功能
```javascript
// 用户输入搜索关键词
const searchKeyword = '文艺';
fetch(`/api/past-activities?title=${encodeURIComponent(searchKeyword)}`)
  .then(response => response.json())
  .then(data => {
    renderSearchResults(data.data.content);
  });
```

### 4. 管理员操作（需要认证）
```javascript
// 创建新活动
const newActivity = {
  title: '2024年元旦晚会',
  coverImage: '/images/newyear2024.jpg',
  pushUrl: '/activities/newyear-2024',
  year: 2024
};

fetch('/api/past-activities', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer your-jwt-token'
  },
  body: JSON.stringify(newActivity)
})
.then(response => response.json())
.then(data => {
  if (data.code === 200) {
    console.log('活动创建成功:', data.data);
  }
});
```

## 技术特性

1. **分页查询**: 使用PageHelper插件实现高效分页
2. **模糊搜索**: 支持按活动标题进行模糊匹配
3. **多条件查询**: 可同时按年份和标题筛选
4. **权限控制**: 基于注解的权限验证
5. **参数验证**: 使用Bean Validation进行数据校验
6. **事务支持**: 数据操作具有事务保障
7. **统一响应**: 标准化的API响应格式