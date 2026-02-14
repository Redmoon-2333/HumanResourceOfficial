<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
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
      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
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
      <!-- Hero区域 -->
      <div class="hero-section">
        <div class="hero-background">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="floating-shapes">
            <div class="shape shape-1">
              <el-icon :size="40" color="rgba(245, 158, 11, 0.3)"><Trophy /></el-icon>
            </div>
            <div class="shape shape-2">
              <el-icon :size="32" color="rgba(255, 107, 74, 0.3)"><Star /></el-icon>
            </div>
            <div class="shape shape-3">
              <el-icon :size="36" color="rgba(16, 185, 129, 0.3)"><Calendar /></el-icon>
            </div>
          </div>
        </div>

        <div class="hero-content">
          <div class="hero-badge">
            <el-icon :size="16"><Trophy /></el-icon>
            <span>往届活动</span>
          </div>
          <!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
          <h1 class="hero-title" style="color: #1C1917;">
            精彩回顾
            <span style="color: #000000;">传承创新</span>
          </h1>
          <!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
          <p class="hero-subtitle" style="color: #78716C;">
            回顾人力资源中心历届精彩活动，见证成长与荣耀
          </p>

          <!-- 管理员操作按钮 -->
          <div v-if="userStore.isMinister" class="admin-actions">
            <el-button type="primary" size="large" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              添加往届活动
            </el-button>
          </div>

          <!-- 统计卡片 -->
          <div class="stats-row">
            <div class="stat-item">
              <div class="stat-icon" style="background: linear-gradient(135deg, #FF6B4A, #E35532);">
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
              <div class="stat-icon" style="background: linear-gradient(135deg, #F59E0B, #D97706);">
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
        </div>
      </div>

      <!-- 筛选器 -->
      <div class="filter-section">
        <div class="filter-container">
          <div class="filter-group">
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
  background: linear-gradient(135deg, #FEF9F6 0%, #FFF5F0 100%);
}

/* Hero区域 */
.hero-section {
  position: relative;
  padding: 80px 40px 60px;
  overflow: hidden;
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
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  top: -100px;
  right: 10%;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #FB7185, #FF6B4A);
  bottom: -50px;
  left: 5%;
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
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
  padding: 8px 20px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 50px;
  color: #F59E0B;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  color: #1F2937;
  margin-bottom: 16px;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, #F59E0B, #FF6B4A);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 18px;
  color: #6B7280;
  max-width: 500px;
  margin: 0 auto 40px;
  line-height: 1.6;
}

/* 统计 */
.stats-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
}

/* 筛选器 */
.filter-section {
  padding: 0 40px 40px;
}

.filter-container {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.filter-label {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.filter-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #E5E7EB;
  background: white;
  border-radius: 8px;
  font-size: 14px;
  color: #6B7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-btn:hover {
  border-color: #FF6B4A;
  color: #FF6B4A;
}

.filter-btn.active {
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  border-color: transparent;
}

/* 时间轴 */
.timeline-section {
  padding: 0 40px 60px;
}

.timeline-container {
  max-width: 1200px;
  margin: 0 auto;
}

.year-group {
  margin-bottom: 60px;
}

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
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  border-radius: 16px;
  color: white;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.3);
}

.year-text {
  font-size: 32px;
  font-weight: 800;
}

.year-count {
  font-size: 13px;
  opacity: 0.9;
}

.year-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(90deg, #FF6B4A, transparent);
}

/* 活动网格 */
.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
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

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 40px;
  color: #9CA3AF;
}

.empty-state p {
  margin-top: 16px;
  font-size: 16px;
}

/* ============================================
   管理员操作按钮 - 毛玻璃悬浮风格
   ============================================ */
.admin-actions {
  margin-bottom: 32px;
  display: flex;
  justify-content: center;
}

.admin-actions .el-button {
  background: linear-gradient(135deg, rgba(255, 107, 74, 0.9), rgba(245, 158, 11, 0.9));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50px;
  padding: 14px 32px;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.25);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.admin-actions .el-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(255, 107, 74, 0.35);
  background: linear-gradient(135deg, rgba(255, 107, 74, 1), rgba(245, 158, 11, 1));
}

.admin-actions .el-button:active {
  transform: translateY(-1px);
}

/* ============================================
   卡片操作按钮 - 毛玻璃图标按钮
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
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.25s ease;
}

.card-actions .el-button:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.card-actions .el-button--primary {
  color: #FFFFFF;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  border: none;
}

.card-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #2563EB, #1D4ED8);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.card-actions .el-button--danger {
  color: #FFFFFF;
  background: linear-gradient(135deg, #EF4444, #DC2626);
  border: none;
}

.card-actions .el-button--danger:hover {
  background: linear-gradient(135deg, #DC2626, #B91C1C);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

/* 表单样式 */
.activity-form {
  padding: 20px 0;
}

.cover-uploader {
  border: 2px dashed var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 300px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-uploader:hover {
  border-color: var(--el-color-primary);
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
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
  color: var(--el-text-color-secondary);
}

.upload-loading {
  margin-top: 8px;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    padding: 60px 20px 40px;
  }

  .hero-title {
    font-size: 32px;
  }

  .filter-section,
  .timeline-section {
    padding-left: 20px;
    padding-right: 20px;
  }

  .filter-group {
    flex-wrap: wrap;
  }

  .activities-grid {
    grid-template-columns: 1fr;
  }

  .cover-uploader {
    width: 100%;
    height: 160px;
  }
}
</style>
