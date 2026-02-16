<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Layout from '@/components/Layout.vue'
import OrganicBlob from '@/components/OrganicBlob.vue'
import SparkleEffect from '@/components/SparkleEffect.vue'
import { getActivities, createActivity, updateActivity, deleteActivity, getActivityImages } from '@/api/activity'
import type { ActivityIntro, ActivityIntroRequest, ActivityImage } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getFullImageUrl, getFullImageUrlList } from '@/utils/image'

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

// 搜索和筛选
const searchQuery = ref('')
const expandedCards = ref<Set<number>>(new Set())

// 过滤后的活动列表
const filteredActivities = computed(() => {
  if (!searchQuery.value.trim()) return activities.value
  const query = searchQuery.value.toLowerCase()
  return activities.value.filter(activity =>
    activity.activityName.toLowerCase().includes(query) ||
    activity.background.toLowerCase().includes(query) ||
    activity.significance.toLowerCase().includes(query)
  )
})

// 切换卡片展开状态
const toggleCardExpand = (activityId: number) => {
  if (expandedCards.value.has(activityId)) {
    expandedCards.value.delete(activityId)
  } else {
    expandedCards.value.add(activityId)
  }
}

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
              activity.images = imageRes.data.map(img => ({
                ...img,
                displayOrder: img.sortOrder
              })) as ActivityImage[]
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
    <div class="activities-page">
      <!-- Hero Section - 活动介绍页 (活力橙 #F97316) -->
      <section class="hero-section hero-organic page-activities">
        <!-- 有机Blob装饰层 - 每个Blob有独特的seed -->
        <div class="hero-organic__blobs">
          <!-- Blob 1: 右上角，主色活力橙，seed=1 -->
          <OrganicBlob
            size="large"
            color="#F97316"
            color-light="#FF8A70"
            :position="{ top: '-120px', right: '-80px' }"
            :delay="0"
            :opacity="0.55"
            :seed="1"
            glow
          />
          <!-- Blob 2: 右下角，次色暖橙红，seed=2 -->
          <OrganicBlob
            size="medium"
            color="#E85A3C"
            color-light="#FF6B4A"
            :position="{ bottom: '-80px', right: '50px' }"
            :delay="-2"
            :opacity="0.4"
            :seed="2"
            float
          />
          <!-- Blob 3: 左下角，琥珀金，seed=3 -->
          <OrganicBlob
            size="small"
            color="#F59E0B"
            color-light="#FDBA74"
            :position="{ bottom: '-40px', left: '-20px' }"
            :delay="-4"
            :opacity="0.35"
            :seed="3"
          />
          <!-- 网格纹理 -->
          <div class="grid-pattern"></div>
        </div>

        <!-- Sparkle粒子层 -->
        <SparkleEffect :count="12" color="rgba(255, 255, 255, 0.95)" />

        <!-- 内容层 -->
        <div class="hero-organic__content hero-organic__content--left">
          <div class="hero-badge">
            <el-icon><StarFilled /></el-icon>
            <span>特色活动</span>
          </div>
          <h1 class="hero-title">精彩活动</h1>
          <p class="hero-description">
            探索我们精心策划的特色活动，了解活动背景、意义与流程
            <br />开启一段充满成长与收获的精彩旅程
          </p>
        </div>
      </section>

      <!-- Toolbar Section - 工具栏 -->
      <section class="toolbar-section">
        <div class="hero-stats" v-if="activities.length > 0">
          <div class="stat-item">
            <span class="stat-number">{{ activities.length }}</span>
            <span class="stat-label">特色活动</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-number">{{ activities.reduce((sum, a) => sum + (a.images?.length || 0), 0) }}</span>
            <span class="stat-label">精彩图片</span>
          </div>
        </div>
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索活动名称、背景或意义..."
            class="search-input"
          />
          <el-icon v-if="searchQuery" class="clear-icon" @click="searchQuery = ''"><CircleClose /></el-icon>
        </div>
        <el-button
          v-if="userStore.isMinister"
          type="primary"
          class="create-btn"
          @click="handleCreate"
        >
          <el-icon><Plus /></el-icon>
          <span>新增活动</span>
        </el-button>
      </section>

      <!-- Activities Grid - 活动卡片网格 -->
      <section class="activities-section" v-loading="loading">
        <div v-if="filteredActivities.length === 0 && !loading" class="empty-state">
          <div class="empty-illustration">
            <el-icon :size="64"><Calendar /></el-icon>
          </div>
          <h3 class="empty-title">{{ searchQuery ? '未找到匹配的活动' : '暂无活动介绍' }}</h3>
          <p class="empty-desc">{{ searchQuery ? '尝试使用其他关键词搜索' : '管理员可以添加新的活动介绍' }}</p>
          <el-button v-if="userStore.isMinister && !searchQuery" type="primary" @click="handleCreate">
            添加首个活动
          </el-button>
        </div>

        <div class="activities-grid">
          <div
            v-for="(activity, index) in filteredActivities"
            :key="activity.id"
            class="activity-card"
            :class="{ 'is-expanded': expandedCards.has(activity.id) }"
            :style="{ animationDelay: `${index * 100}ms` }"
          >
            <!-- Card Header -->
            <div class="card-header-section">
              <div class="card-number">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="card-title-wrapper">
                <h3 class="card-title">{{ activity.activityName }}</h3>
                <div class="card-meta">
                  <span class="meta-item" v-if="activity.images && activity.images.length > 0">
                    <el-icon><Picture /></el-icon>
                    {{ activity.images.length }} 张图片
                  </span>
                </div>
              </div>
              <div class="card-actions" v-if="userStore.isMinister">
                <el-tooltip content="图片管理" placement="top">
                  <button class="action-btn image-btn" @click.stop="handleManageImages(activity)">
                    <el-icon><Picture /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="编辑" placement="top">
                  <button class="action-btn edit-btn" @click.stop="handleEdit(activity)">
                    <el-icon><Edit /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <button class="action-btn delete-btn" @click.stop="handleDelete(activity)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>

            <!-- Card Content -->
            <div class="card-content-section">
              <!-- Background -->
              <div class="content-block">
                <div class="block-header">
                  <div class="block-icon bg-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <h4 class="block-title">活动背景</h4>
                </div>
                <p class="block-text">{{ activity.background }}</p>
              </div>

              <!-- Significance -->
              <div class="content-block">
                <div class="block-header">
                  <div class="block-icon sig-icon">
                    <el-icon><Star /></el-icon>
                  </div>
                  <h4 class="block-title">活动意义</h4>
                </div>
                <p class="block-text">{{ activity.significance }}</p>
              </div>

              <!-- Purpose & Process - Collapsible -->
              <div class="collapsible-content" :class="{ 'is-expanded': expandedCards.has(activity.id) }">
                <!-- Purpose -->
                <div class="content-block">
                  <div class="block-header">
                    <div class="block-icon pur-icon">
                      <el-icon><Target /></el-icon>
                    </div>
                    <h4 class="block-title">活动目的</h4>
                  </div>
                  <p class="block-text">{{ activity.purpose }}</p>
                </div>

                <!-- Process -->
                <div class="content-block process-block">
                  <div class="block-header">
                    <div class="block-icon proc-icon">
                      <el-icon><List /></el-icon>
                    </div>
                    <h4 class="block-title">活动流程</h4>
                  </div>
                  <div class="process-flow">
                    <div
                      v-for="(step, stepIndex) in activity.process.split('\\n').filter(s => s.trim())"
                      :key="stepIndex"
                      class="process-step"
                    >
                      <div class="step-number">{{ stepIndex + 1 }}</div>
                      <div class="step-content">{{ step.trim() }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Expand/Collapse Button -->
              <button class="expand-btn" @click="toggleCardExpand(activity.id)">
                <span>{{ expandedCards.has(activity.id) ? '收起详情' : '查看详情' }}</span>
                <el-icon :class="{ 'is-rotated': expandedCards.has(activity.id) }"><ArrowDown /></el-icon>
              </button>

              <!-- Images Gallery -->
              <div v-if="activity.images && activity.images.length > 0" class="images-section">
                <div class="section-divider"></div>
                <div class="gallery-header">
                  <el-icon><Picture /></el-icon>
                  <span>活动精彩瞬间</span>
                </div>
                <div class="images-gallery">
                  <div
                    v-for="(image, imgIndex) in activity.images.slice(0, 4)"
                    :key="image.id"
                    class="gallery-item"
                    :class="{ 'has-more': imgIndex === 3 && activity.images.length > 4 }"
                    @click="() => {
                      const previewList = activity.images?.map(img => img.imageUrl) || []
                      // 使用 Element Plus 的图片预览
                    }"
                  >
                    <el-image
                      :src="getFullImageUrl(image.imageUrl)"
                      :alt="image.description || '活动图片'"
                      fit="cover"
                      class="gallery-image"
                      :preview-src-list="getFullImageUrlList(activity.images.map(img => img.imageUrl))"
                      :initial-index="imgIndex"
                      :preview-teleported="true"
                      :z-index="3000"
                    >
                      <template #error>
                        <div class="image-placeholder">
                          <el-icon><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <div v-if="imgIndex === 3 && activity.images.length > 4" class="more-overlay">
                      <span class="more-count">+{{ activity.images.length - 4 }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Create/Edit Dialog -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="720px"
        class="activity-dialog"
        destroy-on-close
      >
        <div class="dialog-body">
          <el-form :model="activityForm" label-width="90px" class="activity-form">
            <el-form-item label="活动名称" required>
              <el-input
                v-model="activityForm.activityName"
                placeholder="请输入活动名称"
                class="form-input-enhanced"
              />
            </el-form-item>
            <el-form-item label="活动背景" required>
              <el-input
                v-model="activityForm.background"
                type="textarea"
                :rows="3"
                placeholder="请输入活动背景"
                class="form-input-enhanced"
              />
            </el-form-item>
            <el-form-item label="活动意义" required>
              <el-input
                v-model="activityForm.significance"
                type="textarea"
                :rows="3"
                placeholder="请输入活动意义"
                class="form-input-enhanced"
              />
            </el-form-item>
            <el-form-item label="活动目的" required>
              <el-input
                v-model="activityForm.purpose"
                type="textarea"
                :rows="3"
                placeholder="请输入活动目的"
                class="form-input-enhanced"
              />
            </el-form-item>
            <el-form-item label="活动流程" required>
              <el-input
                v-model="activityForm.process"
                type="textarea"
                :rows="5"
                placeholder="请输入活动流程，每行一个步骤"
                class="form-input-enhanced"
              />
            </el-form-item>
          </el-form>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false" class="cancel-btn">取消</el-button>
            <el-button type="primary" @click="handleSave" :loading="loading" class="submit-btn">
              {{ isEdit ? '保存修改' : '创建活动' }}
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- Image Manager Dialog -->
      <el-dialog
        v-model="imageDialogVisible"
        :title="`图片管理 - ${currentActivityForImages?.activityName}`"
        width="900px"
        class="image-dialog"
        destroy-on-close
      >
        <div class="image-manager">
          <!-- Upload Section -->
          <div class="upload-card">
            <div class="upload-header">
              <div class="upload-icon">
                <el-icon><Upload /></el-icon>
              </div>
              <div class="upload-title">
                <h4>上传新图片</h4>
                <p>支持 JPG、PNG、WEBP 格式，最大 5MB</p>
              </div>
            </div>
            <div class="upload-body">
              <el-upload
                :auto-upload="false"
                :on-change="handleImageUpload"
                :limit="1"
                :file-list="imageFile ? [{ name: imageFile.name, url: '' }] : []"
                accept="image/jpeg,image/png,image/jpg,image/webp"
                list-type="picture-card"
                class="upload-component"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="upload-form">
                <el-input
                  v-model="imageDescription"
                  placeholder="图片描述（可选）"
                  class="form-input-enhanced"
                />
                <el-input-number
                  v-model="imageSortOrder"
                  :min="0"
                  placeholder="排序号"
                  class="sort-input"
                />
                <el-button
                  type="primary"
                  @click="uploadImage"
                  :loading="uploadingImage"
                  :disabled="!imageFile"
                  class="upload-submit-btn"
                >
                  <el-icon><Upload /></el-icon>
                  上传图片
                </el-button>
              </div>
            </div>
          </div>

          <!-- Images Grid -->
          <div class="images-list-card">
            <div class="list-header">
              <h4>已上传图片</h4>
              <span class="image-count">{{ activityImages.length }} 张</span>
            </div>
            <div v-if="activityImages.length > 0" class="images-list">
              <div
                v-for="(image, imgIdx) in activityImages"
                :key="image.imageId"
                class="image-card"
                :style="{ animationDelay: `${imgIdx * 50}ms` }"
              >
                <div class="image-wrapper">
                  <el-image
                    :src="getFullImageUrl(image.imageUrl)"
                    fit="cover"
                    class="list-image"
                    :preview-src-list="getFullImageUrlList(activityImages.map((img: any) => img.imageUrl))"
                    :initial-index="imgIdx"
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
                </div>
                <div class="image-details">
                  <p class="image-desc">{{ image.description || '无描述' }}</p>
                  <p class="image-meta">排序: {{ image.sortOrder }}</p>
                </div>
                <button class="delete-image-btn" @click="deleteImage(image.imageId)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
            <el-empty v-else description="还没有上传图片" class="empty-images" />
          </div>
        </div>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
/* ============================================
   Activities Page - Modern Magazine Style
   色彩增强版：活力渐变 + 动态装饰
   ============================================ */

.activities-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

/* ============================================
   Hero Section - 视觉焦点区域（增强版）
   ============================================ */
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
    radial-gradient(ellipse 80% 60% at 90% 10%, rgba(249, 115, 22, 0.12) 0%, transparent 40%),
    radial-gradient(ellipse 60% 50% at 10% 90%, rgba(232, 90, 60, 0.1) 0%, transparent 35%),
    linear-gradient(135deg, #FFF5F0 0%, #FFEDE0 50%, #FFE5D5 100%);
  border-radius: 32px;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 50% 35% at 75% 15%, rgba(249, 115, 22, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse 40% 25% at 25% 85%, rgba(232, 90, 60, 0.15) 0%, transparent 45%);
  pointer-events: none;
  z-index: 0;
}

@keyframes heroGlow {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.8; }
  50% { transform: translate(20px, -20px) scale(1.1); opacity: 1; }
}

/* 装饰性浮动元素 */
.hero-organic__blobs .grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(249, 115, 22, 0.1) 1px, transparent 0);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
}

.hero-section .floating-deco {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  animation: floatDeco 6s ease-in-out infinite;
}

.hero-section .deco-1 {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.25), rgba(232, 90, 60, 0.2));
  top: 15%;
  right: 15%;
  animation-delay: 0s;
}

.hero-section .deco-2 {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(249, 115, 22, 0.15));
  bottom: 25%;
  left: 10%;
  animation-delay: -2s;
}

.hero-section .deco-3 {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(232, 90, 60, 0.25), rgba(249, 115, 22, 0.2));
  top: 50%;
  right: 25%;
  animation-delay: -4s;
}

@keyframes floatDeco {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(10deg); }
}

.hero-organic__content {
  position: relative;
  z-index: 2;
  padding: 32px 32px 48px;
}

/* 左对齐布局 - 内容向上移动 */
.hero-organic__content--left {
  text-align: left;
  padding-left: 48px;
  padding-top: 24px;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, #F97316, #FB923C);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 16px rgba(249, 115, 22, 0.3);
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

.hero-badge .el-icon {
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.hero-title {
  font-size: clamp(1.875rem, 4vw, 2.5rem);
  font-weight: var(--font-bold);
  line-height: 1.2;
  color: var(--text-primary);
  margin: 0 0 var(--space-4) 0;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.title-highlight {
  background: linear-gradient(135deg, #F97316, #FB923C, #F59E0B);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 4s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% center; }
  50% { background-position: 100% center; }
}

.hero-description {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
  max-width: 600px;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* ============================================
   Toolbar Section - 工具栏（增强版）
   ============================================ */
.toolbar-section {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin: var(--space-6);
  margin-top: var(--space-6);
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(249, 115, 22, 0.1);
  position: relative;
  overflow: hidden;
}

.toolbar-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #F97316, #FB923C, #F59E0B);
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.08), rgba(249, 115, 22, 0.08));
  border-radius: var(--radius-lg);
  min-width: 80px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid rgba(249, 115, 22, 0.1);
}

.stat-item:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.2);
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), rgba(249, 115, 22, 0.12));
}

.stat-number {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #F97316, #FB923C);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.stat-label {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: linear-gradient(180deg, transparent, rgba(249, 115, 22, 0.4), transparent);
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: rgba(249, 115, 22, 0.03);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}

.search-box:focus-within {
  background: white;
  box-shadow: 0 0 0 2px rgba(249, 115, 22, 0.2);
  border-color: rgba(249, 115, 22, 0.3);
}

.search-icon {
  color: #F97316;
  font-size: var(--text-lg);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--text-base);
  color: var(--text-primary);
  outline: none;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.clear-icon {
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.clear-icon:hover {
  color: #F97316;
}

.create-btn {
  height: 48px;
  padding: 0 var(--space-6);
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #F97316, #FB923C);
  border: none;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.25);
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.35);
}

.create-btn :deep(span) {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* ============================================
   Activities Grid - 活动卡片网格（增强版）
   ============================================ */
.activities-section {
  min-height: 400px;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(520px, 1fr));
  gap: var(--space-6);
}

/* Activity Card - 增强版 */
.activity-card {
  background: white;
  border-radius: var(--radius-2xl);
  border: 1px solid rgba(251, 146, 60, 0.1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  position: relative;
}

.activity-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #FB923C, #FB923C, rgba(252, 211, 77, 0.7));
  background-size: 200% 100%;
  animation: gradientFlow 3s linear infinite;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.activity-card:hover {
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(251, 146, 60, 0.15);
  transform: translateY(-6px);
}

.activity-card:hover::before {
  opacity: 1;
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.activity-card.is-expanded {
  grid-row: span 2;
}

/* Card Header - 增强版 */
.card-header-section {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-6);
  background: linear-gradient(135deg, 
    rgba(251, 146, 60, 0.06) 0%, 
    rgba(251, 146, 60, 0.08) 50%,
    rgba(251, 146, 60, 0.04) 100%);
  border-bottom: 1px solid rgba(251, 146, 60, 0.08);
  position: relative;
  overflow: hidden;
}

.card-header-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(251, 146, 60, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

.card-number {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  background: linear-gradient(135deg, #FDBA74, #FDBA74);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  font-family: var(--font-mono);
}

.card-title-wrapper {
  flex: 1;
}

.card-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2) 0;
  line-height: 1.3;
  transition: color 0.3s ease;
}

.activity-card:hover .card-title {
  color: #F97316;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.card-actions {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-lg);
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  min-width: var(--touch-target-min);
  min-height: var(--touch-target-min);
}

.action-btn:hover {
  transform: scale(1.1) translateY(-2px);
}

.image-btn:hover {
  background: linear-gradient(135deg, var(--amber-100), var(--amber-50));
  color: #F97316;
  box-shadow: 0 4px 12px rgba(251, 146, 60, 0.2);
}

.edit-btn:hover {
  background: linear-gradient(135deg, var(--amber-100), var(--amber-50));
  color: #F97316;
  box-shadow: 0 4px 12px rgba(251, 146, 60, 0.2);
}

.delete-btn:hover {
  background: linear-gradient(135deg, rgba(252, 211, 77, 0.2), rgba(252, 211, 77, 0.1));
  color: rgba(251, 191, 36, 1);
  box-shadow: 0 4px 12px rgba(252, 211, 77, 0.2);
}

/* Card Content - 增强版 */
.card-content-section {
  padding: var(--space-6);
}

.content-block {
  margin-bottom: var(--space-5);
}

.content-block:last-of-type {
  margin-bottom: 0;
}

.block-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.block-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: var(--text-lg);
  transition: all 0.3s ease;
}

.bg-icon {
  background: linear-gradient(135deg, var(--amber-100), var(--amber-50));
  color: #F97316;
  box-shadow: 0 2px 8px rgba(251, 146, 60, 0.15);
}

.sig-icon {
  background: linear-gradient(135deg, var(--amber-100), var(--amber-50));
  color: #F97316;
  box-shadow: 0 2px 8px rgba(251, 146, 60, 0.15);
}

.pur-icon {
  background: linear-gradient(135deg, rgba(252, 211, 77, 0.2), rgba(252, 211, 77, 0.1));
  color: rgba(251, 191, 36, 1);
  box-shadow: 0 2px 8px rgba(252, 211, 77, 0.15);
}

.proc-icon {
  background: linear-gradient(135deg, var(--amber-100), var(--amber-100));
  color: #F97316;
  box-shadow: 0 2px 8px rgba(251, 146, 60, 0.15);
}

.activity-card:hover .block-icon {
  transform: scale(1.05);
}

.block-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
}

.block-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
  padding-left: var(--space-10);
}

/* Collapsible Content */
.collapsible-content {
  max-height: 0;
  overflow: hidden;
  transition: max-height var(--transition-slow);
}

.collapsible-content.is-expanded {
  max-height: 2000px;
}

/* Process Flow - 增强版 */
.process-flow {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding-left: var(--space-10);
}

.process-step {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.03), rgba(251, 146, 60, 0.03));
  border-radius: var(--radius-lg);
  border-left: 3px solid;
  border-image: linear-gradient(180deg, #FB923C, #FB923C) 1;
  transition: all 0.3s ease;
}

.process-step:hover {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.06), rgba(251, 146, 60, 0.06));
  transform: translateX(4px);
}

.step-number {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FB923C, #FB923C);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(251, 146, 60, 0.3);
}

.step-content {
  flex: 1;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.6;
}

/* Expand Button - 增强版 */
.expand-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-3);
  margin-top: var(--space-4);
  background: transparent;
  border: 1px dashed rgba(251, 146, 60, 0.3);
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-height: var(--touch-target-md);
}

.expand-btn:hover {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.05), rgba(251, 146, 60, 0.05));
  border-color: #FDBA74;
  color: #F97316;
  border-style: solid;
}

.expand-btn .el-icon {
  transition: transform var(--transition-fast);
}

.expand-btn .el-icon.is-rotated {
  transform: rotate(180deg);
}

/* Images Section - 增强版 */
.images-section {
  margin-top: var(--space-5);
}

.section-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(251, 146, 60, 0.2), transparent);
  margin-bottom: var(--space-4);
}

.gallery-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: #F97316;
  margin-bottom: var(--space-4);
}

.images-gallery {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.gallery-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.gallery-item:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(251, 146, 60, 0.2);
  border-color: var(--amber-300);
}

.gallery-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--amber-50), var(--amber-50));
  color: #FDBA74;
}

.more-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.7), rgba(251, 146, 60, 0.7));
  color: white;
}

.more-count {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
}

/* Empty State - 增强版 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16);
  text-align: center;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.1), rgba(251, 146, 60, 0.1));
  border-radius: var(--radius-2xl);
  color: #FDBA74;
  margin-bottom: var(--space-6);
  position: relative;
  overflow: hidden;
}

.empty-illustration::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(255, 255, 255, 0.3) 50%, transparent 60%);
  animation: emptyShine 3s ease-in-out infinite;
}

@keyframes emptyShine {
  0%, 100% { transform: translateX(-100%); }
  50% { transform: translateX(100%); }
}

.empty-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2) 0;
}

.empty-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin: 0 0 var(--space-6) 0;
}

/* ============================================
   Animations - 增强版
   ============================================ */
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
   Dialog Styles - 弹窗样式（增强版）
   ============================================ */
.activity-dialog :deep(.el-dialog__header) {
  padding: var(--space-6) var(--space-6) var(--space-4);
  border-bottom: 1px solid rgba(251, 146, 60, 0.1);
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.02), rgba(251, 146, 60, 0.02));
}

.activity-dialog :deep(.el-dialog__title) {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
}

.dialog-body {
  padding: var(--space-4) 0;
}

.activity-form :deep(.el-form-item__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
}

.form-input-enhanced :deep(.el-input__wrapper),
.form-input-enhanced :deep(.el-textarea__inner) {
  border-radius: var(--radius-lg);
  box-shadow: 0 0 0 1px rgba(251, 146, 60, 0.2) inset;
  transition: all var(--transition-fast);
}

.form-input-enhanced :deep(.el-input__wrapper.is-focus),
.form-input-enhanced :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #FDBA74 inset, 0 0 0 3px rgba(251, 146, 60, 0.1);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.cancel-btn {
  padding: var(--space-3) var(--space-6);
}

.submit-btn {
  padding: var(--space-3) var(--space-6);
  background: linear-gradient(135deg, #FB923C, #FB923C);
  border: none;
  box-shadow: 0 4px 12px rgba(251, 146, 60, 0.25);
}

.submit-btn:hover {
  box-shadow: 0 6px 16px rgba(251, 146, 60, 0.35);
}

/* Image Dialog - 增强版 */
.image-dialog :deep(.el-dialog__body) {
  padding: var(--space-6);
}

.image-manager {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.upload-card {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.03), rgba(251, 146, 60, 0.03));
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  border: 1px solid rgba(251, 146, 60, 0.1);
}

.upload-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.upload-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FB923C, #FB923C);
  color: white;
  border-radius: var(--radius-lg);
  font-size: var(--text-xl);
  box-shadow: 0 4px 12px rgba(251, 146, 60, 0.25);
}

.upload-title h4 {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
}

.upload-title p {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}

.upload-body {
  display: flex;
  gap: var(--space-6);
  align-items: flex-start;
}

.upload-component :deep(.el-upload--picture-card) {
  width: 148px;
  height: 148px;
  border-radius: var(--radius-lg);
  border: 2px dashed rgba(251, 146, 60, 0.3);
  background: white;
  transition: all 0.3s ease;
}

.upload-component :deep(.el-upload--picture-card:hover) {
  border-color: #FDBA74;
  color: #FB923C;
  background: rgba(251, 146, 60, 0.02);
}

.upload-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.sort-input {
  width: 100%;
}

.sort-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-lg);
}

.upload-submit-btn {
  height: 44px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #FB923C, #FB923C);
  border: none;
  box-shadow: 0 4px 12px rgba(251, 146, 60, 0.25);
}

.upload-submit-btn:hover {
  box-shadow: 0 6px 16px rgba(251, 146, 60, 0.35);
}

/* Images List - 增强版 */
.images-list-card {
  background: white;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(251, 146, 60, 0.1);
  overflow: hidden;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.04), rgba(251, 146, 60, 0.04));
  border-bottom: 1px solid rgba(251, 146, 60, 0.08);
}

.list-header h4 {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin: 0;
}

.image-count {
  font-size: var(--text-sm);
  color: #F97316;
  background: rgba(251, 146, 60, 0.1);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
}

.images-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--space-4);
  padding: var(--space-6);
}

.image-card {
  position: relative;
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(251, 146, 60, 0.1);
  overflow: hidden;
  transition: all var(--transition-fast);
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
}

.image-card:hover {
  box-shadow: 0 8px 24px rgba(251, 146, 60, 0.15);
  transform: translateY(-3px);
  border-color: rgba(251, 146, 60, 0.2);
}

.image-card:hover .delete-image-btn {
  opacity: 1;
}

.image-wrapper {
  aspect-ratio: 4/3;
  overflow: hidden;
}

.list-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.image-card:hover .list-image {
  transform: scale(1.05);
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--amber-50), var(--amber-50));
  color: #FDBA74;
}

.image-details {
  padding: var(--space-3);
}

.image-desc {
  font-size: var(--text-sm);
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin: 0;
}

.delete-image-btn {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(252, 211, 77, 1), rgba(251, 191, 36, 1));
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  opacity: 0;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(252, 211, 77, 0.3);
  min-width: var(--touch-target-min);
  min-height: var(--touch-target-min);
}

.delete-image-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(252, 211, 77, 0.4);
}

.empty-images {
  padding: var(--space-12);
}

/* ============================================
   Animations
   ============================================ */
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

/* ============================================
   Responsive Design - 增强版
   ============================================ */
/* 极小屏幕 (<= 320px) - iPhone SE、小屏手机 */
@media (max-width: 320px) {
  .activities-page {
    padding: 0 var(--space-3) var(--space-6);
  }

  .hero-section {
    padding: var(--space-6);
    margin: var(--space-3);
    margin-bottom: 0;
  }

  .hero-title {
    font-size: var(--text-2xl);
  }

  .hero-description {
    font-size: var(--text-sm);
  }

  .toolbar-section {
    padding: var(--space-3);
    margin: var(--space-3);
  }

  .stat-item {
    min-width: 60px;
    padding: var(--space-1) var(--space-2);
  }

  .stat-number {
    font-size: 16px;
  }

  .card-header-section {
    padding: var(--space-4);
  }

  .card-content-section {
    padding: var(--space-4);
  }

  .activities-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
}

/* 小屏手机 (>= 320px) - iPhone SE 基准 */
@media (max-width: 480px) {
  .activities-page {
    padding: 0 var(--space-4) var(--space-8);
  }

  .hero-section {
    padding: var(--space-6);
    margin: var(--space-4);
    margin-bottom: 0;
  }

  .hero-title {
    font-size: var(--text-2xl);
  }

  .hero-stats {
    gap: var(--space-2);
  }

  .stat-item {
    min-width: 70px;
    padding: var(--space-1) var(--space-3);
  }

  .stat-number {
    font-size: 18px;
  }

  .block-text,
  .process-flow {
    padding-left: 0;
  }

  .images-gallery {
    grid-template-columns: 1fr;
  }

  .images-list {
    grid-template-columns: 1fr;
  }

  .activities-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-section {
    flex-direction: column;
    gap: var(--space-3);
    margin: var(--space-4);
  }

  .search-box {
    width: 100%;
  }

  .create-btn {
    width: 100%;
  }

  .card-header-section {
    flex-wrap: wrap;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: var(--space-3);
  }
  
  /* 小屏幕弹窗优化 */
  .activity-dialog :deep(.el-dialog),
  .image-dialog :deep(.el-dialog) {
    width: 100% !important;
    max-width: 100% !important;
    margin: 2vh auto !important;
    border-radius: 16px 16px 0 0 !important;
    position: fixed !important;
    bottom: 0 !important;
    top: auto !important;
    left: 0 !important;
    right: 0 !important;
  }
  
  .activity-dialog :deep(.el-dialog__header),
  .image-dialog :deep(.el-dialog__header) {
    border-radius: 16px 16px 0 0 !important;
  }
  
  .activity-dialog :deep(.el-dialog__body),
  .image-dialog :deep(.el-dialog__body) {
    max-height: 55vh;
    padding: var(--space-3) !important;
  }
  
  .activity-dialog :deep(.el-dialog__footer),
  .image-dialog :deep(.el-dialog__footer) {
    padding: var(--space-3) !important;
    display: flex;
    gap: var(--space-2);
  }
  
  .activity-dialog :deep(.el-dialog__footer .el-button),
  .image-dialog :deep(.el-dialog__footer .el-button) {
    flex: 1;
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

/* 平板 (<= 768px) */
@media (max-width: 768px) {
  .activities-page {
    padding: 0 var(--space-4) var(--space-8);
  }

  .hero-section {
    padding: var(--space-6);
    margin: var(--space-4);
    margin-bottom: 0;
  }

  .hero-section .floating-deco {
    display: none;
  }

  .hero-title {
    font-size: var(--text-3xl);
  }

  .toolbar-section {
    flex-direction: column;
    gap: var(--space-3);
    margin: var(--space-4);
  }

  .hero-stats {
    width: 100%;
    justify-content: center;
  }

  .search-box {
    width: 100%;
  }

  .create-btn {
    width: 100%;
  }

  .card-header-section {
    flex-wrap: wrap;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: var(--space-3);
  }

  .images-gallery {
    grid-template-columns: repeat(2, 1fr);
  }

  .images-list {
    grid-template-columns: repeat(2, 1fr);
  }

  .activities-grid {
    grid-template-columns: 1fr;
  }
  
  /* 弹窗移动端适配 */
  .activity-dialog :deep(.el-dialog) {
    width: 95% !important;
    max-width: 95% !important;
    margin: 5vh auto !important;
    border-radius: 20px !important;
  }
  
  .activity-dialog :deep(.el-dialog__header) {
    padding: var(--space-4) !important;
  }
  
  .activity-dialog :deep(.el-dialog__body) {
    padding: var(--space-4) !important;
    max-height: 60vh;
    overflow-y: auto;
  }
  
  .activity-dialog :deep(.el-dialog__footer) {
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
  
  /* 图片管理弹窗移动端适配 */
  .image-dialog :deep(.el-dialog) {
    width: 95% !important;
    max-width: 95% !important;
    margin: 3vh auto !important;
    border-radius: 20px !important;
  }
  
  .image-dialog :deep(.el-dialog__body) {
    padding: var(--space-4) !important;
    max-height: 70vh;
    overflow-y: auto;
  }
  
  .upload-body {
    flex-direction: column;
    align-items: stretch;
  }
  
  .upload-component :deep(.el-upload--picture-card) {
    width: 100% !important;
    height: 120px !important;
  }
}

/* 平板横屏 (<= 1024px) */
@media (max-width: 1024px) {
  .hero-section {
    padding: var(--space-8);
  }

  .hero-section::before {
    width: 400px;
    height: 400px;
  }

  .hero-section::after {
    width: 300px;
    height: 300px;
  }

  .hero-description {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-stats {
    justify-content: center;
  }

  .activities-grid {
    grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  }

  .upload-body {
    flex-direction: column;
    align-items: stretch;
  }
}

/* 横屏模式优化 */
@media (orientation: landscape) and (max-width: 768px) {
  .activities-page {
    padding: 0 var(--space-4) var(--space-6);
  }

  .hero-section {
    padding: var(--space-6);
  }

  .activities-grid {
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
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
