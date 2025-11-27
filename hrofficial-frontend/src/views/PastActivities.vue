<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import { getPastActivities, createPastActivity, deletePastActivity, getYears } from '@/api/pastActivity'
import type { PastActivity } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const activities = ref<PastActivity[]>([])
const years = ref<number[]>([])
const selectedYear = ref<number | undefined>()

const dialogVisible = ref(false)
const uploadForm = ref({
  title: '',
  description: '',
  articleUrl: '',
  coverImageUrl: '',
  year: new Date().getFullYear(),
  activityDate: ''
})

const coverImageFile = ref<File | null>(null)
const uploadingCover = ref(false)

// 加载年份列表
const loadYears = async () => {
  try {
    const res = await getYears()
    if (res.code === 200 && res.data) {
      years.value = res.data
    }
  } catch (error: any) {
    console.error('加载年份失败:', error)
  }
}

// 加载往届活动列表
const loadActivities = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (selectedYear.value) {
      params.year = selectedYear.value
    }

    const res = await getPastActivities(params)
    console.log('获取往届活动响应:', res)
    
    if (res.code === 200 && res.data) {
      console.log('响应数据类型:', typeof res.data, 'isArray:', Array.isArray(res.data))
      console.log('响应数据内容:', res.data)
      console.log('content字段:', res.data.content)
      console.log('content是否为数组:', Array.isArray(res.data.content))
      
      let activityList = []
      if (Array.isArray(res.data)) {
        // 直接返回数组
        activityList = res.data
        console.log('数据是数组，直接使用')
      } else if (res.data.content && Array.isArray(res.data.content)) {
        // 分页对象，字段名是 content
        activityList = res.data.content
        console.log('从分页对象的content字段提取列表')
      } else if (res.data.list && Array.isArray(res.data.list)) {
        // 分页对象，字段名是 list（兼容旧格式）
        activityList = res.data.list
        console.log('从分页对象的list字段提取列表')
      }
      
      console.log('提取到的活动列表:', activityList)
      console.log('活动列表长度:', activityList.length)
      
      // 转换字段名以兼容前端模板
      activities.value = activityList.map((activity: any) => ({
        ...activity,
        id: activity.pastActivityId,  // 添加 id 别名
        coverImageUrl: activity.coverImage,  // 添加 coverImageUrl 别名
        articleUrl: activity.pushUrl  // 添加 articleUrl 别名
      }))
      
      console.log('转换后的活动列表:', activities.value)
    } else {
      console.warn('响应格式异常:', res)
    }
  } catch (error: any) {
    console.error('加载往届活动失败:', error)
    ElMessage.error(error.message || '加载活动失败')
  } finally {
    loading.value = false
  }
}

// 年份筛选
const handleYearFilter = (year: number | undefined) => {
  selectedYear.value = year
  loadActivities()
}

// 处理封面图片上传
const handleCoverUpload = (file: any) => {
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
  
  coverImageFile.value = file.raw
  return false // 阻止自动上传
}

// 删除封面图片
const handleCoverRemove = () => {
  coverImageFile.value = null
  uploadForm.value.coverImageUrl = ''
}

// 上传封面图片到服务器
const uploadCoverImage = async () => {
  if (!coverImageFile.value) {
    return uploadForm.value.coverImageUrl // 如果没有新图片，返回现有URL
  }
  
  uploadingCover.value = true
  try {
    const formData = new FormData()
    formData.append('file', coverImageFile.value)
    
    const token = localStorage.getItem('token')
    const response = await fetch('/api/activities/upload-image', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
    
    // 检查响应状态
    if (!response.ok) {
      throw new Error(`上传失败: ${response.status} ${response.statusText}`)
    }
    
    // 检查响应内容类型
    const contentType = response.headers.get('content-type')
    if (!contentType || !contentType.includes('application/json')) {
      throw new Error('服务器返回的不是JSON格式')
    }
    
    const result = await response.json()
    if (result.code === 200 && result.data) {
      return result.data // 直接返回URL字符串
    } else {
      throw new Error(result.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传封面图片失败:', error)
    throw error
  } finally {
    uploadingCover.value = false
  }
}

// 打开创建对话框
const handleCreate = () => {
  uploadForm.value = {
    title: '',
    description: '',
    articleUrl: '',
    coverImageUrl: '',
    year: new Date().getFullYear(),
    activityDate: ''
  }
  coverImageFile.value = null
  dialogVisible.value = true
}

// 保存往届活动
const handleSave = async () => {
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入活动标题')
    return
  }
  if (!uploadForm.value.articleUrl) {
    ElMessage.warning('请输入推文链接')
    return
  }
  if (!coverImageFile.value && !uploadForm.value.coverImageUrl) {
    ElMessage.warning('请上传封面图片')
    return
  }

  loading.value = true
  try {
    // 先上传封面图片
    const coverUrl = await uploadCoverImage()
    console.log('图片上传结果:', coverUrl)
    
    if (!coverUrl) {
      throw new Error('图片上传失败，未获取到图片URL')
    }
    
    uploadForm.value.coverImageUrl = coverUrl
    
    console.log('准备创建往届活动:', {
      title: uploadForm.value.title,
      articleUrl: uploadForm.value.articleUrl,
      coverImageUrl: uploadForm.value.coverImageUrl,
      year: uploadForm.value.year
    })
    
    const res = await createPastActivity(uploadForm.value)

    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadActivities()
      loadYears()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error: any) {
    console.error('创建往届活动失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 删除往届活动
const handleDelete = async (activity: PastActivity) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除往届活动“${activity.title}”吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const activityId = activity.id || activity.pastActivityId
    const res = await deletePastActivity(activityId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadActivities()
      loadYears()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除往届活动失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 打开推文链接
const openArticle = (url: string) => {
  window.open(url, '_blank')
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  loadYears()
  loadActivities()
})
</script>

<template>
  <Layout>
    <div class="past-activities-container">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h1 class="page-title">往届活动</h1>
          <p class="page-subtitle">浏览人力资源中心历年精彩活动</p>
        </div>
        <el-button 
          v-if="userStore.isMinister" 
          type="primary" 
          @click="handleCreate"
        >
          <el-icon><Plus /></el-icon>
          添加往届活动
        </el-button>
      </div>

      <!-- 年份筛选 -->
      <el-card class="filter-card">
        <div class="year-filters">
          <el-button
            :type="selectedYear === undefined ? 'primary' : ''"
            @click="handleYearFilter(undefined)"
          >
            全部
          </el-button>
          <el-button
            v-for="year in years"
            :key="year"
            :type="selectedYear === year ? 'primary' : ''"
            @click="handleYearFilter(year)"
          >
            {{ year }}年
          </el-button>
        </div>
      </el-card>

      <!-- 活动列表 -->
      <div v-loading="loading" class="activities-grid">
        <el-card
          v-for="activity in activities"
          :key="activity.id"
          class="activity-card hover-card"
          :body-style="{ padding: '0' }"
        >
          <!-- 活动封面 -->
          <div class="activity-cover" @click="openArticle(activity.articleUrl || activity.pushUrl)">
            <el-image
              :src="activity.coverImageUrl || activity.coverImage"
              fit="cover"
              style="width: 100%; height: 240px; cursor: pointer"
            >
              <template #error>
                <div class="image-error">
                  <el-icon :size="60" color="var(--color-border)">
                    <Picture />
                  </el-icon>
                  <p>封面加载失败</p>
                </div>
              </template>
            </el-image>
            <div class="activity-year-badge">
              {{ activity.year }}年
            </div>
            <div class="hover-overlay">
              <el-icon :size="48" color="#fff">
                <View />
              </el-icon>
              <p>点击查看推文</p>
            </div>
          </div>

          <!-- 活动信息 -->
          <div class="activity-content">
            <h3 class="activity-title">{{ activity.title }}</h3>
            <p class="activity-description">点击封面查看推文详情</p>
            
            <div class="activity-footer">
              <div class="activity-meta">
                <el-icon color="var(--color-primary)">
                  <Calendar />
                </el-icon>
                <span>{{ formatDate(activity.createTime) }}</span>
              </div>
              
              <div class="actions" v-if="userStore.isMinister">
                <el-button 
                  type="danger" 
                  size="small" 
                  @click.stop="handleDelete(activity)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && activities.length === 0"
        description="暂无往届活动记录"
      />

      <!-- 创建对话框 -->
      <el-dialog
        v-model="dialogVisible"
        title="添加往届活动"
        width="600px"
      >
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="活动标题" required>
            <el-input v-model="uploadForm.title" placeholder="请输入活动标题" />
          </el-form-item>
          <el-form-item label="活动描述">
            <el-input
              v-model="uploadForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入活动描述"
            />
          </el-form-item>
          <el-form-item label="推文链接" required>
            <el-input 
              v-model="uploadForm.articleUrl" 
              placeholder="请输入推文链接(如微信公众号文章链接)" 
            />
          </el-form-item>
          <el-form-item label="封面图片" required>
            <el-upload
              :auto-upload="false"
              :on-change="handleCoverUpload"
              :on-remove="handleCoverRemove"
              :limit="1"
              :file-list="coverImageFile ? [{ name: coverImageFile.name, url: '' }] : []"
              accept="image/jpeg,image/png,image/jpg,image/webp"
              list-type="picture-card"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 JPG/PNG/WEBP 格式，大小不超过 5MB</div>
          </el-form-item>
          <el-form-item label="活动年份" required>
            <el-input-number
              v-model="uploadForm.year"
              :min="2000"
              :max="2100"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="活动日期">
            <el-date-picker
              v-model="uploadForm.activityDate"
              type="date"
              placeholder="选择活动日期"
              style="width: 100%"
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
    </div>
  </Layout>
</template>

<style scoped>
.past-activities-container {
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

.filter-card {
  margin-bottom: var(--spacing-lg);
}

.year-filters {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.activity-card {
  transition: all 0.3s ease;
  overflow: hidden;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.activity-cover {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.activity-year-badge {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 14px;
  backdrop-filter: blur(4px);
}

.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.activity-cover:hover .hover-overlay {
  opacity: 1;
}

.hover-overlay p {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 240px;
  background: var(--color-bg-light);
}

.image-error p {
  color: var(--color-text-light);
  margin-top: var(--spacing-sm);
}

.activity-content {
  padding: var(--spacing-md);
}

.activity-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-description {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 var(--spacing-md) 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 44px;
}

.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.activity-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 14px;
  color: var(--color-text-light);
}

.actions {
  display: flex;
  gap: var(--spacing-xs);
}

.upload-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}
</style>
