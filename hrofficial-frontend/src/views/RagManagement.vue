<template>
  <div class="rag-management">
    <el-card class="header-card">
      <div class="header-content">
        <h2><i class="el-icon-document"></i> RAG知识库管理</h2>
        <p class="subtitle">管理校园知识库向量数据，初始化和同步知识内容</p>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <i class="el-icon-files stat-icon"></i>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalVectors }}</div>
              <div class="stat-label">向量总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <i class="el-icon-folder stat-icon"></i>
            <div class="stat-info">
              <div class="stat-value">{{ stats.collectionName }}</div>
              <div class="stat-label">Collection名称</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <i class="el-icon-data-line stat-icon"></i>
            <div class="stat-info">
              <div class="stat-value">{{ stats.vectorDimension }}</div>
              <div class="stat-label">向量维度</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <i class="el-icon-time stat-icon"></i>
            <div class="stat-info">
              <div class="stat-value">{{ formatTime(stats.lastUpdateTime) }}</div>
              <div class="stat-label">最后更新</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作区域 -->
    <el-card class="operation-card">
      <div class="operation-header">
        <h3>知识库操作</h3>
      </div>
      
      <el-form :model="initForm" label-width="120px">
        <el-form-item label="知识库路径">
          <el-input 
            v-model="initForm.sourcePath" 
            placeholder="留空使用默认路径: src/main/resources/rag-knowledge-base"
          />
        </el-form-item>
        
        <el-form-item label="强制重建">
          <el-switch 
            v-model="initForm.forceReindex"
            active-text="是"
            inactive-text="否"
          />
          <span class="form-tip">警告：重建将删除现有所有数据</span>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            :loading="initializing" 
            @click="initializeKnowledgeBase"
            icon="el-icon-upload"
          >
            初始化知识库
          </el-button>
          <el-button 
            @click="loadStats"
            icon="el-icon-refresh"
          >
            刷新统计
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 初始化结果 -->
    <el-card v-if="initResult" class="result-card">
      <div class="result-header">
        <h3>初始化结果</h3>
        <el-tag :type="initResult.failedFiles > 0 ? 'warning' : 'success'">
          {{ initResult.failedFiles > 0 ? '部分成功' : '全部成功' }}
        </el-tag>
      </div>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总文件数">{{ initResult.totalFiles }}</el-descriptions-item>
        <el-descriptions-item label="成功处理">{{ initResult.processedFiles }}</el-descriptions-item>
        <el-descriptions-item label="失败文件">{{ initResult.failedFiles }}</el-descriptions-item>
        <el-descriptions-item label="总分块数">{{ initResult.totalChunks }}</el-descriptions-item>
        <el-descriptions-item label="新增分块">{{ initResult.newChunks }}</el-descriptions-item>
        <el-descriptions-item label="重复跳过">{{ initResult.duplicateChunks }}</el-descriptions-item>
      </el-descriptions>

      <!-- 错误列表 -->
      <div v-if="initResult.errors && initResult.errors.length > 0" class="error-section">
        <h4>错误详情：</h4>
        <el-table :data="initResult.errors" border style="width: 100%">
          <el-table-column prop="fileName" label="文件名" width="300" />
          <el-table-column prop="reason" label="错误原因" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../utils/http'

interface RagStats {
  totalDocuments: number
  totalVectors: number
  categoryStats: Record<string, number>
  lastUpdateTime: string
  collectionName: string
  vectorDimension: number
}

interface InitForm {
  sourcePath: string
  forceReindex: boolean
}

interface InitResult {
  totalFiles: number
  processedFiles: number
  failedFiles: number
  totalChunks: number
  newChunks: number
  duplicateChunks: number
  errors: Array<{
    fileName: string
    reason: string
  }>
}

export default defineComponent({
  name: 'RagManagement',
  setup() {
    const stats = ref<RagStats>({
      totalDocuments: 0,
      totalVectors: 0,
      categoryStats: {},
      lastUpdateTime: '',
      collectionName: '',
      vectorDimension: 0
    })

    const initForm = ref<InitForm>({
      sourcePath: '',
      forceReindex: false
    })

    const initializing = ref(false)
    const initResult = ref<InitResult | null>(null)

    // 加载统计信息
    const loadStats = async () => {
      try {
        const response = await axios.get('/api/rag/stats')
        const data = response.data as any
        if (data.code === 200) {
          stats.value = data.data
        } else {
          ElMessage.error(data.message || '获取统计信息失败')
        }
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '获取统计信息失败')
      }
    }

    // 初始化知识库
    const initializeKnowledgeBase = async () => {
      if (initForm.value.forceReindex) {
        const confirmed = await ElMessageBox.confirm(
          '强制重建将删除所有现有数据，确定要继续吗？',
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).catch(() => false)

        if (!confirmed) return
      }

      initializing.value = true
      initResult.value = null

      try {
        const response = await axios.post('/api/rag/initialize', initForm.value)
        const data = response.data as any
        if (data.code === 200) {
          initResult.value = data.data
          ElMessage.success('知识库初始化完成')
          loadStats()
        } else {
          ElMessage.error(data.message || '初始化失败')
        }
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '初始化失败')
      } finally {
        initializing.value = false
      }
    }

    // 格式化时间
    const formatTime = (time: string) => {
      if (!time) return '--'
      return new Date(time).toLocaleString('zh-CN')
    }

    onMounted(() => {
      loadStats()
    })

    return {
      stats,
      initForm,
      initializing,
      initResult,
      loadStats,
      initializeKnowledgeBase,
      formatTime
    }
  }
})
</script>

<style scoped>
.rag-management {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  text-align: center;
}

.header-content h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
}

.subtitle {
  color: #909399;
  margin: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.stat-icon {
  font-size: 48px;
  color: #409EFF;
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.operation-card {
  margin-bottom: 20px;
}

.operation-header {
  margin-bottom: 20px;
}

.operation-header h3 {
  margin: 0;
  color: #303133;
}

.form-tip {
  margin-left: 10px;
  color: #E6A23C;
  font-size: 12px;
}

.result-card {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header h3 {
  margin: 0;
  color: #303133;
}

.error-section {
  margin-top: 20px;
}

.error-section h4 {
  color: #F56C6C;
  margin-bottom: 10px;
}
</style>
