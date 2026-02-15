<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import Layout from '@/components/Layout.vue'
import HtmlRenderer from '@/components/HtmlRenderer.vue'
import { generatePlanStream, createStreamController, type PlanGeneratorRequest } from '@/api/planGenerator'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  OfficeBuilding,
  Location,
  User,
  UserFilled,
  EditPen,
  MagicStick,
  Loading,
  View,
  Download,
  RefreshRight,
  InfoFilled
} from '@element-plus/icons-vue'

// 表单数据
const formData = reactive<PlanGeneratorRequest>({
  theme: '',
  organizer: '学生会',
  eventTime: '',
  eventLocation: '',
  staff: '',
  participants: '',
  purpose: '',
  leaderCount: 1,
  memberCount: 1
})

// 状态管理
const loading = ref(false)
const isGenerating = ref(false)
const generatedContent = ref('')
const showPreview = ref(false)
const streamController = ref<ReturnType<typeof createStreamController> | null>(null)

// 表单引用
const formRef = ref()

// 表单验证规则
const formRules = {
  theme: [
    { required: true, message: '请输入活动主题', trigger: 'blur' },
    { min: 2, max: 100, message: '活动主题长度在2-100个字符之间', trigger: 'blur' }
  ],
  organizer: [
    { required: true, message: '请输入主办单位', trigger: 'blur' },
    { max: 100, message: '主办单位长度不能超过100个字符', trigger: 'blur' }
  ],
  eventTime: [
    { required: true, message: '请选择活动时间', trigger: 'change' }
  ],
  eventLocation: [
    { required: true, message: '请输入活动地点', trigger: 'blur' },
    { max: 200, message: '活动地点长度不能超过200个字符', trigger: 'blur' }
  ]
}

// 计算表单完成度
const formProgress = computed(() => {
  const requiredFields = ['theme', 'organizer', 'eventTime', 'eventLocation']
  const filledFields = requiredFields.filter(field =>
    formData[field as keyof typeof formData]?.toString()?.trim()
  ).length
  return Math.round((filledFields / requiredFields.length) * 100)
})

// 提交表单生成策划案
const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  isGenerating.value = true
  generatedContent.value = ''
  showPreview.value = true

  try {
    // 创建流式控制器
    streamController.value = createStreamController()

    // 流式生成策划案
    await generatePlanStream(
      formData,
      (chunk) => {
        // 直接追加内容，不做复杂的处理
        generatedContent.value += chunk
      },
      streamController.value.signal
    )

    ElMessage.success('策划案生成成功')
  } catch (error: unknown) {
    console.error('生成策划案失败:', error)
    const errMsg = error instanceof Error ? error.message : '生成失败，请检查网络连接'
    // 如果是中断操作，不显示错误
    if (errMsg !== 'The user aborted a request.') {
      ElMessage.error(errMsg)
    }
  } finally {
    isGenerating.value = false
    loading.value = false
    streamController.value = null
  }
}

// 重置表单
const handleReset = () => {
  ElMessageBox.confirm('确定要重置表单吗？所有已填写的信息将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formRef.value?.resetFields()
    generatedContent.value = ''
    showPreview.value = false
    ElMessage.success('表单已重置')
  }).catch(() => {})
}

// 导出为HTML文件
const handleExport = () => {
  if (!generatedContent.value) {
    ElMessage.warning('请先生成策划案')
    return
  }

  const blob = new Blob([generatedContent.value], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `策划案_${formData.theme}_${new Date().toLocaleDateString()}.html`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)

  ElMessage.success('导出成功')
}

// 停止生成
const handleStop = () => {
  if (streamController.value) {
    streamController.value.abort()
    ElMessage.info('已停止生成')
  }
}
</script>

<template>
  <Layout>
    <div class="plan-generator-page">
      <!-- 页面头部 - 大标题风格 -->
      <div class="page-header">
        <div class="header-main">
          <div class="header-icon-wrapper">
            <div class="header-icon">
              <el-icon :size="32"><MagicStick /></el-icon>
            </div>
            <div class="icon-glow"></div>
          </div>
          <div class="hero-badge">
            <el-icon :size="14"><MagicStick /></el-icon>
            <span>AI 智能助手</span>
          </div>
          <h1 class="page-title">AI策划案生成</h1>
          <p class="page-subtitle">填写活动信息，AI将为您生成专业的活动策划案</p>
        </div>
        
        <!-- 进度条区域 - 放在标题下方 -->
        <div class="progress-section">
          <div class="progress-wrapper">
            <div class="progress-label">
              <span>表单完成度</span>
              <span class="progress-value">{{ formProgress }}%</span>
            </div>
            <el-progress
              :percentage="formProgress"
              :stroke-width="8"
              :color="['var(--coral-400)', 'var(--amber-400)', 'var(--emerald-500)']"
              :show-text="false"
              class="custom-progress"
            />
          </div>
        </div>
      </div>

      <div class="content-wrapper">
        <!-- 左侧表单区域 -->
        <div class="form-section">
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-position="top"
            class="plan-form"
            status-icon
          >
            <!-- 基本信息区块 -->
            <div class="form-block form-block-primary">
              <div class="block-header">
                <div class="block-icon primary">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="block-title-wrapper">
                  <h3 class="block-title">基本信息</h3>
                  <span class="block-subtitle">填写活动的核心信息</span>
                </div>
              </div>

              <div class="form-grid">
                <el-form-item label="活动主题" prop="theme" class="form-item-full">
                  <el-input
                    v-model="formData.theme"
                    placeholder="请输入活动主题/名称"
                    :prefix-icon="Document"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="主办单位" prop="organizer">
                  <el-input
                    v-model="formData.organizer"
                    placeholder="请输入主办单位"
                    :prefix-icon="OfficeBuilding"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="活动时间" prop="eventTime">
                  <el-input
                    v-model="formData.eventTime"
                    placeholder="例如: 2024年9月15日 19:00-21:00"
                    :prefix-icon="User"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="活动地点" prop="eventLocation">
                  <el-input
                    v-model="formData.eventLocation"
                    placeholder="请输入活动地点"
                    :prefix-icon="Location"
                    size="large"
                    clearable
                  />
                </el-form-item>
              </div>
            </div>

            <!-- 人员信息区块 -->
            <div class="form-block form-block-secondary">
              <div class="block-header">
                <div class="block-icon secondary">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <div class="block-title-wrapper">
                  <h3 class="block-title">人员信息</h3>
                  <span class="block-subtitle">配置活动参与人员</span>
                </div>
              </div>

              <div class="form-grid">
                <el-form-item label="工作人员">
                  <el-input
                    v-model="formData.staff"
                    placeholder="请输入工作人员信息"
                    :prefix-icon="User"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="参与人员">
                  <el-input
                    v-model="formData.participants"
                    placeholder="请输入参与人员信息"
                    :prefix-icon="UserFilled"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item label="部长/副部长人数">
                  <el-input-number
                    v-model="formData.leaderCount"
                    :min="0"
                    :max="20"
                    size="large"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>

                <el-form-item label="部员人数">
                  <el-input-number
                    v-model="formData.memberCount"
                    :min="0"
                    :max="100"
                    size="large"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
            </div>

            <!-- 活动目的 -->
            <div class="form-block form-block-accent">
              <div class="block-header">
                <div class="block-icon accent">
                  <el-icon><EditPen /></el-icon>
                </div>
                <div class="block-title-wrapper">
                  <h3 class="block-title">活动目的</h3>
                  <span class="block-subtitle">描述活动目标与预期效果</span>
                </div>
              </div>

              <el-form-item label="活动目的和预期效果">
                <el-input
                  v-model="formData.purpose"
                  type="textarea"
                  :rows="5"
                  placeholder="请详细描述活动目的和预期效果，这将帮助AI生成更精准的策划案..."
                  :prefix-icon="EditPen"
                  size="large"
                  resize="none"
                  maxlength="1000"
                  show-word-limit
                />
              </el-form-item>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleSubmit"
                class="submit-btn"
              >
                <el-icon v-if="!loading"><MagicStick /></el-icon>
                <span>{{ loading ? '生成中...' : '生成策划案' }}</span>
              </el-button>

              <el-button
                size="large"
                @click="handleReset"
                class="reset-btn"
              >
                <el-icon><RefreshRight /></el-icon>
                <span>重置表单</span>
              </el-button>
            </div>
          </el-form>
        </div>

        <!-- 右侧预览区域 -->
        <div class="preview-section" :class="{ active: showPreview }">
          <div class="preview-header">
            <div class="preview-title-wrapper">
              <div class="preview-icon">
                <el-icon><View /></el-icon>
              </div>
              <div class="preview-title-text">
                <span class="preview-title">策划案预览</span>
                <span v-if="isGenerating" class="preview-status">实时生成中</span>
              </div>
            </div>
            <div class="preview-actions">
              <el-button
                v-if="isGenerating"
                type="danger"
                size="small"
                @click="handleStop"
                class="stop-btn"
              >
                <el-icon><Loading /></el-icon>
                <span>停止生成</span>
              </el-button>
              <el-button
                v-if="generatedContent"
                type="primary"
                size="small"
                @click="handleExport"
                class="export-btn"
              >
                <el-icon><Download /></el-icon>
                <span>导出HTML</span>
              </el-button>
            </div>
          </div>

          <div class="preview-content">
            <!-- 加载状态 -->
            <div v-if="loading && !generatedContent" class="loading-state">
              <div class="loading-animation">
                <div class="loading-spinner">
                  <el-icon :size="40"><Loading /></el-icon>
                </div>
                <div class="loading-pulse"></div>
              </div>
              <p class="loading-title">AI正在生成策划案...</p>
              <span class="loading-desc">请稍候，这可能需要几秒钟时间</span>
            </div>

            <!-- 生成中状态 -->
            <div v-else-if="isGenerating && generatedContent" class="generating-state">
              <div class="state-header">
                <div class="state-indicator">
                  <el-icon class="loading-icon-small"><Loading /></el-icon>
                  <span>正在生成中... (实时更新)</span>
                </div>
              </div>
              <div class="content-display">
                <pre class="raw-content">{{ generatedContent }}</pre>
              </div>
            </div>

            <!-- 生成完成状态 -->
            <div v-else-if="generatedContent && !isGenerating" class="completed-state">
              <div class="content-display">
                <HtmlRenderer :content="generatedContent" />
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <div class="empty-decoration">
                <div class="empty-icon">
                  <el-icon :size="48"><Document /></el-icon>
                </div>
                <div class="empty-glow"></div>
              </div>
              <h3 class="empty-title">暂无预览</h3>
              <p class="empty-desc">填写左侧表单并点击"生成策划案"按钮<br>AI将为您生成专业的活动策划案</p>
              <div class="empty-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>提示：填写的信息越详细，生成的策划案越精准</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
/* ============================================================
   AI策划案生成页面 - 工作台风格优化
   严格遵循设计系统规范
   ============================================================ */

.plan-generator-page {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  background: #FFFFFF;
  min-height: calc(100vh - 64px);
  position: relative;
}

/* ============================================================
   页面头部 / Page Header
   大标题居中风格，参考往届活动页面
   ============================================================ */
.page-header {
  position: relative;
  border-radius: var(--radius-3xl);
  padding: var(--space-8) var(--space-8) var(--space-6);
  margin-bottom: var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-5);
  overflow: hidden;
  background: linear-gradient(135deg, 
    rgba(244, 63, 94, 0.08) 0%, 
    rgba(251, 113, 133, 0.04) 50%,
    rgba(255, 255, 255, 0.9) 100%);
  border: 1px solid rgba(244, 63, 94, 0.12);
  box-shadow: 0 4px 20px rgba(244, 63, 94, 0.06);
}

/* 页面头部顶部装饰线 */
.page-header::after {
  content: '';
  position: absolute;
  top: 0;
  left: 3px;
  right: 3px;
  height: 4px;
  background: linear-gradient(90deg, #F43F5E, #FB7185, #FDA4AF, #FB7185, #F43F5E);
  border-radius: 29px 29px 0 0;
  z-index: 5;
}

/* 背景装饰 - 有机Blob - 玫瑰橙 */
.page-header::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(ellipse 60% 50% at 40% 60%, #F43F5E 0%, rgba(244, 63, 94, 0.4) 45%, transparent 70%);
  border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  animation: blobMorph 10s ease-in-out infinite;
  filter: blur(60px);
  opacity: 0.35;
  pointer-events: none;
  z-index: 0;
}

.page-header::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(ellipse 55% 65% at 55% 35%, #FB7185 0%, rgba(251, 113, 133, 0.35) 50%, transparent 65%);
  border-radius: 40% 60% 70% 30% / 40% 50% 50% 60%;
  animation: blobMorph 12s ease-in-out infinite;
  animation-delay: -3s;
  filter: blur(50px);
  opacity: 0.25;
  pointer-events: none;
  z-index: 0;
}

/* 主标题区域 */
.header-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-4);
  position: relative;
  z-index: 1;
}

.header-icon-wrapper {
  position: relative;
  animation: fadeInDown 0.6s ease both;
}

.header-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #F43F5E, #FB7185);
  border-radius: var(--radius-2xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  animation: iconFloat 3s ease-in-out infinite;
  position: relative;
  z-index: 2;
  box-shadow: 0 8px 24px rgba(244, 63, 94, 0.35);
  animation: iconFloat 3s ease-in-out infinite;
}

/* 图标光晕效果 */
.icon-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 90px;
  height: 90px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.3) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 1;
  animation: glowPulse 2s ease-in-out infinite;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #F43F5E, #FB7185);
  color: white;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);
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

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #1F2937;
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.2;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.page-subtitle {
  font-size: 16px;
  color: #6B7280;
  margin: 0;
  line-height: 1.6;
  max-width: 500px;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 进度条区域 - 放在标题下方 */
.progress-section {
  width: 100%;
  max-width: 400px;
  position: relative;
  z-index: 1;
}

/* 进度条区域 */
.progress-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: 100%;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: var(--font-medium);
}

.progress-value {
  font-weight: var(--font-semibold);
  color: var(--coral-500);
  font-size: var(--text-base);
}

:deep(.custom-progress .el-progress-bar__outer) {
  border-radius: var(--radius-full);
  background-color: rgba(244, 63, 94, 0.1);
}

:deep(.custom-progress .el-progress-bar__inner) {
  border-radius: var(--radius-full);
  transition: all var(--transition-slow) var(--ease-out);
  background: linear-gradient(90deg, #F43F5E, #FB7185);
}

/* ============================================================
   内容区域 / Content Area
   ============================================================ */
.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
  width: 100%;
  min-width: 0;
}

/* ============================================================
   表单区域 / Form Section
   纯净圆角无边框风格
   ============================================================ */
.form-section {
  position: relative;
  background: #FFFFFF;
  border-radius: var(--radius-2xl);
  padding: var(--space-6);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  transition: all var(--transition-normal) var(--ease-out);
}

/* 表单区域顶部装饰线 */
.form-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 6px;
  right: 6px;
  height: 3px;
  background: linear-gradient(90deg, #F43F5E, #FB7185, #FDA4AF);
  border-radius: 18px 18px 0 0;
}

.form-section:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: rgba(244, 63, 94, 0.15);
}

.plan-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* 表单区块 */
.form-block {
  position: relative;
  background: #FAFAFA;
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all var(--transition-normal) var(--ease-out);
  overflow: hidden;
}

/* 区块顶部装饰线 */
.form-block::before {
  content: '';
  position: absolute;
  top: 0;
  left: 3px;
  right: 3px;
  height: 2px;
  background: linear-gradient(90deg, #F43F5E, #FB7185);
  border-radius: 17px 17px 0 0;
  opacity: 0.6;
}

.form-block:hover {
  background: #F5F5F5;
  border-color: rgba(244, 63, 94, 0.1);
}

.form-block:hover::before {
  opacity: 1;
}

/* 区块头部 */
.block-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  position: relative;
  z-index: 1;
}

.block-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--text-xl);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.block-icon.primary {
  background: linear-gradient(135deg, #F43F5E, #FB7185);
}

.block-icon.secondary {
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
}

.block-icon.accent {
  background: linear-gradient(135deg, #10B981, #34D399);
}

.block-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.block-title {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
  letter-spacing: -0.01em;
}

.block-subtitle {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.4;
}

/* 表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-item-full {
  grid-column: 1 / -1;
}

/* 表单项样式优化 */
:deep(.el-form-item__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  padding-bottom: var(--space-1);
  font-size: var(--text-sm);
}

:deep(.el-form-item) {
  margin-bottom: var(--space-4);
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* 输入框样式 - 柔和阴影 + 圆角 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-input-number .el-input__wrapper) {
  border-radius: var(--radius-xl);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 0 0 1px var(--border-light) inset;
  transition: all var(--transition-normal) var(--ease-out);
  background: var(--bg-primary);
}

:deep(.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 0 0 1px var(--border-medium) inset;
  transform: translateY(-1px);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 
    0 4px 16px rgba(255, 107, 74, 0.12),
    0 0 0 2px var(--coral-300) inset;
  transform: translateY(-1px);
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
}

/* 操作按钮 */
.form-actions {
  display: flex;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-light);
}

.submit-btn {
  flex: 1;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #F43F5E, #FB7185);
  border: none;
  border-radius: var(--radius-xl);
  box-shadow: 0 8px 24px rgba(244, 63, 94, 0.35);
  transition: all var(--transition-normal) var(--ease-spring);
  color: white;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(244, 63, 94, 0.45);
}

.reset-btn {
  height: 52px;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--radius-xl);
  border: 2px solid rgba(244, 63, 94, 0.2);
  color: #6B7280;
  background: transparent;
  transition: all var(--transition-normal) var(--ease-out);
  padding: 0 var(--space-6);
}

.reset-btn:hover {
  border-color: #F43F5E;
  color: #F43F5E;
  background: rgba(244, 63, 94, 0.05);
}

/* ============================================================
   预览区域 / Preview Section
   纯净圆角风格 + 柔和光晕
   ============================================================ */
.preview-section {
  position: relative;
  background: #FFFFFF;
  border-radius: var(--radius-2xl);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 700px;
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  transition: all var(--transition-normal) var(--ease-out);
}

/* 预览区域顶部装饰线 */
.preview-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 6px;
  right: 6px;
  height: 3px;
  background: linear-gradient(90deg, #FDA4AF, #FB7185, #F43F5E);
  border-radius: 18px 18px 0 0;
  z-index: 10;
}

.preview-section:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: rgba(244, 63, 94, 0.15);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: #FAFAFA;
}

.preview-title-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.preview-icon {
  width: 40px;
  height: 40px;
  background: rgba(244, 63, 94, 0.1);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F43F5E;
  font-size: var(--text-xl);
}

.preview-title-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-title {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  letter-spacing: -0.01em;
}

.preview-status {
  font-size: 13px;
  color: #F43F5E;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.preview-status::before {
  content: '';
  width: 6px;
  height: 6px;
  background: #F43F5E;
  border-radius: 50%;
  animation: dotPulse 1.5s ease-in-out infinite;
}

.preview-actions {
  display: flex;
  gap: var(--space-2);
}

.stop-btn {
  border-radius: var(--radius-lg);
  font-weight: var(--font-medium);
}

.export-btn {
  font-weight: var(--font-medium);
  border-radius: var(--radius-lg);
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--coral-500), var(--coral-600));
  border: none;
  box-shadow: var(--shadow-coral);
  transition: all var(--transition-normal) var(--ease-out);
}

.export-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-coral-lg);
}

.export-btn:active {
  transform: translateY(0);
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-6);
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
}

/* ============================================================
   状态显示 / State Displays
   ============================================================ */
.loading-state,
.generating-state,
.completed-state,
.empty-state {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: var(--space-4);
}

.loading-animation {
  position: relative;
  width: 80px;
  height: 80px;
}

.loading-spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--coral-500);
  animation: spin 1.5s linear infinite;
  z-index: 2;
}

.loading-pulse {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle, rgba(255, 107, 74, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.loading-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
}

.loading-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

/* 生成中状态 */
.state-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-4);
}

.state-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--coral-600);
}

.loading-icon-small {
  animation: spin 1.5s linear infinite;
  color: var(--coral-500);
  font-size: var(--text-lg);
}

.content-display {
  flex: 1;
  overflow: hidden;
}

.raw-content {
  background: var(--bg-secondary);
  border: none;
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 100%;
  overflow-y: auto;
  color: var(--text-primary);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: var(--space-8);
}

.empty-decoration {
  position: relative;
  margin-bottom: var(--space-5);
}

.empty-icon {
  width: 100px;
  height: 100px;
  background: #F3F4F6;
  border-radius: var(--radius-3xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F43F5E;
  position: relative;
  z-index: 2;
}

.empty-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 1;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 var(--space-2) 0;
  letter-spacing: -0.01em;
}

.empty-desc {
  font-size: 15px;
  line-height: 1.6;
  color: #6B7280;
  margin: 0 0 var(--space-5) 0;
}

.empty-tip {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: #F9FAFB;
  border-radius: var(--radius-lg);
  font-size: 13px;
  color: #6B7280;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* ============================================================
   动画关键帧 / Animation Keyframes
   ============================================================ */
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes iconFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    box-shadow: var(--shadow-coral);
  }
  25% {
    transform: translateY(-6px) rotate(2deg);
    box-shadow: 0 12px 32px rgba(255, 107, 74, 0.4);
  }
  50% {
    transform: translateY(-3px) rotate(0deg);
    box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
  }
  75% {
    transform: translateY(-8px) rotate(-2deg);
    box-shadow: 0 14px 36px rgba(255, 107, 74, 0.45);
  }
}

@keyframes glowPulse {
  0%, 100% {
    opacity: 0.6;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.15);
  }
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  }
  25% {
    transform: translate(20px, -20px) scale(1.05);
    border-radius: 30% 60% 70% 40% / 50% 60% 30% 60%;
  }
  50% {
    transform: translate(-10px, 15px) scale(0.95);
    border-radius: 50% 60% 30% 60% / 30% 60% 70% 40%;
  }
  75% {
    transform: translate(15px, 10px) scale(1.02);
    border-radius: 60% 40% 60% 30% / 70% 30% 50% 60%;
  }
}

@keyframes dotPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.4;
    transform: scale(0.8);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

/* ============================================================
   响应式设计 / Responsive Design
   ============================================================ */
@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 1024px) {
  .content-wrapper {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-section {
    min-height: 400px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-5);
  }

  .progress-wrapper {
    width: 100%;
    min-width: auto;
  }
}

@media (max-width: 768px) {
  .plan-generator-page {
    padding: var(--space-4);
  }

  .page-header {
    padding: var(--space-5);
  }

  .header-icon {
    width: 56px;
    height: 56px;
  }

  .page-title {
    font-size: var(--text-xl);
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .submit-btn,
  .reset-btn {
    width: 100%;
  }

  .block-icon {
    width: 40px;
    height: 40px;
    font-size: var(--text-lg);
  }
}

@media (max-width: 480px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }

  .preview-header {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }

  .preview-actions {
    width: 100%;
  }

  .stop-btn,
  .export-btn {
    flex: 1;
  }
}
</style>
