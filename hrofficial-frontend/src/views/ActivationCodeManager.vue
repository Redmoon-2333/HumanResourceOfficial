<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted } from 'vue'
import { getActivationCodes, generateActivationCode, deleteActivationCode } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Key,
  Plus,
  CopyDocument,
  Delete,
  Refresh,
  Check,
  Close,
  Warning,
  User,
  Clock,
  Search,
  Filter,
  MoreFilled,
  TrendCharts
} from '@element-plus/icons-vue'

// 直接测试API调用
const testApi = async () => {
  console.log('Testing API call...')
  try {
    const response = await getActivationCodes()
    console.log('Full API response:', response)
    console.log('Response data:', response.data)
    console.log('Response data.codes:', response.data.codes)
    console.log('Response data.stats:', response.data.stats)
    ElMessage.success('API test completed')
  } catch (error) {
    console.error('API test error:', error)
    ElMessage.error('API test failed')
  }
}

// 立即测试
testApi()

interface ActivationCode {
  id: number
  code: string
  used: boolean
  usedBy?: number
  usedByName?: string
  usedTime?: string
  createTime: string
  expireTime?: string
}

const codes = ref<ActivationCode[]>([])
const loading = ref(false)
const generating = ref(false)
const searchQuery = ref('')
const filterStatus = ref<'all' | 'unused' | 'used'>('all')
const backendStats = ref<{
  totalCount: number
  unusedCount: number
  usedCount: number
  expiredCount: number
}>({
  totalCount: 0,
  unusedCount: 0,
  usedCount: 0,
  expiredCount: 0
})

// 获取激活码列表
const fetchCodes = async () => {
  loading.value = true
  try {
    console.log('Starting fetchCodes...')
    const response = await getActivationCodes()
    console.log('Full response:', response)
    console.log('Response code:', response.code)

    // 防御性编程：检查响应是否有效
    if (!response) {
      console.error('API返回空响应')
      ElMessage.error('获取激活码失败：服务器无响应')
      return
    }

    if (response.code === 200) {
      // 防御性编程：确保 data 存在
      const responseData = response.data || {}
      console.log('Response data:', responseData)

      // 提取激活码列表 - 兼容多种可能的字段名
      const codesList = responseData.codes || responseData.list || responseData.activationCodes || []
      console.log('Extracted codes list:', codesList)

      // 使用展开运算符强制替换数组，确保响应式更新触发
      codes.value = [...codesList]
      console.log('Codes value after update:', codes.value)

      // 提取统计数据 - 兼容多种可能的字段名和结构
      const statsData = responseData.stats || responseData.statistics || {}
      console.log('Extracted stats:', statsData)

      // 更新后端统计数据 - 使用对象替换确保响应式更新
      backendStats.value = {
        totalCount: statsData.totalCount ?? statsData.total ?? 0,
        unusedCount: statsData.unusedCount ?? statsData.unused ?? 0,
        usedCount: statsData.usedCount ?? statsData.used ?? 0,
        expiredCount: statsData.expiredCount ?? statsData.expired ?? 0
      }
      console.log('BackendStats after update:', backendStats.value)

      // 检查stats计算属性
      console.log('Stats computed value:', stats.value)

      // 提示用户数据加载成功
      if (codes.value.length === 0) {
        console.log('激活码列表为空')
      } else {
        console.log(`成功加载 ${codes.value.length} 个激活码`)
      }
    } else {
      console.error('API返回错误:', response.message)
      ElMessage.error(response.message || '获取激活码失败')
    }
  } catch (error: any) {
    console.error('Error in fetchCodes:', error)
    ElMessage.error(error.message || '获取激活码失败')
  } finally {
    loading.value = false
  }
}

// 生成激活码
const handleGenerate = async () => {
  try {
    await ElMessageBox.confirm('确定要生成一个新的激活码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    generating.value = true
    const response = await generateActivationCode()
    if (response.code === 200) {
      ElMessage.success('激活码生成成功')
      fetchCodes()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '生成失败')
    }
  } finally {
    generating.value = false
  }
}

// 复制激活码
const handleCopy = (code: string) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('已复制到剪贴板')
}

// 撤销激活码
const handleRevoke = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要撤销此激活码吗？此操作不可恢复', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await deleteActivationCode(id)
    if (response.code === 200) {
      ElMessage.success('撤销成功')
      fetchCodes()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤销失败')
    }
  }
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 过滤后的激活码
const filteredCodes = computed(() => {
  let result = codes.value

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(c =>
      c.code.toLowerCase().includes(query) ||
      c.usedByName?.toLowerCase().includes(query)
    )
  }
  
  // 状态过滤
  if (filterStatus.value === 'unused') {
    result = result.filter(c => !c.used)
  } else if (filterStatus.value === 'used') {
    result = result.filter(c => c.used)
  }
  
  return result
})

// 统计数据（直接使用后端数据）
const stats = computed(() => {
  // 防御性编程：确保 backendStats.value 存在且有有效值
  const stats = backendStats.value || {}
  const total = Number(stats.totalCount) || 0
  const used = Number(stats.usedCount) || 0
  const unused = Number(stats.unusedCount) || 0
  const usageRate = total > 0 ? Math.round((used / total) * 100) : 0

  console.log('Stats computed:', { total, used, unused, usageRate })
  return { total, used, unused, usageRate }
})

onMounted(() => {
  fetchCodes()
})
</script>

<template>
  <Layout>
    <div class="activation-code-page">
      <!-- Hero区域 -->
      <div class="hero-section">
        <div class="hero-background">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="floating-keys">
            <div class="key-icon key-1">
              <el-icon :size="32" color="rgba(255, 107, 74, 0.3)"><Key /></el-icon>
            </div>
            <div class="key-icon key-2">
              <el-icon :size="24" color="rgba(245, 158, 11, 0.3)"><Key /></el-icon>
            </div>
            <div class="key-icon key-3">
              <el-icon :size="28" color="rgba(16, 185, 129, 0.3)"><Key /></el-icon>
            </div>
          </div>
        </div>
        
        <div class="hero-content">
          <div class="hero-badge">
            <el-icon><Key /></el-icon>
            <span>激活码管理</span>
          </div>
          <!-- Inline CSS Fix: Prevent FOUC - Hero title should be black (#1C1917), not orange -->
          <h1 class="hero-title" style="color: #1C1917;">
            成员账号<span style="color: #FF6B4A;">激活系统</span>
          </h1>
          <!-- Inline CSS Fix: Prevent FOUC - Subtitle should be gray (#78716C) -->
          <p class="hero-subtitle" style="color: #78716C;">
            生成和管理成员注册激活码，控制账号注册权限
          </p>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-section">
        <div class="stats-grid">
          <div class="stat-card total">
            <div class="stat-icon-bg">
              <el-icon :size="32" color="white"><Key /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-label">激活码总数</span>
              <span class="stat-value">
                <AnimatedCounter :value="stats.total" />
              </span>
            </div>
            <div class="stat-trend">
              <el-icon><TrendCharts /></el-icon>
            </div>
          </div>
          
          <div class="stat-card unused">
            <div class="stat-icon-bg">
              <el-icon :size="32" color="white"><Check /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-label">未使用</span>
              <span class="stat-value">
                <AnimatedCounter :value="stats.unused" />
              </span>
            </div>
            <div class="stat-badge available">
              {{ stats.total > 0 ? Math.round((stats.unused / stats.total) * 100) : 0 }}%
            </div>
          </div>
          
          <div class="stat-card used">
            <div class="stat-icon-bg">
              <el-icon :size="32" color="white"><User /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-label">已使用</span>
              <span class="stat-value">
                <AnimatedCounter :value="stats.used" />
              </span>
            </div>
            <div class="stat-badge used-badge">
              {{ stats.usageRate }}%
            </div>
          </div>
          
          <div class="stat-card rate">
            <div class="stat-icon-bg">
              <el-icon :size="32" color="white"><Key /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-label">使用率</span>
              <span class="stat-value">
                <AnimatedCounter :value="stats.usageRate" suffix="%" />
              </span>
            </div>
            <div class="progress-ring">
              <svg viewBox="0 0 36 36">
                <path
                  class="progress-ring-bg"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
                <path
                  class="progress-ring-fill"
                  :stroke-dasharray="`${stats.usageRate}, 100`"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <button class="generate-btn" @click="handleGenerate" :disabled="generating">
          <el-icon v-if="!generating" :size="18"><Plus /></el-icon>
          <el-icon v-else class="animate-spin"><Refresh /></el-icon>
          <span>{{ generating ? '生成中...' : '生成激活码' }}</span>
        </button>
        
        <div class="filter-group">
          <div class="search-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索激活码..."
              class="search-input"
            >
          </div>
          
          <div class="filter-tabs">
            <button
              class="filter-tab"
              :class="{ active: filterStatus === 'all' }"
              @click="filterStatus = 'all'"
            >
              全部
            </button>
            <button
              class="filter-tab"
              :class="{ active: filterStatus === 'unused' }"
              @click="filterStatus = 'unused'"
            >
              未使用
            </button>
            <button
              class="filter-tab"
              :class="{ active: filterStatus === 'used' }"
              @click="filterStatus = 'used'"
            >
              已使用
            </button>
          </div>
        </div>
      </div>

      <!-- 激活码列表 -->
      <div class="codes-section">
        <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
          <div class="table-container">
            <table class="codes-table">
              <thead>
                <tr>
                  <th class="col-status">状态</th>
                  <th class="col-code">激活码</th>
                  <th class="col-user">使用者</th>
                  <th class="col-time">使用时间</th>
                  <th class="col-created">创建时间</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="code in filteredCodes"
                  :key="code.id"
                  :class="{ used: code.used }"
                >
                  <td class="col-status">
                    <div class="status-badge" :class="code.used ? 'used' : 'unused'">
                      <el-icon :size="14">
                        <Check v-if="!code.used" />
                        <Close v-else />
                      </el-icon>
                      <span>{{ code.used ? '已使用' : '未使用' }}</span>
                    </div>
                  </td>
                  <td class="col-code">
                    <div class="code-cell">
                      <span class="code-text">{{ code.code }}</span>
                      <button class="copy-btn" @click="handleCopy(code.code)" title="复制">
                        <el-icon :size="14"><CopyDocument /></el-icon>
                      </button>
                    </div>
                  </td>
                  <td class="col-user">
                    <span v-if="code.usedByName" class="user-name">{{ code.usedByName }}</span>
                    <span v-else class="empty-text">-</span>
                  </td>
                  <td class="col-time">
                    <span v-if="code.usedTime" class="time-text">
                      <el-icon :size="12"><Clock /></el-icon>
                      {{ formatDate(code.usedTime) }}
                    </span>
                    <span v-else class="empty-text">-</span>
                  </td>
                  <td class="col-created">
                    <span class="time-text">{{ formatDate(code.createTime) }}</span>
                  </td>
                  <td class="col-action">
                    <div class="action-btns">
                      <button
                        v-if="!code.used"
                        class="action-btn revoke"
                        @click="handleRevoke(code.id)"
                        title="撤销"
                      >
                        <el-icon :size="16"><Delete /></el-icon>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- 加载状态 -->
          <div v-if="loading" class="empty-state">
            <div class="empty-icon">
              <el-icon :size="64" color="#FF6B4A" class="animate-spin"><Refresh /></el-icon>
            </div>
            <h3 class="empty-title">加载中...</h3>
            <p class="empty-desc">正在获取激活码数据</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="filteredCodes.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon :size="64" color="#D6D3D1"><Key /></el-icon>
            </div>
            <h3 class="empty-title">暂无激活码</h3>
            <p class="empty-desc">点击上方按钮生成新的激活码</p>
          </div>
        </GlassPanel>
      </div>

      <!-- 使用说明 -->
      <div class="info-section">
        <GlassPanel :blur="15" :opacity="0.9" :border-radius="20">
          <div class="info-header">
            <el-icon :size="20" color="#F59E0B"><Warning /></el-icon>
            <span>使用说明</span>
          </div>
          <ul class="info-list">
            <li>激活码用于新成员注册账号，每个激活码只能使用一次</li>
            <li>未使用的激活码可以撤销，已使用的激活码无法撤销</li>
            <li>请妥善保管激活码，避免泄露给非组织成员</li>
            <li>建议定期清理已使用的激活码，保持列表整洁</li>
          </ul>
        </GlassPanel>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
.activation-code-page {
  min-height: calc(100vh - 64px);
  background: linear-gradient(180deg, #FFFBF7 0%, #FFF5F0 100%);
  padding-bottom: 40px;
}

/* Hero区域 */
.hero-section {
  position: relative;
  padding: 50px 24px 30px;
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
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #FF6B4A, #FB7185);
  top: -80px;
  right: -80px;
  animation: float-orb 7s ease-in-out infinite;
}

.orb-2 {
  width: 280px;
  height: 280px;
  background: linear-gradient(135deg, #F59E0B, #FFD93D);
  bottom: -40px;
  left: -40px;
  animation: float-orb 7s ease-in-out infinite -3.5s;
}

@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(15px, -15px) scale(1.05); }
}

.floating-keys {
  position: absolute;
  inset: 0;
}

.key-icon {
  position: absolute;
  animation: float-key 5s ease-in-out infinite;
}

.key-1 {
  top: 25%;
  left: 8%;
  animation-delay: 0s;
}

.key-2 {
  top: 65%;
  right: 12%;
  animation-delay: -1.5s;
}

.key-3 {
  bottom: 25%;
  left: 15%;
  animation-delay: -3s;
}

@keyframes float-key {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-12px) rotate(8deg); }
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
  padding: 8px 16px;
  background: rgba(255, 107, 74, 0.1);
  border: 1px solid rgba(255, 107, 74, 0.2);
  border-radius: 20px;
  color: #FF6B4A;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 38px;
  font-weight: 700;
  color: #1C1917;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, #FF6B4A 0%, #F59E0B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 15px;
  color: #78716C;
}

/* 统计区域 */
.stats-section {
  max-width: 1200px;
  margin: 0 auto 24px;
  padding: 0 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-icon-bg {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card.total .stat-icon-bg {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
}

.stat-card.unused .stat-icon-bg {
  background: linear-gradient(135deg, #10B981, #059669);
}

.stat-card.used .stat-icon-bg {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
}

.stat-card.rate .stat-icon-bg {
  background: linear-gradient(135deg, #F59E0B, #D97706);
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: #A8A29E;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1C1917;
}

.stat-trend {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 107, 74, 0.1);
  color: #FF6B4A;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.stat-badge.available {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.stat-badge.used-badge {
  background: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
}

.progress-ring {
  width: 50px;
  height: 50px;
}

.progress-ring svg {
  transform: rotate(-90deg);
}

.progress-ring-bg {
  fill: none;
  stroke: #E7E5E4;
  stroke-width: 3;
}

.progress-ring-fill {
  fill: none;
  stroke: #F59E0B;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

/* 操作栏 */
.action-bar {
  max-width: 1200px;
  margin: 0 auto 20px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.generate-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.35);
}

.generate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 74, 0.45);
}

.generate-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-box {
  position: relative;
  width: 240px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #A8A29E;
}

.search-input {
  width: 100%;
  padding: 10px 16px 10px 42px;
  border: 1px solid #E7E5E4;
  border-radius: 10px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #FF8A70;
  box-shadow: 0 0 0 3px rgba(255, 138, 112, 0.15);
}

.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #F5F5F4;
  border-radius: 10px;
}

.filter-tab {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #78716C;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-tab.active {
  background: white;
  color: #FF6B4A;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 激活码列表 */
.codes-section {
  max-width: 1200px;
  margin: 0 auto 24px;
  padding: 0 24px;
}

.table-container {
  overflow-x: auto;
}

.codes-table {
  width: 100%;
  border-collapse: collapse;
}

.codes-table th {
  text-align: left;
  padding: 16px;
  font-size: 13px;
  font-weight: 600;
  color: #78716C;
  border-bottom: 1px solid #E7E5E4;
  white-space: nowrap;
}

.codes-table td {
  padding: 16px;
  border-bottom: 1px solid #F5F5F4;
  font-size: 14px;
}

.codes-table tr:hover td {
  background: #FAFAF9;
}

.codes-table tr.used {
  opacity: 0.7;
}

.codes-table tr.used .code-text {
  text-decoration: line-through;
  color: #A8A29E;
}

.col-status {
  width: 100px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.unused {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.status-badge.used {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
}

.col-code {
  min-width: 200px;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-text {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 15px;
  font-weight: 600;
  color: #1C1917;
  letter-spacing: 0.5px;
}

.copy-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: #F5F5F4;
  color: #78716C;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: all 0.3s ease;
}

tr:hover .copy-btn {
  opacity: 1;
}

.copy-btn:hover {
  background: #FF6B4A;
  color: white;
}

.user-name {
  font-weight: 500;
  color: #44403C;
}

.empty-text {
  color: #A8A29E;
}

.time-text {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #78716C;
  font-size: 13px;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn.revoke {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
}

.action-btn.revoke:hover {
  background: #EF4444;
  color: white;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 24px;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1C1917;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #A8A29E;
}

/* 信息区域 */
.info-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1C1917;
  margin-bottom: 16px;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list li {
  position: relative;
  padding-left: 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #57534E;
  line-height: 1.6;
}

.info-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  background: #F59E0B;
  border-radius: 50%;
}

.info-list li:last-child {
  margin-bottom: 0;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-group {
    flex-direction: column;
  }
  
  .search-box {
    width: 100%;
  }
  
  .filter-tabs {
    width: 100%;
    justify-content: center;
  }
  
  .codes-table {
    font-size: 13px;
  }
  
  .codes-table th,
  .codes-table td {
    padding: 12px 8px;
  }
  
  .col-created,
  .col-time {
    display: none;
  }
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 28px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .col-user {
    display: none;
  }
}
</style>
