<script setup lang="ts">
import { computed } from 'vue'
import { markdownService } from '@/utils/markdown-service'

/**
 * Markdown渲染组件（markdown-it增强版）
 *
 * Why: 使用markdown-it渲染，支持任务列表、表情符号、代码高亮等丰富格式
 * Warning: 仅用于AI对话场景，策划案请使用HtmlRenderer
 */

const props = defineProps<{
  content: string
}>()

const renderedContent = computed(() => {
  if (!props.content) return ''
  return markdownService.render(props.content)
})
</script>

<template>
  <div class="markdown-renderer" v-html="renderedContent"></div>
</template>

<style scoped>
.markdown-renderer {
  line-height: 1.7;
  color: #1C1917;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.markdown-renderer :deep(h1) {
  color: #1C1917;
  margin: 24px 0 16px;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  border-left: 4px solid #FF6B4A;
  padding-left: 16px;
}

.markdown-renderer :deep(h2) {
  color: #1C1917;
  margin: 20px 0 12px;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
  border-left: 3px solid #FF8A70;
  padding-left: 12px;
}

.markdown-renderer :deep(h3) {
  color: #1C1917;
  margin: 16px 0 10px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-renderer :deep(h4),
.markdown-renderer :deep(h5),
.markdown-renderer :deep(h6) {
  color: #1C1917;
  margin: 14px 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.markdown-renderer :deep(p) {
  margin: 12px 0;
  line-height: 1.7;
}

.markdown-renderer :deep(strong) {
  color: #1C1917;
  font-weight: 600;
  background: linear-gradient(120deg, #FFE5D9 0%, #FFCCCB 100%);
  padding: 2px 4px;
  border-radius: 3px;
}

.markdown-renderer :deep(em) {
  color: #78716C;
  font-style: italic;
  background: rgba(245, 158, 11, 0.1);
  padding: 2px 4px;
  border-radius: 3px;
}

.markdown-renderer :deep(ul),
.markdown-renderer :deep(ol) {
  padding-left: 28px;
  margin: 16px 0;
}

.markdown-renderer :deep(ul) {
  list-style-type: disc;
}

.markdown-renderer :deep(ol) {
  list-style-type: decimal;
}

.markdown-renderer :deep(li) {
  margin: 8px 0;
  line-height: 1.6;
  padding-left: 4px;
}

/* 任务列表样式 */
.markdown-renderer :deep(.task-list-item) {
  list-style: none;
  margin-left: -24px;
}

.markdown-renderer :deep(.task-list-item input[type="checkbox"]) {
  margin-right: 8px;
  width: 16px;
  height: 16px;
  accent-color: #FF6B4A;
  cursor: pointer;
}

.markdown-renderer :deep(.task-list-item.checked) {
  text-decoration: line-through;
  color: #78716C;
}

/* 代码块样式 - 深色主题 */
.markdown-renderer :deep(.code-block) {
  background: #1e1e1e;
  padding: 16px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #3f3f46;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  position: relative;
}

.markdown-renderer :deep(.code-block::before) {
  content: attr(data-lang);
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 11px;
  color: #78716C;
  text-transform: uppercase;
  font-family: 'JetBrains Mono', monospace;
}

.markdown-renderer :deep(.code-block code) {
  background: none;
  padding: 0;
  color: #e5e5e5;
  font-size: 14px;
  line-height: 1.5;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

/* 行内代码样式 */
.markdown-renderer :deep(code:not([class*="language-"])) {
  background: #f5f5f4;
  padding: 3px 6px;
  border-radius: 5px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  color: #EF4444;
  border: 1px solid #e7e5e4;
}

.markdown-renderer :deep(pre) {
  margin: 0;
}

/* 引用块样式 */
.markdown-renderer :deep(blockquote) {
  border-left: 4px solid #FF6B4A;
  padding: 12px 20px;
  margin: 16px 0;
  color: #78716C;
  font-style: italic;
  background: linear-gradient(90deg, rgba(255, 107, 74, 0.08) 0%, rgba(255, 107, 74, 0.02) 100%);
  border-radius: 0 8px 8px 0;
}

/* 链接样式 */
.markdown-renderer :deep(a) {
  color: #3B82F6;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
  font-weight: 500;
}

.markdown-renderer :deep(a:hover) {
  border-bottom-color: #3B82F6;
  color: #2563EB;
}

/* 表格样式 */
.markdown-renderer :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
  font-size: 14px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.markdown-renderer :deep(thead) {
  background: linear-gradient(180deg, #FFFBF7 0%, #FFF5F0 100%);
}

.markdown-renderer :deep(th),
.markdown-renderer :deep(td) {
  border: 1px solid #e7e5e4;
  padding: 12px 16px;
  text-align: left;
}

.markdown-renderer :deep(th) {
  font-weight: 600;
  color: #1C1917;
  background: rgba(255, 107, 74, 0.05);
}

.markdown-renderer :deep(tr:nth-child(even)) {
  background: #fafaf9;
}

.markdown-renderer :deep(tr:hover) {
  background: #f5f5f4;
}

/* 分割线样式 */
.markdown-renderer :deep(hr) {
  border: none;
  border-top: 2px solid #e7e5e4;
  margin: 24px 0;
  background: linear-gradient(90deg, transparent, #FF6B4A, transparent);
  height: 2px;
}

/* 图片样式 */
.markdown-renderer :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 12px 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.markdown-renderer :deep(img:hover) {
  transform: scale(1.02);
}

/* 表情符号样式 */
.markdown-renderer :deep(.emoji) {
  font-size: 1.2em;
  vertical-align: middle;
}

/* 容器样式（warning, info, tip） */
.markdown-renderer :deep(.warning) {
  background: #FEF3C7;
  border-left: 4px solid #F59E0B;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 0 8px 8px 0;
}

.markdown-renderer :deep(.info) {
  background: #DBEAFE;
  border-left: 4px solid #3B82F6;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 0 8px 8px 0;
}

.markdown-renderer :deep(.tip) {
  background: #D1FAE5;
  border-left: 4px solid #10B981;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 0 8px 8px 0;
}

/* 删除线样式 */
.markdown-renderer :deep(del) {
  color: #78716C;
  text-decoration: line-through;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .markdown-renderer :deep(h1) {
    font-size: 20px;
    padding-left: 12px;
  }

  .markdown-renderer :deep(h2) {
    font-size: 18px;
    padding-left: 10px;
  }

  .markdown-renderer :deep(h3) {
    font-size: 16px;
  }

  .markdown-renderer :deep(.code-block) {
    padding: 12px;
    font-size: 13px;
  }

  .markdown-renderer :deep(table) {
    font-size: 13px;
  }

  .markdown-renderer :deep(th),
  .markdown-renderer :deep(td) {
    padding: 8px 12px;
  }
}
</style>
