# HumanResourceOfficial - Project Knowledge Base

**Generated:** 2026-04-15
**Commit:** f6416f0
**Branch:** RedMoon

---

## OVERVIEW

Student union HR management system with AI capabilities. Spring Boot 3.3.5 + Vue 3.5 + Spring AI 1.0.0. ECNU AI integration (ecnu-plus/ecnu-max), RAG knowledge base (Redis Vector Store), JWT auth, MyBatis-Plus ORM.

---

## STRUCTURE

```
HumanResourceOfficial/
├── src/main/java/com/redmoon2333/    # Backend (108 Java files, 13 packages)
│   ├── config/       # 16 config classes (Security, Redis, AI, OSS)
│   ├── controller/   # 10 REST controllers
│   ├── service/      # 12 business services
│   ├── dto/          # 28 data transfer objects
│   ├── entity/       # 9 JPA entities
│   └── mapper/       # 9 MyBatis-Plus mappers
├── src/main/resources/
│   ├── rag-knowledge-base/   # 8 categories, 27+ docs (RAG)
│   ├── prompttemplate/       # AI prompts (YAML)
│   └── mapper/               # 8 MyBatis XML files
├── hrofficial-frontend/      # Vue3 + TS + Element Plus (rolldown-vite)
│   ├── src/api/              # 11 API modules
│   ├── src/views/            # 14 page components
│   └── src/components/       # 9 reusable components
├── deploy/                   # Docker Compose, deployment scripts
├── docs/                     # 9 documentation files
└── .github/workflows/        # 2 CI/CD pipelines
```

---

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| **Add API endpoint** | `controller/` + `service/` | Follow existing REST patterns, use `@RequireMemberRole` |
| **Modify AI behavior** | `prompttemplate/system-prompts.yml` | YAML-based prompts, no code change needed |
| **Add RAG knowledge** | `rag-knowledge-base/{category}/` | 8 categories, auto-indexed on startup |
| **Configure security** | `config/SecurityConfig.java` | JWT + role-based access control |
| **Add database entity** | `entity/` + `mapper/` + XML | MyBatis-Plus, logical delete (`deleted` field) |
| **Frontend page** | `hrofficial-frontend/src/views/` | Vue3 + Pinia + Element Plus |
| **Docker deployment** | `deploy/docker-compose.yml` | MySQL + Redis Stack + Backend |
| **CI/CD pipeline** | `.github/workflows/` | Backend build/test, Docker push |

---

## CODE MAP (Backend Core)

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `Main` | Application | `Main.java` | Spring Boot entry point |
| `SecurityConfig` | Config | `config/` | JWT auth, CORS, role-based security |
| `RedisConfig` | Config | `config/` | Redis connection, vector store setup |
| `AiService` | Service | `service/` | ECNU AI chat, tool calling, RAG |
| `RagService` | Service | `service/` | Knowledge base management, vector indexing |
| `JwtUtil` | Utility | `util/` | Token generation, validation |
| `PermissionAspect` | Aspect | `aspect/` | Permission logging AOP |
| `GlobalExceptionHandler` | Handler | `exception/` | Unified error codes, business exceptions |

---

## CONVENTIONS (Deviations from Standard)

### Build & Tools
- **Frontend build**: `rolldown-vite` (Rust-based), NOT standard Vite
- **Node version**: `^20.19.0 || >=22.12.0` (strict requirement)
- **Dual linting**: `oxlint` (fast) + `eslint` (comprehensive)
- **Maven repos**: Aliyun Central + Aliyun Spring + Spring Milestones

### Code Style
- **Frontend**: No semicolons, single quotes, 100 char width, 2-space indent
- **Path alias**: `@/*` → `./src/*` (TypeScript)
- **Chunk splitting**: 6 manual chunks (vue-vendor, element-plus, markdown-it, markstream, markdown-plugins, security)

### RAG Configuration
- **Chunk size**: 400 chars, 100 char overlap (25%)
- **Semantic chunking**: Enabled (auto-detect doc type)
- **Vector model**: `ecnu-embedding-small` (1024 dimensions)
- **Low-memory mode**: Configurable (70% warning, 85% critical)

### Database (MyBatis-Plus)
- **Logical delete**: `deleted` field (0 = active, 1 = deleted)
- **ID strategy**: Auto-increment
- **Naming**: Underscore to camelCase mapping enabled

### AI Models
- **Chat**: `ecnu-plus` (default)
- **Tool calling**: `ecnu-max`
- **Embedding**: `ecnu-embedding-small`
- **Chat memory TTL**: 168 hours (7 days)

### Git Workflow
- **Branching**: GitFlow (main/develop/feature/*)
- **Commits**: Conventional Commits (feat/fix/docs/refactor)
- **Feature naming**: `feature/module-description`

---

## ANTI-PATTERNS (THIS PROJECT)

| Forbidden | Reason | Use Instead |
|-----------|--------|-------------|
| Default `JWT_SECRET` in production | Security risk | Generate 32+ char random key via env var |
| Pure white background (`#ffffff`) | Breaks design system | Warm cream `#fffefb` |
| Standard Redis without RediSearch | Vector search fails | `redis/redis-stack:latest` image |
| `MarkdownRenderer` for 策划案 | Specialized for AI chat only | Use `HtmlRenderer` component |
| Local `application-*.yml` commits | Environment-specific configs | Use env vars, keep in `.gitignore` |
| PageHelper pagination plugin | Removed, use MP built-in | MyBatis-Plus `Page<T>` API |

---

## UNIQUE STYLES

### Design System (Frontend)
- **Background**: Warm cream `#fffefb` (Zapier-like aesthetic)
- **Borders**: Structural backbone, NOT shadow elevation
- **Text colors**: `#201515` (dark), `#36342e` (body), `#939084` (muted)
- **Effects**: Glassmorphism, gradient cards, responsive breakpoints

### RAG Knowledge Base
- **Structure**: 8 numbered categories (00-08)
- **Auto-indexing**: On app startup or manual trigger
- **Document types**: Text, PDF (PDFBox), Excel (Apache POI)
- **Smart chunking**: Section/paragraph/sentence boundary detection

### Security
- **JWT blacklist**: Redis storage for logout invalidation
- **Pre-signed URLs**: Aliyun OSS secure file access
- **XSS protection**: DOMPurify dual config (Markdown + HTML modes)
- **Activation codes**: Required for user registration

---

## COMMANDS

### Backend
```bash
# Development
mvn spring-boot:run

# Build & package
mvn clean package -DskipTests

# Tests + coverage
mvn test
mvn jacoco:report

# Static analysis
mvn spotbugs:check
```

### Frontend
```bash
cd hrofficial-frontend

# Install + dev
npm install
npm run dev          # http://localhost:5173

# Build (with type-check)
npm run build

# Lint + format
npm run lint         # oxlint + eslint
npm run format       # prettier

# Tests
npm run test:unit    # Vitest
npm run test:e2e     # Playwright
```

### Docker Compose (Recommended)
```bash
cd deploy

# First time setup
cp .env.example .env
# Edit: DB_PASSWORD, JWT_SECRET, CHATECNU_API_KEY

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend
```

### RAG Management
```bash
# Initialize knowledge base (requires auth token)
curl -X POST http://localhost:8080/api/rag/initialize \
  -H "Authorization: Bearer YOUR_TOKEN"

# Test retrieval
curl http://localhost:8080/api/rag/test-retrieve?q="学生会结构"
```

---

## NOTES

- **Frontend proxy**: Vite dev server proxies `/api` → `http://localhost:8080`
- **Database init**: `deploy/init/init.sql` auto-runs on first Docker startup
- **Vector storage**: Redis Stack required (RediSearch module for vector search)
- **File uploads**: Local storage default, Aliyun OSS optional (config in `application.yml`)
- **AI streaming**: SSE support for chat (`/api/ai/chat/stream`, `/api/ai/chat-with-rag`)
- **No frontend CI**: Frontend lacks independent GitHub Actions pipeline (consider adding)
- **Uploads directory**: Runtime-generated, should be `.gitignore`d (currently at repo root)

---

## HIERARCHY

- **Backend details** → `src/main/java/com/redmoon2333/AGENTS.md`
- **Config deep-dive** → `src/main/java/com/redmoon2333/config/AGENTS.md`
- **Frontend structure** → `hrofficial-frontend/AGENTS.md`
- **RAG knowledge base** → `src/main/resources/rag-knowledge-base/AGENTS.md`
- **Deployment guide** → `deploy/AGENTS.md`

---

**Total files**: 696 | **Java files**: 108 | **Max depth**: 11 levels | **Mode**: Update