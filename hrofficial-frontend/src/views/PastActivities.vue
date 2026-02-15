<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import OrganicBlob from '@/components/OrganicBlob.vue'
import SparkleEffect from '@/components/SparkleEffect.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted } from 'vue'
import {
  getPastActivities,
  createPastActivity,
  updatePastActivity,
  deletePastActivity
} from '@/api/activities'
import { uploadImageByType } from '@/api/dailyImage'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { PastActivityRequest } from '@/types'
import {
  Calendar,
  ArrowRight,
  Filter,
  Picture,
  Star,
  Trophy,
  Plus,
  Edit,
  Delete,
  Loading
} from '@element-plus/icons-vue'

const userStore = useUserStore()

// 与后端 PastActivityResponse 匹配的接口
interface PastActivity {
  pastActivityId: number
  title: string
  coverImage: string
  pushUrl: string
  year: number
  createTime: string
}

// 前端展示用的活动接口
interface Activity {
  id: number
  title: string
  coverImage: string
  pushUrl: string
  year: number
  createTime: string
}

const activities = ref<Activity[]>([])
const loading = ref(false)
const selectedYear = ref<number | 'all'>('all')

// 管理对话框状态
const dialogVisible = ref(false)
const dialogTitle = ref('添加往届活动')
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<PastActivityRequest>({
  title: '',
  coverImage: '',
  pushUrl: '',
  year: new Date().getFullYear()
})
const formRef = ref()
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

// 表单验证规则
const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  coverImage: [{ required: true, message: '请上传封面图片', trigger: 'blur' }],
  pushUrl: [
    { required: true, message: '请输入推送链接', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (!value || value.match(/^(https?:\/\/)/)) {
          callback()
        } else {
          callback(new Error('请输入正确的URL格式（以http://或https://开头）'))
        }
      },
      trigger: 'blur'
    }
  ],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }]
}

// 获取活动列表
const fetchActivities = async () => {
  loading.value = true
  try {
    const response = await getPastActivities(1, 100) // 获取前100条
    if (response.code === 200) {
      // 后端返回分页数据，需要取 content 或 list
      const data = response.data?.content || response.data?.list || []
      // 映射字段名
      activities.value = data.map((item: PastActivity) => ({
        id: item.pastActivityId,
        title: item.title,
        coverImage: item.coverImage,
        pushUrl: item.pushUrl,
        year: item.year,
        createTime: item.createTime
      }))
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取活动列表失败')
  } finally {
    loading.value = false
  }
}

// 按年份分组
const groupedActivities = computed(() => {
  const groups: Record<number, Activity[]> = {}
  
  filteredActivities.value.forEach(activity => {
    const year = activity.year
    if (!groups[year]) {
      groups[year] = []
    }
    groups[year]!.push(activity)
  })
  
  // 按年份降序排列
  return Object.entries(groups)
    .sort(([a], [b]) => Number(b) - Number(a))
    .map(([year, items]) => ({
      year: Number(year),
      activities: items
    }))
})

// 过滤后的活动
const filteredActivities = computed(() => {
  if (selectedYear.value === 'all') {
    return activities.value
  }
  return activities.value.filter(a => a.year === selectedYear.value)
})

// 提取所有年份
const availableYears = computed(() => {
  const years = [...new Set(activities.value.map(a => a.year))]
  return years.sort((a, b) => b - a)
})

// 统计数据
const totalActivities = computed(() => activities.value.length)

// 打开链接
const openUrl = (url: string) => {
  window.open(url, '_blank')
}

// 打开添加对话框
const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '添加往届活动'
  form.value = {
    title: '',
    coverImage: '',
    pushUrl: '',
    year: new Date().getFullYear()
  }
  dialogVisible.value = true
}

// 打开编辑对话框
const handleEdit = (activity: Activity, event: Event) => {
  event.stopPropagation()
  isEdit.value = true
  editId.value = activity.id
  dialogTitle.value = '编辑往届活动'
  form.value = {
    title: activity.title,
    coverImage: activity.coverImage,
    pushUrl: activity.pushUrl,
    year: activity.year
  }
  dialogVisible.value = true
}

// 删除活动
const handleDelete = async (activity: Activity, event: Event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定要删除活动 "${activity.title}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deletePastActivity(activity.id)
    ElMessage.success('删除成功')
    fetchActivities()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择变化
const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    // 使用 activity 类型上传到 images/activities/ 目录
    const res = await uploadImageByType(file, 'activity', (percent) => {
      console.log('上传进度:', percent)
    })
    if (res.code === 200 && res.data) {
      // 拼接完整URL
      // Why: 生产环境使用相对路径，开发环境使用环境变量或默认值
      const baseUrl = import.meta.env.PROD
        ? '' // 生产环境使用相对路径，Nginx会代理到后端
        : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
      form.value.coverImage = res.data.startsWith('http')
        ? res.data
        : `${baseUrl}${res.data.startsWith('/') ? '' : '/'}${res.data}`
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.message || '图片上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploading.value = false
    // 清空input值，允许重复选择同一文件
    input.value = ''
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    try {
      if (isEdit.value && editId.value) {
        await updatePastActivity(editId.value, form.value)
        ElMessage.success('更新成功')
      } else {
        await createPastActivity(form.value)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      fetchActivities()
    } catch (error: any) {
      ElMessage.error(error.message || (isEdit.value ? '更新失败' : '添加失败'))
    }
  })
}

onMounted(() => {
  fetchActivities()
})
</script>

<template>
  <Layout>
    <div class="past-activities-page">
      <!-- Hero区域 - 往届活动页 (琥珀金 #F59E0B) -->
      <section class="hero-section hero-organic page-past">
        <!-- 有机Blob装饰层 - 左倾布局设计，每个Blob有独特的seed -->
        <div class="hero-organic__blobs">
          <!-- Blob 1: 左上大型，主色琥珀金，seed=11 -->
          <OrganicBlob
            size="large"
            color="#F59E0B"
            color-light="#FBBF24"
            :position="{ top: '-100px', left: '-60px' }"
            :delay="0"
            :opacity="0.55"
            :seed="11"
            glow
          />
          <!-- Blob 2: 左下中型，次色活力橙，seed=22 -->
          <OrganicBlob
            size="medium"
            color="#F97316"
            color-light="#FB923C"
            :position="{ bottom: '-60px', left: '80px' }"
            :delay="-2"
            :opacity="0.45"
            :seed="22"
            float
          />
          <!-- Blob 3: 右侧小型，暖金黄，seed=33 -->
          <OrganicBlob
            size="small"
            color="#FBBF24"
            color-light="#FCD34D"
            :position="{ top: '30%', right: '-30px' }"
            :delay="-4"
            :opacity="0.4"
            :seed="33"
          />
          <!-- 网格纹理 -->
          <div class="grid-pattern"></div>
        </div>

        <!-- Sparkle粒子层 -->
        <SparkleEffect :count="12" color="rgba(255, 255, 255, 0.95)" />

        <!-- 浮动装饰 -->
        <div class="floating-shapes">
          <div class="shape shape-1">
            <el-icon :size="40" color="rgba(245, 158, 11, 0.4)"><Trophy /></el-icon>
          </div>
          <div class="shape shape-2">
            <el-icon :size="32" color="rgba(251, 191, 36, 0.4)"><Star /></el-icon>
          </div>
          <div class="shape shape-3">
            <el-icon :size="36" color="rgba(249, 115, 22, 0.4)"><Calendar /></el-icon>
          </div>
        </div>

        <!-- 内容层 -->
        <div class="hero-organic__content hero-organic__content--left">
          <div class="hero-badge">
            <el-icon :size="16"><Trophy /></el-icon>
            <span>往届活动</span>
          </div>
          <h1 class="hero-title">
            精彩回顾
            <span>传承创新</span>
          </h1>
          <p class="hero-subtitle">
            回顾人力资源中心历届精彩活动，见证成长与荣耀
          </p>
        </div>
      </section>

      <!-- 统计和筛选混合区域 -->
      <div class="stats-filter-section">
        <div class="stats-filter-container">
          <div class="stats-filter-row">
            <!-- 左侧统计卡片 -->
            <div class="stats-group">
              <div class="stat-item">
                <div class="stat-icon">
                  <el-icon :size="20" color="white"><Trophy /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">
                    <AnimatedCounter :value="totalActivities" :duration="1500" />
                  </span>
                  <span class="stat-label">活动总数</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon stat-icon--secondary">
                  <el-icon :size="20" color="white"><Calendar /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">
                    <AnimatedCounter :value="availableYears.length" :duration="1500" />
                  </span>
                  <span class="stat-label">年份跨度</span>
                </div>
              </div>
            </div>
            
            <!-- 右侧筛选和操作 -->
            <div class="filter-group">
              <div v-if="userStore.isMinister" class="admin-actions">
                <el-button type="primary" size="large" @click="handleCreate">
                  <el-icon><Plus /></el-icon>
                  添加往届活动
                </el-button>
              </div>
              <el-icon :size="18"><Filter /></el-icon>
              <span class="filter-label">年份筛选:</span>
              <div class="filter-options">
                <button
                  class="filter-btn"
                  :class="{ active: selectedYear === 'all' }"
                  @click="selectedYear = 'all'"
                >
                  全部
                </button>
                <button
                  v-for="year in availableYears"
                  :key="year"
                  class="filter-btn"
                  :class="{ active: selectedYear === year }"
                  @click="selectedYear = year"
                >
                  {{ year }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 时间轴内容 -->
      <div class="timeline-section">
        <div v-loading="loading" class="timeline-container">
          <div
            v-for="group in groupedActivities"
            :key="group.year"
            class="year-group"
          >
            <!-- 年份标记 -->
            <div class="year-marker">
              <div class="year-badge">
                <span class="year-text">{{ group.year }}</span>
                <span class="year-count">{{ group.activities.length }} 个活动</span>
              </div>
              <div class="year-line"></div>
            </div>

            <!-- 活动卡片网格 -->
            <div class="activities-grid">
              <div
                v-for="activity in group.activities"
                :key="activity.id"
                class="activity-card"
                @click="activity.pushUrl && openUrl(activity.pushUrl)"
              >
                <div class="activity-image">
                  <img
                    v-if="activity.coverImage"
                    :src="activity.coverImage"
                    :alt="activity.title"
                    @error="($event.target as any).style.display='none'"
                  />
                  <div v-else class="image-placeholder">
                    <el-icon :size="48" color="#D1D5DB"><Picture /></el-icon>
                  </div>
                  <div class="activity-overlay">
                    <div class="view-details">
                      <span>查看详情</span>
                      <el-icon :size="16"><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>
                <div class="activity-content">
                  <h3 class="activity-title">{{ activity.title }}</h3>
                  <div class="activity-meta">
                    <span class="meta-item">
                      <el-icon :size="14"><Calendar /></el-icon>
                      {{ new Date(activity.createTime).toLocaleDateString() }}
                    </span>
                    <!-- 管理员操作按钮 -->
                    <div v-if="userStore.isMinister" class="card-actions">
                      <el-button
                        type="primary"
                        link
                        size="small"
                        @click="handleEdit(activity, $event)"
                      >
                        <el-icon><Edit /></el-icon>
                      </el-button>
                      <el-button
                        type="danger"
                        link
                        size="small"
                        @click="handleDelete(activity, $event)"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="groupedActivities.length === 0 && !loading" class="empty-state">
            <el-icon :size="64" color="#D1D5DB"><Trophy /></el-icon>
            <p>暂无活动记录</p>
            <el-button v-if="userStore.isMinister" type="primary" @click="handleCreate">
              添加首个活动
            </el-button>
          </div>
        </div>
      </div>

      <!-- 添加/编辑对话框 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
        destroy-on-close
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="activity-form"
        >
          <el-form-item label="活动标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入活动标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="封面图片" prop="coverImage">
            <div
              class="cover-uploader"
              @click="triggerFileInput"
            >
              <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
              <div v-else class="upload-placeholder">
                <el-icon :size="32"><Plus /></el-icon>
                <span>点击上传封面</span>
              </div>
              <div v-if="form.coverImage" class="cover-overlay">
                <el-icon :size="24"><Plus /></el-icon>
                <span>点击更换封面</span>
              </div>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleFileChange"
            />
            <div v-if="uploading" class="upload-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              上传中...
            </div>
          </el-form-item>

          <el-form-item label="推送链接" prop="pushUrl">
            <el-input
              v-model="form.pushUrl"
              placeholder="请输入推送链接（如：https://mp.weixin.qq.com/...）"
            />
          </el-form-item>

          <el-form-item label="年份" prop="year">
            <el-input-number
              v-model="form.year"
              :min="2000"
              :max="2100"
              :step="1"
              placeholder="请输入年份"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.past-activities-page {
  min-height: 100vh;
  max-width: 1400px;
  margin: 0 auto;
}

/* Hero区域 - 往届活动页 (琥珀金 #F59E0B) */
.hero-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  text-align: left;
  padding: var(--space-8) var(--space-8) var(--space-12);
  margin: var(--space-6);
  margin-bottom: 0;
  background:
    radial-gradient(ellipse 80% 60% at 90% 10%, rgba(245, 158, 11, 0.12) 0%, transparent 40%),
    radial-gradient(ellipse 60% 50% at 10% 90%, rgba(249, 115, 22, 0.1) 0%, transparent 35%),
    linear-gradient(135deg, #FFF8F0 0%, #FFF5E0 50%, #FFF0D5 100%);
  border-radius: 32px;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 50% 35% at 75% 15%, rgba(245, 158, 11, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse 40% 25% at 25% 85%, rgba(249, 115, 22, 0.15) 0%, transparent 45%);
  pointer-events: none;
  z-index: 0;
}

.hero-organic__blobs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}

.hero-organic__blobs .grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(245, 158, 11, 0.1) 1px, transparent 0);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
}

.floating-shapes {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.shape {
  position: absolute;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  top: 20%;
  right: 15%;
  animation-delay: 0s;
}

.shape-2 {
  top: 50%;
  right: 8%;
  animation-delay: 2s;
}

.shape-3 {
  bottom: 20%;
  right: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

.hero-organic__content {
  position: relative;
  z-index: 2;
  padding: 32px 32px 48px;
}

.hero-organic__content--left {
  text-align: left;
  padding-left: 48px;
  padding-top: 24px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  color: white;
  border-radius: var(--radius-full, 100px);
  font-size: var(--text-sm);
  font-weight: var(--font-medium, 500);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.35), 0 0 20px rgba(245, 158, 11, 0.15);
  animation: fadeInDown 0.6s ease;
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

@keyframes badgeShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

.hero-title {
  font-size: clamp(1.875rem, 4vw, 2.5rem);
  font-weight: 800;
  color: #1F2937;
  line-height: 1.2;
  margin: 0 0 var(--space-4, 16px) 0;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.hero-title span {
  color: #1F2937;
}

.hero-subtitle {
  font-size: var(--text-lg);
  color: var(--text-secondary, #6B7280);
  line-height: 1.7;
  max-width: 600px;
  margin: 0;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 统计和筛选混合区域 */
.stats-filter-section {
  padding: 0 var(--space-6);
  margin: var(--space-6) 0 0 0;
}

.stats-filter-container {
  max-width: 100%;
  margin: 0;
}

.stats-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.1);
  padding: 20px 32px;
  position: relative;
  overflow: hidden;
}

.stats-filter-row > * {
  flex: 1;
}

.stats-filter-row .stats-group {
  flex: 1;
  justify-content: flex-start;
}

.stats-filter-row .filter-group {
  flex: 1.5;
  justify-content: flex-end;
}

.stats-filter-row::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #F59E0B, #FBBF24, #FCD34D);
}

.stats-group {
  display: flex;
  gap: 20px;
  flex: 1;
  min-width: 0;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  position: relative;
  overflow: visible;
}

.filter-group::before {
  display: none;
}

.admin-actions {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

/* 统计卡片区域 - 增强版 */
.stats-section {
  padding: 0 var(--space-8);
  margin: var(--space-6);
  margin-bottom: 0;
}

/* 统计 - 增强版 */
.stats-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  border: 1px solid rgba(245, 158, 11, 0.15);
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.08);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.stat-item:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 8px 28px rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.25);
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
}

.stat-icon--secondary {
  background: linear-gradient(135deg, #F97316, #FB923C);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.25);
}

.stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
  font-weight: 500;
}

/* 筛选器 - 紧凑布局 */
.filter-section {
  padding: 16px 24px;
}

.filter-container {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.1);
  position: relative;
  overflow: hidden;
}

.filter-group::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #F59E0B, #FBBF24, #FCD34D);
}

.filter-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.filter-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 6px 12px;
  border: 1px solid #E5E7EB;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  color: #6B7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-btn:hover {
  border-color: #F59E0B;
  color: #F59E0B;
  background: rgba(245, 158, 11, 0.03);
}

.filter-btn.active {
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
}

/* 时间轴 - 紧凑布局 */
.timeline-section {
  padding: 16px var(--space-6) 40px;
}

.timeline-container {
  max-width: 100%;
  margin: 0 auto;
}

.year-group {
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
}

.year-group:nth-child(1) { animation-delay: 0ms; }
.year-group:nth-child(2) { animation-delay: 100ms; }
.year-group:nth-child(3) { animation-delay: 200ms; }
.year-group:nth-child(4) { animation-delay: 300ms; }

.year-marker {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
}

.year-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 28px;
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  border-radius: 16px;
  color: white;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.3);
  position: relative;
  overflow: hidden;
}

.year-badge::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: badgeShine 3s ease-in-out infinite;
}

@keyframes badgeShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

.year-text {
  font-size: 32px;
  font-weight: 800;
  position: relative;
  z-index: 1;
}

.year-count {
  font-size: 13px;
  opacity: 0.9;
  position: relative;
  z-index: 1;
}

.year-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(90deg, #F59E0B, #FBBF24, transparent);
}

/* 活动网格 - 增强版 */
.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.08);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  position: relative;
}

.activity-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #F59E0B, #FBBF24, #FCD34D);
  background-size: 200% 100%;
  animation: gradientFlow 3s linear infinite;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.15);
}

.activity-card:hover::before {
  opacity: 1;
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.activity-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.activity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.activity-card:hover .activity-image img {
  transform: scale(1.1);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F3F4F6;
}

.activity-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 20px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.activity-card:hover .activity-overlay {
  opacity: 1;
}

.view-details {
  display: flex;
  align-items: center;
  gap: 6px;
  color: white;
  font-size: 14px;
  font-weight: 500;
}

.activity-content {
  padding: 20px;
}

.activity-title {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 12px;
  line-height: 1.4;
}

.activity-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #9CA3AF;
}

/* 空状态 - 增强版 */
.empty-state {
  text-align: center;
  padding: 80px 40px;
  color: #9CA3AF;
}

.empty-state p {
  margin-top: 16px;
  font-size: 16px;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

/* ============================================
   管理员操作按钮 - 工具栏风格（增强版）
   ============================================ */
.admin-actions {
  display: flex;
  align-items: center;
}

.admin-actions .el-button {
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.admin-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35);
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
}

.admin-actions .el-button:active {
  transform: translateY(0);
}

/* ============================================
   卡片操作按钮 - 毛玻璃图标按钮（增强版）
   ============================================ */
.card-actions {
  display: flex;
  gap: 6px;
  margin-left: auto;
}

.card-actions .el-button {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(245, 158, 11, 0.1);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.08);
  transition: all 0.25s ease;
}

.card-actions .el-button:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.15);
}

.card-actions .el-button--primary {
  color: #FFFFFF;
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
  border: none;
}

.card-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #F97316, #FB923C);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.card-actions .el-button--danger {
  color: #FFFFFF;
  background: linear-gradient(135deg, #F97316, #FB923C);
  border: none;
}

.card-actions .el-button--danger:hover {
  background: linear-gradient(135deg, #E85A3C, #FF6B4A);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
}

/* 表单样式 - 增强版 */
.activity-form {
  padding: 20px 0;
}

.cover-uploader {
  border: 2px dashed rgba(245, 158, 11, 0.3);
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  width: 300px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(245, 158, 11, 0.02);
}

.cover-uploader:hover {
  border-color: #F59E0B;
  background: rgba(245, 158, 11, 0.04);
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(245, 158, 11, 0.7);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: white;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cover-uploader:hover .cover-overlay {
  opacity: 1;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9CA3AF;
}

.upload-loading {
  margin-top: 8px;
  color: #F59E0B;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    padding: var(--space-10) var(--space-6);
    margin: var(--space-4);
    margin-bottom: 0;
    border-radius: 24px;
  }

  .hero-title {
    font-size: 32px;
  }

  .stats-filter-section {
    padding: 0 var(--space-4);
    margin: var(--space-4) 0 0 0;
  }

  .stats-filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px 20px;
  }

  .stats-group {
    justify-content: center;
  }

  .filter-group {
    flex-wrap: wrap;
    justify-content: center;
  }

  .filter-section,
  .timeline-section {
    padding-left: var(--space-4);
    padding-right: var(--space-4);
  }

  .activities-grid {
    grid-template-columns: 1fr;
  }

  .cover-uploader {
    width: 100%;
    height: 160px;
  }
  
  /* 弹窗移动端适配 */
  :deep(.el-dialog) {
    width: 95% !important;
    max-width: 95% !important;
    margin: 5vh auto !important;
    border-radius: 20px !important;
  }
  
  :deep(.el-dialog__header) {
    padding: var(--space-4) !important;
  }
  
  :deep(.el-dialog__body) {
    padding: var(--space-4) !important;
    max-height: 60vh;
    overflow-y: auto;
  }
  
  :deep(.el-dialog__footer) {
    padding: var(--space-4) !important;
  }
  
  .activity-form :deep(.el-form-item) {
    display: block !important;
    margin-bottom: var(--space-4) !important;
  }
  
  .activity-form :deep(.el-form-item__label) {
    display: block !important;
    float: none !important;
    text-align: left !important;
    width: 100% !important;
    padding-bottom: var(--space-2) !important;
    line-height: 1.5 !important;
  }
  
  .activity-form :deep(.el-form-item__content) {
    display: block !important;
    margin-left: 0 !important;
    width: 100% !important;
  }
  
  .activity-form :deep(.el-input),
  .activity-form :deep(.el-textarea) {
    width: 100% !important;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: var(--space-8) var(--space-4);
    margin: var(--space-3);
    margin-bottom: 0;
    border-radius: 20px;
  }
  
  .hero-title {
    font-size: 28px;
  }
  
  .hero-subtitle {
    font-size: 14px;
  }
  
  .stats-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .stat-item {
    width: 100%;
    justify-content: center;
  }
  
  .filter-section,
  .timeline-section {
    padding-left: var(--space-3);
    padding-right: var(--space-3);
  }

  .stats-filter-section {
    padding: 0 var(--space-3);
    margin: var(--space-3) 0 0 0;
  }
  
  .filter-group {
    padding: var(--space-3);
  }
  
  .filter-label {
    display: none;
  }
  
  .admin-actions {
    width: 100%;
    margin-bottom: var(--space-2);
  }
  
  .admin-actions .el-button {
    width: 100%;
  }
  
  .year-marker {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .year-line {
    display: none;
  }
  
  .year-badge {
    flex-direction: row;
    gap: 8px;
    padding: 12px 20px;
  }
  
  .year-text {
    font-size: 24px;
  }
  
  .activity-card {
    border-radius: 16px;
  }
  
  .activity-image {
    height: 160px;
  }
  
  .activity-content {
    padding: var(--space-3);
  }
  
  .activity-title {
    font-size: 16px;
  }
  
  /* 小屏幕弹窗优化 - 底部弹出式 */
  :deep(.el-dialog) {
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    border-radius: 20px 20px 0 0 !important;
    position: fixed !important;
    bottom: 0 !important;
    top: auto !important;
    left: 0 !important;
    right: 0 !important;
  }
  
  :deep(.el-dialog__header) {
    border-radius: 20px 20px 0 0 !important;
    padding: var(--space-3) !important;
  }
  
  :deep(.el-dialog__body) {
    max-height: 55vh;
    padding: var(--space-3) !important;
  }
  
  :deep(.el-dialog__footer) {
    padding: var(--space-3) !important;
    display: flex;
    gap: var(--space-2);
  }
  
  :deep(.el-dialog__footer .el-button) {
    flex: 1;
  }
  
  .cover-uploader {
    height: 140px;
  }
  
  :deep(.el-input-number) {
    width: 100%;
  }
  
  /* 小屏幕表单强制垂直布局 */
  .activity-form :deep(.el-form-item) {
    display: flex !important;
    flex-direction: column !important;
    margin-bottom: 16px !important;
  }
  
  .activity-form :deep(.el-form-item__label) {
    display: block !important;
    float: none !important;
    text-align: left !important;
    width: 100% !important;
    padding: 0 0 8px 0 !important;
    line-height: 1.5 !important;
    box-sizing: border-box !important;
  }
  
  .activity-form :deep(.el-form-item__content) {
    display: block !important;
    margin-left: 0 !important;
    width: 100% !important;
    flex: none !important;
  }
  
  .activity-form :deep(.el-input__wrapper),
  .activity-form :deep(.el-textarea__inner) {
    width: 100% !important;
  }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
