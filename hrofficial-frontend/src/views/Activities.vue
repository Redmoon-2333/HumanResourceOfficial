<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import { getActivities, createActivity, updateActivity, deleteActivity, getActivityImages } from '@/api/activity'
import type { ActivityIntro, ActivityIntroRequest } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const activities = ref<ActivityIntro[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增活动介绍')
const isEdit = ref(false)
const currentActivity = ref<ActivityIntro | null>(null)

const activityForm = ref<ActivityIntroRequest>({
  activityName: '',
  background: '',
  significance: '',
  purpose: '',
  process: ''
})

// 图片管理相关
const imageDialogVisible = ref(false)
const currentActivityForImages = ref<ActivityIntro | null>(null)
const activityImages = ref<any[]>([])
const uploadingImage = ref(false)
const imageFile = ref<File | null>(null)
const imageDescription = ref('')
const imageSortOrder = ref(0)

// 加载活动介绍列表
const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getActivities()
    if (res.code === 200 && res.data) {
      // 处理分页数据
      const dataList = Array.isArray(res.data) ? res.data : (res.data as any).content || (res.data as any).list || []
      
      // 后端返回的字段需要映射
      const activitiesWithImages = await Promise.all(
        dataList.map(async (act: any) => {
          const activity: ActivityIntro = {
            id: act.activityId,
            activityName: act.activityName,
            background: act.background,
            significance: act.significance,
            purpose: act.purpose,
            process: act.process,
            createTime: act.createTime,
            updateTime: act.updateTime,
            images: []
          }
          
          // 加载该活动的图片
          try {
            const imageRes = await getActivityImages(act.activityId)
            if (imageRes.code === 200 && imageRes.data) {
              activity.images = imageRes.data
            }
          } catch (error) {
            console.warn(`加载活动 ${act.activityId} 的图片失败:`, error)
          }
          
          return activity
        })
      )
      
      activities.value = activitiesWithImages
    }
  } catch (error: any) {
    console.error('加载活动失败:', error)
    ElMessage.error(error.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
const handleCreate = () => {
  dialogTitle.value = '新增活动介绍'
  isEdit.value = false
  currentActivity.value = null
  activityForm.value = {
    activityName: '',
    background: '',
    significance: '',
    purpose: '',
    process: ''
  }
  dialogVisible.value = true
}

// 打开编辑对话框
const handleEdit = (activity: any) => {
  dialogTitle.value = '编辑活动介绍'
  isEdit.value = true
  currentActivity.value = activity
  activityForm.value = {
    activityName: activity.activityName,
    background: activity.background,
    significance: activity.significance,
    purpose: activity.purpose,
    process: activity.process
  }
  dialogVisible.value = true
}

// 保存活动
const handleSave = async () => {
  if (!activityForm.value.activityName) {
    ElMessage.warning('请输入活动名称')
    return
  }
  if (!activityForm.value.background) {
    ElMessage.warning('请输入活动背景')
    return
  }
  if (!activityForm.value.significance) {
    ElMessage.warning('请输入活动意义')
    return
  }
  if (!activityForm.value.purpose) {
    ElMessage.warning('请输入活动目的')
    return
  }
  if (!activityForm.value.process) {
    ElMessage.warning('请输入活动流程')
    return
  }

  loading.value = true
  try {
    let res
    if (isEdit.value && currentActivity.value) {
      res = await updateActivity(currentActivity.value.id, activityForm.value as any)
    } else {
      res = await createActivity(activityForm.value as any)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadActivities()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存活动失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除活动
const handleDelete = async (activity: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除活动"${activity.activityName}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const res = await deleteActivity(activity.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadActivities()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除活动失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 打开图片管理对话框
const handleManageImages = async (activity: ActivityIntro) => {
  currentActivityForImages.value = activity
  await loadActivityImages(activity.id)
  imageDialogVisible.value = true
}

// 加载活动图片
const loadActivityImages = async (activityId: number) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/activities/${activityId}/images`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const result = await response.json()
    if (result.code === 200 && result.data) {
      activityImages.value = result.data
    }
  } catch (error: any) {
    console.error('加载图片失败:', error)
    ElMessage.error('加载图片失败')
  }
}

// 处理图片上传
const handleImageUpload = (file: any) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp']
  if (!validTypes.includes(file.raw.type)) {
    ElMessage.error('请上传 JPG/PNG/WEBP 格式的图片')
    return false
  }
  
  const maxSize = 5 * 1024 * 1024 // 5MB
  if (file.raw.size > maxSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  
  imageFile.value = file.raw
  return false
}

// 上传图片
const uploadImage = async () => {
  if (!imageFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  
  if (!currentActivityForImages.value) {
    return
  }
  
  uploadingImage.value = true
  try {
    const formData = new FormData()
    formData.append('file', imageFile.value)
    formData.append('description', imageDescription.value || '')
    formData.append('sortOrder', imageSortOrder.value.toString())
    
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/activities/${currentActivityForImages.value.id}/images`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
    
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('图片上传成功')
      imageFile.value = null
      imageDescription.value = ''
      imageSortOrder.value = 0
      await loadActivityImages(currentActivityForImages.value.id)
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传图片失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploadingImage.value = false
  }
}

// 删除图片
const deleteImage = async (imageId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该图片吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/activities/images/${imageId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const result = await response.json()
    if (result.code === 200) {
      ElMessage.success('删除成功')
      if (currentActivityForImages.value) {
        await loadActivityImages(currentActivityForImages.value.id)
      }
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除图片失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<template>
  <Layout>
    <div class="activities-container">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h1 class="page-title">人力资源中心活动介绍</h1>
          <p class="page-subtitle">了解我们的特色活动</p>
        </div>
        <el-button 
          v-if="userStore.isMinister" 
          type="primary" 
          @click="handleCreate"
        >
          <el-icon><Plus /></el-icon>
          新增活动
        </el-button>
      </div>

      <!-- 活动列表 -->
      <div v-loading="loading" class="activities-grid">
        <el-card
          v-for="activity in activities"
          :key="activity.id"
          class="activity-card"
        >
          <template #header>
            <div class="card-header">
              <h3>{{ activity.activityName }}</h3>
              <div v-if="userStore.isMinister" class="actions">
                <el-button type="success" size="small" @click="handleManageImages(activity)">
                  <el-icon><Picture /></el-icon>
                  图片管理
                </el-button>
                <el-button type="primary" size="small" @click="handleEdit(activity)">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete(activity)">
                  删除
                </el-button>
              </div>
            </div>
          </template>

          <div class="activity-content">
            <div class="section">
              <h4><el-icon><Document /></el-icon> 活动背景</h4>
              <p>{{ activity.background }}</p>
            </div>

            <div class="section">
              <h4><el-icon><Star /></el-icon> 活动意义</h4>
              <p>{{ activity.significance }}</p>
            </div>

            <div class="section">
              <h4><el-icon><Target /></el-icon> 活动目的</h4>
              <p>{{ activity.purpose }}</p>
            </div>

            <div class="section">
              <h4><el-icon><List /></el-icon> 活动流程</h4>
              <p class="process">{{ activity.process }}</p>
            </div>
            
            <!-- 活动图片展示区域 -->
            <div v-if="activity.images && activity.images.length > 0" class="section">
              <h4><el-icon><Picture /></el-icon> 活动图片</h4>
              <div class="activity-images">
                <el-image
                  v-for="image in activity.images"
                  :key="image.id"
                  :src="image.imageUrl"
                  :alt="image.description || '活动图片'"
                  fit="cover"
                  class="activity-image"
                  :preview-src-list="activity.images.map(img => img.imageUrl)"
                  :preview-teleported="true"
                  :z-index="3000"
                >
                  <template #error>
                    <div class="image-error-small">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && activities.length === 0" description="暂无活动介绍" />

      <!-- 创建/编辑对话框 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="700px"
      >
        <el-form :model="activityForm" label-width="100px">
          <el-form-item label="活动名称" required>
            <el-input v-model="activityForm.activityName" placeholder="请输入活动名称" />
          </el-form-item>
          <el-form-item label="活动背景" required>
            <el-input
              v-model="activityForm.background"
              type="textarea"
              :rows="3"
              placeholder="请输入活动背景"
            />
          </el-form-item>
          <el-form-item label="活动意义" required>
            <el-input
              v-model="activityForm.significance"
              type="textarea"
              :rows="3"
              placeholder="请输入活动意义"
            />
          </el-form-item>
          <el-form-item label="活动目的" required>
            <el-input
              v-model="activityForm.purpose"
              type="textarea"
              :rows="3"
              placeholder="请输入活动目的"
            />
          </el-form-item>
          <el-form-item label="活动流程" required>
            <el-input
              v-model="activityForm.process"
              type="textarea"
              :rows="5"
              placeholder="请输入活动流程"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="loading">
            保存
          </el-button>
        </template>
      </el-dialog>

      <!-- 图片管理对话框 -->
      <el-dialog
        v-model="imageDialogVisible"
        :title="`管理活动图片 - ${currentActivityForImages?.activityName}`"
        width="900px"
      >
        <div class="image-manager">
          <!-- 上传区域 -->
          <el-card class="upload-section" shadow="never">
            <template #header>
              <h4>上传新图片</h4>
            </template>
            <el-upload
              :auto-upload="false"
              :on-change="handleImageUpload"
              :limit="1"
              :file-list="imageFile ? [{ name: imageFile.name, url: '' }] : []"
              accept="image/jpeg,image/png,image/jpg,image/webp"
              list-type="picture-card"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-form">
              <el-input
                v-model="imageDescription"
                placeholder="图片描述（可选）"
                style="margin-bottom: 10px"
              />
              <el-input-number
                v-model="imageSortOrder"
                :min="0"
                placeholder="排序号"
                style="width: 100%; margin-bottom: 10px"
              />
              <el-button
                type="primary"
                @click="uploadImage"
                :loading="uploadingImage"
                :disabled="!imageFile"
              >
                <el-icon><Upload /></el-icon>
                上传图片
              </el-button>
            </div>
          </el-card>

          <!-- 图片列表 -->
          <el-card class="images-section" shadow="never">
            <template #header>
              <h4>已上传的图片（{{ activityImages.length }}张）</h4>
            </template>
            <div v-if="activityImages.length > 0" class="images-grid">
              <div
                v-for="image in activityImages"
                :key="image.imageId"
                class="image-item"
              >
                <el-image
                  :src="image.imageUrl"
                  fit="cover"
                  style="width: 100%; height: 150px"
                  :preview-src-list="activityImages.map((img: any) => img.imageUrl)"
                  :initial-index="activityImages.findIndex((img: any) => img.imageId === image.imageId)"
                  :preview-teleported="true"
                  :z-index="3000"
                  hide-on-click-modal
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="image-info">
                  <p class="image-desc">{{ image.description || '无描述' }}</p>
                  <p class="image-order">排序: {{ image.sortOrder }}</p>
                </div>
                <div class="image-actions">
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteImage(image.imageId)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="还没有上传图片" />
          </el-card>
        </div>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.activities-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.page-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.activity-card {
  transition: all 0.3s ease;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.actions {
  display: flex;
  gap: var(--spacing-sm);
}

.activity-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.section h4 {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
  margin: 0 0 var(--spacing-sm) 0;
}

.section p {
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.process {
  background: var(--color-bg-light);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}

.image-manager {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.upload-section,
.images-section {
  border: 1px solid var(--color-border);
}

.upload-form {
  margin-top: var(--spacing-md);
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--spacing-md);
}

.image-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.3s;
}

.image-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.image-info {
  padding: var(--spacing-sm);
}

.image-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-xs) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-order {
  font-size: 12px;
  color: var(--color-text-light);
  margin: 0;
}

.image-actions {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px;
  background: var(--color-bg-light);
  color: var(--color-text-light);
}

/* 活动图片展示区域 */
.activity-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.activity-image {
  width: 100%;
  height: 80px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.activity-image:hover {
  transform: scale(1.05);
}

.image-error-small {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  background: var(--color-bg-light);
  color: var(--color-text-light);
  font-size: 14px;
}
</style>
