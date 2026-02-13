# FOUC (Flash of Unstyled Content) 修复文档

## 问题描述

项目使用了大量 CSS 变量（`var(--*)`）来管理颜色和样式。在样式文件加载完成前，元素会显示浏览器默认样式或 Element Plus 默认主题色（橙色），导致"橙色 → 目标色"的闪烁现象。

## 修复策略

为关键元素添加内联 CSS 样式（`style` 属性），确保在 CSS 文件加载前元素就显示正确的颜色。

## 颜色映射参考

根据 `main.css` 中的定义：
- `--text-primary` → `#1C1917` (近黑色)
- `--text-secondary` → `#57534E` (深灰)
- `--text-tertiary` → `#78716C` (中灰)
- `--primary-500` → `#FF6B4A` (橙色 - Element Plus 默认)

---

## 修复记录

### 1. Layout.vue

#### 位置：Line 136-137
**元素：** Logo 标题和副标题
```vue
<!-- 修复前 -->
<div v-show="!isCollapsed || isMobile" class="logo-text">
  <h1>人力资源中心</h1>
  <span>Human Resources</span>
</div>

<!-- 修复后 -->
<div v-show="!isCollapsed || isMobile" class="logo-text">
  <!-- Inline CSS Fix: Prevent FOUC - Logo title should be black (#1C1917), not orange -->
  <h1 style="color: #1C1917;">人力资源中心</h1>
  <!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
  <span style="color: #78716C;">Human Resources</span>
</div>
```

#### 位置：Line 255-256
**元素：** 页面标题和副标题
```vue
<!-- 修复前 -->
<div class="page-info">
  <h2 class="page-title">{{ route.meta.title || '首页' }}</h2>
  <p class="page-subtitle">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
</div>

<!-- 修复后 -->
<div class="page-info">
  <!-- Inline CSS Fix: Prevent FOUC - Page title should be black (#1C1917), not orange -->
  <h2 class="page-title" style="color: #1C1917;">{{ route.meta.title || '首页' }}</h2>
  <!-- Inline CSS Fix: Prevent FOUC - Page subtitle should be gray (#78716C) -->
  <p class="page-subtitle" style="color: #78716C;">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
</div>
```

---

### 2. Login.vue

#### 位置：Line 87-88
**元素：** 登录标题和副标题
```vue
<!-- 修复前 -->
<h1 class="login-title">人力资源中心</h1>
<p class="login-subtitle">Human Resource Management System</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Login title should be black (#1C1917), not orange -->
<h1 class="login-title" style="color: #1C1917;">人力资源中心</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="login-subtitle" style="color: #78716C;">Human Resource Management System</p>
```

---

### 3. Home.vue

#### 位置：Line 204-209
**元素：** Hero 标题、欢迎文本和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  <span class="name-highlight">{{ userStore.userInfo?.name || '访客' }}</span>
  <span class="welcome-text">，欢迎回来！</span>
</h1>
<p class="hero-subtitle">
  这里是人力资源中心管理系统，为您提供活动管理、资料共享、AI助手等服务
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  <span class="name-highlight">{{ userStore.userInfo?.name || '访客' }}</span>
  <span class="welcome-text" style="color: #1C1917;">，欢迎回来！</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#57534E) -->
<p class="hero-subtitle" style="color: #57534E;">
  这里是人力资源中心管理系统，为您提供活动管理、资料共享、AI助手等服务
</p>
```

---

### 4. Activities.vue

#### 位置：Line 359-366
**元素：** Hero 标题和描述
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  <span class="title-highlight">人力资源中心</span>
  <br />活动介绍
</h1>
<p class="hero-description">
  探索我们精心策划的特色活动，了解活动背景、意义与流程
  <br />开启一段充满成长与收获的精彩旅程
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  <span class="title-highlight">人力资源中心</span>
  <br />活动介绍
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Description should be gray (#57534E) -->
<p class="hero-description" style="color: #57534E;">
  探索我们精心策划的特色活动，了解活动背景、意义与流程
  <br />开启一段充满成长与收获的精彩旅程
</p>
```

---

### 5. Alumni.vue

#### 位置：Line 173-178
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  成员<span class="gradient-text">风采展示</span>
</h1>
<p class="hero-subtitle">
  致敬每一位为人力资源中心付出过的成员，感谢你们的贡献与陪伴
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  成员<span class="gradient-text">风采展示</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  致敬每一位为人力资源中心付出过的成员，感谢你们的贡献与陪伴
</p>
```

---

### 6. Materials.vue

#### 位置：Line 547-553
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  共享知识
  <span class="gradient-text">共创价值</span>
</h1>
<p class="hero-subtitle">
  汇集人力资源中心各类文档资料，助力成员成长与协作
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  共享知识
  <span class="gradient-text">共创价值</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  汇集人力资源中心各类文档资料，助力成员成长与协作
</p>
```

---

### 7. PastActivities.vue

#### 位置：Line 140-146
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  精彩回顾
  <span class="gradient-text">传承创新</span>
</h1>
<p class="hero-subtitle">
  回顾人力资源中心历届精彩活动，见证成长与荣耀
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  精彩回顾
  <span class="gradient-text">传承创新</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  回顾人力资源中心历届精彩活动，见证成长与荣耀
</p>
```

---

### 8. PlanGenerator.vue

#### 位置：Line 216-221
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  <span class="gradient-text">AI 策划案生成</span>
</h1>
<p class="hero-subtitle">
  输入活动基本信息，AI将为您生成一份专业完整的活动策划案
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  <span class="gradient-text">AI 策划案生成</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  输入活动基本信息，AI将为您生成一份专业完整的活动策划案
</p>
```

---

### 9. ActivationCodeManager.vue

#### 位置：Line 178-183
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  成员账号<span class="gradient-text">激活系统</span>
</h1>
<p class="hero-subtitle">
  生成和管理成员注册激活码，控制账号注册权限
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  成员账号<span class="gradient-text">激活系统</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  生成和管理成员注册激活码，控制账号注册权限
</p>
```

---

### 10. RagManagement.vue

#### 位置：Line 120-125
**元素：** Hero 标题和副标题
```vue
<!-- 修复前 -->
<h1 class="hero-title">
  智能知识库<span class="gradient-text">管理系统</span>
</h1>
<p class="hero-subtitle">
  管理 AI 助手的知识库内容，初始化向量数据库以提升问答质量
</p>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
<h1 class="hero-title" style="color: #1C1917;">
  智能知识库<span class="gradient-text">管理系统</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="hero-subtitle" style="color: #78716C;">
  管理 AI 助手的知识库内容，初始化向量数据库以提升问答质量
</p>
```

---

### 11. ActivityDetail.vue

#### 位置：Line 216
**元素：** 页面标题
```vue
<!-- 修复前 -->
<h1 class="page-title">活动详情</h1>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Page title should be black (#1C1917), not orange -->
<h1 class="page-title" style="color: #1C1917;">活动详情</h1>
```

---

### 12. Profile.vue

#### 位置：Line 186-189, 239-242, 266-269, 293-296
**元素：** 各个 section 标题
```vue
<!-- 修复前 -->
<h2 class="section-title">
  <el-icon><User /></el-icon>
  <span>基本信息</span>
</h2>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
<h2 class="section-title" style="color: #1C1917;">
  <el-icon><User /></el-icon>
  <span>基本信息</span>
</h2>
```

（其他 section 标题同理：快捷操作、我的成就、安全设置）

---

### 13. Register.vue

#### 位置：Line 168, 221, 329
**元素：** 注册表单 section 标题
```vue
<!-- 修复前 -->
<span class="section-title">基本信息</span>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
<span class="section-title" style="color: #1C1917;">基本信息</span>
```

（其他 section 标题同理：身份信息、激活验证）

---

### 14. AIChat.vue

#### 位置：Line 168-179
**元素：** 欢迎标题、副标题和快捷提问标题
```vue
<!-- 修复前 -->
<h1 class="welcome-title">
  <span class="gradient-text">AI 智能助手</span>
</h1>
<p class="welcome-subtitle">
  我是人力资源中心的智能助手，可以帮您解答问题、提供建议、协助策划活动
</p>
<div class="quick-title">
  <el-icon><StarFilled /></el-icon>
  <span>试试这样问我</span>
</div>

<!-- 修复后 -->
<!-- Inline CSS Fix: Prevent FOUC - Welcome title should be black (#1C1917), not orange -->
<h1 class="welcome-title" style="color: #1C1917;">
  <span class="gradient-text">AI 智能助手</span>
</h1>
<!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
<p class="welcome-subtitle" style="color: #78716C;">
  我是人力资源中心的智能助手，可以帮您解答问题、提供建议、协助策划活动
</p>
<div class="quick-title">
  <el-icon><StarFilled /></el-icon>
  <!-- Inline CSS Fix: Prevent FOUC - Quick title should be black (#1C1917), not orange -->
  <span style="color: #1C1917;">试试这样问我</span>
</div>
```

---

## 修复统计

| 文件 | 修复元素数量 | 优先级 |
|------|-------------|--------|
| Layout.vue | 4 | P0 |
| Login.vue | 2 | P1 |
| Home.vue | 3 | P2 |
| Activities.vue | 2 | P2 |
| Alumni.vue | 2 | P2 |
| Materials.vue | 2 | P2 |
| PastActivities.vue | 2 | P2 |
| PlanGenerator.vue | 2 | P2 |
| ActivationCodeManager.vue | 2 | P2 |
| RagManagement.vue | 2 | P2 |
| ActivityDetail.vue | 1 | P2 |
| Profile.vue | 4 | P3 |
| Register.vue | 3 | P3 |
| AIChat.vue | 3 | P3 |
| **总计** | **36** | - |

---

## 维护建议

1. **后续开发注意：** 新增页面或组件时，如果使用了 CSS 变量设置文字颜色，建议同时添加内联样式作为回退。

2. **代码审查：** 在代码审查时检查是否有新的 CSS 变量使用，确保关键元素有内联样式保护。

3. **测试验证：** 在慢速网络环境下测试页面加载，验证 FOUC 问题是否已解决。

4. **长期方案：** 考虑使用 CSS-in-JS 方案（如 styled-components）或 CSS Modules 来替代全局 CSS 变量，从根本上避免 FOUC 问题。

---

## 修复日期

2026-02-13
