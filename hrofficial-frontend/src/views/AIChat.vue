<script setup lang="ts">
import { ref, nextTick } from 'vue'
import Layout from '@/components/Layout.vue'
import { chat } from '@/api/ai'
import type { ChatRequest } from '@/types'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ... existing code ...
// HTML转义函数，用于流式输出时显示原始文本
const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  streaming?: boolean  // 标记是否正在流式输出
}

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement | null>(null)

// 发送消息（流式）
const handleSend = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入消息')
    return
  }

  const userMessage = inputMessage.value.trim()
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
    timestamp: new Date().toLocaleTimeString()
  })

  inputMessage.value = ''
  scrollToBottom()

  // 创建一个AI消息占位符，标记为流式输出中
  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: new Date().toLocaleTimeString(),
    streaming: true
  })
  scrollToBottom()

  loading.value = true
  try {
    const response = await chat({ message: userMessage })
    // 非流式，直接返回完整内容
    const aiResponse = response.data as any
    messages.value[aiMessageIndex].content = aiResponse.response || aiResponse
    messages.value[aiMessageIndex].streaming = false
    console.log('完整内容:', messages.value[aiMessageIndex].content)
    console.log('streaming状态:', messages.value[aiMessageIndex].streaming)
    console.log('开始markdown渲染...')
    // 强制重新渲染
    nextTick(() => {
      console.log('渲染完成')
    })
  } catch (error: any) {
    console.error('AI对话失败:', error)
    // 如果AI没有回复任何内容，显示错误信息
    if (!messages.value[aiMessageIndex].content) {
      messages.value[aiMessageIndex].content = '抱歉，回复失败，请稍后重试'
    }
    messages.value[aiMessageIndex].streaming = false
    ElMessage.error(error.message || 'AI回复失败')
  } finally {
    loading.value = false
  }
}

// 清空聊天
const handleClear = () => {
  messages.value = []
  ElMessage.success('已清空聊天记录')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 快速问题
const quickQuestions = [
  '如何组织一场成功的活动？',
  '活动策划需要注意哪些方面？',
  '如何提高团队协作效率？',
  '我感觉学业压力好大，该怎么办？'
]

// 快速提问
const handleQuickQuestion = (question: string) => {
  inputMessage.value = question
  handleSend()
}
</script>

<template>
  <Layout>
    <div class="ai-chat-container">
      <el-card class="chat-card">
        <template #header>
          <div class="chat-header">
            <div>
              <el-icon :size="24" color="var(--color-primary)">
                <ChatDotRound />
              </el-icon>
              <span class="chat-title">人力资源中心小助理</span>
            </div>
            <el-button size="small" @click="handleClear" :disabled="messages.length === 0">
              <el-icon><Delete /></el-icon>
              清空
            </el-button>
          </div>
        </template>

        <!-- 聊天内容区 -->
        <div ref="chatContainer" class="chat-messages">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <el-icon :size="64" color="var(--color-primary)">
              <ChatDotRound />
            </el-icon>
            <h2>你好，{{ userStore.userInfo?.name }}！</h2>
            <p>我是人力资源中心的小助理，有什么可以帮助你的吗？</p>
            <p style="font-size: 14px; color: var(--color-text-light); margin-top: 8px;">
              🌟 无论是部门工作还是学习生活上的问题，我都会尽力帮助你~
            </p>
            
            <div class="quick-questions">
              <p class="quick-title">快速提问：</p>
              <div class="quick-buttons">
                <el-button
                  v-for="(q, index) in quickQuestions"
                  :key="index"
                  size="small"
                  @click="handleQuickQuestion(q)"
                >
                  {{ q }}
                </el-button>
              </div>
            </div>
          </div>

          <!-- 消息列表 -->
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message-item', message.role]"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.role === 'user'" :size="36">
                {{ userStore.userInfo?.name?.charAt(0) || 'U' }}
              </el-avatar>
              <el-avatar v-else :size="36" style="background-color: var(--color-primary)">
                <el-icon><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">
                  {{ message.role === 'user' ? userStore.userInfo?.name : '人力资源中心小助理' }}
                </span>
                <span class="message-time">{{ message.timestamp }}</span>
              </div>
              <div 
                class="message-text" 
                style="white-space: pre-wrap; font-family: inherit;" 
              >
                {{ message.content }}
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="message-item assistant">
            <div class="message-avatar">
              <el-avatar :size="36" style="background-color: var(--color-primary)">
                <el-icon><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-text loading-dots">
                AI正在思考<span class="dots">...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入你的问题..."
            @keydown.enter.ctrl="handleSend"
            :disabled="loading"
          />
          <div class="input-actions">
            <span class="input-tip">Ctrl + Enter 发送</span>
            <el-button
              type="primary"
              @click="handleSend"
              :loading="loading"
              :disabled="!inputMessage.trim()"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </Layout>
</template>

<style scoped>
.ai-chat-container {
  padding: var(--spacing-lg);
  max-width: 1000px;
  margin: 0 auto;
}

.chat-card {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header > div {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-md);
}

.welcome-message {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-secondary);
}

.welcome-message h2 {
  margin: var(--spacing-lg) 0 var(--spacing-sm);
  color: var(--color-primary);
}

.quick-questions {
  margin-top: var(--spacing-xl);
}

.quick-title {
  font-size: 14px;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-md);
}

.quick-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  justify-content: center;
}

.message-item {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.message-item.user .message-text {
  background-color: var(--color-primary);
  color: white;
}

.message-item.user .message-text :deep(*) {
  color: white;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-light);
}

.message-sender {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.message-text {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  background-color: white;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.loading-dots {
  color: var(--color-text-secondary);
}

.loading-dots .dots {
  display: inline-block;
  animation: blink 1.4s infinite;
}

@keyframes blink {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.chat-input {
  margin-top: var(--spacing-md);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-sm);
}

.input-tip {
  font-size: 12px;
  color: var(--color-text-light);
}

/* Markdown渲染样式 */
.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) {
  margin-top: 16px;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: block;
}

.message-text :deep(h1) {
  font-size: 24px;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: var(--spacing-sm);
  margin-top: 20px;
  margin-bottom: 16px;
}

.message-text :deep(h2) {
  font-size: 20px;
  margin-top: 18px;
  margin-bottom: 14px;
}

.message-text :deep(h3) {
  font-size: 18px;
  margin-top: 16px;
  margin-bottom: 12px;
}

.message-text :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
  display: block;
}

.message-text :deep(p:first-child) {
  margin-top: 0;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(strong),
.message-text :deep(b) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.message-text :deep(em),
.message-text :deep(i) {
  font-style: italic;
  color: var(--color-text-secondary);
}

.message-text :deep(code) {
  background-color: rgba(0, 0, 0, 0.05);
  color: #c41d7f;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.message-text :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05);
  border-left: 3px solid var(--color-primary);
  padding: var(--spacing-md);
  border-radius: 4px;
  overflow-x: auto;
  margin: var(--spacing-md) 0;
  font-size: 12px;
}

.message-text :deep(pre code) {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

.message-text :deep(ul) {
  margin: var(--spacing-md) 0;
  padding-left: 24px;
  list-style-type: disc;
}

.message-text :deep(ol) {
  margin: var(--spacing-md) 0;
  padding-left: 24px;
  list-style-type: decimal;
}

.message-text :deep(li) {
  margin: 6px 0;
  line-height: 1.6;
}

.message-text :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  padding-left: var(--spacing-md);
  margin: var(--spacing-md) 0;
  color: var(--color-text-secondary);
  font-style: italic;
}

.message-text :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
  border-bottom: 1px solid var(--color-primary);
}

.message-text :deep(a:hover) {
  text-decoration: underline;
}

.message-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: var(--spacing-md) 0;
  font-size: 13px;
}

.message-text :deep(table th) {
  background-color: var(--color-bg-secondary);
  font-weight: 600;
  padding: 8px 12px;
  text-align: left;
  border: 1px solid var(--color-border);
}

.message-text :deep(table td) {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
}

.message-text :deep(table tr:nth-child(even)) {
  background-color: rgba(0, 0, 0, 0.02);
}

.message-text :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: var(--spacing-lg) 0;
}
</style>
