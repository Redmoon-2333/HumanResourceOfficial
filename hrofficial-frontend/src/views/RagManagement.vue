<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted } from 'vue'
import { initRag, getRagStatus } from '@/api/rag'
import { ElMessage } from 'element-plus'
import {
  Collection,
  Refresh,
  Check,
  Warning,
  InfoFilled,
  Timer,
  Document,
  Cpu,
  TrendCharts,
  Loading,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'

const loading = ref(false)
const initResult = ref<any>(null)
const showResult = ref(false)
const ragStatus = ref<any>(null)

// 获取知识库状态
const fetchStatus = async () => {
  try {
    const response = await getRagStatus()
    if (response.code === 200) {
      ragStatus.value = response.data
    }
  } catch (error: any) {
    console.error('获取知识库状态失败:', error)
  }
}

// 初始化知识库
const handleInit = async () => {
  loading.value = true
  showResult.value = false
  initResult.value = null

  try {
    const response = await initRag()
    initResult.value = response
    showResult.value = true

    if (response.code === 200) {
      ElMessage.success('知识库初始化成功')
      fetchStatus()
    } else {
      ElMessage.error(response.message || '初始化失败')
    }
  } catch (error: any) {
    initResult.value = {
      code: 500,
      message: error.message || '初始化失败'
    }
    showResult.value = true
    ElMessage.error(error.message || '初始化失败')
  } finally {
    loading.value = false
  }
}

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}

onMounted(() => {
  fetchStatus()
})
</script>

<template>
  <Layout>
    <div class="rag-management-page">
      <!-- Hero区域 -->
      <div class="hero-section">
        <div class="hero-background">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="network-pattern">
            <svg viewBox="0 0 400 400" class="network-svg">
              <defs>
                <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#BE123C;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#E11D48;stop-opacity:0.3" />
                </linearGradient>
              </defs>
              <!-- 网络节点和连线 -->
              <circle cx="100" cy="100" r="4" fill="#BE123C" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" repeatCount="indefinite" />
              </circle>
              <circle cx="300" cy="150" r="4" fill="#F59E0B" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="0.5s" repeatCount="indefinite" />
              </circle>
              <circle cx="200" cy="300" r="4" fill="#FB7185" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="1s" repeatCount="indefinite" />
              </circle>
              <line x1="100" y1="100" x2="300" y2="150" stroke="url(#lineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="300" y1="150" x2="200" y2="300" stroke="url(#lineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="200" y1="300" x2="100" y2="100" stroke="url(#lineGradient)" stroke-width="1" opacity="0.5" />
            </svg>
          </div>
        </div>
        
        <div class="hero-content">
          <div class="hero-badge">
            <el-icon><Collection /></el-icon>
            <span>AI 知识库</span>
          </div>
          <!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
          <h1 class="hero-title" style="color: #000000;">
            智能知识库<span style="color: #000000;">管理系统</span>
          </h1>
          <!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
          <p class="hero-subtitle" style="color: #78716C;">
            管理 AI 助手的知识库内容，初始化向量数据库以提升问答质量
          </p>
        </div>
      </div>

      <!-- 状态仪表板 -->
      <div class="dashboard-section">
        <div class="dashboard-grid">
          <!-- 文档统计 -->
          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#BE123C"><Document /></el-icon>
              <span>文档数量</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">
                <AnimatedCounter :value="ragStatus?.totalDocuments || 0" />
              </span>
              <span class="stat-label">已索引文档</span>
            </div>
            <div class="stat-trend up">
              <el-icon><TrendCharts /></el-icon>
              <span>实时更新</span>
            </div>
          </div>

          <!-- 向量统计 -->
          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#3B82F6"><TrendCharts /></el-icon>
              <span>向量数量</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">
                <AnimatedCounter :value="ragStatus?.totalVectors || 0" />
              </span>
              <span class="stat-label">已生成向量</span>
            </div>
            <div class="stat-trend up">
              <el-icon><TrendCharts /></el-icon>
              <span>AI 可用</span>
            </div>
          </div>

          <!-- 最后更新 -->
          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#F59E0B"><Timer /></el-icon>
              <span>最后更新</span>
            </div>
            <div class="stat-content">
              <span class="stat-time">
                {{ ragStatus?.lastUpdateTime ? new Date(ragStatus.lastUpdateTime).toLocaleDateString() : '从未更新' }}
              </span>
              <span class="stat-label">上次初始化时间</span>
            </div>
            <div class="stat-trend" :class="{ warning: !ragStatus?.lastUpdateTime }">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ ragStatus?.lastUpdateTime ? '已同步' : '待初始化' }}</span>
            </div>
          </div>

          <!-- 向量维度 -->
          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#10B981"><Cpu /></el-icon>
              <span>向量维度</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">
                {{ ragStatus?.vectorDimension || 0 }}
              </span>
              <span class="stat-label">维度配置</span>
            </div>
            <div class="stat-trend up">
              <el-icon><Check /></el-icon>
              <span>标准配置</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 初始化操作区 -->
      <div class="init-section">
        <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
          <div class="init-content">
            <div class="init-header">
              <div class="init-icon" :class="{ loading: loading }">
                <el-icon :size="32" color="white"><Cpu /></el-icon>
              </div>
              <div class="init-title-group">
                <h2 class="init-title">知识库初始化</h2>
                <p class="init-desc">将文档转换为向量数据，供 AI 助手检索使用</p>
              </div>
            </div>

            <!-- 进度指示器 -->
            <div v-if="loading" class="progress-section">
              <div class="progress-steps">
                <div class="progress-step active">
                  <div class="step-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <span>读取文档</span>
                </div>
                <div class="progress-line">
                  <div class="line-fill"></div>
                </div>
                <div class="progress-step active">
                  <div class="step-icon">
                    <el-icon><Loading class="animate-spin" /></el-icon>
                  </div>
                  <span>生成向量</span>
                </div>
                <div class="progress-line">
                  <div class="line-fill" style="animation-delay: 0.5s"></div>
                </div>
                <div class="progress-step">
                  <div class="step-icon">
                    <el-icon><Collection /></el-icon>
                  </div>
                  <span>存储索引</span>
                </div>
              </div>
            </div>

            <!-- 初始化按钮 -->
            <div v-if="!showResult && !loading" class="init-action">
              <button class="init-btn" @click="handleInit" :disabled="loading">
                <el-icon :size="20"><Refresh /></el-icon>
                <span>开始初始化</span>
              </button>
              <p class="init-hint">初始化过程可能需要几分钟，请耐心等待</p>
            </div>

            <!-- 结果展示 -->
            <div v-if="showResult" class="result-section">
              <!-- Warning: 初始化成功但无数据时的警告提示 -->
              <div v-if="initResult?.code === 200 && initResult?.data && initResult.data.processedFiles === 0" class="warning-banner">
                <el-icon :size="20" color="#F59E0B"><Warning /></el-icon>
                <span>未找到可处理的文档，请检查文档路径配置是否正确</span>
              </div>
              <div 
                class="result-card"
                :class="initResult?.code === 200 ? 'success' : 'error'"
              >
                <div class="result-icon">
                  <el-icon :size="32" color="white">
                    <CircleCheck v-if="initResult?.code === 200" />
                    <CircleClose v-else />
                  </el-icon>
                </div>
                <div class="result-content">
                  <h3 class="result-title">
                    {{ initResult?.code === 200 ? '初始化成功' : '初始化失败' }}
                  </h3>
                  <p class="result-message">{{ initResult?.message }}</p>
                  <div v-if="initResult?.data" class="result-details">
                    <div class="detail-item">
                      <span class="detail-label">处理文档:</span>
                      <span class="detail-value">{{ initResult.data.processedFiles || 0 }} 个</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">生成向量:</span>
                      <span class="detail-value">{{ initResult.data.totalChunks || initResult.data.generatedVectors || 0 }} 个</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">失败文件:</span>
                      <span class="detail-value">{{ initResult.data.failedFiles || 0 }} 个</span>
                    </div>
                  </div>
                </div>
              </div>
              <button class="reinit-btn" @click="showResult = false">
                <el-icon><Refresh /></el-icon>
                <span>重新初始化</span>
              </button>
            </div>
          </div>
        </GlassPanel>
      </div>

      <!-- 信息说明 -->
      <div class="info-section">
        <div class="info-grid">
          <GlassPanel :blur="15" :opacity="0.9" :border-radius="20">
            <div class="info-card">
              <div class="info-icon" style="background: linear-gradient(135deg, #BE123C, #E11D48);">
                <el-icon :size="24" color="white"><InfoFilled /></el-icon>
              </div>
              <h3 class="info-title">什么是知识库？</h3>
              <p class="info-text">
                知识库是 AI 助手的"大脑"，存储了所有可供检索的文档信息。通过向量技术，AI 能够快速找到与用户问题最相关的资料。
              </p>
            </div>
          </GlassPanel>

          <GlassPanel :blur="15" :opacity="0.9" :border-radius="20">
            <div class="info-card">
              <div class="info-icon" style="background: linear-gradient(135deg, #3B82F6, #2563EB);">
                <el-icon :size="24" color="white"><TrendCharts /></el-icon>
              </div>
              <h3 class="info-title">向量数据库</h3>
              <p class="info-text">
                文档被转换为高维向量后存储，使得语义相似的文本在向量空间中距离相近，从而实现智能语义检索。
              </p>
            </div>
          </GlassPanel>

          <GlassPanel :blur="15" :opacity="0.9" :border-radius="20">
            <div class="info-card">
              <div class="info-icon" style="background: linear-gradient(135deg, #10B981, #059669);">
                <el-icon :size="24" color="white"><Check /></el-icon>
              </div>
              <h3 class="info-title">初始化建议</h3>
              <p class="info-text">
                建议每月至少初始化一次，或在添加大量新文档后进行。保持知识库最新可确保 AI 回答的准确性和时效性。
              </p>
            </div>
          </GlassPanel>
        </div>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
.rag-management-page {
  min-height: calc(100vh - 64px);
  background: #FFFFFF;
  padding: 32px;
  padding-bottom: 40px;
  max-width: 1400px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* Hero区域 */
.hero-section {
  position: relative;
  padding: var(--space-10) var(--space-8);
  margin: var(--space-8);
  margin-bottom: 0;
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(190, 18, 60, 0.06) 0%, rgba(255, 255, 255, 0.95) 100%);
  border: 1px solid rgba(190, 18, 60, 0.1);
}

.hero-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(ellipse 60% 50% at 40% 60%, #BE123C 0%, rgba(190, 18, 60, 0.4) 45%, transparent 70%);
  top: -100px;
  right: -100px;
  animation: float-orb 8s ease-in-out infinite;
  border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  filter: blur(60px);
  opacity: 0.35;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(ellipse 55% 65% at 55% 35%, #E11D48 0%, rgba(225, 29, 72, 0.35) 50%, transparent 65%);
  bottom: -50px;
  left: -50px;
  animation: float-orb 10s ease-in-out infinite -3s;
  border-radius: 40% 60% 70% 30% / 40% 50% 50% 60%;
  filter: blur(50px);
  opacity: 0.25;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes badgeShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}

.network-pattern {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.6;
}

.network-svg {
  width: 100%;
  height: 100%;
  max-width: 600px;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #BE123C, #E11D48);
  border: none;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(190, 18, 60, 0.3);
  animation: fadeInDown 0.6s ease both;
  position: relative;
  overflow: hidden;
}

.hero-badge::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: badgeShine 3s ease-in-out infinite;
}

.hero-title {
  font-size: 38px;
  font-weight: 700;
  color: #1C1917;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.gradient-text {
  background: linear-gradient(135deg, #BE123C 0%, #E11D48 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 15px;
  color: #78716C;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 仪表板 */
.dashboard-section {
  position: relative;
  max-width: 1200px;
  margin: var(--space-8);
  margin-top: var(--space-6);
  padding: 24px;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid rgba(190, 18, 60, 0.1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.dashboard-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 6px;
  right: 6px;
  height: 3px;
  background: linear-gradient(90deg, #BE123C, #E11D48);
  border-radius: 18px 18px 0 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.dashboard-card {
  background: #FAFAFA;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(190, 18, 60, 0.1);
  border-color: rgba(190, 18, 60, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #78716C;
  margin-bottom: 16px;
}

/* 统计卡片 */
.stat-card {
  display: flex;
  flex-direction: column;
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  margin-bottom: 12px;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #1C1917;
}

.stat-time {
  font-size: 20px;
  font-weight: 600;
  color: #1C1917;
}

.stat-label {
  font-size: 13px;
  color: #A8A29E;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #10B981;
  font-weight: 500;
}

.stat-trend.warning {
  color: #F59E0B;
}

/* 初始化区域 */
.init-section {
  position: relative;
  max-width: 1200px;
  margin: var(--space-8);
  margin-top: var(--space-6);
  padding: 24px;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid rgba(190, 18, 60, 0.1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.init-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 6px;
  right: 6px;
  height: 3px;
  background: linear-gradient(90deg, #E11D48, #BE123C);
  border-radius: 18px 18px 0 0;
}

.init-content {
  padding: 8px;
}

.init-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #E7E5E4;
}

.init-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #BE123C, #E11D48);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 32px rgba(190, 18, 60, 0.35);
  transition: all 0.3s ease;
}

.init-icon.loading {
  animation: pulse-icon 1.5s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%, 100% { transform: scale(1); box-shadow: 0 12px 32px rgba(190, 18, 60, 0.35); }
  50% { transform: scale(1.05); box-shadow: 0 16px 40px rgba(190, 18, 60, 0.5); }
}

.init-title-group {
  flex: 1;
}

.init-title {
  font-size: 22px;
  font-weight: 600;
  color: #1C1917;
  margin: 0 0 6px;
}

.init-desc {
  font-size: 14px;
  color: #78716C;
  margin: 0;
}

/* 进度步骤 */
.progress-section {
  margin-bottom: 32px;
}

.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.progress-step .step-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #F5F5F4;
  color: #A8A29E;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
}

.progress-step.active .step-icon {
  background: linear-gradient(135deg, #BE123C, #E11D48);
  color: white;
  box-shadow: 0 8px 24px rgba(190, 18, 60, 0.35);
}

.progress-step.active span {
  color: #BE123C;
}

.progress-step span {
  font-size: 13px;
  color: #A8A29E;
  font-weight: 500;
}

.progress-line {
  width: 80px;
  height: 4px;
  background: #E7E5E4;
  border-radius: 2px;
  overflow: hidden;
}

.line-fill {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, #BE123C, #E11D48);
  border-radius: 2px;
  animation: fill-line 1.5s ease-in-out infinite;
}

@keyframes fill-line {
  0% { width: 0; }
  50% { width: 100%; }
  100% { width: 100%; }
}

/* 初始化按钮 */
.init-action {
  text-align: center;
  padding: 20px 0;
}

.init-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 16px 40px;
  background: linear-gradient(135deg, #BE123C, #E11D48);
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 8px 24px rgba(190, 18, 60, 0.35);
  margin-bottom: 16px;
}

.init-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(190, 18, 60, 0.45);
}

.init-hint {
  font-size: 13px;
  color: #A8A29E;
  margin: 0;
}

/* 结果区域 */
.result-section {
  text-align: center;
}

/* Warning: 初始化无数据警告横幅样式 */
.warning-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 12px;
  margin-bottom: 16px;
  color: #D97706;
  font-size: 14px;
  font-weight: 500;
}

.result-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  border-radius: 20px;
  margin-bottom: 24px;
  text-align: left;
}

.result-card.success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05));
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.result-card.error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.05));
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.result-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.result-card.success .result-icon {
  background: linear-gradient(135deg, #10B981, #059669);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
}

.result-card.error .result-icon {
  background: linear-gradient(135deg, #EF4444, #DC2626);
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.35);
}

.result-content {
  flex: 1;
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.result-card.success .result-title {
  color: #10B981;
}

.result-card.error .result-title {
  color: #EF4444;
}

.result-message {
  font-size: 14px;
  color: #78716C;
  margin: 0 0 16px;
}

.result-details {
  display: flex;
  gap: 24px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-label {
  font-size: 13px;
  color: #A8A29E;
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: #1C1917;
}

.reinit-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #F5F5F4;
  color: #57534E;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.reinit-btn:hover {
  background: #E7E5E4;
}

/* 信息区域 */
.info-section {
  position: relative;
  max-width: 1200px;
  margin: var(--space-8);
  margin-top: var(--space-6);
  padding: 24px;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid rgba(190, 18, 60, 0.1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.info-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 6px;
  right: 6px;
  height: 3px;
  background: linear-gradient(90deg, #BE123C, #E11D48);
  border-radius: 18px 18px 0 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-card {
  padding: 8px;
}

.info-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.info-title {
  font-size: 16px;
  font-weight: 600;
  color: #1C1917;
  margin: 0 0 8px;
}

.info-text {
  font-size: 14px;
  color: #78716C;
  line-height: 1.6;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  
  .progress-steps {
    flex-direction: column;
    gap: 16px;
  }
  
  .progress-line {
    width: 4px;
    height: 40px;
  }
  
  .line-fill {
    animation: fill-line-vertical 1.5s ease-in-out infinite;
  }
  
  @keyframes fill-line-vertical {
    0% { height: 0; }
    50% { height: 100%; }
    100% { height: 100%; }
  }
  
  .result-card {
    flex-direction: column;
    text-align: center;
  }
  
  .result-details {
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 28px;
  }

  .init-header {
    flex-direction: column;
    text-align: center;
  }
}
</style>
