<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Layout from '@/components/Layout.vue'
import { getActivities, createActivity, updateActivity, deleteActivity } from '@/api/activity'
import type { Activity, ActivityRequest } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const activities = ref<Activity[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增活动介绍')
const isEdit = ref(false)
const currentActivity = ref<Activity | null>(null)

const activityForm = ref<ActivityRequest>({
  activityName: '',
  background: '',
  significance: '',
  purpose: '',
  process: ''
})

// 加载活动介绍列表
const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getActivities()
    if (res.code === 200 && res.data) {
      // 后端返回的字段需要映射
      activities.value = (res.data as any[]).map((act: any) => ({
        id: act.activityId,
        activityName: act.activityName,
        background: act.background,
        significance: act.significance,
        purpose: act.purpose,
        process: act.process,
        createTime: act.createTime,
        updateTime: act.updateTime
      }))
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
      res = await updateActivity(currentActivity.value.id, activityForm.value)
    } else {
      res = await createActivity(activityForm.value)
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
</style>
