# RAG Knowledge Base - AI Training Documents

**Parent:** `../../AGENTS.md` | **Categories:** 8 | **Documents:** 27+

---

## OVERVIEW

Retrieval-Augmented Generation (RAG) knowledge base for campus AI assistant. Organized into 8 numbered categories covering student union operations, campus life, and academic resources. Auto-indexed into Redis Vector Store on application startup.

---

## STRUCTURE

```
rag-knowledge-base/
├── 00-使用说明.txt              # Knowledge base writing guidelines
├── 01-组织概况/                 # Student union overview, culture, structure
│   ├── 学生会简介.txt
│   ├── 组织架构.txt
│   └── 组织文化.txt
├── 02-规章制度/                 # Student union regulations, bylaws
│   └── 学生会章程.txt
├── 03-活动管理/                 # Activity planning, execution, reporting
│   ├── 活动策划流程.txt
│   ├── 活动执行指南.txt
│   └── 活动总结模板.txt
├── 04-部门信息/                 # Department introductions (5 centers)
│   ├── 人力资源中心.txt
│   ├── 学术发展中心.txt
│   ├── 文艺体育中心.txt
│   ├── 社会实践中心.txt
│   └── 新媒体中心.txt
├── 05-财务制度/                 # Financial management, budgeting
│   └── 经费管理制度.txt
├── 06-校园生活/                 # Campus practical info
│   ├── 交通指南.txt
│   ├── 饮食服务.txt
│   ├── 住宿管理.txt
│   ├── 医疗服务.txt
│   └── 图书馆服务.txt
├── 07-学习指导/                 # Academic support, courses, competitions
│   ├── 选课指南.txt
│   ├── 学习技巧.txt
│   └── 学科竞赛.txt
└── 08-职业发展/                 # Internships, career planning
    ├── 实习机会.txt
    └── 社会实践.txt
```

---

## WHERE TO LOOK

| Task | Location | Action |
|------|----------|--------|
| **Add new category** | Create `XX-类别名/` folder | Number sequentially (09, 10, ...) |
| **Add document** | `{category}/文档名.txt` | Follow writing guidelines |
| **Update content** | Edit existing `.txt` file | Re-index required |
| **Re-index all** | POST `/api/rag/initialize` | Force re-indexing |
| **Test retrieval** | GET `/api/rag/test-retrieve?q=...` | Query vector store |
| **Debug files** | GET `/api/rag/debug/list-files` | List indexed documents |

---

## KNOWLEDGE BASE GUIDELINES

### Document Format

**File Type:** Plain text (`.txt`) or supported formats (PDF, Excel auto-parsed)

**Encoding:** UTF-8

**Structure:**
```
# 标题

## 小节标题

正文内容...

- 列表项 1
- 列表项 2

**重要提示**：加粗强调内容
```

### Writing Standards

1. **Clarity**: Use clear, concise Chinese language
2. **Completeness**: Cover all aspects of the topic
3. **Accuracy**: Ensure information is up-to-date and correct
4. **Consistency**: Follow same structure across similar documents
5. **Searchability**: Include common search terms and variations

### Chunking Behavior

**Configuration:**
- Chunk size: 400 characters
- Overlap: 100 characters (25%)
- Semantic chunking: Enabled
- Auto-detect doc type: Enabled

**How it works:**
1. Document loaded and parsed (PDF/Excel auto-detected)
2. Text split by semantic boundaries (sections, paragraphs, sentences)
3. Each chunk embedded using `ecnu-embedding-small` (1024 dimensions)
4. Vectors stored in Redis with metadata (category, filename, chunk index)

**Example:**
```
Original: "学生会人力资源中心负责成员管理、活动协调和培训组织。"
Chunk 1: "学生会人力资源中心负责成员管理、活动协调和培训组织。主要职能包括..."
Chunk 2: "...主要职能包括：1. 成员档案管理 2. 活动资源调配 3. 新成员培训"
```

---

## API ENDPOINTS

### Initialize Knowledge Base

```bash
curl -X POST http://localhost:8080/api/rag/initialize \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "forceReindex": false
  }'
```

**Parameters:**
- `forceReindex`: If `true`, clears existing vectors and re-indexes all documents

**Response:**
```json
{
  "success": true,
  "message": "知识库初始化完成",
  "stats": {
    "documentsIndexed": 27,
    "chunksCreated": 156,
    "vectorsStored": 156
  }
}
```

### Test Retrieval

```bash
curl "http://localhost:8080/api/rag/test-retrieve?q=学生会结构" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**
```json
{
  "results": [
    {
      "content": "学生会组织架构包括...",
      "score": 0.89,
      "metadata": {
        "source": "01-组织概况/组织架构.txt",
        "chunkIndex": 3
      }
    },
    {
      "content": "人力资源中心负责...",
      "score": 0.76,
      "metadata": {
        "source": "04-部门信息/人力资源中心.txt",
        "chunkIndex": 1
      }
    }
  ]
}
```

### Get Statistics

```bash
curl http://localhost:8080/api/rag/stats \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**
```json
{
  "totalDocuments": 27,
  "totalChunks": 156,
  "totalVectors": 156,
  "categories": {
    "01-组织概况": 3,
    "02-规章制度": 1,
    "03-活动管理": 3,
    "04-部门信息": 5,
    "05-财务制度": 1,
    "06-校园生活": 5,
    "07-学习指导": 3,
    "08-职业发展": 2
  }
}
```

### Debug: List Files

```bash
curl http://localhost:8080/api/rag/debug/list-files \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response:**
```json
{
  "files": [
    {
      "path": "01-组织概况/学生会简介.txt",
      "size": 2048,
      "lastModified": "2026-04-14T10:30:00Z",
      "indexed": true
    },
    ...
  ]
}
```

---

## CONFIGURATION

### RAG Parameters (application.yml)

```yaml
rag:
  chunk-size: 400              # Characters per chunk
  chunk-overlap: 100           # Overlap between chunks
  min-chunk-size: 120          # Minimum chunk size
  enable-semantic-chunking: true  # Semantic-aware splitting
  auto-detect-doc-type: true   # Detect structured/narrative/technical
  retrieval-top-k: 5           # Top-K results for retrieval
  
  # Low-memory mode (for 1GB servers)
  low-memory-mode: false
  memory-warning-threshold: 0.7      # GC at 70% memory
  memory-critical-threshold: 0.85    # Pause at 85% memory
  low-memory-batch-size: 5           # Reduced batch size
  low-memory-file-delay-ms: 500      # Processing delay
```

### Vector Store (Redis)

```yaml
spring:
  ai:
    vectorstore:
      redis:
        index-name: campus-knowledge-index
        prefix: "rag:embedding:"
    embedding:
      options:
        model: ecnu-embedding-small  # 1024 dimensions
```

---

## DOCUMENT TYPES

### Supported Formats

| Format | Parser | Notes |
|--------|--------|-------|
| `.txt` | Built-in | UTF-8 text files |
| `.pdf` | PDFBox | Text extraction from PDFs |
| `.xlsx` | Apache POI | Excel spreadsheet parsing |
| `.xls` | Apache POI | Legacy Excel format |

### Document Categories

| Category | Purpose | Example Queries |
|----------|---------|-----------------|
| 01-组织概况 | Union structure, culture | "学生会是什么", "组织架构" |
| 02-规章制度 | Bylaws, regulations | "学生会章程", "规章制度" |
| 03-活动管理 | Activity lifecycle | "活动策划", "活动流程" |
| 04-部门信息 | Department details | "人力资源中心职责", "各部门介绍" |
| 05-财务制度 | Financial rules | "经费申请", "报销流程" |
| 06-校园生活 | Campus services | "食堂", "图书馆", "校医院" |
| 07-学习指导 | Academic support | "选课", "学习技巧", "竞赛" |
| 08-职业发展 | Career planning | "实习", "社会实践" |

---

## NOTES

- **Auto-indexing**: Knowledge base indexed on app startup (can be disabled)
- **Manual trigger**: POST `/api/rag/initialize` for on-demand re-indexing
- **Redis Stack required**: Must use `redis/redis-stack` image with RediSearch
- **Vector dimensions**: 1024 (fixed by `ecnu-embedding-small` model)
- **Memory limits**: 2GB server supports ~30k vectors max
- **File encoding**: All text files must be UTF-8 encoded
- **Numbered categories**: Use 2-digit numbers (00, 01, 02, ...) for sorting
- **No nested folders**: Keep structure flat within each category

---

## TROUBLESHOOTING

### Vectors Not Retrieved

**Symptoms:** Empty results from `/api/rag/test-retrieve`

**Solutions:**
1. Check if knowledge base is indexed: GET `/api/rag/stats`
2. Re-index if needed: POST `/api/rag/initialize`
3. Verify Redis Stack is running (not standard Redis)
4. Check vector index exists: `FT.INFO campus-knowledge-index` in Redis CLI

### Poor Retrieval Quality

**Symptoms:** Irrelevant results, low scores

**Solutions:**
1. Increase `chunk-overlap` (more context)
2. Adjust `retrieval-top-k` (more/fewer results)
3. Improve document quality (clearer writing, better structure)
4. Enable semantic chunking if disabled

### Memory Issues

**Symptoms:** OOM errors during indexing

**Solutions:**
1. Enable `low-memory-mode: true`
2. Reduce `chunk-size` (smaller chunks = less memory)
3. Increase `low-memory-file-delay-ms` (slower but safer)
4. Upgrade server RAM or use cloud vector service

---

## HIERARCHY

- **Parent:** `../../AGENTS.md` (resources)
- **Root:** `../../../AGENTS.md`