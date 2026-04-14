# Frontend Application - Vue3 + TypeScript

**Parent:** `../AGENTS.md` | **Build Tool:** rolldown-vite | **Node:** ^20.19.0 || >=22.12.0

---

## OVERVIEW

Vue 3.5 frontend application with TypeScript, Element Plus UI, Pinia state management. Uses rolldown-vite (Rust-based) for build, dual linting (oxlint + ESLint), and custom chunk splitting strategy.

---

## STRUCTURE

```
hrofficial-frontend/
├── src/
│   ├── api/                    # 11 API modules (axios/fetch wrappers)
│   │   ├── index.ts            # API module exports
│   │   ├── auth.ts             # Authentication APIs
│   │   ├── activity.ts         # Activity management APIs
│   │   ├── material.ts         # Material management APIs
│   │   ├── ai.ts               # AI chat, RAG, plan generation APIs
│   │   └── ...
│   ├── components/             # 9 reusable components
│   │   ├── MarkdownRenderer.vue  # AI chat Markdown (SSE + markstream)
│   │   ├── HtmlRenderer.vue    # 策划案 HTML (iframe sandbox)
│   │   ├── FileUpload.vue      # File upload with progress
│   │   └── ...
│   ├── composables/            # Vue composition functions
│   │   ├── usePermission.ts    # Role-based permission checks
│   │   └── ...
│   ├── directives/             # Custom Vue directives
│   ├── router/
│   │   └── index.ts            # Vue Router config + guards
│   ├── stores/                 # Pinia state management
│   │   ├── user.ts             # User info + token persistence
│   │   └── ...
│   ├── styles/                 # Design system (CSS variables)
│   │   ├── variables.css       # Colors, spacing, borders
│   │   └── main.css            # Global styles
│   ├── types/                  # TypeScript type definitions
│   │   └── index.ts            # Global types (User, Activity, Material, etc.)
│   ├── utils/                  # Utility functions
│   │   ├── http-client.ts      # Custom fetch wrapper (SSE support)
│   │   ├── markdown-service.ts # Markdown rendering config
│   │   └── __tests__/          # Utility unit tests
│   ├── views/                  # 14 page components
│   │   ├── Login.vue           # Login page
│   │   ├── Dashboard.vue       # Dashboard
│   │   ├── Activities.vue      # Activity list
│   │   ├── ActivityDetail.vue  # Activity detail
│   │   ├── Materials.vue       # Material management
│   │   ├── AiChat.vue          # AI chat interface
│   │   └── ...
│   ├── App.vue                 # Root component
│   └── main.ts                 # Application entry point
├── e2e/                        # Playwright E2E tests
├── public/                     # Static assets
├── .env.production             # Production environment variables
├── vite.config.ts              # Vite build config (chunk splitting)
├── tsconfig.json               # TypeScript config
├── vitest.config.ts            # Vitest unit test config
├── playwright.config.ts        # Playwright E2E test config
└── package.json                # Dependencies + scripts
```

---

## WHERE TO LOOK

| Task | Location | Pattern |
|------|----------|---------|
| **Add API call** | `src/api/{module}.ts` | Extend existing API module |
| **Add page** | `src/views/` + `src/router/index.ts` | Create Vue component + route |
| **Add component** | `src/components/` | Reusable Vue component |
| **Add state** | `src/stores/` | Pinia store with `defineStore` |
| **Add permission** | `src/composables/usePermission.ts` | Use `usePermission()` composable |
| **Modify routes** | `src/router/index.ts` | Add route + meta permissions |
| **Add utility** | `src/utils/` + `__tests__/` | Function + unit test |
| **Change design** | `src/styles/variables.css` | CSS variables (colors, spacing) |

---

## KEY PATTERNS

### 1. API Calls (Custom HTTP Client)

```typescript
import { httpClient } from '@/utils/http-client'

// Regular request
const response = await httpClient.get('/api/activities')

// SSE streaming (AI chat)
const eventSource = await httpClient.stream('/api/ai/chat/stream', {
  method: 'POST',
  body: JSON.stringify({ message: 'Hello' })
})

for await (const chunk of eventSource) {
  console.log('Chunk:', chunk)
}
```

**Features:**
- Token injection from Pinia store
- SSE support for streaming
- File upload with progress
- Timeout control

### 2. Role-Based Permissions

```typescript
// In composables/usePermission.ts
const { hasMemberPermission, hasMinisterPermission } = usePermission()

// In component
if (hasMemberPermission()) {
  // Show member-only content
}

// In router
{
  path: '/materials',
  component: Materials,
  meta: { requiresMember: true }  // Route-level permission
}
```

### 3. Pinia Store with Persistence

```typescript
// stores/user.ts
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }
  
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }
  
  return { token, userInfo, setToken, logout }
})
```

### 4. Vue Router with Guards

```typescript
// router/index.ts
const router = createRouter({ ... })

router.beforeEach((to, from, next) => {
  const token = useUserStore().token
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresMember && !hasMemberPermission()) {
    next('/unauthorized')
  } else {
    next()
  }
})
```

### 5. Markdown Rendering (AI Chat)

```typescript
// utils/markdown-service.ts
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})
  .use(markdownItAnchor)
  .use(markdownItEmoji)
  .use(markdownItTaskLists)

export function renderMarkdown(text: string): string {
  const html = md.render(text)
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true }
  })
}
```

### 6. Streaming Markdown (markstream-vue)

```vue
<template>
  <MarkStreamVue :stream="aiMessageStream" :renderer="markdownRenderer" />
</template>

<script setup>
import { MarkStreamVue } from 'markstream-vue'
import { markdownRenderer } from '@/utils/markdown-service'

const aiMessageStream = ref(null)  // ReadableStream from SSE
</script>
```

---

## BUILD CONFIGURATION

### Node Version Requirement
```json
{
  "engines": {
    "node": "^20.19.0 || >=22.12.0"
  }
}
```

### Custom Chunk Splitting (vite.config.ts)
```typescript
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'vue-vendor': ['vue', 'vue-router', 'pinia'],
        'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
        'vendor-markdown-it': ['markdown-it'],
        'vendor-markstream': ['markstream-vue'],
        'vendor-markdown-plugins': ['markdown-it-task-lists', 'markdown-it-container'],
        'vendor-security': ['dompurify']
      }
    }
  }
}
```

### Asset Organization
```typescript
build: {
  rollupOptions: {
    output: {
      assetFileNames: (chunkInfo) => {
        const type = chunkInfo.name?.slice(-1)
        if (type === '.js') return 'assets/js/[name]-[hash].js'
        if (type === '.css') return 'assets/css/[name]-[hash].css'
        return 'assets/[ext]/[name]-[hash].[ext]'
      }
    }
  }
}
```

---

## LINTING & FORMATTING

### Dual Linting Strategy
```json
{
  "scripts": {
    "lint:oxlint": "oxlint . --fix -D correctness --ignore-path .gitignore",
    "lint:eslint": "eslint . --fix --cache",
    "lint": "run-s lint:*",
    "format": "prettier --write src/"
  }
}
```

**Oxlint:** Fast, Rust-based, correctness rules only
**ESLint:** Comprehensive, Vue + TypeScript rules

### Prettier Config (.prettierrc.json)
```json
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 100,
  "tabWidth": 2
}
```

---

## TESTING

### Unit Tests (Vitest)
```bash
npm run test:unit
```

**Pattern:**
```typescript
// src/utils/__tests__/markdown-service.test.ts
import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '../markdown-service'

describe('renderMarkdown', () => {
  it('should render bold text', () => {
    const result = renderMarkdown('**bold**')
    expect(result).toContain('<strong>bold</strong>')
  })
})
```

### E2E Tests (Playwright)
```bash
npm run test:e2e
```

**Pattern:**
```typescript
// e2e/vue.spec.ts
import { test, expect } from '@playwright/test'

test('visits the app', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('h1')).toContainText('Expected')
})
```

---

## NOTES

- **Path alias**: `@/*` → `./src/*` (configured in tsconfig)
- **No semicolons**: Prettier config enforces no semicolons
- **Single quotes**: All strings use single quotes
- **100 char width**: Line length limit for readability
- **SSE support**: Custom HTTP client handles Server-Sent Events
- **DOMPurify dual config**: Separate configs for Markdown vs HTML rendering
- **rolldown-vite**: Rust-based build tool, NOT standard Vite
- **Chunk size limit**: 1MB warning threshold in build config

---

## HIERARCHY

- **Parent:** `../AGENTS.md` (root)