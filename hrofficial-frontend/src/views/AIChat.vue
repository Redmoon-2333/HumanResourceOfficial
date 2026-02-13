<script setup lang="ts">
import { ref, nextTick, computed, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import FloatingParticles from '@/components/FloatingParticles.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { chatWithRag, chatStream, createStreamController } from '@/api/ai'
import { initRag } from '@/api/rag'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  User,
  ChatDotRound,
  ArrowUp,
  MagicStick,
  Document,
  StarFilled,
  Collection,
  SetUp,
  ChatLineSquare,
  Delete,
  Upload
} from '@element-plus/icons-vue'

const userStore = useUserStore()

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp?: Date
  isLoading?: boolean
  mode?: ChatMode
}

type ChatMode = 'normal' | 'rag' | 'tool'

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const hasStartedChat = ref(false)
const chatMode = ref<ChatMode>('normal')

const modeOptions = [
  { value: 'normal' as ChatMode, label: '普通对话', icon: ChatLineSquare, color: 'var(--coral-500)', description: '标准AI对话模式' },
  { value: 'rag' as ChatMode, label: '知识库增强', icon: Collection, color: 'var(--color-info)', description: '基于知识库检索增强回答' },
  { value: 'tool' as ChatMode, label: '工具调用', icon: SetUp, color: 'var(--emerald-500)', description: '可查询部门成员信息' }
]

const currentModeInfo = computed(() => {
  return modeOptions.find(m => m.value === chatMode.value)!
})

const initializing = ref(false)

const quickQuestions = [
  { icon: StarFilled, text: '如何策划一场成功的团建活动？', color: 'var(--amber-500)' },
  { icon: Document, text: '请帮我写一份活动策划案模板', color: 'var(--emerald-500)' },
  { icon: User, text: '人力资源中心的组织架构是怎样的？', color: 'var(--color-info)' },
  { icon: MagicStick, text: '有什么创意的破冰游戏推荐？', color: 'var(--rose-500)' }
]

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const postprocessMarkdown = (content: string): string => {
  if (!content) return ''
  let processed = content
  processed = processed.replace(/###+([^\n\s]+)/g, '## $1\n')
  processed = processed.replace(/##([^\n\s]+)/g, '## $1\n')
  processed = processed.replace(/([^\n])(- )/g, '$1\n- ')
  processed = processed.replace(/\*\*(.+?)\*\*/g, '**$1**')
  processed = processed.replace(/([^\n])(##)/g, '$1\n\n$2')
  processed = processed.replace(/[ \t]+/g, ' ')
  processed = processed.replace(/- ([^\n]+?)- /g, '- $1\n- ')
  return processed
}

const sendMessage = async (content: string = inputMessage.value) => {
  if (!content.trim() || loading.value) return

  hasStartedChat.value = true

  messages.value.push({
    role: 'user',
    content: content.trim(),
    timestamp: new Date()
  })

  inputMessage.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const controller = createStreamController()
    let aiResponse = ''

    messages.value.push({
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isLoading: true,
      mode: chatMode.value
    })

    const messageIndex = messages.value.length - 1

    if (chatMode.value === 'normal') {
      const finalContent = await chatStream(
        { message: content.trim() },
        (chunk) => {
          aiResponse += chunk
          if (messages.value[messageIndex]) {
            messages.value[messageIndex].content = aiResponse
            messages.value = [...messages.value]
            scrollToBottom()
          }
        },
        controller.signal
      )

      if (messages.value[messageIndex]) {
        messages.value[messageIndex].content = finalContent
      }
    } else {
      await chatWithRag(
        {
          message: content.trim(),
          useRAG: chatMode.value === 'rag',
          enableTools: chatMode.value === 'tool'
        },
        (chunk) => {
          aiResponse += chunk
          if (messages.value[messageIndex]) {
            messages.value[messageIndex].content = aiResponse
            messages.value = [...messages.value]
            scrollToBottom()
          }
        }
      )
    }

    if (messages.value[messageIndex]) {
      messages.value[messageIndex].content = postprocessMarkdown(messages.value[messageIndex].content)
      messages.value[messageIndex].isLoading = false
      messages.value = [...messages.value]
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '发送消息失败'
    ElMessage.error(errMsg)
    messages.value.pop()
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const handleQuickQuestion = (question: string) => {
  sendMessage(question)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const formatTime = (date?: Date) => {
  if (!date) return ''
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const getModeTag = (mode?: ChatMode) => {
  if (!mode || mode === 'normal') return null
  return mode === 'rag' ? '知识库' : '工具'
}

const getModeTagColor = (mode?: ChatMode) => {
  if (!mode || mode === 'normal') return ''
  return mode === 'rag' ? 'var(--color-info)' : 'var(--emerald-500)'
}

const handleClear = () => {
  messages.value = []
  hasStartedChat.value = false
  ElMessage.success('已清空聊天记录')
}

const handleInitializeKnowledgeBase = async () => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '初始化知识库将重新处理所有知识文件，可能需要较长时间。确定要继续吗？',
      '确认初始化',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    initializing.value = true
    
    const response = await initRag({ forceReindex: false })
    
    const data = response.data as any
    if (data.code === 200) {
      ElMessage.success(`知识库初始化成功！\n处理文件：${data.data.totalFiles}个\n成功：${data.data.processedFiles}个\n失败：${data.data.failedFiles}个`)
    } else {
      ElMessage.error(data.message || '初始化失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      if (error.response?.status === 403) {
        ElMessage.error('权限不足：只有部长可以初始化知识库')
      } else {
        ElMessage.error(error.response?.data?.message || error.message || '初始化失败')
      }
    }
  } finally {
    initializing.value = false
  }
}

onMounted(() => {
  setTimeout(() => {
    document.querySelector('.welcome-section')?.classList.add('animate-in')
  }, 100)
})
</script>

<template>
  <Layout>
    <div class="ai-chat-page">
      <div v-if="!hasStartedChat" class="welcome-section">
        <div class="welcome-background">
          <FloatingParticles :count="30" :speed="0.3" />
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="gradient-orb orb-3"></div>
        </div>

        <div class="welcome-content">
          <div class="ai-avatar-container">
            <div class="ai-avatar">
              <el-icon :size="48" color="white"><ChatDotRound /></el-icon>
            </div>
            <div class="avatar-ring ring-1"></div>
            <div class="avatar-ring ring-2"></div>
            <div class="avatar-ring ring-3"></div>
          </div>

          <h1 class="welcome-title">
            <span class="gradient-text">AI 智能助手</span>
          </h1>
          <p class="welcome-subtitle">
            我是人力资源中心的智能助手，可以帮您解答问题、提供建议、协助策划活动
          </p>

          <div class="mode-selector">
            <div class="mode-label">选择对话模式</div>
            <div class="mode-buttons">
              <button
                v-for="mode in modeOptions"
                :key="mode.value"
                class="mode-btn"
                :class="{ active: chatMode === mode.value }"
                :style="{ '--mode-color': mode.color }"
                @click="chatMode = mode.value"
              >
                <el-icon class="mode-icon"><component :is="mode.icon" /></el-icon>
                <span class="mode-name">{{ mode.label }}</span>
              </button>
            </div>
            <div class="mode-description" :style="{ color: currentModeInfo.color }">
              {{ currentModeInfo.description }}
            </div>
          </div>

          <div class="quick-questions">
            <div class="quick-title">
              <el-icon><StarFilled /></el-icon>
              <span>试试这样问我</span>
            </div>
            <div class="quick-grid">
              <button
                v-for="(item, index) in quickQuestions"
                :key="index"
                class="quick-btn"
                :style="{ '--btn-color': item.color }"
                @click="handleQuickQuestion(item.text)"
              >
                <div class="quick-icon" :style="{ background: item.color }">
                  <el-icon><component :is="item.icon" /></el-icon>
                </div>
                <span class="quick-text">{{ item.text }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="chat-section" ref="messagesContainer">
        <div class="chat-messages">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-wrapper"
            :class="message.role"
          >
            <div class="message-avatar">
              <div v-if="message.role === 'assistant'" class="ai-avatar-small">
                <el-icon :size="20" color="white"><ChatDotRound /></el-icon>
              </div>
              <div v-else class="user-avatar-small">
                <el-icon :size="20" color="white"><User /></el-icon>
              </div>
            </div>

            <div class="message-content">
              <div class="message-bubble" :class="message.role">
                <div v-if="message.role === 'user'" class="message-text user-message">{{ message.content }}</div>
                <div v-else class="message-text ai-message">
                  <div v-if="message.isLoading && !message.content" class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <template v-else>
                    <div v-if="getModeTag(message.mode)" class="mode-tag" :style="{ background: getModeTagColor(message.mode) }">
                      {{ getModeTag(message.mode) }}
                    </div>
                    <MarkdownRenderer :content="message.content" />
                  </template>
                </div>
              </div>
              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>

          <div v-if="loading && !messages.some(m => m.isLoading)" class="message-wrapper assistant">
            <div class="message-avatar">
              <div class="ai-avatar-small">
                <el-icon :size="20" color="white"><ChatDotRound /></el-icon>
              </div>
            </div>
            <div class="message-content">
              <div class="message-bubble assistant">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-section">
        <GlassPanel :blur="30" :opacity="0.95" :border-radius="24" padding="16px">
          <div class="input-header" v-if="hasStartedChat">
            <div class="mode-switcher">
              <button
                v-for="mode in modeOptions"
                :key="mode.value"
                class="switch-btn"
                :class="{ active: chatMode === mode.value }"
                :style="{ '--mode-color': mode.color }"
                @click="chatMode = mode.value"
                :disabled="loading"
              >
                <el-icon><component :is="mode.icon" /></el-icon>
                <span>{{ mode.label }}</span>
              </button>
            </div>
            <div class="header-actions">
              <button class="action-btn" @click="handleInitializeKnowledgeBase" :disabled="initializing">
                <el-icon><Upload /></el-icon>
              </button>
              <button class="action-btn" @click="handleClear" :disabled="messages.length === 0">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
          <div class="input-container">
            <div class="input-wrapper">
              <textarea
                v-model="inputMessage"
                class="chat-input"
                placeholder="输入您的问题..."
                rows="1"
                @keydown="handleKeydown"
                @input="(e) => {
                  const target = e.target as HTMLTextAreaElement
                  target.style.height = 'auto'
                  target.style.height = target.scrollHeight + 'px'
                }"
              ></textarea>
            </div>
            <button
              class="send-btn"
              :class="{ active: inputMessage.trim(), loading }"
              @click="sendMessage()"
              :disabled="!inputMessage.trim() || loading"
            >
              <el-icon v-if="!loading" :size="20"><ArrowUp /></el-icon>
              <div v-else class="loading-spinner"></div>
            </button>
          </div>
        </GlassPanel>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
.ai-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: linear-gradient(180deg, var(--bg-warm) 0%, var(--coral-50) 100%);
  position: relative;
  overflow: hidden;
}

.welcome-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: var(--space-10) var(--space-6);
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s var(--ease-spring);
}

.welcome-section.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.welcome-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 8s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, var(--coral-500), var(--rose-400));
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, var(--amber-500), var(--amber-300));
  bottom: -50px;
  left: -50px;
  animation-delay: -3s;
}

.orb-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, var(--rose-400), var(--coral-500));
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -5s;
}

.welcome-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 700px;
}

.ai-avatar-container {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto var(--space-8);
}

.ai-avatar {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--coral-500), var(--coral-600));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  box-shadow: 0 20px 60px rgba(255, 107, 74, 0.4);
  animation: pulse-glow 3s ease-in-out infinite;
}

.avatar-ring {
  position: absolute;
  border: 2px solid rgba(255, 107, 74, 0.3);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.ring-1 {
  width: 140px;
  height: 140px;
  animation: ripple 2s ease-out infinite;
}

.ring-2 {
  width: 160px;
  height: 160px;
  animation: ripple 2s ease-out infinite 0.5s;
}

.ring-3 {
  width: 180px;
  height: 180px;
  animation: ripple 2s ease-out infinite 1s;
}

@keyframes ripple {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0;
  }
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 20px 60px rgba(255, 107, 74, 0.4);
  }
  50% {
    box-shadow: 0 20px 80px rgba(255, 107, 74, 0.6);
  }
}

.welcome-title {
  font-size: var(--text-5xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--space-4);
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.gradient-text {
  background: linear-gradient(135deg, var(--coral-500) 0%, var(--amber-500) 50%, var(--rose-400) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  margin-bottom: var(--space-8);
  line-height: var(--leading-relaxed);
}

.mode-selector {
  margin-bottom: var(--space-8);
}

.mode-label {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--space-3);
  font-weight: var(--font-medium);
}

.mode-buttons {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.mode-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-out);
  color: var(--text-secondary);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  font-weight: var(--font-medium);
}

.mode-btn:hover {
  border-color: var(--mode-color);
  color: var(--mode-color);
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.mode-btn.active {
  background: var(--mode-color);
  border-color: var(--mode-color);
  color: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.mode-icon {
  font-size: var(--text-lg);
}

.mode-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.mode-description {
  font-size: var(--text-xs);
  margin-top: var(--space-3);
  font-weight: var(--font-medium);
}

.quick-questions {
  text-align: left;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--space-4);
  justify-content: center;
  font-weight: var(--font-medium);
}

.quick-title .el-icon {
  color: var(--amber-500);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-2xl);
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-spring);
  text-align: left;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.quick-btn:hover {
  transform: translateY(-3px);
  border-color: var(--btn-color);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  font-weight: var(--font-medium);
  line-height: var(--leading-normal);
}

.chat-section {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  scroll-behavior: smooth;
}

.chat-messages {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.message-wrapper {
  display: flex;
  gap: var(--space-3);
  animation: message-enter 0.4s var(--ease-spring);
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar-small {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--coral-500), var(--coral-600));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.3);
}

.user-avatar-small {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--amber-500), var(--amber-600));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  max-width: 70%;
}

.message-wrapper.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-2xl);
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  word-wrap: break-word;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.message-bubble.assistant {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-top-left-radius: var(--space-1);
  color: var(--text-primary);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.message-bubble.user {
  background: linear-gradient(135deg, var(--coral-500), var(--coral-600));
  color: white;
  border-top-right-radius: var(--space-1);
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
}

.message-time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: var(--font-medium);
}

.mode-tag {
  display: inline-block;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  color: white;
  margin-bottom: var(--space-2);
  font-weight: var(--font-semibold);
}

.typing-indicator {
  display: flex;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-1);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--gray-300);
  border-radius: 50%;
  animation: typing 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

.input-section {
  padding: var(--space-4) var(--space-6) var(--space-6);
  position: relative;
  z-index: 10;
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.mode-switcher {
  display: flex;
  gap: var(--space-2);
  justify-content: center;
  flex-wrap: wrap;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-out);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.switch-btn:hover:not(:disabled) {
  border-color: var(--mode-color);
  color: var(--mode-color);
  background: rgba(255, 255, 255, 0.8);
}

.switch-btn.active {
  background: var(--mode-color);
  border-color: var(--mode-color);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.switch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.header-actions {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-out);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.8);
  color: var(--coral-500);
  border-color: var(--coral-300);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-container {
  display: flex;
  gap: var(--space-3);
  align-items: flex-end;
}

.input-wrapper {
  flex: 1;
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: var(--space-1);
  transition: all var(--transition-normal) var(--ease-out);
}

.input-wrapper:focus-within {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--coral-400);
  box-shadow: 0 0 0 3px rgba(255, 138, 112, 0.1);
}

.chat-input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-base);
  border: none;
  background: transparent;
  resize: none;
  outline: none;
  color: var(--text-primary);
  line-height: var(--leading-relaxed);
  max-height: 150px;
  min-height: 48px;
}

.chat-input::placeholder {
  color: var(--text-tertiary);
}

.send-btn {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-xl);
  border: none;
  background: var(--gray-200);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-spring);
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, var(--coral-500), var(--coral-600));
  color: white;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
}

.send-btn.active:hover {
  transform: scale(1.08);
  box-shadow: 0 12px 32px rgba(255, 107, 74, 0.45);
}

.send-btn:disabled {
  cursor: not-allowed;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

@media (max-width: 640px) {
  .welcome-title {
    font-size: var(--text-4xl);
  }

  .mode-buttons {
    flex-direction: column;
    align-items: center;
  }

  .mode-btn {
    width: 100%;
    max-width: 200px;
    justify-content: center;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-section {
    padding: var(--space-4);
  }

  .input-section {
    padding: var(--space-3) var(--space-4) var(--space-5);
  }

  .mode-switcher {
    flex-wrap: wrap;
  }

  .switch-btn span {
    display: none;
  }

  .switch-btn {
    padding: var(--space-2);
  }
}
</style>
