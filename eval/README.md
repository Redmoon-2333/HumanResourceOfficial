# Java 后端简历量化指标测试套件

> **目的**：为 Java 后端简历中的所有量化指标提供真实、可验证的测试数据
> **适用方向**：Java 后端开发、Spring Boot 开发、后端工程师

---

## 📁 目录结构

```
HumanResourceOfficial/eval/
├── benchmark_sse.py              # SSE 流式延迟测试
├── interview_assignment_ab_test.py # 面试调度 A/B 测试
├── analyze_cache_performance.py   # 缓存性能分析
├── run_jmeter_cache_test.py       # JMeter 缓存测试自动化
├── run_all_tests.py               # 一键运行所有测试
├── jmeter_cache_test/
│   ├── cache_performance_test.jmx # JMeter 测试脚本
│   ├── result_cold.csv            # 冷缓存测试结果（运行后生成）
│   ├── result_hot.csv             # 热缓存测试结果（运行后生成）
│   └── performance_report.json    # 性能分析报告（运行后生成）
├── ragas_result.json              # RAGAS 评估结果
├── sse_benchmark_result.json      # SSE 测试结果
└── assignment_ab_test_result.json # A/B 测试结果

LangChain-RAG/eval/
├── ragas_evaluation.py            # RAGAS 检索质量评估
└── ragas_result.json              # 评估结果（运行后生成）
```

---

## 🚀 快速开始

### 方式 1：一键运行所有测试

```bash
cd d:\大学相关\01_学业与课程\05_项目实战与案例\HumanResourceOfficial

python eval/run_all_tests.py
```

### 方式 2：逐项测试（推荐）

```bash
# 1. RAGAS 检索质量评估
cd d:\大学相关\01_学业与课程\05_项目实战与案例\LangChain-RAG
python eval/ragas_evaluation.py

# 2. SSE 流式延迟测试
cd d:\大学相关\01_学业与课程\05_项目实战与案例\HumanResourceOfficial
python eval/benchmark_sse.py

# 3. 面试调度 A/B 测试
python eval/interview_assignment_ab_test.py

# 4. 缓存性能测试（需要 JMeter）
python eval/run_jmeter_cache_test.py
```

---

## 📊 测试项目总览

| 测试项目 | 脚本 | 预计耗时 | 前置条件 |
|---------|------|---------|---------|
| RAGAS 检索质量 | `ragas_evaluation.py` | 15 分钟 | Python 环境 |
| SSE 流式延迟 | `benchmark_sse.py` | 15 分钟 | Spring Boot 运行中 |
| 面试调度 A/B | `interview_assignment_ab_test.py` | 10 分钟 | 无 |
| 缓存性能 | `run_jmeter_cache_test.py` | 20 分钟 | JMeter、Redis |

---

## 🧪 测试详细说明

### 1. RAGAS 检索质量评估

**测试目的**：验证 RAG 系统的检索准确率

**测试原理**：
- 使用 Ragas 框架评估 RAG 回答质量
- 指标：忠实度、答案相关性、上下文召回率
- 综合得分 = (忠实度 + 答案相关性 + 上下文召回率) / 3

**执行命令**：
```bash
cd LangChain-RAG
python eval/ragas_evaluation.py
```

**达标标准**：综合得分 ≥ 0.85

**结果文件**：`eval/ragas_result.json`

---

### 2. SSE 流式延迟测试

**测试目的**：验证 SSE 流式对话的首字延迟优化效果

**测试原理**：
- 发送 100 次 HTTP 请求到 `/api/ai/chat-stream`
- 记录从请求发出到收到第一个 SSE chunk 的时间
- 统计平均值、P50、P95、P99

**执行命令**：
```bash
cd HumanResourceOfficial
python eval/benchmark_sse.py
```

**前置条件**：
- Spring Boot 应用运行在 `http://localhost:8080`
- AI 对话接口可访问

**达标标准**：平均首字延迟 ≤ 300ms

**结果文件**：`eval/sse_benchmark_result.json`

---

### 3. 面试调度 A/B 测试

**测试目的**：验证面试调度算法优化的效果

**测试原理**：
- 旧算法（对照组）：先到先得，无优先级
- 新算法（实验组）：三层排序策略（部门 - 时间段 - 紧迫度）
- 对比两者的分配成功率

**执行命令**：
```bash
python eval/interview_assignment_ab_test.py
```

**达标标准**：相对提升 ≥ 30%

**结果文件**：`eval/assignment_ab_test_result.json`

---

### 4. 缓存性能测试

**测试目的**：验证 Redis 缓存对接口响应时间的优化效果

**测试原理**：
- 场景 1（冷缓存）：清空 Redis 后压测
- 场景 2（热缓存）：缓存命中后压测
- 对比两者的平均响应时间

**执行命令**：
```bash
python eval/run_jmeter_cache_test.py
```

**前置条件**：
- JMeter 已安装
- Redis 可访问
- Spring Boot 应用运行中

**达标标准**：性能提升 ≥ 60%

**结果文件**：`eval/jmeter_cache_test/performance_report.json`

---

## 📝 填写测试记录

完成测试后，打开 `d:\大学相关\01_学业与课程\量化指标测试记录表.md`，将实测数据填入对应表格。

### 示例

```markdown
## 1️⃣ 检索准确率达 85%+

### 测试信息
| 字段 | 值 |
|-----|---|
| 测试日期 | 2026-04-17 |
| 测试工具 | Ragas |
| 测试样本数 | 30 |

### 测试结果
| 指标 | 得分 |
|-----|------|
| 忠实度 | 0.87 |
| 答案相关性 | 0.91 |
| 上下文召回率 | 0.88 |
| **综合准确率** | **0.887** |

### 结论
✅ 综合准确率 88.7% ≥ 85%，可以在简历中使用
```

---

## 📄 更新简历

将实测数据填入 Java 后端简历：

```latex
\item \textbf{RAG 检索增强架构}：...，Ragas 评估\textbf{综合得分 0.89}
\item \textbf{SSE 流式对话引擎}：...，首字延迟\textbf{从 2.1s 降至 280ms}
\item \textbf{Redis 缓存架构}：...，\textbf{接口响应时间降低 62.5\%}
\item \textbf{面试调度引擎}：...，\textbf{分配成功率提升 32\%}
```

---

## ⚠️ 常见问题

### Q: 测试结果不达标怎么办？

**A**: 先优化项目再重测：
- RAG 准确率低 → 改进分块策略、添加重排序
- SSE 延迟高 → 启用 Redis 缓存、优化向量检索
- 缓存效果差 → 为更多接口添加 `@Cacheable`
- 分配提升小 → 调整排序策略、增加约束

### Q: JMeter 安装失败怎么办？

**A**: 使用绿色版：
1. 下载 ZIP 包而非 MSI
2. 解压到 `d:\tools\apache-jmeter-5.6`
3. 运行 `bin\jmeter.bat`

### Q: Spring Boot 接口返回 401 怎么办？

**A**: 需要添加认证：
1. 临时关闭接口的认证要求（测试环境）
2. 或在测试脚本中添加 token

---

## 📚 参考文档

- `Java 后端_量化指标测试指南.md` - 详细测试方法
- `量化指标测试方法论.md` - 通用测试理论
- `量化指标测试记录表.md` - 数据记录模板
- `量化指标测试_快速开始.md` - 快速入门

---

## ✅ 测试完成清单

- [ ] RAGAS 检索质量测试完成
- [ ] SSE 流式延迟测试完成
- [ ] 面试调度 A/B 测试完成
- [ ] 缓存性能测试完成
- [ ] 测试记录表已填写
- [ ] 简历已更新实测数据
- [ ] 面试回答模板已熟悉

---

**最后提醒**：所有数据必须真实测试，严禁编造。测试报告是你在面试中辩护的底气。
