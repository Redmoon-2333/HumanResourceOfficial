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
  RefreshRight
} from '@element-plus/icons-vue'

const formData = reactive<PlanGeneratorRequest>({
  theme: '',
  organizer: '',
  eventTime: '',
  eventLocation: '',
  staff: '',
  participants: '',
  purpose: '',
  leaderCount: 1,
  memberCount: 1
})

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
  ],
  staff: [
    { required: true, message: '请输入工作人员', trigger: 'blur' },
    { max: 500, message: '工作人员信息长度不能超过500个字符', trigger: 'blur' }
  ],
  participants: [
    { required: true, message: '请输入参与人员', trigger: 'blur' },
    { max: 500, message: '参与人员信息长度不能超过500个字符', trigger: 'blur' }
  ],
  purpose: [
    { required: true, message: '请输入活动目的', trigger: 'blur' },
    { max: 1000, message: '活动目的长度不能超过1000个字符', trigger: 'blur' }
  ],
  leaderCount: [
    { required: true, message: '请输入部长/副部长数量', trigger: 'blur' },
    { type: 'number', min: 0, max: 50, message: '数量范围0-50', trigger: 'blur' }
  ],
  memberCount: [
    { required: true, message: '请输入部员数量', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '数量范围0-100', trigger: 'blur' }
  ]
}

const formRef = ref()
const loading = ref(false)
const generatedPlan = ref('')
const showPreview = ref(false)
const streamController = ref<{ signal: AbortSignal; abort: () => void } | null>(null)
const isGenerating = ref(false)

const formProgress = computed(() => {
  const fields = ['theme', 'organizer', 'eventTime', 'eventLocation', 'staff', 'participants', 'purpose']
  const filled = fields.filter(field => {
    const value = formData[field as keyof PlanGeneratorRequest]
    return value !== '' && value !== null && value !== undefined
  }).length
  return Math.round((filled / fields.length) * 100)
})

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善表单信息')
    return
  }

  loading.value = true
  isGenerating.value = true
  generatedPlan.value = ''
  showPreview.value = true

  try {
    streamController.value = createStreamController()

    const finalContent = await generatePlanStream(
      formData,
      (chunk) => {
        generatedPlan.value += chunk
      },
      streamController.value.signal
    )

    if (finalContent !== generatedPlan.value) {
      generatedPlan.value = finalContent
    }
    isGenerating.value = false
    ElMessage.success('策划案生成成功')
  } catch (error: unknown) {
    console.error('生成策划案失败:', error)
    const errMsg = error instanceof Error ? error.message : '生成失败，请检查网络连接'
    if (errMsg !== 'The user aborted a request.') {
      ElMessage.error(errMsg)
    }
    isGenerating.value = false
  } finally {
    loading.value = false
    streamController.value = null
  }
}

const handleReset = () => {
  ElMessageBox.confirm('确定要重置表单吗？所有已填写的信息将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    formRef.value.resetFields()
    generatedPlan.value = ''
    showPreview.value = false
    ElMessage.success('表单已重置')
  }).catch(() => {})
}

const handleExport = () => {
  if (!generatedPlan.value) {
    ElMessage.warning('请先生成策划案')
    return
  }

  const blob = new Blob([generatedPlan.value], { type: 'text/html;charset=utf-8' })
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
      <div class="page-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon :size="32"><MagicStick /></el-icon>
          </div>
          <div class="header-text">
            <h1 class="page-title">AI策划案生成</h1>
            <p class="page-subtitle">填写活动信息，AI将为您生成专业的活动策划案</p>
          </div>
        </div>
        <div class="progress-wrapper">
          <div class="progress-label">表单完成度</div>
          <el-progress
            :percentage="formProgress"
            :stroke-width="8"
            :color="['#FF6B4A', '#F59E0B', '#10B981']"
            class="custom-progress"
          />
        </div>
      </div>

      <div class="content-wrapper">
        <div class="form-section">
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-position="top"
            class="plan-form"
            status-icon
          >
            <div class="form-block">
              <div class="block-header">
                <div class="block-icon primary">
                  <el-icon><Document /></el-icon>
                </div>
                <h3 class="block-title">基本信息</h3>
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
                  <el-date-picker
                    v-model="formData.eventTime"
                    type="datetime"
                    placeholder="选择活动日期和时间"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    size="large"
                    style="width: 100%"
                  />
                </el-form-item>

                <el-form-item label="活动地点" prop="eventLocation" class="form-item-full">
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

            <div class="form-block">
              <div class="block-header">
                <div class="block-icon secondary">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <h3 class="block-title">人员信息</h3>
              </div>

              <div class="form-grid">
                <el-form-item label="工作人员" prop="staff" class="form-item-full">
                  <el-input
                    v-model="formData.staff"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入工作人员名单，如：张三、李四、王五"
                    :prefix-icon="User"
                    size="large"
                    resize="none"
                  />
                </el-form-item>

                <el-form-item label="参与人员" prop="participants" class="form-item-full">
                  <el-input
                    v-model="formData.participants"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入参与人员范围，如：全体学生会成员、2024级新生等"
                    :prefix-icon="UserFilled"
                    size="large"
                    resize="none"
                  />
                </el-form-item>

                <el-form-item label="部长/副部长数量" prop="leaderCount">
                  <el-input-number
                    v-model="formData.leaderCount"
                    :min="0"
                    :max="50"
                    size="large"
                    style="width: 100%"
                    controls-position="right"
                  />
                </el-form-item>

                <el-form-item label="部员数量" prop="memberCount">
                  <el-input-number
                    v-model="formData.memberCount"
                    :min="0"
                    :max="100"
                    size="large"
                    style="width: 100%"
                    controls-position="right"
                  />
                </el-form-item>
              </div>
            </div>

            <div class="form-block">
              <div class="block-header">
                <div class="block-icon accent">
                  <el-icon><EditPen /></el-icon>
                </div>
                <h3 class="block-title">活动目的</h3>
              </div>

              <el-form-item label="活动目的" prop="purpose">
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

        <div class="preview-section" :class="{ active: showPreview }">
          <div class="preview-header">
            <div class="preview-title">
              <el-icon><View /></el-icon>
              <span>策划案预览</span>
            </div>
            <div class="preview-actions">
              <el-button
                v-if="isGenerating"
                type="danger"
                size="small"
                @click="handleStop"
              >
                <el-icon><Loading /></el-icon>
                <span>停止生成</span>
              </el-button>
              <el-button
                v-if="generatedPlan"
                type="primary"
                link
                size="small"
                @click="handleExport"
              >
                <el-icon><Download /></el-icon>
                <span>导出HTML</span>
              </el-button>
            </div>
          </div>

          <div class="preview-content">
            <div v-if="loading && !generatedPlan" class="loading-state">
              <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
              <p>AI正在生成策划案...</p>
              <span>请稍候，这可能需要几秒钟时间</span>
            </div>

            <div v-else-if="isGenerating && generatedPlan" class="generating-state">
              <div class="state-header">
                <el-icon class="loading-icon-small"><Loading /></el-icon>
                <span>正在生成中...</span>
              </div>
              <div class="content-display">
                <pre class="raw-content">{{ generatedPlan }}</pre>
              </div>
            </div>

            <div v-else-if="generatedPlan && !isGenerating" class="completed-state">
              <div class="content-display">
                <HtmlRenderer :content="generatedPlan" />
              </div>
            </div>

            <div v-else class="empty-state">
              <div class="empty-icon">
                <el-icon :size="64"><Document /></el-icon>
              </div>
              <h3>暂无预览</h3>
              <p>填写左侧表单并点击"生成策划案"按钮<br>AI将为您生成专业的活动策划案</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
.plan-generator-page {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.page-header {
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.08) 0%, rgba(245, 158, 11, 0.08) 100%);
  border-radius: 24px;
  padding: var(--space-6) var(--space-8);
  margin-bottom: var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  border: 1px solid rgba(255, 107, 74, 0.1);
  width: 100%;
  box-sizing: border-box;
  flex-wrap: wrap;
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.header-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1); box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35); }
  50% { transform: scale(1.05); box-shadow: 0 12px 32px rgba(255, 107, 74, 0.45); }
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
}

.page-subtitle {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin: 0;
}

.progress-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 200px;
}

.progress-label {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  text-align: right;
}

:deep(.custom-progress .el-progress-bar__outer) {
  border-radius: 10px;
  background-color: rgba(255, 107, 74, 0.1);
}

:deep(.custom-progress .el-progress-bar__inner) {
  border-radius: 10px;
  transition: all 0.5s ease;
}

.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
  width: 100%;
  min-width: 0;
}

.form-section {
  background: white;
  border-radius: 24px;
  padding: var(--space-6);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 107, 74, 0.08);
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
}

.plan-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.form-block {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: var(--space-5);
  border: 1px solid rgba(255, 107, 74, 0.1);
  transition: all 0.3s ease;
}

.form-block:hover {
  border-color: rgba(255, 107, 74, 0.2);
  box-shadow: 0 4px 20px rgba(255, 107, 74, 0.08);
}

.block-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.block-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.block-icon.primary {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
}

.block-icon.secondary {
  background: linear-gradient(135deg, #F59E0B, #D97706);
}

.block-icon.accent {
  background: linear-gradient(135deg, #10B981, #059669);
}

.block-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-item-full {
  grid-column: 1 / -1;
}

:deep(.el-form-item__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  padding-bottom: var(--space-1);
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-input-number .el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(229, 231, 235, 0.8) inset;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #FF8A70 inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #FF8A70 inset, 0 0 0 4px rgba(255, 138, 112, 0.15);
}

:deep(.el-date-editor.el-input__wrapper) {
  border-radius: 12px;
}

.form-actions {
  display: flex;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid rgba(255, 107, 74, 0.1);
}

.submit-btn {
  flex: 1;
  height: 48px;
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: none;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(255, 107, 74, 0.45);
}

.reset-btn {
  height: 48px;
  font-size: var(--text-base);
  border-radius: 12px;
  border: 2px solid rgba(255, 107, 74, 0.2);
  color: var(--text-secondary);
  transition: all 0.3s ease;
}

.reset-btn:hover {
  border-color: #FF6B4A;
  color: #FF6B4A;
  background: rgba(255, 107, 74, 0.05);
}

.preview-section {
  background: white;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 107, 74, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 600px;
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid rgba(255, 107, 74, 0.1);
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.03) 0%, rgba(245, 158, 11, 0.03) 100%);
}

.preview-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.preview-actions {
  display: flex;
  gap: var(--space-2);
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

.loading-state,
.generating-state,
.completed-state,
.empty-state {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.state-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.1), rgba(245, 158, 11, 0.1));
  border-radius: 12px;
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: #FF6B4A;
}

.loading-icon {
  animation: spin 1.5s linear infinite;
  color: #FF6B4A;
}

.loading-icon-small {
  animation: spin 1.5s linear infinite;
  color: #FF6B4A;
  font-size: 18px;
}

.content-display {
  flex: 1;
  overflow: hidden;
}

.raw-content {
  background: #f8f9fa;
  border: 1px solid rgba(255, 107, 74, 0.2);
  border-radius: 12px;
  padding: 16px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 100%;
  overflow-y: auto;
  color: #1C1917;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: var(--space-4);
  color: var(--text-secondary);
}

.loading-state p {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
}

.loading-state span {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  color: var(--text-tertiary);
}

.empty-icon {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.1), rgba(245, 158, 11, 0.1));
  border-radius: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FF6B4A;
  margin-bottom: var(--space-4);
}

.empty-state h3 {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2) 0;
}

.empty-state p {
  font-size: var(--text-base);
  line-height: 1.8;
  margin: 0;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

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
  }

  .progress-wrapper {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .plan-generator-page {
    padding: var(--space-4);
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
}
</style>
