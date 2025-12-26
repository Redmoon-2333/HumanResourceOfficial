# Tool Calling功能使用说明

## 🎯 功能概述

Tool Calling功能允许AI在对话过程中自动调用数据库查询工具，实现更智能的信息检索。

## ✨ 已实现的工具

### 1. searchDepartmentMembers(name)
**功能**：根据姓名搜索部门成员
**参数**：
- `name` (String) - 成员姓名或姓名关键词

**触发条件**：
- 用户消息包含"搜索"、"查找"或"查询"
- 同时包含"成员"或"人"

**示例**：
```
用户：搜索张三
用户：查找李四这个成员
用户：查询王五
```

**返回格式**：
```
找到1位成员：
- 姓名：张三，角色历史：2024级部员
```

### 2. getAlumniByYear(year)
**功能**：获取指定年份的往届成员
**参数**：
- `year` (Integer) - 年份，可选，不提供则返回所有年份

**触发条件**：
- 用户消息包含"往届"或"历史"
- 可选包含四位数字年份

**示例**：
```
用户：查询2023级往届成员
用户：往届成员有哪些
用户：历史成员列表
```

**返回格式**：
```
2023级成员（共5人）：
- 张三：部长
- 李四：副部长
- 王五：部员
```

### 3. getDepartmentStats()
**功能**：获取部门统计信息
**参数**：无

**触发条件**：
- 用户消息包含"统计"、"总共"、"多少人"或"多少届"

**示例**：
```
用户：部门有多少人？
用户：统计一下往届成员
用户：总共有多少届？
```

**返回格式**：
```
人力资源中心统计信息：
- 历史年份：5届
- 历史成员总数：30人
- 最早记录：2019级
- 最新记录：2024级
```

## 🔧 使用方式

### API调用

```bash
curl -X POST http://localhost:8080/api/ai/chat-with-rag \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "搜索张三",
    "useRAG": true,
    "enableTools": true
  }'
```

### 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| message | String | 是 | - | 用户消息 |
| useRAG | Boolean | 否 | true | 是否启用RAG检索 |
| enableTools | Boolean | 否 | false | 是否启用Tool Calling |

## 💡 工作流程

```
用户提问
    ↓
检测触发条件
    ↓
提取参数（姓名、年份等）
    ↓
调用相应工具（Java Service方法）
    ↓
获取数据库查询结果
    ↓
将结果附加到用户消息
    ↓
AI基于查询结果生成自然语言回复
    ↓
SSE流式返回给用户
```

## 📊 触发机制说明

Tool Calling采用**关键词匹配**机制，自动检测用户意图并调用相应工具：

### 搜索成员触发逻辑
```java
if (message包含"搜索" || "查找" || "查询") 
   AND (message包含"成员" || "人") {
    提取姓名 → 调用searchDepartmentMembers()
}
```

### 往届成员触发逻辑
```java
if (message包含"往届" || "历史") {
    提取年份（如有） → 调用getAlumniByYear()
}
```

### 统计信息触发逻辑
```java
if (message包含"统计" || "总共" || "多少人" || "多少届") {
    调用getDepartmentStats()
}
```

## 🎨 对话示例

### 示例1：搜索成员
```
用户：帮我查找一下张三这个人
系统：[自动调用searchDepartmentMembers("张三")]

[查询结果]
找到1位成员：
- 姓名：张三，角色历史：2024级部员

根据查询结果，张三是我们人力资源中心的2024级部员。他目前在部门担任普通部员职位。如果您需要了解更多关于张三的信息，或者想联系他，可以通过部门内部通讯录查找详细联系方式。
```

### 示例2：查询往届成员
```
用户：2023级有哪些往届成员？
系统：[自动调用getAlumniByYear(2023)]

[查询结果]
2023级成员（共5人）：
- 张三：部长
- 李四：副部长
- 王五：部员
- 赵六：部员
- 孙七：部员

2023级共有5位成员，其中张三担任部长，李四担任副部长，其他三位是部员。这一届成员在部门发展中做出了重要贡献，组织了多次成功的招新和培训活动。
```

### 示例3：统计信息
```
用户：部门总共有多少届历史成员？
系统：[自动调用getDepartmentStats()]

[查询结果]
人力资源中心统计信息：
- 历史年份：5届
- 历史成员总数：30人
- 最早记录：2019级
- 最新记录：2024级

人力资源中心从2019级开始有记录，到2024级共有5届，累计30位成员。这些成员为部门的发展和学生会的运作做出了重要贡献。
```

## 🔍 调试和日志

### 日志输出示例
```
INFO  - Tool Calling: 搜索部门成员，姓名: 张三
INFO  - 执行工具调用: searchDepartmentMembers(张三)
INFO  - Tool Calling: 搜索完成，找到1位成员
```

### 查看日志
```bash
tail -f logs/spring.log | grep "Tool Calling"
```

## ⚙️ 配置选项

Tool Calling功能默认**关闭**，需要在请求时显式启用：

```json
{
  "message": "查询消息",
  "useRAG": true,
  "enableTools": true  // 设置为true启用
}
```

## 🚀 扩展新工具

如需添加新的工具方法，按以下步骤操作：

### 1. 在ToolService中添加方法

```java
@Description("工具描述")
public String newTool(String param) {
    logger.info("Tool Calling: 新工具，参数: {}", param);
    
    try {
        // 实现工具逻辑
        String result = someService.query(param);
        return formatResult(result);
    } catch (Exception e) {
        logger.error("Tool Calling: 新工具失败", e);
        return "查询失败：" + e.getMessage();
    }
}
```

### 2. 在AIChatService.processToolCalls中添加触发逻辑

```java
// 检测是否需要调用新工具
if (lowerMessage.contains("关键词")) {
    String param = extractParam(message);
    String toolResult = toolService.newTool(param);
    result.append("\n\n[查询结果]\n").append(toolResult);
    logger.info("执行工具调用: newTool({})", param);
}
```

### 3. 在系统提示词中添加工具说明

```java
enhancedSystemPrompt += "\n\n你可以调用以下工具查询数据库：\n" +
    "...\n" +
    "4. newTool(param): 新工具描述\n";
```

## ⚠️ 注意事项

1. **性能考虑**
   - 工具调用会增加响应时间（~100-500ms）
   - 建议仅在必要时启用

2. **错误处理**
   - 工具调用失败不会影响AI回复
   - 会在日志中记录错误信息

3. **参数提取**
   - 当前使用简单的关键词和正则匹配
   - 复杂场景可能需要优化提取逻辑

4. **安全性**
   - 工具仅查询公开信息
   - 已实现权限控制（需要部员及以上）

## 📈 未来改进方向

1. **智能参数提取**
   - 使用NLP技术提取参数
   - 支持更复杂的查询意图

2. **多工具组合**
   - 支持一次调用多个工具
   - 工具间数据传递

3. **工具结果缓存**
   - 缓存常用查询结果
   - 减少数据库负载

4. **动态工具注册**
   - 支持运行时注册新工具
   - 配置化工具管理

---

**Tool Calling功能已完整实现并可投入使用！** 🎉
