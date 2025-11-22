<script setup lang="ts">
import { ref } from 'vue'
import Layout from '@/components/Layout.vue'
import { generatePlan, generatePlanStream } from '@/api/ai'
import type { PlanGeneratorRequest } from '@/types'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const useStream = ref(true)
const planForm = ref<PlanGeneratorRequest>({
  theme: '',
  organizer: '',
  eventTime: '',
  eventLocation: '',
  staff: '',
  participants: '',
  purpose: '',
  leaderCount: 0,
  memberCount: 0
})

const generatedPlan = ref('')
const showPreview = ref(false)
const streamProgress = ref({
  show: false,
  chunks: 0,
  length: 0
})

// 生成策划案
const handleGenerate = async () => {
  if (!planForm.value.theme) {
    ElMessage.warning('请输入活动主题')
    return
  }

  loading.value = true
  generatedPlan.value = ''
  showPreview.value = false

  try {
    if (useStream.value) {
      // 流式输出
      streamProgress.value = { show: true, chunks: 0, length: 0 }
      
      try {
        const content = await generatePlanStream(
          planForm.value,
          (chunk) => {
            generatedPlan.value += chunk
            streamProgress.value.chunks++
            streamProgress.value.length = generatedPlan.value.length
          }
        )
        
        streamProgress.value.show = false
        
        // 检查是否接收到了内容
        if (generatedPlan.value.length > 0) {
          ElMessage.success('策划案生成完成')
        } else {
          ElMessage.warning('未接收到完整内容，请重试')
        }
      } catch (streamError: any) {
        console.error('流式生成失败:', streamError)
        
        // 如果流式失败但已经有部分内容，仍然显示
        if (generatedPlan.value.length > 100) {
          ElMessage.warning('连接中断，已保留部分生成内容')
          streamProgress.value.show = false
        } else {
          throw streamError
        }
      }
    } else {
      // 非流式输出
      const res = await generatePlan(planForm.value)
      if (res.code === 200 && res.data) {
        generatedPlan.value = res.data
        ElMessage.success('策划案生成完成')
      } else {
        ElMessage.error(res.message || '生成失败')
      }
    }

    // 清理markdown标记
    if (generatedPlan.value) {
      let cleanedHtml = generatedPlan.value
        .replace(/^```html\n/, '')
        .replace(/\n```$/, '')
        .trim()
      
      if (cleanedHtml.startsWith('```')) {
        cleanedHtml = cleanedHtml.replace(/^```[a-z]*\n?/, '').replace(/\n?```$/, '').trim()
      }
      
      generatedPlan.value = cleanedHtml
      
      // 只有内容足够长才显示预览
      if (generatedPlan.value.length > 50) {
        showPreview.value = true
      }
    }
  } catch (error: any) {
    console.error('生成策划案失败:', error)
    
    // 如果已经有部分内容，保留它
    if (generatedPlan.value.length > 100) {
      ElMessage.warning('生成被中断，已保留部分内容')
      showPreview.value = true
    } else {
      generatedPlan.value = ''
      showPreview.value = false
      
      if (error.name === 'AbortError') {
        ElMessage.warning('请求超时，请重试')
      } else if (error.message && error.message.includes('Failed to fetch')) {
        ElMessage.error('网络连接失败，请检查网络')
      } else {
        ElMessage.error(error.message || '生成失败，请重试')
      }
    }
  } finally {
    loading.value = false
    streamProgress.value.show = false
  }
}

// 下载策划案
const handleDownload = () => {
  if (!generatedPlan.value) {
    ElMessage.warning('没有可下载的策划案')
    return
  }

  const blob = new Blob([generatedPlan.value], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${planForm.value.theme || '活动'}策划案.html`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('下载成功')
}

// 重置表单
const handleReset = () => {
  planForm.value = {
    theme: '',
    organizer: '',
    eventTime: '',
    eventLocation: '',
    staff: '',
    participants: '',
    purpose: '',
    leaderCount: 0,
    memberCount: 0
  }
  generatedPlan.value = ''
  showPreview.value = false
}
</script>

<template>
  <Layout>
    <div class="plan-generator-container">
      <el-row :gutter="20">
        <!-- 左侧表单 -->
        <el-col :xs="24" :lg="10">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>策划案信息</span>
                <el-switch
                  v-model="useStream"
                  active-text="流式输出"
                  inactive-text="普通模式"
                />
              </div>
            </template>

            <el-form :model="planForm" label-width="100px" label-position="top">
              <!-- 提示信息 -->
              <el-alert
                v-if="!useStream"
                title="建议使用流式输出"
                type="info"
                :closable="false"
                style="margin-bottom: 16px"
              >
                <template #default>
                  策划案生成通常需要1-2分钟，普通模式可能超时。建议切换至“流式输出”以获得更好的体验。
                </template>
              </el-alert>
              
              <el-form-item label="活动主题" required>
                <el-input
                  v-model="planForm.theme"
                  placeholder="例如：企业参观活动"
                />
              </el-form-item>

              <el-form-item label="组织者">
                <el-input
                  v-model="planForm.organizer"
                  placeholder="例如：人力资源部"
                />
              </el-form-item>

              <el-form-item label="活动时间">
                <el-input
                  v-model="planForm.eventTime"
                  placeholder="例如：2024年1月15日"
                />
              </el-form-item>

              <el-form-item label="活动地点">
                <el-input
                  v-model="planForm.eventLocation"
                  placeholder="例如：XX企业总部"
                />
              </el-form-item>

              <el-form-item label="工作人员">
                <el-input
                  v-model="planForm.staff"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：总负责人、活动协调员等"
                />
              </el-form-item>

              <el-form-item label="参与人员">
                <el-input
                  v-model="planForm.participants"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：全体部门成员"
                />
              </el-form-item>

              <el-form-item label="活动目的">
                <el-input
                  v-model="planForm.purpose"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：增进交流、开拓视野"
                />
              </el-form-item>

              <el-row :gutter="10">
                <el-col :span="12">
                  <el-form-item label="部长人数">
                    <el-input-number
                      v-model="planForm.leaderCount"
                      :min="0"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="部员人数">
                    <el-input-number
                      v-model="planForm.memberCount"
                      :min="0"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleGenerate"
                  :loading="loading"
                  style="width: 100%"
                  size="large"
                >
                  <el-icon><MagicStick /></el-icon>
                  {{ loading ? '生成中...' : '生成策划案' }}
                </el-button>
              </el-form-item>

              <el-form-item>
                <el-button @click="handleReset" style="width: 100%">
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧预览 -->
        <el-col :xs="24" :lg="14">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>策划案预览</span>
                <el-button
                  size="small"
                  @click="handleDownload"
                  :disabled="!showPreview"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </div>
            </template>

            <!-- 流式进度 -->
            <div v-if="streamProgress.show" class="stream-progress">
              <el-icon class="is-loading" :size="24" color="var(--color-primary)">
                <Loading />
              </el-icon>
              <p>正在流式生成中...</p>
              <p class="progress-info">
                已接收 {{ streamProgress.chunks }} 个数据块，
                当前长度: {{ streamProgress.length }} 字符
              </p>
            </div>

            <!-- 预览区域 -->
            <div v-if="showPreview" class="preview-container">
              <div v-html="generatedPlan" class="plan-preview"></div>
            </div>

            <!-- 空状态 -->
            <el-empty
              v-if="!loading && !showPreview && !streamProgress.show"
              description="请填写左侧表单并生成策划案"
            >
              <el-icon :size="80" color="var(--color-border)">
                <DocumentAdd />
              </el-icon>
            </el-empty>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </Layout>
</template>

<style scoped>
.plan-generator-container {
  padding: var(--spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stream-progress {
  text-align: center;
  padding: var(--spacing-2xl);
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-md);
}

.stream-progress p {
  margin-top: var(--spacing-md);
  color: var(--color-text-secondary);
}

.progress-info {
  font-size: 12px;
  color: var(--color-text-light);
}

.preview-container {
  background-color: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.plan-preview {
  line-height: 1.8;
}

.plan-preview :deep(h1) {
  color: var(--color-primary);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 2px solid var(--color-primary);
}

.plan-preview :deep(h2) {
  color: var(--color-secondary);
  margin-top: var(--spacing-xl);
  margin-bottom: var(--spacing-md);
}

.plan-preview :deep(h3) {
  color: var(--color-text-primary);
  margin-top: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
}

.plan-preview :deep(p) {
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-secondary);
}

.plan-preview :deep(ul),
.plan-preview :deep(ol) {
  margin-left: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.plan-preview :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: var(--spacing-md) 0;
}

.plan-preview :deep(table th),
.plan-preview :deep(table td) {
  border: 1px solid var(--color-border);
  padding: var(--spacing-sm);
  text-align: left;
}

.plan-preview :deep(table th) {
  background-color: var(--color-bg-secondary);
  font-weight: 600;
}
</style>
