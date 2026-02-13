<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api/user'
import { ElMessage } from 'element-plus'
import {
  User,
  Edit,
  Message,
  Phone,
  Location,
  Calendar,
  Check,
  Medal,
  Star,
  Trophy,
  Clock,
  Document,
  Setting,
  Lock
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const user = computed(() => userStore.userInfo)

const editDialogVisible = ref(false)
const saving = ref(false)
const editForm = ref({
  nickname: user.value?.name || '',
  email: user.value?.email || '',
  phone: user.value?.phone || ''
})

const handleEdit = () => {
  editForm.value = {
    nickname: user.value?.name || '',
    email: user.value?.email || '',
    phone: user.value?.phone || ''
  }
  editDialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    const updateData: Record<string, string> = {}
    if (editForm.value.nickname && editForm.value.nickname !== user.value?.name) {
      updateData.name = editForm.value.nickname
    }
    if (editForm.value.email && editForm.value.email !== user.value?.email) {
      updateData.email = editForm.value.email
    }
    if (editForm.value.phone && editForm.value.phone !== user.value?.phone) {
      updateData.phone = editForm.value.phone
    }
    
    if (Object.keys(updateData).length === 0) {
      ElMessage.info('没有需要更新的内容')
      editDialogVisible.value = false
      return
    }
    
    const response = await updateProfile(updateData)
    if (response.code === 200) {
      await userStore.fetchUserInfo()
      ElMessage.success('保存成功')
      editDialogVisible.value = false
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleChangePassword = () => {
  ElMessage.info('功能开发中')
}

// 获取头像背景色
const getAvatarColor = (name: string) => {
  const colors = ['#FF6B4A', '#F59E0B', '#10B981', '#3B82F6', '#8B5CF6', '#EC4899']
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

// 快捷操作
const quickActions = [
  { icon: Document, label: '我的资料', color: '#FF6B4A' },
  { icon: Clock, label: '历史记录', color: '#F59E0B' },
  { icon: Setting, label: '账号设置', color: '#3B82F6' },
  { icon: Lock, label: '隐私安全', color: '#10B981' }
]

// 成就徽章
const achievements = [
  { icon: Star, name: '活跃成员', desc: '连续登录30天', color: '#F59E0B' },
  { icon: Trophy, name: '活动策划', desc: '成功策划5场活动', color: '#FF6B4A' },
  { icon: Medal, name: '优秀部员', desc: '获得年度优秀称号', color: '#8B5CF6' },
  { icon: Star, name: '资深成员', desc: '加入超过1年', color: '#10B981' }
]
</script>

<template>
  <Layout>
    <div class="profile-page">
      <!-- 头部背景 -->
      <div class="profile-header">
        <div class="header-background">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="header-pattern">
            <svg viewBox="0 0 400 200" class="pattern-svg">
              <defs>
                <linearGradient id="headerGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#FF6B4A;stop-opacity:0.2" />
                  <stop offset="100%" style="stop-color:#F59E0B;stop-opacity:0.2" />
                </linearGradient>
              </defs>
              <circle cx="50" cy="50" r="30" fill="url(#headerGradient)" opacity="0.5">
                <animate attributeName="cy" values="50;60;50" dur="4s" repeatCount="indefinite" />
              </circle>
              <circle cx="350" cy="100" r="40" fill="url(#headerGradient)" opacity="0.3">
                <animate attributeName="cy" values="100;90;100" dur="5s" repeatCount="indefinite" />
              </circle>
            </svg>
          </div>
        </div>

        <!-- 用户信息卡片 -->
        <div class="user-card">
          <div class="avatar-section">
            <div class="avatar-container">
              <div 
                class="user-avatar"
                :style="{ background: getAvatarColor(user?.name || user?.username || 'U') }"
              >
                <span class="avatar-text">
                  {{ (user?.name || user?.username || 'U').charAt(0).toUpperCase() }}
                </span>
              </div>
              <div class="avatar-ring ring-1"></div>
              <div class="avatar-ring ring-2"></div>
              <div class="avatar-badge">
                <el-icon :size="14" color="white"><Check /></el-icon>
              </div>
            </div>
            
            <div class="user-titles">
              <h1 class="user-name">{{ user?.name || user?.username }}</h1>
              <p class="user-role">{{ user?.roleHistory?.includes('部长') ? '管理员' : '普通成员' }}</p>
              <div class="user-badges">
                <span class="badge verified">
                  <el-icon :size="12"><Lock /></el-icon>
                  已认证
                </span>
                <span class="badge member">
                  <el-icon :size="12"><Medal /></el-icon>
                  正式成员
                </span>
              </div>
            </div>
          </div>

          <button class="edit-btn" @click="handleEdit">
            <el-icon :size="16"><Edit /></el-icon>
            <span>编辑资料</span>
          </button>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="profile-content">
        <div class="content-grid">
          <!-- 左侧：基本信息 -->
          <div class="left-column">
            <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
              <div class="info-section">
                <!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
                <h2 class="section-title" style="color: #1C1917;">
                  <el-icon><User /></el-icon>
                  <span>基本信息</span>
                </h2>
                
                <div class="info-list">
                  <div class="info-item">
                    <div class="info-icon" style="background: rgba(255, 107, 74, 0.1); color: #FF6B4A;">
                      <el-icon :size="18"><User /></el-icon>
                    </div>
                    <div class="info-content">
                      <span class="info-label">用户名</span>
                      <span class="info-value">{{ user?.username }}</span>
                    </div>
                  </div>

                  <div class="info-item">
                    <div class="info-icon" style="background: rgba(245, 158, 11, 0.1); color: #F59E0B;">
                      <el-icon :size="18"><Message /></el-icon>
                    </div>
                    <div class="info-content">
                      <span class="info-label">邮箱</span>
                      <span class="info-value">{{ user?.email || '未设置' }}</span>
                    </div>
                  </div>

                  <div class="info-item">
                    <div class="info-icon" style="background: rgba(16, 185, 129, 0.1); color: #10B981;">
                      <el-icon :size="18"><Phone /></el-icon>
                    </div>
                    <div class="info-content">
                      <span class="info-label">手机号</span>
                      <span class="info-value">{{ user?.phone || '未设置' }}</span>
                    </div>
                  </div>

                  <div class="info-item">
                    <div class="info-icon" style="background: rgba(59, 130, 246, 0.1); color: #3B82F6;">
                      <el-icon :size="18"><Calendar /></el-icon>
                    </div>
                    <div class="info-content">
                      <span class="info-label">注册时间</span>
                      <span class="info-value">{{ user?.createTime ? new Date(user.createTime).toLocaleDateString() : '-' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </GlassPanel>

            <!-- 快捷操作 -->
            <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
              <div class="quick-actions">
                <!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
                <h2 class="section-title" style="color: #1C1917;">
                  <el-icon><Setting /></el-icon>
                  <span>快捷操作</span>
                </h2>
                
                <div class="actions-grid">
                  <button
                    v-for="action in quickActions"
                    :key="action.label"
                    class="action-card"
                    :style="{ '--action-color': action.color }"
                  >
                    <div class="action-icon" :style="{ background: action.color }">
                      <el-icon :size="20" color="white"><component :is="action.icon" /></el-icon>
                    </div>
                    <span class="action-label">{{ action.label }}</span>
                  </button>
                </div>
              </div>
            </GlassPanel>
          </div>

          <!-- 右侧：成就与统计 -->
          <div class="right-column">
            <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
              <div class="achievements-section">
                <!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
                <h2 class="section-title" style="color: #1C1917;">
                  <el-icon><Trophy /></el-icon>
                  <span>我的成就</span>
                </h2>
                
                <div class="achievements-grid">
                  <div
                    v-for="achievement in achievements"
                    :key="achievement.name"
                    class="achievement-card"
                    :style="{ '--achievement-color': achievement.color }"
                  >
                    <div class="achievement-icon" :style="{ background: achievement.color }">
                      <el-icon :size="24" color="white"><component :is="achievement.icon" /></el-icon>
                    </div>
                    <div class="achievement-info">
                      <span class="achievement-name">{{ achievement.name }}</span>
                      <span class="achievement-desc">{{ achievement.desc }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </GlassPanel>

            <!-- 安全设置 -->
            <GlassPanel :blur="20" :opacity="0.95" :border-radius="24">
              <div class="security-section">
                <!-- Inline CSS Fix: Prevent FOUC - Section title should be black (#1C1917), not orange -->
                <h2 class="section-title" style="color: #1C1917;">
                  <el-icon><Lock /></el-icon>
                  <span>安全设置</span>
                </h2>
                
                <div class="security-list">
                  <div class="security-item">
                    <div class="security-info">
                      <span class="security-title">修改密码</span>
                      <span class="security-desc">定期更换密码可以保护账号安全</span>
                    </div>
                    <button class="security-btn" @click="handleChangePassword">
                      修改
                    </button>
                  </div>
                  
                  <div class="security-item">
                    <div class="security-info">
                      <span class="security-title">绑定手机</span>
                      <span class="security-desc">{{ user?.phone ? '已绑定' : '未绑定' }}</span>
                    </div>
                    <button class="security-btn">
                      {{ user?.phone ? '更换' : '绑定' }}
                    </button>
                  </div>
                </div>
              </div>
            </GlassPanel>
          </div>
        </div>
      </div>

      <!-- 编辑对话框 -->
      <el-dialog
        v-model="editDialogVisible"
        title="编辑资料"
        width="500px"
        class="edit-dialog"
      >
        <div class="edit-form">
          <div class="form-item">
            <label>昵称</label>
            <input
              v-model="editForm.nickname"
              type="text"
              placeholder="请输入昵称"
            >
          </div>
          <div class="form-item">
            <label>邮箱</label>
            <input
              v-model="editForm.email"
              type="email"
              placeholder="请输入邮箱"
            >
          </div>
          <div class="form-item">
            <label>手机号</label>
            <input
              v-model="editForm.phone"
              type="tel"
              placeholder="请输入手机号"
            >
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button class="btn-secondary" @click="editDialogVisible = false">取消</button>
            <button class="btn-primary" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 64px);
  background: linear-gradient(180deg, #FFFBF7 0%, #FFF5F0 100%);
  padding-bottom: 40px;
}

/* 头部区域 */
.profile-header {
  position: relative;
  padding-bottom: 60px;
}

.header-background {
  position: relative;
  height: 280px;
  background: linear-gradient(135deg, #FFEDE6 0%, #FFF5F0 50%, #FFFBF7 100%);
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #FF6B4A, #FB7185);
  top: -100px;
  right: 10%;
  animation: float-orb 6s ease-in-out infinite;
}

.orb-2 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #F59E0B, #FFD93D);
  bottom: -50px;
  left: 10%;
  animation: float-orb 6s ease-in-out infinite -3s;
}

@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(15px, -15px) scale(1.05); }
}

.header-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.6;
}

.pattern-svg {
  width: 100%;
  height: 100%;
}

/* 用户卡片 */
.user-card {
  position: relative;
  max-width: 1200px;
  margin: -80px auto 0;
  padding: 0 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.avatar-section {
  display: flex;
  align-items: flex-end;
  gap: 24px;
}

.avatar-container {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  border: 4px solid white;
}

.avatar-text {
  font-size: 56px;
  font-weight: 700;
  color: white;
}

.avatar-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px dashed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.ring-1 {
  width: 160px;
  height: 160px;
  border-color: rgba(255, 107, 74, 0.3);
  animation: rotate 20s linear infinite;
}

.ring-2 {
  width: 180px;
  height: 180px;
  border-color: rgba(245, 158, 11, 0.2);
  animation: rotate 25s linear infinite reverse;
}

@keyframes rotate {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.avatar-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
  border: 3px solid white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.user-titles {
  padding-bottom: 16px;
}

.user-name {
  font-size: 32px;
  font-weight: 700;
  color: #1C1917;
  margin: 0 0 8px;
}

.user-role {
  font-size: 16px;
  color: #78716C;
  margin: 0 0 12px;
}

.user-badges {
  display: flex;
  gap: 8px;
}

.badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.badge.verified {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.badge.member {
  background: rgba(255, 107, 74, 0.1);
  color: #FF6B4A;
}

.edit-btn {
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
  margin-bottom: 16px;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 74, 0.45);
}

/* 内容区域 */
.profile-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 通用标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1C1917;
  margin: 0 0 20px;
}

.section-title .el-icon {
  color: #FF6B4A;
}

/* 信息列表 */
.info-section {
  padding: 8px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #FAFAF9;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.info-item:hover {
  background: #F5F5F4;
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
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #A8A29E;
}

.info-value {
  font-size: 15px;
  font-weight: 500;
  color: #1C1917;
}

/* 快捷操作 */
.quick-actions {
  padding: 8px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: #FAFAF9;
  border: 1px solid transparent;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.action-card:hover {
  background: white;
  border-color: var(--action-color);
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-label {
  font-size: 14px;
  font-weight: 500;
  color: #44403C;
}

/* 成就 */
.achievements-section {
  padding: 8px;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.achievement-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #FAFAF9;
  border-radius: 16px;
  border: 1px solid transparent;
  transition: all 0.3s ease;
}

.achievement-card:hover {
  background: white;
  border-color: var(--achievement-color);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
}

.achievement-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.achievement-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.achievement-name {
  font-size: 14px;
  font-weight: 600;
  color: #1C1917;
}

.achievement-desc {
  font-size: 12px;
  color: #A8A29E;
}

/* 安全设置 */
.security-section {
  padding: 8px;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #FAFAF9;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.security-item:hover {
  background: #F5F5F4;
}

.security-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-title {
  font-size: 15px;
  font-weight: 600;
  color: #1C1917;
}

.security-desc {
  font-size: 13px;
  color: #A8A29E;
}

.security-btn {
  padding: 8px 16px;
  background: white;
  border: 1px solid #E7E5E4;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #57534E;
  cursor: pointer;
  transition: all 0.3s ease;
}

.security-btn:hover {
  border-color: #FF8A70;
  color: #FF6B4A;
}

/* 编辑对话框 */
.edit-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

.edit-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 0;
  margin: 0;
}

.edit-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: #1C1917;
}

.edit-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #44403C;
}

.form-item input {
  padding: 12px 16px;
  border: 1px solid #E7E5E4;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.form-item input:focus {
  outline: none;
  border-color: #FF8A70;
  box-shadow: 0 0 0 3px rgba(255, 138, 112, 0.15);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

.btn-secondary {
  padding: 10px 20px;
  background: #F5F5F4;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #57534E;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #E7E5E4;
}

.btn-primary {
  padding: 10px 20px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.3);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 74, 0.4);
}

/* 响应式 */
@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .user-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .avatar-section {
    flex-direction: column;
    align-items: center;
  }
  
  .user-badges {
    justify-content: center;
  }
  
  .edit-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .header-background {
    height: 200px;
  }
  
  .avatar-container {
    width: 100px;
    height: 100px;
  }
  
  .avatar-text {
    font-size: 40px;
  }
  
  .ring-1 {
    width: 115px;
    height: 115px;
  }
  
  .ring-2 {
    width: 130px;
    height: 130px;
  }
  
  .user-name {
    font-size: 24px;
  }
  
  .actions-grid,
  .achievements-grid {
    grid-template-columns: 1fr;
  }
}
</style>
