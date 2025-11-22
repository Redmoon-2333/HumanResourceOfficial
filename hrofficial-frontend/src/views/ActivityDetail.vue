<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Layout from '@/components/Layout.vue'
import { getActivity, getActivityImages, uploadActivityImage, deleteActivityImage } from '@/api/activity'
import type { Activity, ActivityImage } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activity = ref<Activity | null>(null)
const images = ref<ActivityImage[]>([])
const uploadDialogVisible = ref(false)
const imageDescription = ref('')
const uploadFile = ref<File | null>(null)
const uploadProgress = ref(0)

const statusColorMap: Record<string, string> = {
  DRAFT: 'info',
  PUBLISHED: 'success',
  CANCELLED: 'danger',
  COMPLETED: 'warning'
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
      activityId,
      uploadFile.value,
      imageDescription.value,
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
const handleDeleteImage = async (image: ActivityImage) => {
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

    const activityId = Number(route.params.id)
    loading.value = true
    const res = await deleteActivityImage(activityId, image.id)
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
    <div class="activity-detail-container">
      <el-card v-loading="loading">
        <!-- 返回按钮 -->
        <div class="header-actions">
          <el-button @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>

        <!-- 活动信息 -->
        <div v-if="activity" class="activity-info">
          <div class="activity-header">
            <h1 class="activity-title">{{ activity.title }}</h1>
            <el-tag :type="statusColorMap[activity.status]" size="large">
              {{ statusTextMap[activity.status] }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border class="activity-descriptions">
            <el-descriptions-item label="活动地点">
              {{ activity.location || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="组织者">
              {{ activity.organizer || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ formatDate(activity.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ formatDate(activity.endTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="参与人数">
              {{ activity.participantCount || 0 }} 人
            </el-descriptions-item>
            <el-descriptions-item label="创建者">
              {{ activity.creatorName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">
              {{ formatDate(activity.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="活动描述" :span="2">
              <div class="activity-description">
                {{ activity.description || '无' }}
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 活动图片 -->
      <el-card class="images-card">
        <template #header>
          <div class="card-header">
            <span>活动图片</span>
            <el-button
              type="primary"
              size="small"
              @click="uploadDialogVisible = true"
              v-if="userStore.isMinister"
            >
              <el-icon><Upload /></el-icon>
              上传图片
            </el-button>
          </div>
        </template>

        <div v-if="images.length > 0" class="images-grid">
          <div v-for="image in images" :key="image.id" class="image-item">
            <el-image
              :src="image.imageUrl"
              :preview-src-list="images.map(img => img.imageUrl)"
              fit="cover"
              class="activity-image"
            />
            <div class="image-info">
              <p class="image-description">{{ image.description || '无描述' }}</p>
              <el-button
                type="danger"
                size="small"
                @click="handleDeleteImage(image)"
                v-if="userStore.isMinister"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无图片" />
      </el-card>

      <!-- 上传图片对话框 -->
      <el-dialog
        v-model="uploadDialogVisible"
        title="上传活动图片"
        width="500px"
      >
        <el-form label-width="80px">
          <el-form-item label="选择图片">
            <el-upload
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
              accept="image/*"
              drag
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或<em>点击上传</em>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="图片描述">
            <el-input
              v-model="imageDescription"
              type="textarea"
              :rows="3"
              placeholder="请输入图片描述（可选）"
            />
          </el-form-item>
          <el-form-item v-if="uploadProgress > 0">
            <el-progress :percentage="uploadProgress" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpload" :loading="loading">
            上传
          </el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.activity-detail-container {
  padding: var(--spacing-lg);
  max-width: 1200px;
  margin: 0 auto;
}

.header-actions {
  margin-bottom: var(--spacing-lg);
}

.activity-info {
  margin-top: var(--spacing-lg);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-md);
  border-bottom: 2px solid var(--color-primary);
}

.activity-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0;
}

.activity-descriptions {
  margin-top: var(--spacing-lg);
}

.activity-description {
  white-space: pre-wrap;
  line-height: 1.8;
}

.images-card {
  margin-top: var(--spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--spacing-lg);
}

.image-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.3s;
}

.image-item:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-4px);
}

.activity-image {
  width: 100%;
  height: 200px;
  cursor: pointer;
}

.image-info {
  padding: var(--spacing-md);
}

.image-description {
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-secondary);
  font-size: 14px;
}
</style>
