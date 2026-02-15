<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getActivity, getActivityImages, uploadActivityImage, deleteActivityImage } from '@/api/activity'
import type { Activity, ActivityImage, ActivityResponse, ActivityImageDTO } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getFullImageUrl, getFullImageUrlList } from '@/utils/image'
import { 
  ArrowLeft, 
  Upload, 
  Location, 
  User, 
  Calendar, 
  Clock, 
  Delete,
  Picture,
  InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activity = ref<ActivityResponse | null>(null)
const images = ref<ActivityImageDTO[]>([])
const uploadDialogVisible = ref(false)
const imageDescription = ref('')
const uploadFile = ref<File | null>(null)
const uploadProgress = ref(0)

// 获取状态标签样式 - 渐变胶囊
const getStatusStyle = (status: string) => {
  switch (status) {
    case 'PUBLISHED':
      return {
        background: 'linear-gradient(135deg, #10B981, #059669)',
        color: 'white'
      }
    case 'DRAFT':
      return {
        background: 'linear-gradient(135deg, #F59E0B, #D97706)',
        color: 'white'
      }
    case 'CANCELLED':
      return {
        background: 'linear-gradient(135deg, #EF4444, #DC2626)',
        color: 'white'
      }
    case 'COMPLETED':
      return {
        background: 'linear-gradient(135deg, #6B7280, #4B5563)',
        color: 'white'
      }
    default:
      return {
        background: 'linear-gradient(135deg, #6B7280, #4B5563)',
        color: 'white'
      }
  }
}

const statusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CANCELLED: '已取消',
  COMPLETED: '已完成'
}

// 加载活动详情
const loadActivity = async () => {
  const activityId = Number(route.params.id)
  if (!activityId) {
    ElMessage.error('活动ID不存在')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await getActivity(activityId)
    if (res.code === 200 && res.data) {
      activity.value = res.data
    } else {
      ElMessage.error(res.message || '加载活动详情失败')
      router.back()
    }
  } catch (error: any) {
    console.error('加载活动详情失败:', error)
    ElMessage.error(error.message || '加载活动详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 加载活动图片
const loadImages = async () => {
  const activityId = Number(route.params.id)
  if (!activityId) return

  try {
    const res = await getActivityImages(activityId)
    if (res.code === 200 && res.data) {
      images.value = res.data
    }
  } catch (error: any) {
    console.error('加载图片失败:', error)
  }
}

// 文件选择
const handleFileChange = (file: any) => {
  uploadFile.value = file.raw
  return false // 阻止自动上传
}

// 上传图片
const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择要上传的图片')
    return
  }

  const activityId = Number(route.params.id)
  loading.value = true
  uploadProgress.value = 0

  try {
    const res = await uploadActivityImage(
      uploadFile.value,
      (percent) => {
        uploadProgress.value = percent
      }
    )

    if (res.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      imageDescription.value = ''
      uploadFile.value = null
      uploadProgress.value = 0
      loadImages()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传图片失败:', error)
    ElMessage.error(error.message || '上传失败')
  } finally {
    loading.value = false
  }
}

// 删除图片
const handleDeleteImage = async (image: ActivityImageDTO) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这张图片吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const res = await deleteActivityImage(image.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadImages()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除图片失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadActivity()
  loadImages()
})
</script>

<template>
  <Layout>
    <div class="activity-detail-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <button class="back-btn" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
          <div class="header-title">
            <!-- Inline CSS Fix: Prevent FOUC - Page title should be black (#1C1917), not orange -->
            <h1 class="page-title" style="color: #1C1917;">活动详情</h1>
          </div>
        </div>
        <div class="header-decoration"></div>
      </div>

      <!-- 活动信息卡片 -->
      <div v-if="activity" class="activity-card" v-loading="loading">
        <div class="activity-header">
          <h2 class="activity-title">{{ activity.activityName }}</h2>
          <!-- 渐变胶囊状态标签 -->
          <span class="status-badge" :style="getStatusStyle(activity.status)">
            {{ statusTextMap[activity.status] }}
          </span>
        </div>

        <div class="activity-info-grid">
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #FF6B4A, #E35532);">
              <el-icon color="white"><Location /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">活动地点</span>
              <span class="info-value">{{ activity.location || '-' }}</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #F59E0B, #D97706);">
              <el-icon color="white"><User /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">组织者</span>
              <span class="info-value">{{ activity.organizer || '-' }}</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #FB7185, #F43F5E);">
              <el-icon color="white"><Calendar /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">开始时间</span>
              <span class="info-value">{{ formatDate(activity.startTime) }}</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #10B981, #059669);">
              <el-icon color="white"><Clock /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">结束时间</span>
              <span class="info-value">{{ formatDate(activity.endTime) }}</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #3B82F6, #2563EB);">
              <el-icon color="white"><User /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">参与人数</span>
              <span class="info-value">{{ activity.participantCount || 0 }} 人</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon" style="background: linear-gradient(135deg, #8B5CF6, #7C3AED);">
              <el-icon color="white"><User /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">创建者</span>
              <span class="info-value">{{ activity.creatorName || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="activity-description-section">
          <div class="section-title">
            <el-icon><InfoFilled /></el-icon>
            <span>活动描述</span>
          </div>
          <p class="description-text">{{ activity.description || '暂无描述' }}</p>
        </div>

        <div class="activity-meta">
          <span class="meta-item">
            <el-icon><Calendar /></el-icon>
            创建时间：{{ formatDate(activity.createTime) }}
          </span>
        </div>
      </div>

      <!-- 活动图片 -->
      <div class="images-section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Picture /></el-icon>
            <span>活动图片</span>
          </div>
          <button
            v-if="userStore.isMinister"
            class="upload-btn"
            @click="uploadDialogVisible = true"
          >
            <el-icon><Upload /></el-icon>
            <span>上传图片</span>
          </button>
        </div>

        <div v-if="images.length > 0" class="images-grid">
          <div v-for="image in images" :key="image.id" class="image-card">
            <el-image
              :src="getFullImageUrl(image.imageUrl)"
              :preview-src-list="getFullImageUrlList(images.map(img => img.imageUrl))"
              fit="cover"
              class="activity-image"
            />
            <div class="image-overlay">
              <p class="image-description">{{ image.description || '无描述' }}</p>
              <button
                v-if="userStore.isMinister"
                class="delete-btn"
                @click="handleDeleteImage(image)"
              >
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <div class="empty-icon">
            <el-icon :size="64" color="#ddd"><Picture /></el-icon>
          </div>
          <p class="empty-text">暂无图片</p>
        </div>
      </div>

      <!-- 上传图片对话框 -->
      <el-dialog
        v-model="uploadDialogVisible"
        title="上传活动图片"
        width="500px"
        class="custom-dialog"
        destroy-on-close
      >
        <div class="dialog-content">
          <div class="upload-area">
            <el-upload
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
              accept="image/*"
              drag
              class="custom-upload"
            >
              <el-icon class="el-icon--upload" :size="48"><Upload /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或<em>点击上传</em>
              </div>
            </el-upload>
          </div>
          <div class="form-group">
            <label class="form-label">图片描述</label>
            <textarea
              v-model="imageDescription"
              class="form-textarea"
              rows="3"
              placeholder="请输入图片描述（可选）"
            ></textarea>
          </div>
          <div v-if="uploadProgress > 0" class="progress-wrapper">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <span class="progress-text">{{ uploadProgress }}%</span>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button class="cancel-btn" @click="uploadDialogVisible = false">取消</button>
            <button class="save-btn" @click="handleUpload" :disabled="loading">
              <span>{{ loading ? '上传中...' : '上传' }}</span>
            </button>
          </div>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.activity-detail-page {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  position: relative;
  margin-bottom: var(--space-6);
  padding: var(--space-5);
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-radius: 24px;
  overflow: hidden;
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  z-index: 2;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: white;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.header-title {
  flex: 1;
  text-align: center;
  margin-right: 80px;
}

.page-title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: white;
  animation: fadeInUp 0.6s ease both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-decoration {
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

/* 活动卡片 */
.activity-card {
  background: white;
  border-radius: 24px;
  padding: var(--space-6);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: var(--space-6);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 2px solid rgba(255, 107, 74, 0.1);
}

.activity-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0;
}

/* 渐变胶囊状态标签 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-4);
  border-radius: 9999px;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 信息网格 */
.activity-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

@media (max-width: 1024px) {
  .activity-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .activity-info-grid {
    grid-template-columns: 1fr;
  }
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: rgba(255, 107, 74, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 74, 0.1);
}

.info-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.info-value {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

/* 描述区域 */
.activity-description-section {
  margin-bottom: var(--space-5);
  padding: var(--space-5);
  background: rgba(255, 107, 74, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 74, 0.1);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

.section-title .el-icon {
  color: #FF6B4A;
}

.description-text {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 元信息 */
.activity-meta {
  display: flex;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

/* 图片区域 */
.images-section {
  margin-bottom: var(--space-6);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: white;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.35);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 74, 0.45);
}

/* 图片网格 */
.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-4);
}

.image-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.image-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.activity-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-3);
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.image-description {
  margin: 0;
  font-size: var(--text-sm);
  color: white;
  flex: 1;
}

.delete-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239, 68, 68, 0.9);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background: #DC2626;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10);
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.empty-text {
  color: var(--text-tertiary);
  margin-top: var(--space-3);
}

/* 对话框样式 */
:deep(.custom-dialog .el-dialog__header) {
  padding: var(--space-5) var(--space-5) 0;
}

:deep(.custom-dialog .el-dialog__title) {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--text-primary);
}

:deep(.custom-dialog .el-dialog__body) {
  padding: var(--space-4) var(--space-5);
}

:deep(.custom-dialog .el-dialog__footer) {
  padding: 0 var(--space-5) var(--space-5);
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.upload-area {
  :deep(.custom-upload .el-upload-dragger) {
    border-radius: 16px;
    border: 2px dashed rgba(255, 107, 74, 0.3);
    background: rgba(255, 107, 74, 0.02);
    transition: all 0.3s ease;
  }
  
  :deep(.custom-upload .el-upload-dragger:hover) {
    border-color: #FF6B4A;
    background: rgba(255, 107, 74, 0.05);
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
}

.form-textarea {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-sm);
  border: 2px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: #FAFAFA;
  resize: vertical;
  outline: none;
  transition: all 0.3s ease;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #FF8A70;
  background: white;
  box-shadow: 0 0 0 4px rgba(255, 138, 112, 0.1);
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #FF6B4A, #F59E0B);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: #FF6B4A;
  min-width: 40px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.cancel-btn {
  padding: var(--space-2) var(--space-5);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.save-btn {
  padding: var(--space-2) var(--space-5);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: white;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.35);
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 74, 0.45);
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 768px) {
  .activity-detail-page {
    padding: var(--space-4);
  }

  .page-header {
    padding: var(--space-4);
  }

  .header-title {
    margin-right: 0;
  }

  .activity-header {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }

  .images-grid {
    grid-template-columns: 1fr;
  }
}
</style>
