<script setup lang="ts">
import { computed, ref, onMounted, watch, nextTick } from 'vue'

/**
 * HTML内容渲染组件（策划案专用）
 * 
 * Why: 使用iframe渲染完整的HTML文档，支持<!DOCTYPE>, <html>, <head>等标签
 * Warning: 仅用于策划案场景，AI对话请使用MarkdownRenderer
 */

const props = defineProps<{
  content: string
}>()

const iframeRef = ref<HTMLIFrameElement | null>(null)
const isRendered = ref(false)

const processedContent = computed(() => {
  if (!props.content) return ''
  
  let html = props.content.trim()
  
  html = html.replace(/^```(?:html|HTML)?\s*\n?/g, '')
  html = html.replace(/\n?```\s*$/g, '')
  
  return html.trim()
})

const renderHtml = async () => {
  if (!iframeRef.value || !processedContent.value) return
  
  const iframe = iframeRef.value
  const doc = iframe.contentDocument || iframe.contentWindow?.document
  
  if (doc) {
    doc.open()
    doc.write(processedContent.value)
    doc.close()
    
    await nextTick()
    
    try {
      const body = doc.body
      if (body) {
        const height = Math.max(
          body.scrollHeight,
          body.offsetHeight,
          doc.documentElement.scrollHeight,
          doc.documentElement.offsetHeight
        )
        iframe.style.height = height + 'px'
      }
    } catch (e) {
      iframe.style.height = '600px'
    }
    
    isRendered.value = true
  }
}

watch(() => props.content, () => {
  nextTick(() => renderHtml())
})

onMounted(() => {
  renderHtml()
})
</script>

<template>
  <div class="html-renderer-wrapper">
    <iframe
      ref="iframeRef"
      class="html-renderer-iframe"
      sandbox="allow-same-origin"
      frameborder="0"
      scrolling="no"
    ></iframe>
    <div v-if="!isRendered && processedContent" class="loading-placeholder">
      正在渲染...
    </div>
  </div>
</template>

<style scoped>
.html-renderer-wrapper {
  width: 100%;
  min-height: 400px;
  position: relative;
}

.html-renderer-iframe {
  width: 100%;
  min-height: 400px;
  border: none;
  background: white;
  border-radius: 8px;
}

.loading-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #78716C;
  font-size: 14px;
}
</style>
