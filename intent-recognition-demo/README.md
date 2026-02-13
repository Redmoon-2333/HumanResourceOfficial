# 混合式意图识别教学项目

## 项目概述

本项目展示如何结合规则匹配和大模型实现混合式意图识别，适合大学生学习和实践。

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     混合式意图识别系统                        │
├─────────────────────────────────────────────────────────────┤
│  输入层  │  用户查询 → 预处理 → 分词                          │
├─────────────────────────────────────────────────────────────┤
│  规则层  │  关键词匹配 → 正则匹配 → 相似度计算 → 结果          │
│          │     ↓ 未命中/置信度低                              │
│  模型层  │  大模型API调用 → 提示词工程 → 结构化输出            │
├─────────────────────────────────────────────────────────────┤
│  缓存层  │  Redis缓存 → 降低API调用成本                        │
├─────────────────────────────────────────────────────────────┤
│  输出层  │  意图类型 + 置信度 + 实体 + 推理过程                 │
└─────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 环境配置

```bash
# 创建虚拟环境
python -m venv venv

# 激活环境
# Windows:
venv\Scripts\activate
# macOS/Linux:
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 2. 配置API密钥

```bash
# 复制配置文件
cp config.example.yaml config.yaml

# 编辑 config.yaml，填入你的API密钥
```

### 3. 运行示例

```bash
# 命令行交互
python main.py

# Web界面
python web_app.py
```

## 项目结构

```
intent-recognition-demo/
├── README.md                 # 项目说明
├── requirements.txt          # 依赖列表
├── config.example.yaml       # 配置示例
├── main.py                   # 命令行入口
├── web_app.py                # Web界面
├── src/                      # 源代码
│   ├── __init__.py
│   ├── rule_matcher.py       # 规则匹配模块
│   ├── llm_classifier.py     # 大模型分类模块
│   ├── hybrid_recognizer.py  # 混合识别模块
│   ├── cache_manager.py      # 缓存管理
│   └── entity_extractor.py   # 实体提取
├── data/                     # 数据文件
│   ├── intent_patterns.json  # 意图模式
│   └── domain_dict.txt       # 领域词典
├── tests/                    # 测试用例
│   └── test_recognizer.py
└── docs/                     # 文档
    ├── architecture.md
    └── api_reference.md
```

## 核心概念

### 1. 意图类型定义

```python
class IntentType(Enum):
    QUERY_INFO = "查询信息"      # 询问具体信息
    QUERY_LOCATION = "查询位置"  # 询问地点
    QUERY_PROCESS = "查询流程"   # 询问操作步骤
    QUERY_COMPARE = "比较对比"   # 询问区别
    GREETING = "问候"           # 打招呼
    UNKNOWN = "未知"            # 无法识别
```

### 2. 混合识别策略

```
用户查询
    ↓
规则匹配（快速）
    ├── 命中 + 高置信度 → 返回结果
    └── 未命中/低置信度
            ↓
        大模型分类（准确）
            ├── 成功 → 返回结果
            └── 失败 → 返回规则结果或未知
```

### 3. 成本优化

- 80%查询通过规则层处理